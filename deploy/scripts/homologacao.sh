#!/usr/bin/env bash
# Homologacao automatizada da stack Docker do ProSint com credenciais ficticias e banco descartavel.
# Projeto Compose isolado (prosint-homolog): nunca toca volumes, redes ou conteineres de producao.
# O back-end roda sem rede de saida: nenhuma API externa real e consumida.
# Uso: deploy/scripts/homologacao.sh            (MANTER=1 preserva a stack ao final para inspecao)
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
  "$RAIZ/deploy/scripts/verificar-rede.sh" "$PREFIXO" "$PROSINT_PROJECT"
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
for servico in backend:8080 mysql:3306 frontend:8080; do
  verificar "${servico%%:*} sem porta publicada no host" test -z "$(dc port "${servico%%:*}" "${servico##*:}" 2>/dev/null)"
done
host_sem_portas() { ! (exec 3<>/dev/tcp/127.0.0.1/3306) 2>/dev/null && ! curl -s --max-time 3 -o /dev/null http://127.0.0.1:8080/; }
mysql_sem_saida() { ! dc exec -T mysql timeout 5 bash -c 'exec 3<>/dev/tcp/1.1.1.1/80' 2>/dev/null; }
backend_sem_saida() { ! dc exec -T backend curl -s --max-time 5 -o /dev/null http://1.1.1.1/; }
backend_alcanca_mysql() { dc exec -T backend timeout 5 bash -c 'exec 3<>/dev/tcp/mysql/3306'; }
verificar "nada escutando em 127.0.0.1:8080 e :3306 do host" host_sem_portas
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
backup="$("$RAIZ/deploy/scripts/backup-mysql.sh")"
checksum_ok() { (cd "$(dirname "$backup")" && sha256sum -c "$(basename "$backup").sha256" >/dev/null); }
verificar "backup gerado com checksum" checksum_ok
verificar "backup restauravel e consistente" "$RAIZ/deploy/scripts/testar-restauracao.sh" "$backup"

echo
if (( falhas > 0 )); then echo "Homologacao: $falhas verificacao(oes) falharam." >&2; exit 1; fi
echo "Homologacao concluida sem falhas."
