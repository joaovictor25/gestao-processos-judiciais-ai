#!/usr/bin/env python3
"""Confere a publicacao de portas com os dados EFETIVOS dos conteineres (docker inspect).

Uso:
  docker inspect $(docker compose -p <projeto> ps -aq) | python3 deploy/scripts/verificar-portas.py \
      --projeto <projeto> [--esperado-proxy 127.0.0.1:18080->80/tcp,127.0.0.1:18443->443/tcp]

Regras:
- Todos os servicos esperados (proxy, frontend, backend, mysql) existem no projeto.
- Nenhum servico alem do proxy publica portas em QUALQUER interface do host: HostConfig.PortBindings e
  NetworkSettings.Ports sem HostPort, PublishAllPorts falso e NetworkMode diferente de "host"
  (no modo host todas as portas do conteiner ficariam no host sem aparecer como binding).
- O proxy publica somente 80/tcp, 443/tcp e 443/udp; com --esperado-proxy, exatamente a lista informada.
Registra os bindings encontrados de cada conteiner. Nenhuma variavel de ambiente e exibida.
"""
import argparse
import json
import sys

SERVICOS = ("proxy", "frontend", "backend", "mysql")
PORTAS_PROXY = {"80/tcp", "443/tcp", "443/udp"}


def bindings(mapa):
    """{'80/tcp': [{'HostIp': '127.0.0.1', 'HostPort': '18080'}], '8080/tcp': None} -> lista de publicacoes."""
    saida = []
    for porta, lista in (mapa or {}).items():
        for b in lista or []:
            if b and str(b.get("HostPort") or "") not in ("", "0"):
                ip = b.get("HostIp") or "0.0.0.0"  # vazio = todas as interfaces
                ip = f"[{ip}]" if ":" in ip else ip
                saida.append(f"{ip}:{b['HostPort']}->{porta}")
    return sorted(set(saida))


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--projeto", required=True)
    parser.add_argument("--esperado-proxy", default="")
    args = parser.parse_args()
    esperado_proxy = sorted(filter(None, args.esperado_proxy.split(",")))

    try:
        conteineres = json.load(sys.stdin)
    except json.JSONDecodeError:
        print("  FALHA saida do docker inspect ausente ou invalida")
        return 1
    falhas = []

    def checar(condicao, mensagem):
        print(("  OK    " if condicao else "  FALHA ") + mensagem)
        if not condicao:
            falhas.append(mensagem)

    por_servico = {}
    for c in conteineres:
        rotulos = (c.get("Config") or {}).get("Labels") or {}
        if rotulos.get("com.docker.compose.project") != args.projeto:
            continue
        por_servico.setdefault(rotulos.get("com.docker.compose.service"), []).append(c)

    for servico in SERVICOS:
        lista = por_servico.get(servico, [])
        checar(len(lista) == 1, f"{servico}: um conteiner no projeto {args.projeto} (encontrados {len(lista)})")
        for c in lista:
            host = c.get("HostConfig") or {}
            rede = c.get("NetworkSettings") or {}
            configurados = bindings(host.get("PortBindings"))
            efetivos = bindings(rede.get("Ports"))
            expostas = sorted((c.get("Config") or {}).get("ExposedPorts") or {})
            modo = host.get("NetworkMode") or ""
            print(f"        {servico}: estado={(c.get('State') or {}).get('Status')} rede={modo} "
                  f"expostas={expostas or '-'} PortBindings={configurados or '-'} Ports={efetivos or '-'}")
            checar(modo != "host", f"{servico}: nao usa a rede do host")
            checar(not host.get("PublishAllPorts"), f"{servico}: PublishAllPorts desativado")
            if servico == "proxy":
                portas = {b.split("->", 1)[1] for b in configurados + efetivos}
                checar(bool(portas) and portas <= PORTAS_PROXY, f"proxy publica somente 80/tcp, 443/tcp e 443/udp ({sorted(portas)})")
                if esperado_proxy:
                    checar(efetivos == esperado_proxy, f"proxy com bindings efetivos {efetivos} == esperado {esperado_proxy}")
            else:
                checar(not configurados and not efetivos,
                       f"{servico}: nenhuma porta publicada em nenhuma interface do host")

    print(f"{len(falhas)} falha(s)")
    return 1 if falhas else 0


if __name__ == "__main__":
    sys.exit(main())
