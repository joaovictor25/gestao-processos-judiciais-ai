#!/usr/bin/env bash
# Prova que um backup e restauravel SEM tocar na producao: importa o arquivo em um MySQL
# descartavel (sem rede, senha aleatoria, removido ao final) e confere o schema e o Flyway.
# Uso: bash deploy/scripts/testar-restauracao.sh backups/prosint-AAAAMMDDTHHMMSSZ.sql.gz
#   EXIGIR_DADOS=1 tambem exige ao menos um usuario e um processo no backup (homologacao).
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
pronto=0
for _ in $(seq 1 90); do
  if docker exec "$nome" mysqladmin ping -h 127.0.0.1 --silent >/dev/null 2>&1; then pronto=1; break; fi
  sleep 2
done
(( pronto )) || { log "ERRO: MySQL descartavel nao ficou pronto em 180 s"; exit 1; }
senha="$(docker logs "$nome" 2>&1 | sed -n 's/.*GENERATED ROOT PASSWORD: \(.*\)$/\1/p' | tail -n 1)"
[[ -n "$senha" ]] || { log "ERRO: senha do MySQL descartavel nao encontrada"; exit 1; }
# MYSQL_PWD repassado pelo ambiente do cliente Docker ("-e MYSQL_PWD" sem valor): a senha nao aparece
# na linha de comando do host (ps) nem em logs.
export MYSQL_PWD="$senha"
importar() { docker exec -i -e MYSQL_PWD "$nome" mysql -uroot; }
consulta() { docker exec -e MYSQL_PWD "$nome" mysql -uroot -N -B -e "$1"; }

log "Importando $arquivo"
gzip -dc "$arquivo" | importar

tabelas="$(consulta "select count(*) from information_schema.tables where table_schema='$banco'")"
flyway="$(consulta "select group_concat(concat(version,':',success) order by installed_rank) from \`$banco\`.flyway_schema_history")"
usuarios="$(consulta "select count(*) from \`$banco\`.usuarios")"
processos="$(consulta "select count(*) from \`$banco\`.processos")"
unset MYSQL_PWD senha
log "Tabelas=$tabelas Flyway=$flyway Usuarios=$usuarios Processos=$processos"

# V1 cria 9 tabelas; o Flyway acrescenta flyway_schema_history. Migracoes futuras ajustam o minimo.
[[ "$tabelas" =~ ^[0-9]+$ && "$tabelas" -ge 10 ]] || { log "ERRO: tabelas ausentes"; exit 1; }
[[ "$flyway" == 1:1* ]] || { log "ERRO: historico Flyway invalido"; exit 1; }
[[ "$flyway" != *":0"* ]] || { log "ERRO: migracao com falha registrada"; exit 1; }
if [[ "${EXIGIR_DADOS:-0}" == "1" ]]; then
  [[ "$usuarios" -ge 1 && "$processos" -ge 1 ]] || { log "ERRO: backup sem os dados esperados"; exit 1; }
fi
log "Restauracao verificada com sucesso"
