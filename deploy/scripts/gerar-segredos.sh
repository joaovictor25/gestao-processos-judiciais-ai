#!/usr/bin/env bash
# Cria os arquivos de segredo usados pelo compose.yaml (Docker secrets -> /run/secrets).
# Executar NA VPS, uma unica vez. Arquivos existentes nunca sao sobrescritos.
# Uso: bash deploy/scripts/gerar-segredos.sh [diretorio]   (padrao: ./secrets)
#
# Permissoes: diretorio 700 (somente o dono do deploy e o root atravessam) e arquivos 444, pois os
# conteineres leem com usuarios proprios (MySQL uid 999, back-end uid 10001).
set -Eeuo pipefail
source "$(dirname "${BASH_SOURCE[0]}")/lib.sh"

DIR="${1:-$RAIZ/secrets}"
umask 077
mkdir -p "$DIR"
chmod 700 "$DIR"

aleatorio() { head -c "$1" /dev/urandom | base64 | tr -d '\n'; }
senha() { head -c 64 /dev/urandom | base64 | tr -dc 'A-Za-z0-9' | head -c "$1"; }

escrever() {
  local nome="$1" valor="$2" arquivo="$DIR/$1"
  if [[ -e "$arquivo" ]]; then log "mantido: $nome"; return; fi
  printf '%s' "$valor" > "$arquivo"
  chmod 444 "$arquivo"
  log "criado: $nome"
}

perguntar() {
  local nome="$1" descricao="$2" valor=""
  if [[ -e "$DIR/$nome" ]]; then log "mantido: $nome"; return; fi
  if [[ -t 0 ]]; then
    # Leitura sem eco: o valor nao aparece na tela nem no historico do shell.
    read -rsp "$descricao (Enter para deixar vazio): " valor; echo >&2
  fi
  escrever "$nome" "$valor"
  [[ -n "$valor" ]] || log "ATENCAO: $nome vazio. Preencha antes do deploy (ver docs/deploy-producao.md)."
  valor=""
}

# JWT: Base64 de 48 bytes (o back-end exige >= 32 bytes apos decodificar).
escrever jwt_secret "$(aleatorio 48)"
# Senhas do MySQL: usadas pelo conteiner somente na PRIMEIRA inicializacao do volume.
escrever mysql_password "$(senha 40)"
escrever mysql_root_password "$(senha 40)"
perguntar datajud_api_key "Chave publica da API DataJud (obrigatoria)"
perguntar gemini_api_key "Chave da API Gemini (opcional; vazia desativa a IA)"

ls -l "$DIR" >&2
