#!/usr/bin/env bash
# Homologacao automatizada da stack Docker do ProSint com credenciais ficticias e banco descartavel.
# Projeto Compose isolado (prosint-homolog): nunca toca volumes, redes ou conteineres de producao.
# O back-end roda sem rede de saida: nenhuma API externa real e consumida.
# Uso: bash deploy/scripts/homologacao.sh            (MANTER=1 preserva a stack ao final para inspecao)
set -Eeuo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

export PROSINT_PROJECT=prosint-homolog
export PROSINT_COMPOSE_FILES=compose.yaml:compose.homolog.yaml
export PROSINT_ENV_FILE="$RAIZ/deploy/homolog/homolog.env"
export BACKUP_DIR="$RAIZ/.homolog/backups"
[[ "$PROSINT_PROJECT" != "prosint" ]] || { echo "Recusado: projeto de producao" >&2; exit 2; }

HTTPS="https://localhost:18443"
HTTP="http://localhost:18080"
SEGREDOS="$RAIZ/.homolog/secrets"
LOG_FALHA="$RAIZ/.homolog/stack.log"
DIAGNOSTICO_REDE="$RAIZ/.homolog/rede.txt"
# Mesmo valor do homolog.env; a producao usa 172.31.250.
PREFIXO="$(sed -n 's/^PROSINT_EDGE_PREFIX=//p' "$PROSINT_ENV_FILE")"
REDE_EDGE="${PROSINT_PROJECT}_edge"
falhas=0
ok() { printf '  OK    %s\n' "$*"; }
erro() { printf '  FALHA %s\n' "$*"; falhas=$((falhas + 1)); }
verificar() { local descricao="$1"; shift; if "$@"; then ok "$descricao"; else erro "$descricao"; fi; }
status() { curl -sk -o /dev/null -w '%{http_code}' "$@"; }

# Estado das redes para diagnosticar falhas de inicializacao (ex.: "Address already in use").
diagnosticar_rede() {
  {
    echo "== docker compose ps -a"; dc ps -a
    echo; echo "== redes Docker (nome, driver, sub-redes)"
    docker network ls -q | xargs -r docker network inspect \
      --format '{{.Name}} {{.Driver}} internal={{.Internal}} {{range .IPAM.Config}}{{.Subnet}} range={{.IPRange}} gw={{.Gateway}} {{end}}'
    local rede
    for rede in $(docker network ls --filter "name=^${PROSINT_PROJECT}_" --format '{{.Name}}'); do
      echo; echo "== $rede: conteineres e enderecos"
      docker network inspect "$rede" --format '{{range .Containers}}{{.Name}} {{.IPv4Address}}{{"\n"}}{{end}}'
    done
    echo; echo "== enderecos por conteiner do projeto"
    dc ps -aq | xargs -r docker inspect \
      --format '{{.Name}} {{.State.Status}} {{range $r, $c := .NetworkSettings.Networks}}{{$r}}={{$c.IPAddress}} {{end}}'
    # Somente portas: o docker inspect completo nao e gravado (contem variaveis de ambiente).
    echo; echo "== bindings de portas (HostConfig.PortBindings | NetworkSettings.Ports)"
    dc ps -aq | xargs -r docker inspect \
      --format '{{.Name}} {{json .HostConfig.PortBindings}} | {{json .NetworkSettings.Ports}}'
    echo; echo "== sockets TCP em escuta no host"; ss -Hltn 2>/dev/null || true
    echo; echo "== rotas IPv4 do host"; ip -4 route show 2>/dev/null || true
  } > "$DIAGNOSTICO_REDE" 2>&1 || true
  echo "Diagnostico de rede: $DIAGNOSTICO_REDE" >&2
}

limpar() {
  local codigo=$?
  if (( codigo != 0 || falhas > 0 )); then
    dc logs --no-color > "$LOG_FALHA" 2>&1 || true; echo "Logs: $LOG_FALHA" >&2
    diagnosticar_rede
  fi
  if [[ "${MANTER:-0}" != "1" ]]; then
    log "Removendo a stack de homologacao e seus volumes descartaveis"
    dc down -v --remove-orphans >/dev/null 2>&1 || true
  fi
}
trap limpar EXIT

