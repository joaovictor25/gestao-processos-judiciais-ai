#!/usr/bin/env python3
"""Confere o plano de enderecos da rede edge na configuracao RESOLVIDA do Compose.

Uso: docker compose ... config --format json | python3 deploy/scripts/verificar-enderecos.py [--homologacao]

Regras: proxy, frontend e backend com IP fixo, unico, dentro da sub-rede e FORA de ip_range (faixa
dinamica), sem usar gateway/rede/broadcast; TRUSTED_PROXY_REGEX aceita somente o IP do proxy; MySQL
apenas em redes internas; na homologacao o back-end nao participa da rede de saida.
"""
import ipaddress
import json
import re
import sys

FIXOS = ("proxy", "frontend", "backend")


def main() -> int:
    homologacao = "--homologacao" in sys.argv[1:]
    config = json.load(sys.stdin)
    servicos, redes = config["services"], config["networks"]
    falhas = []

    def checar(condicao, mensagem):
        print(("  OK    " if condicao else "  FALHA ") + mensagem)
        if not condicao:
            falhas.append(mensagem)

    ipam = (redes["edge"].get("ipam") or {}).get("config") or [{}]
    sub = ipaddress.ip_network(ipam[0]["subnet"])
    faixa = ipaddress.ip_network(ipam[0]["ip_range"]) if ipam[0].get("ip_range") else None
    gateway = ipaddress.ip_address(ipam[0]["gateway"]) if ipam[0].get("gateway") else None
    checar(faixa is not None and faixa.subnet_of(sub), f"edge {sub} com ip_range {faixa} para enderecos dinamicos")
    checar(gateway is not None and gateway in sub, f"gateway {gateway} explicito")

    enderecos = {}
    for nome in FIXOS:
        rede = (servicos[nome].get("networks") or {}).get("edge") or {}
        valor = rede.get("ipv4_address")
        checar(valor is not None, f"{nome} com IP fixo na edge ({valor})")
        if valor is None:
            continue
        ip = ipaddress.ip_address(valor)
        enderecos[nome] = ip
        checar(ip in sub and ip not in (sub.network_address, sub.broadcast_address, gateway),
               f"{nome} {ip} valido dentro de {sub}")
        checar(faixa is None or ip not in faixa, f"{nome} {ip} fora da faixa dinamica {faixa}")
    checar(len(set(enderecos.values())) == len(enderecos), f"IPs fixos distintos: {sorted(map(str, enderecos.values()))}")

    regex = servicos["backend"]["environment"]["TRUSTED_PROXY_REGEX"]
    if "proxy" in enderecos:
        checar(re.fullmatch(regex, str(enderecos["proxy"])) is not None, f"TRUSTED_PROXY_REGEX '{regex}' aceita o proxy")
    outros = [ip for nome, ip in enderecos.items() if nome != "proxy"]
    outros += [sub.network_address + 1, sub.network_address + 5, sub.network_address + 20, sub.network_address + 200]
    outros += [x for x in (gateway, faixa.network_address if faixa else None) if x is not None]
    for ip in outros:
        checar(re.fullmatch(regex, str(ip)) is None, f"TRUSTED_PROXY_REGEX rejeita {ip}")

    checar(redes["edge"].get("internal") is True and redes["dados"].get("internal") is True, "edge e dados internas")
    checar(set(servicos["mysql"].get("networks") or {}) == {"dados"}, "MySQL somente na rede dados")
    redes_backend = set(servicos["backend"].get("networks") or {})
    if homologacao:
        checar(redes_backend == {"edge", "dados"}, f"homologacao: back-end sem rede de saida ({sorted(redes_backend)})")
    else:
        checar(redes_backend == {"edge", "dados", "saida"}, f"producao: back-end com saida para APIs ({sorted(redes_backend)})")
    publicadores = sorted(n for n, s in servicos.items() if s.get("ports"))
    checar(publicadores == ["proxy"], f"somente o proxy publica portas ({publicadores})")

    print(f"{len(falhas)} falha(s)")
    return 1 if falhas else 0


if __name__ == "__main__":
    sys.exit(main())
