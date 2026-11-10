#!/usr/bin/env bash
# Prova que um backup e restauravel SEM tocar na producao: importa o arquivo em um MySQL
# descartavel (sem rede, senha aleatoria, removido ao final) e confere o schema e o Flyway.
# Uso: deploy/scripts/testar-restauracao.sh backups/prosint-AAAAMMDDTHHMMSSZ.sql.gz
set -Eeuo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

arquivo="${1:?Uso: $0 <backup.sql.gz>}"
imagem="${MYSQL_IMAGE:-mysql:8.4.11}"
banco="${MYSQL_DATABASE:-prosint}"
nome="prosint-teste-restauracao-$$"
gzip -t "$arquivo"

trap 'docker rm -f "$nome" >/dev/null 2>&1 || true' EXIT
log "Iniciando MySQL descartavel ($imagem)"
docker run -d --name "$nome" --network none \
  -e MYSQL_RANDOM_ROOT_PASSWORD=yes "$imagem" >/dev/null

# TCP em 127.0.0.1 so responde depois da inicializacao completa (o servidor temporario usa apenas
# socket). A senha root aleatoria do conteiner descartavel e lida do proprio log de inicializacao.
for _ in $(seq 1 90); do
  if docker exec "$nome" mysqladmin ping -h 127.0.0.1 --silent >/dev/null 2>&1; then break; fi
  sleep 2
done
senha="$(docker logs "$nome" 2>&1 | sed -n 's/.*GENERATED ROOT PASSWORD: \(.*\)$/\1/p' | tail -n 1)"
[[ -n "$senha" ]] || { log "ERRO: MySQL descartavel nao iniciou"; exit 1; }
cliente() { docker exec -i -e MYSQL_PWD="$senha" "$nome" mysql -uroot -N -B "$@"; }

log "Importando $arquivo"
gzip -dc "$arquivo" | cliente

tabelas="$(cliente -e "select count(*) from information_schema.tables where table_schema='$banco'")"
flyway="$(cliente -e "select group_concat(concat(version,':',success) order by installed_rank) from \`$banco\`.flyway_schema_history")"
usuarios="$(cliente -e "select count(*) from \`$banco\`.usuarios")"
processos="$(cliente -e "select count(*) from \`$banco\`.processos")"
log "Tabelas=$tabelas Flyway=$flyway Usuarios=$usuarios Processos=$processos"

# V1 cria 9 tabelas; o Flyway acrescenta flyway_schema_history. Migracoes futuras ajustam o minimo.
[[ "$tabelas" -ge 10 ]] || { log "ERRO: tabelas ausentes"; exit 1; }
[[ "$flyway" == 1:1* ]] || { log "ERRO: historico Flyway invalido"; exit 1; }
[[ "$flyway" != *":0"* ]] || { log "ERRO: migracao com falha registrada"; exit 1; }
log "Restauracao verificada com sucesso"