log "1. Segredos ficticios (novos a cada execucao)"
rm -rf "$RAIZ/.homolog"
mkdir -p "$SEGREDOS" && chmod 700 "$SEGREDOS"
aleatorio() { head -c "$1" /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c 40; }
head -c 48 /dev/urandom | base64 | tr -d '\n' > "$SEGREDOS/jwt_secret"
aleatorio 64 > "$SEGREDOS/mysql_password"
aleatorio 64 > "$SEGREDOS/mysql_root_password"
printf 'chave-ficticia-homologacao' > "$SEGREDOS/datajud_api_key"
: > "$SEGREDOS/gemini_api_key" # vazio: IA desativada
chmod 444 "$SEGREDOS"/*

log "2. Sintaxe e consistencia do Compose"
verificar "compose.yaml de producao valido" env ACME_EMAIL=ci@example.invalid \
  docker compose --project-directory "$RAIZ" -f "$RAIZ/compose.yaml" --env-file /dev/null config -q
verificar "compose.yaml + compose.homolog.yaml validos" dc config -q
caddy_valido() {
  docker run --rm -e PROSINT_DOMAIN="$1" -e ACME_EMAIL=ci@example.invalid \
    -v "$RAIZ/deploy/caddy/Caddyfile:/etc/caddy/Caddyfile:ro" caddy:2.11.6-alpine \
    caddy validate --config /etc/caddy/Caddyfile --adapter caddyfile >/dev/null
}
verificar "Caddyfile valido para prosint.tech (producao)" caddy_valido prosint.tech
verificar "Caddyfile valido para localhost (homologacao)" caddy_valido localhost
publicados="$(dc config --format json | grep -o '"published"' | wc -l | tr -d ' ')"
verificar "somente o proxy declara portas publicadas (2)" test "$publicados" -eq 2
plano_producao() {
  env ACME_EMAIL=ci@example.invalid docker compose --project-directory "$RAIZ" -f "$RAIZ/compose.yaml" \
    --env-file /dev/null config --format json | python3 "$RAIZ/deploy/scripts/verificar-enderecos.py"
}
plano_homologacao() { dc config --format json | python3 "$RAIZ/deploy/scripts/verificar-enderecos.py" --homologacao; }
verificar "plano de enderecos da producao (IPs fixos fora da faixa dinamica)" plano_producao
verificar "plano de enderecos da homologacao (overrides preservam IPs fixos)" plano_homologacao
verificar "sub-rede $PREFIXO.0/24 livre (outras redes Docker e rotas do host)" \
  bash "$RAIZ/deploy/scripts/verificar-rede.sh" "$PREFIXO" "$PROSINT_PROJECT"
(( falhas == 0 )) || { echo "Configuracao invalida; build e inicializacao nao executados." >&2; exit 1; }

log "3. Build das imagens"
dc build --pull

# IP efetivo de cada servico na edge e ausencia de enderecos duplicados.
ip_de() { docker inspect -f "{{with index .NetworkSettings.Networks \"$REDE_EDGE\"}}{{.IPAddress}}{{end}}" "$(dc ps -q "$1")"; }
enderecos_efetivos() {
  local esperado servico ip duplicados
  for servico in proxy:2 frontend:3 backend:4; do
    esperado="$PREFIXO.${servico##*:}"
    ip="$(ip_de "${servico%%:*}")"
    [[ "$ip" == "$esperado" ]] || { echo "    ${servico%%:*}: $ip (esperado $esperado)" >&2; return 1; }
  done
  duplicados="$(docker network inspect "$REDE_EDGE" --format '{{range .Containers}}{{.IPv4Address}}{{"\n"}}{{end}}' | sed '/^$/d' | sort | uniq -d)"
  [[ -z "$duplicados" ]] || { echo "    duplicados: $duplicados" >&2; return 1; }
}

log "4. Inicializacao (MySQL -> Flyway -> Hibernate validate -> proxy)"
dc up -d --wait --wait-timeout 420
verificar "IPs fixos efetivos (proxy .2, frontend .3, backend .4) e sem duplicidade" enderecos_efetivos

log "5. Verificacoes"
verificar "Flyway aplicou somente a V1 com sucesso" test \
  "$(mysql_root sh -c 'exec mysql -uroot -N -B -e "select group_concat(concat(version,\":\",success)) from \`$MYSQL_DATABASE\`.flyway_schema_history"')" = "1:1"
sem_url_local() { ! dc exec -T frontend grep -rIoE '(localhost|127\.0\.0\.1|0\.0\.0\.0):[0-9]{2,5}' /usr/share/nginx/html; }
verificar "frontend sem URL local no bundle" sem_url_local

pagina="$(curl -sk "$HTTPS/")"
verificar "GET / devolve o index.html" grep -q 'id="root"' <<< "$pagina"
for rota in /login /dashboard /processos/123 /processos/123/editar /rota-inexistente; do
  verificar "fallback React Router em $rota" test "$(status "$HTTPS$rota")" = 200
done
asset="$(grep -oE '/assets/[^"]+\.js' <<< "$pagina" | head -n 1)"
cabecalho() { curl -skI "$1" | grep -qi "$2"; }
sem_cabecalho() { ! curl -skI "$1" | grep -qi "$2"; }
verificar "asset com cache imutavel ($asset)" cabecalho "$HTTPS$asset" 'cache-control: public, max-age=31536000, immutable'
verificar "index.html sem cache" cabecalho "$HTTPS/" 'cache-control: no-cache'
verificar "asset inexistente devolve 404" test "$(status "$HTTPS/assets/inexistente.js")" = 404
verificar "HSTS presente" cabecalho "$HTTPS/" 'strict-transport-security: max-age='
verificar "X-Content-Type-Options presente" cabecalho "$HTTPS/" 'x-content-type-options: nosniff'
verificar "cabecalho Server removido" sem_cabecalho "$HTTPS/" '^server:'
redirecionamento="$(curl -s -o /dev/null -w '%{http_code} %{redirect_url}' "$HTTP/processos?x=1")"
verificar "HTTP redireciona para HTTPS ($redirecionamento)" grep -qE '^30[18] https://localhost/processos\?x=1$' <<< "$redirecionamento"

api_401() { curl -sk "$HTTPS/api/auth/me" | grep -q '"status":401'; }
verificar "API em /api preserva o caminho (/api/auth/me -> 401 JSON)" api_401
for interno in /estatisticas/calcular /h2 /h2/ /error /actuator/health; do
  verificar "endpoint interno $interno bloqueado no proxy" test "$(status -X POST "$HTTPS$interno")" = 404
done

email="homolog-$RANDOM@example.invalid"
senha="Senha-homolog-$RANDOM"
verificar "cadastro de usuario" test "$(status -H 'Content-Type: application/json' \
  -d "{\"nome\":\"Homologacao\",\"email\":\"$email\",\"senha\":\"$senha\"}" "$HTTPS/api/auth/cadastro")" = 201
entrar() {
  curl -sk -H 'Content-Type: application/json' -d "{\"email\":\"$email\",\"senha\":\"$senha\"}" "$HTTPS/api/auth/login" \
    | sed -n 's/.*"token":"\([^"]*\)".*/\1/p'
}
token="$(entrar)"
verificar "login devolve JWT" test -n "$token"
verificar "cadastro de processo" test "$(status -H "Authorization: Bearer $token" -H 'Content-Type: application/json' \
  -d '{"numeroProcesso":"HOMOLOG-0001","tribunal":"TJPR"}' "$HTTPS/api/processo/cadastrar")" = 200

