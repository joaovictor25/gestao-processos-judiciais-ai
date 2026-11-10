#!/usr/bin/env bash
# Pre-verificacao: a /24 da rede edge (PROSINT_EDGE_PREFIX.0/24) nao pode sobrepor outra rede Docker
# nem uma rota IPv4 do host; caso contrario o Docker recusa a rede ("Pool overlaps with other one on
# this address space") ou o trafego e roteado para o lugar errado.
# Uso: bash deploy/scripts/verificar-rede.sh [prefixo] [projeto]
#   prefixo: tres octetos, padrao 172.31.250 (producao) | projeto: redes ignoradas (padrao prosint)
set -Eeuo pipefail

prefixo="${1:-${PROSINT_EDGE_PREFIX:-172.31.250}}"
projeto="${2:-${PROSINT_PROJECT:-prosint}}"
[[ "$prefixo" =~ ^([0-9]{1,3})\.([0-9]{1,3})\.([0-9]{1,3})$ ]] || { echo "Prefixo invalido: $prefixo" >&2; exit 2; }
alvo="$prefixo.0/24"

ip_int() { local IFS=.; read -r a b c d <<< "$1"; echo $(( (a << 24) | (b << 16) | (c << 8) | d )); }
sobrepoe() { # sobrepoe A/n B/m -> verdadeiro se as faixas se cruzam
  local ia="${1%/*}" na="${1#*/}" ib="${2%/*}" nb="${2#*/}"
  [[ "$1" == */* ]] || na=32
  [[ "$2" == */* ]] || nb=32
  local n=$(( na < nb ? na : nb )) mascara
  mascara=$(( n == 0 ? 0 : (0xFFFFFFFF << (32 - n)) & 0xFFFFFFFF ))
  (( ( $(ip_int "$ia") & mascara ) == ( $(ip_int "$ib") & mascara ) ))
}

conflitos=0
# Falha fechada: sem acesso ao Docker nao ha como afirmar que a sub-rede esta livre.
ids="$(docker network ls -q)" || { echo "ERRO: nao foi possivel listar as redes Docker" >&2; exit 1; }
inventario=""
if [[ -n "$ids" ]]; then
  # shellcheck disable=SC2086
  inventario="$(docker network inspect --format '{{.Name}} {{range .IPAM.Config}}{{.Subnet}} {{end}}' $ids)" \
    || { echo "ERRO: nao foi possivel inspecionar as redes Docker" >&2; exit 1; }
fi
# Redes Docker existentes, exceto as do proprio projeto (que serao reutilizadas/recriadas).
while read -r nome subredes; do
  [[ -z "$nome" || "$nome" == "${projeto}_"* ]] && continue
  for sub in $subredes; do
    [[ "$sub" == *:* ]] && continue # IPv6
    if sobrepoe "$alvo" "$sub"; then echo "CONFLITO: rede Docker '$nome' usa $sub" >&2; conflitos=$((conflitos + 1)); fi
  done
done <<< "$inventario"

# Rotas do host (VPN, rede privada do provedor). Ignora pontes do proprio projeto e a rota padrao.
if command -v ip >/dev/null 2>&1; then
  pontes_proprias=" $(docker network ls -q --filter "name=^${projeto}_" | cut -c1-12 | sed 's/^/br-/' | tr '\n' ' ') "
  while read -r destino _ dispositivo _; do
    [[ "$destino" == default || "$pontes_proprias" == *" $dispositivo "* ]] && continue
    if sobrepoe "$alvo" "$destino"; then echo "CONFLITO: rota do host $destino (dev $dispositivo)" >&2; conflitos=$((conflitos + 1)); fi
  done < <(ip -4 route show | awk '{for (i = 1; i <= NF; i++) if ($i == "dev") print $1, "dev", $(i + 1), "x"}')
fi

if (( conflitos > 0 )); then
  echo "A sub-rede $alvo colide com $conflitos rede(s). Defina outro PROSINT_EDGE_PREFIX livre (ex.: 172.31.252)." >&2
  exit 1
fi
echo "Sub-rede $alvo livre (projeto $projeto)." >&2
