#!/usr/bin/env bash
# Backup logico consistente do MySQL (mysqldump --single-transaction, sem parar a aplicacao).
# Saida: $BACKUP_DIR/prosint-AAAAMMDDTHHMMSSZ.sql.gz + .sha256. Remove backups locais mais antigos
# que $BACKUP_RETENCAO_DIAS. A copia para fora da VPS e feita por BACKUP_COPIA_CMD (opcional).
# Uso (cron diario, ver docs/deploy-producao.md):
#   bash deploy/scripts/backup-mysql.sh
set -Eeuo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

DESTINO="${BACKUP_DIR:-$RAIZ/backups}"
RETENCAO_DIAS="${BACKUP_RETENCAO_DIAS:-14}"
umask 077
mkdir -p "$DESTINO"
chmod 700 "$DESTINO"

carimbo="$(date -u +%Y%m%dT%H%M%SZ)"
arquivo="$DESTINO/prosint-$carimbo.sql.gz"
parcial="$arquivo.parcial"
trap 'rm -f "$parcial"' EXIT

log "Gerando backup em $arquivo"
# --databases inclui CREATE DATABASE/USE: a restauracao recria o banco completo, incluindo flyway_schema_history.
mysql_root sh -c 'exec mysqldump -uroot --single-transaction --quick --routines --triggers --events \
  --hex-blob --set-gtid-purged=OFF --no-tablespaces --databases "$MYSQL_DATABASE"' | gzip -9 > "$parcial"

# Integridade: gzip valido e rodape gravado pelo mysqldump somente ao concluir.
gzip -t "$parcial"
gzip -dc "$parcial" | tail -n 1 | grep -q '^-- Dump completed' || { log "ERRO: dump incompleto"; exit 1; }
mv "$parcial" "$arquivo"
( cd "$DESTINO" && sha256sum "$(basename "$arquivo")" > "$(basename "$arquivo").sha256" )
log "Backup concluido: $(du -h "$arquivo" | cut -f1)"

if [[ -n "${BACKUP_COPIA_CMD:-}" ]]; then
  # Exemplo: BACKUP_COPIA_CMD='rclone copy "$1" remoto:prosint-backups && rclone copy "$1.sha256" remoto:prosint-backups'
  log "Copiando para fora da VPS"
  # Saida para stderr: o stdout deste script e somente o caminho do backup (lido por outros scripts).
  bash -c "$BACKUP_COPIA_CMD" copia "$arquivo" >&2
fi

find "$DESTINO" -maxdepth 1 -name 'prosint-*.sql.gz*' -type f -mtime +"$RETENCAO_DIAS" -print -delete >&2
echo "$arquivo"
