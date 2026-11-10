#!/usr/bin/env bash
# Restaura um backup gerado por backup-mysql.sh SUBSTITUINDO o banco atual.
# Antes de restaurar: valida o checksum, gera um backup de seguranca do estado atual e para o back-end.
# Uso: bash deploy/scripts/restaurar-mysql.sh backups/prosint-AAAAMMDDTHHMMSSZ.sql.gz --confirmar-substituicao
set -Eeuo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

arquivo="${1:-}"
if [[ -z "$arquivo" || "${2:-}" != "--confirmar-substituicao" ]]; then
  echo "Uso: $0 <backup.sql.gz> --confirmar-substituicao" >&2
  exit 2
fi
[[ -f "$arquivo" ]] || { log "Arquivo inexistente: $arquivo"; exit 2; }
if [[ -f "$arquivo.sha256" ]]; then
  ( cd "$(dirname "$arquivo")" && sha256sum -c "$(basename "$arquivo").sha256" ) >&2
else
  log "AVISO: sem arquivo .sha256; validando apenas o gzip"
fi
gzip -t "$arquivo"

log "Backup de seguranca do estado atual"
bash "$(dirname "${BASH_SOURCE[0]}")/backup-mysql.sh" >&2

log "Parando o back-end para impedir gravacoes durante a restauracao"
dc stop backend

log "Recriando o banco a partir de $arquivo"
mysql_root sh -c 'exec mysql -uroot -e "DROP DATABASE IF EXISTS \`$MYSQL_DATABASE\`"'
gzip -dc "$arquivo" | mysql_root mysql -uroot

log "Iniciando o back-end (Flyway valida o historico; Hibernate valida o schema)"
dc up -d --wait --wait-timeout 300 backend
log "Restauracao concluida"