log "6. Persistencia apos 'docker compose down' (sem -v) e novo 'up' em ordem invertida"
dc down
# Proxy e frontend primeiro (ignorando depends_on): os IPs fixos nao dependem da ordem de inicializacao.
dc up -d --no-deps proxy frontend
dc up -d --wait --wait-timeout 420
verificar "IPs fixos preservados com ordem de inicializacao invertida" enderecos_efetivos
token="$(entrar)"
verificar "login apos reinicio" test -n "$token"
processo_listado() { curl -sk -H "Authorization: Bearer $token" "$HTTPS/api/processo/listar-usuario" | grep -q HOMOLOG-0001; }
verificar "processo persistido apos reinicio" processo_listado
verificar "Flyway nao reaplicou migracoes" test \
  "$(mysql_root sh -c 'exec mysql -uroot -N -B -e "select count(*) from \`$MYSQL_DATABASE\`.flyway_schema_history"')" = "1"

log "7. Isolamento de rede"
# "docker compose port" nao serve para provar ausencia: para porta apenas EXPOSE (8080 do frontend e do
# backend, 3306/33060 do MySQL) ele imprime ":0" com codigo 0. Usa os dados efetivos do docker inspect.
portas_efetivas() {
  local ids
  ids="$(dc ps -aq)" && [[ -n "$ids" ]] || return 1
  # shellcheck disable=SC2086
  docker inspect $ids | python3 "$RAIZ/deploy/scripts/verificar-portas.py" --projeto "$PROSINT_PROJECT" \
    --esperado-proxy "127.0.0.1:18080->80/tcp,127.0.0.1:18443->443/tcp"
}
verificar "bindings efetivos: somente o proxy publica, apenas em 127.0.0.1:18080/18443" portas_efetivas

# Portas internas dos servicos: nao podem existir em NENHUMA interface do host.
PORTAS_INTERNAS="8080 3306 33060"
sem_escuta_no_host() {
  local escutas
  command -v ss >/dev/null || { echo "    ss indisponivel" >&2; return 1; }
  escutas="$(ss -Hltn | awk '{print $4}' | grep -E ':(8080|3306|33060)$' || true)"
  echo "    sockets TCP em escuta em 8080/3306/33060 (qualquer endereco): ${escutas:-nenhum}" >&2
  [[ -z "$escutas" ]]
}
sem_conexao_em_interfaces() {
  local lista endereco porta abertos=""
  command -v ip >/dev/null || { echo "    ip indisponivel" >&2; return 1; }
  # Todos os enderecos IPv4/IPv6 do host (inclui loopback, interface publica e pontes Docker).
  lista="$(ip -o addr show | awk '{print $4}' | cut -d/ -f1 | grep -v '^fe80' || true)"
  [[ -n "$lista" ]] || { echo "    nenhum endereco encontrado" >&2; return 1; }
  for endereco in $lista; do
    for porta in $PORTAS_INTERNAS; do
      if timeout 2 bash -c "exec 3<>/dev/tcp/$endereco/$porta" 2>/dev/null; then abertos+=" $endereco:$porta"; fi
    done
  done
  echo "    enderecos do host testados: $(tr '\n' ' ' <<< "$lista")" >&2
  [[ -z "$abertos" ]] || { echo "    conexao aceita em:$abertos" >&2; return 1; }
}
verificar "nenhum socket do host escutando em 8080/3306/33060" sem_escuta_no_host
verificar "8080/3306/33060 recusadas em todos os enderecos do host" sem_conexao_em_interfaces
mysql_sem_saida() { ! dc exec -T mysql timeout 5 bash -c 'exec 3<>/dev/tcp/1.1.1.1/80' 2>/dev/null; }
backend_sem_saida() { ! dc exec -T backend curl -s --max-time 5 -o /dev/null http://1.1.1.1/; }
backend_alcanca_mysql() { dc exec -T backend timeout 5 bash -c 'exec 3<>/dev/tcp/mysql/3306'; }
verificar "MySQL sem saida para a internet" mysql_sem_saida
verificar "back-end de homologacao sem saida para a internet" backend_sem_saida
verificar "back-end alcanca o MySQL pela rede interna" backend_alcanca_mysql

log "8. Cabecalhos encaminhados: X-Forwarded-For/Forwarded falsificados nao contornam o limitador"
codigos=""
for i in $(seq 1 8); do
  codigos+="$(curl -sk -o /dev/null -w '%{http_code}' -H 'Content-Type: application/json' \
    -H "X-Forwarded-For: 198.51.100.$i" -H "Forwarded: for=198.51.100.$i;proto=http" -H "X-Real-IP: 198.51.100.$i" \
    -d '{"email":"ninguem@example.invalid","senha":"incorreta"}' "$HTTPS/api/auth/login") "
done
verificar "limite por IP aplicado ao IP real apesar dos cabecalhos ($codigos)" grep -q 429 <<< "$codigos"

log "9. Backup e teste de restauracao em banco descartavel"
# Scripts sempre via "bash": a execucao nao depende do bit executavel (checkout no Windows, scp, zip).
if ! backup="$(bash "$RAIZ/deploy/scripts/backup-mysql.sh")" || [[ ! -s "$backup" ]]; then
  erro "backup gerado"; exit 1
fi
ok "backup gerado ($(basename "$backup"))"
checksum_ok() { (cd "$(dirname "$backup")" && sha256sum -c "$(basename "$backup").sha256" >/dev/null); }
verificar "checksum SHA-256 do backup confere" checksum_ok
verificar "backup restauravel e consistente em MySQL descartavel" \
  env EXIGIR_DADOS=1 bash "$RAIZ/deploy/scripts/testar-restauracao.sh" "$backup"

log "10. Restauracao real na stack de homologacao (restaurar-mysql.sh)"
historico_flyway() {
  mysql_root sh -c 'exec mysql -uroot -N -B -e "select group_concat(concat(version,\":\",success)) from \`$MYSQL_DATABASE\`.flyway_schema_history"'
}
criar_processo() {
  test "$(status -H "Authorization: Bearer $token" -H 'Content-Type: application/json' \
    -d "{\"numeroProcesso\":\"$1\",\"tribunal\":\"TJPR\"}" "$HTTPS/api/processo/cadastrar")" = 200
}
listado() { curl -sk -H "Authorization: Bearer $token" "$HTTPS/api/processo/listar-usuario" | grep -q "\"$1\""; }
nao_listado() { ! listado "$1"; }
quantidade_backups() { find "$BACKUP_DIR" -maxdepth 1 -name 'prosint-*.sql.gz' -type f | wc -l; }
verificar "alteracao posterior ao backup gravada (HOMOLOG-POS-BACKUP)" criar_processo HOMOLOG-POS-BACKUP
verificar "alteracao posterior visivel antes da restauracao" listado HOMOLOG-POS-BACKUP
backups_antes="$(quantidade_backups)"
restaurar() { bash "$RAIZ/deploy/scripts/restaurar-mysql.sh" "$backup" --confirmar-substituicao; }
verificar "restaurar-mysql.sh: checksum, backup de seguranca, parada, importacao e subida" restaurar
verificar "backup de seguranca criado antes de substituir o banco" test "$(quantidade_backups)" -gt "$backups_antes"
verificar "dados do backup presentes apos restauracao (HOMOLOG-0001)" listado HOMOLOG-0001
verificar "alteracao posterior ao backup desfeita pela restauracao" nao_listado HOMOLOG-POS-BACKUP
verificar "historico Flyway integro apos restauracao" test "$(historico_flyway)" = "1:1"
verificar "login funciona apos restauracao" test -n "$(entrar)"
verificar "IPs fixos preservados apos reinicio do back-end" enderecos_efetivos

echo
if (( falhas > 0 )); then echo "Homologacao: $falhas verificacao(oes) falharam." >&2; exit 1; fi
echo "Homologacao concluida sem falhas."
