#!/usr/bin/env bash
# Funcoes comuns dos scripts de operacao. Variaveis opcionais:
#   PROSINT_PROJECT        nome do projeto Compose (padrao: o "name" do compose.yaml, prosint)
#   PROSINT_COMPOSE_FILES  arquivos separados por ":" (padrao: compose.yaml)
#   PROSINT_ENV_FILE       arquivo de variaveis (padrao: .env da raiz, lido pelo Compose)
RAIZ="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"

dc() {
  local args=(--project-directory "$RAIZ")
  local arquivo
  IFS=':' read -r -a _arquivos <<< "${PROSINT_COMPOSE_FILES:-compose.yaml}"
  for arquivo in "${_arquivos[@]}"; do args+=(-f "$RAIZ/$arquivo"); done
  [[ -n "${PROSINT_PROJECT:-}" ]] && args+=(-p "$PROSINT_PROJECT")
  [[ -n "${PROSINT_ENV_FILE:-}" ]] && args+=(--env-file "$PROSINT_ENV_FILE")
  docker compose "${args[@]}" "$@"
}

log() { printf '[%s] %s\n' "$(date -u +%H:%M:%S)" "$*" >&2; }

# Executa um comando mysql/mysqldump no conteiner com a senha root lida do secret, sem expor a senha
# na linha de comando do host nem no historico do shell.
mysql_root() {
  dc exec -T mysql sh -c 'MYSQL_PWD="$(cat /run/secrets/mysql_root_password)" exec "$@"' sh "$@"
}
