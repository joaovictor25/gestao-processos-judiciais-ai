# Implantação do ProSint em produção (Hostinger VPS + Docker Compose)

Guia operacional para implantar, atualizar, fazer backup e reverter o ProSint em `https://prosint.tech`.
Nenhum valor secreto aparece neste documento. Todos os comandos partem do diretório do repositório na VPS
(`/opt/prosint` nos exemplos).

## 1. Arquitetura

```
Internet ──80/443──> proxy (Caddy 2.11, HTTPS automático)
                       │ rede "edge" (interna, 172.31.250.0/24; proxy = 172.31.250.2)
                       ├── /            → frontend (Nginx sem root, React compilado)
                       └── /api/*       → backend (Spring Boot 4.1.1, Java 25, perfil prod)
                                            │ rede "dados" (interna)    │ rede "saida"
                                            └── mysql (8.4 LTS)         └── DataJud, DJEN, Gemini
```

| Serviço | Imagem | Portas no host | Limites (CPU / RAM) |
|---|---|---|---|
| proxy | `caddy:2.11.6-alpine` | 80, 443/tcp, 443/udp | 0,5 / 256 MiB |
| frontend | `prosint-frontend` (Node 24 → Nginx 1.30) | nenhuma | 0,25 / 128 MiB |
| backend | `prosint-backend` (Temurin 25 JRE) | nenhuma | 1,5 / 1536 MiB (heap ≈ 70%) |
| mysql | `mysql:8.4.11` | nenhuma | 1,0 / 2048 MiB (buffer pool 768 MiB) |

O total fica em cerca de 4 GiB dos 7,7 GiB da VPS e deixa folga para o sistema, o cache de disco e os builds.

**Rotas expostas pelo proxy.** Somente `/api/*` chega ao back-end, com o caminho preservado.
O proxy responde 404 para endpoints do Spring Boot fora de `/api`:

- `/estatisticas/calcular`: legado, sem uso pelo front-end;
- `/h2`: existe só no perfil `dev`;
- `/error`;
- `/actuator`: não está instalado.

Qualquer outro caminho vai para o React. O Nginx devolve `index.html` para as rotas do React Router
(`/login`, `/processos/10`, ...) e 404 para assets inexistentes.

**Cache.** `/assets/*` (arquivos com hash) usa `max-age=31536000, immutable`. O HTML usa `no-cache`,
para que um novo deploy seja carregado imediatamente.

## 2. Pré-requisitos na VPS

- Ubuntu 26.04 LTS com Docker Engine e o plugin **Docker Compose v2.24 ou superior**
  (`docker compose version`). O arquivo de homologação usa `!override`.
- Firewall: liberar somente SSH, 80/tcp, 443/tcp e 443/udp. Exemplo com UFW:
  ```bash
  sudo ufw allow OpenSSH && sudo ufw allow 80/tcp && sudo ufw allow 443/tcp && sudo ufw allow 443/udp
  sudo ufw enable
  ```
  Nenhum outro serviço publica portas, então as regras que o Docker cria fora do UFW afetam apenas o proxy.
- Git, `curl`, `gzip` e `sha256sum` (já presentes no Ubuntu).

## 3. DNS

No painel DNS do domínio, crie o registro abaixo e aguarde a propagação. Use um TTL baixo (300) na primeira implantação.

| Tipo | Nome | Valor |
|---|---|---|
| A | `@` | IPv4 público da VPS |

- **AAAA (IPv6): não crie por enquanto.** Sem IPv6 habilitado no Docker, as conexões IPv6 passam pelo
  `docker-proxy` e chegam ao back-end com o IP do gateway. Assim, todos os clientes IPv6 dividiriam o mesmo
  limite de login. Crie o AAAA somente depois de habilitar IPv6 no daemon do Docker.
- **www (opcional):** crie `A www → IP da VPS` e descomente o bloco `www.` em `deploy/caddy/Caddyfile`.
- Confira antes de subir o proxy:
  ```bash
  dig +short prosint.tech A          # deve retornar o IP da VPS
  curl -4 -s ifconfig.me; echo       # IP público da própria VPS, para comparar
  ```

O Caddy obtém e renova o certificado automaticamente, pelo desafio HTTP-01 na porta 80 ou TLS-ALPN na 443,
assim que o DNS aponta para a VPS. Os certificados ficam no volume `caddy_data`. **Não apague esse volume**:
reemitir certificados em excesso esbarra nos limites do Let's Encrypt.

## 4. Segredos

| Arquivo em `./secrets/` | Uso | Origem |
|---|---|---|
| `jwt_secret` | assinatura dos tokens (Base64, 48 bytes) | gerado pelo script |
| `mysql_password` | usuário da aplicação no MySQL | gerado pelo script |
| `mysql_root_password` | root do MySQL (backup/restauração) | gerado pelo script |
| `datajud_api_key` | chave pública da API DataJud (**obrigatória**) | digitada no script |
| `gemini_api_key` | chave do Gemini (vazia desativa a IA) | digitada no script |

**Como os segredos chegam à aplicação.** O Compose monta cada arquivo como Docker secret em `/run/secrets/<NOME>`.
O perfil `prod` já importa `optional:configtree:/run/secrets/`, então o nome do arquivo vira o nome da propriedade
(`JWT_SECRET`, `MYSQL_PASSWORD`, `DATAJUD_API_KEY`, `GEMINI_API_KEY`) e preenche os placeholders
`${...}` do `application-prod.properties`. A quebra de linha final é removida.
O `RequiredEnvironmentInitializer` recusa a inicialização se faltar algum segredo obrigatório, e a mensagem
de erro mostra apenas o nome, nunca o valor. O teste `SegredosConfigTreeTests` cobre esse comportamento.
O MySQL lê as próprias senhas por `MYSQL_PASSWORD_FILE` e `MYSQL_ROOT_PASSWORD_FILE`.

**Regras:**
- `secrets/`, `.env`, `backups/` e `.homolog/` estão no `.gitignore`. Nunca faça commit desses arquivos.
- Permissões: diretório `700` (só o dono do deploy e o root entram) e arquivos `444`, porque os contêineres
  leem com os próprios usuários (MySQL uid 999, back-end uid 10001).
- Guarde uma cópia de `secrets/` em um cofre de senhas, fora da VPS.
- As senhas do MySQL valem apenas na **primeira** inicialização do volume. Regenerar `mysql_password` depois
  quebra o login do back-end. Para trocar, veja a seção 11.

## 5. Primeira implantação (sequência exata)

```bash
# 1. Código
sudo mkdir -p /opt/prosint && sudo chown "$USER": /opt/prosint
git clone https://github.com/joaovictor25/gestao-processos-judiciais-ai.git /opt/prosint
# Repositório privado: use uma deploy key somente leitura ou um token de acesso com escopo mínimo.
cd /opt/prosint
git checkout <tag-ou-commit-aprovado>

# 2. Configuração não secreta
cp .env.example .env
nano .env                         # ACME_EMAIL real; PROSINT_VERSION=<hash curto do commit>

# 3. Segredos (pede as chaves DataJud e Gemini sem ecoar na tela)
bash deploy/scripts/gerar-segredos.sh
ls -l secrets/                    # 5 arquivos, -r--r--r--; datajud_api_key com tamanho > 0

# 4. Homologação na própria VPS (projeto isolado prosint-homolog, portas 18080/18443 só em loopback,
#    credenciais fictícias, sem acesso à internet pelo back-end). Remove tudo ao final.
bash deploy/scripts/homologacao.sh

# 5. Validação do Compose de produção
docker compose config -q

# 6. Build e subida (aguarda MySQL → Flyway V1 → validação Hibernate → healthchecks)
docker compose build --pull
docker compose up -d --wait --wait-timeout 420
docker compose ps                 # 4 serviços "healthy"; só o proxy com 0.0.0.0:80/443

# 7. Verificação externa
curl -sI https://prosint.tech | head -n 5                 # HTTP/2 200 + strict-transport-security
curl -s -o /dev/null -w '%{http_code} %{redirect_url}\n' http://prosint.tech/   # 308 https://prosint.tech/
curl -s https://prosint.tech/api/auth/me                  # {"status":401,...}
docker compose exec -T mysql sh -c 'MYSQL_PWD="$(cat /run/secrets/mysql_root_password)" mysql -uroot -N -e \
  "select version, success from $MYSQL_DATABASE.flyway_schema_history"'   # 1  1

# 8. Backup diário (seção 8) e primeiro backup manual
bash deploy/scripts/backup-mysql.sh
```

Se quiser testar a emissão de certificado sem consumir limites, faça uma rodada com
`ACME_CA=https://acme-staging-v02.api.letsencrypt.org/directory` no `.env`. Depois comente a linha, rode
`docker compose down proxy && docker volume rm prosint_caddy_data` (apaga apenas os certificados de staging)
e execute `docker compose up -d --wait`.

## 6. Logs e operação

```bash
docker compose ps
docker compose logs -f --tail 200 backend       # aplicação (sem segredos, SQL ou parâmetros)
docker compose logs -f --tail 200 proxy         # acesso em JSON e emissão/renovação de certificados
docker compose logs --tail 200 mysql
docker stats --no-stream                        # consumo frente aos limites
docker compose restart backend                  # reinício com shutdown gracioso (até 30 s)
```

Os logs usam o driver `json-file` com rotação: 10 MiB × 5 arquivos por serviço.

## 7. Atualização

```bash
cd /opt/prosint
bash deploy/scripts/backup-mysql.sh                         # sempre antes de atualizar
git fetch --tags && git checkout <nova-tag-ou-commit>
sed -i "s/^PROSINT_VERSION=.*/PROSINT_VERSION=$(git rev-parse --short HEAD)/" .env
docker compose build --pull
docker compose up -d --wait --wait-timeout 420              # recria somente o que mudou
docker compose ps && curl -sI https://prosint.tech | head -n 1
```

Novas migrações Flyway (`V2__...`) são aplicadas automaticamente na subida do back-end, antes da validação
do Hibernate. Se a migração ou a validação falhar, o novo back-end fica `unhealthy`. O `up --wait` retorna erro e a
API fica indisponível, enquanto o front-end continua no ar. Nesse caso, siga a seção 9 imediatamente.

## 8. Backup

**O que é salvo.** `deploy/scripts/backup-mysql.sh` gera um `mysqldump --single-transaction`, consistente e sem
parar a aplicação, com `CREATE DATABASE` e o histórico do Flyway. O resultado é compactado, testado
(gzip + rodapé `Dump completed`) e acompanhado de `.sha256`.
Arquivo gerado: `backups/prosint-AAAAMMDDTHHMMSSZ.sql.gz`.

**Agendamento** (crontab do usuário do deploy, horário UTC):
```cron
15 3 * * * cd /opt/prosint && BACKUP_RETENCAO_DIAS=14 bash deploy/scripts/backup-mysql.sh >> /opt/prosint/backups/backup.log 2>&1
```

**Retenção.** São mantidos 14 dias na VPS (`BACKUP_RETENCAO_DIAS`). Fora da VPS, recomenda-se 30 cópias
diárias e 12 mensais.

**Cópia fora da VPS (obrigatória para recuperar desastre).** Defina `BACKUP_COPIA_CMD`, que recebe o caminho
do arquivo em `$1`. Exemplo com `rclone` e um remoto criptografado (`rclone config`, tipo `crypt` sobre
S3, Backblaze B2 ou Google Drive):
```cron
15 3 * * * cd /opt/prosint && BACKUP_COPIA_CMD='rclone copy "$1" prosint-crypt:diario && rclone copy "$1.sha256" prosint-crypt:diario' bash deploy/scripts/backup-mysql.sh >> /opt/prosint/backups/backup.log 2>&1
```
Configure a retenção remota pela política de ciclo de vida do bucket. Guarde `secrets/` e o `.env` em um
cofre de senhas: eles não fazem parte do dump.

**Teste de restauração (mensal ou após mudanças de schema).** O script abaixo importa o backup em um MySQL
descartável, sem rede e removido ao final, e confere as tabelas e o Flyway. Ele não toca na produção.
```bash
bash deploy/scripts/testar-restauracao.sh backups/prosint-AAAAMMDDTHHMMSSZ.sql.gz
```

## 9. Rollback

**Somente código, sem migração nova.** As imagens anteriores continuam no Docker local com a tag antiga:
```bash
git checkout <tag-anterior>
sed -i "s/^PROSINT_VERSION=.*/PROSINT_VERSION=<hash-anterior>/" .env
docker compose up -d --wait --wait-timeout 420     # sem build: reutiliza prosint-*:<hash-anterior>
```

**Com migração aplicada.** O Flyway só avança e a versão antiga não valida um schema mais novo. Restaure o
backup feito antes da atualização e volte o código como acima:
```bash
bash deploy/scripts/restaurar-mysql.sh backups/prosint-<antes-da-atualizacao>.sql.gz --confirmar-substituicao
```

O `restaurar-mysql.sh` executa, nesta ordem:
1. confere o checksum;
2. gera um backup de segurança do estado atual;
3. para o back-end;
4. recria o banco a partir do dump;
5. sobe o back-end, que revalida o schema com Flyway e Hibernate.

## 10. Persistência e riscos

- Volumes nomeados: `prosint_mysql_data` (banco), `prosint_caddy_data` (certificados) e `prosint_caddy_config`.
- `docker compose down`, `stop`, `restart`, `up --build` e o reinício da VPS **preservam** os dados.
  A homologação comprova isso derrubando e subindo a stack.
- **Destrutivos, nunca use em produção:**
  - `docker compose down -v` apaga o banco e os certificados;
  - `docker volume rm prosint_mysql_data`;
  - `docker system prune --volumes`.

  A homologação usa `down -v` somente no projeto `prosint-homolog`, e o script se recusa a rodar com o nome `prosint`.
- O projeto se chama sempre `prosint` (campo `name:` do `compose.yaml`), seja qual for o diretório.

## 11. Segurança

**Exposição.**
- Somente o proxy publica portas.
- Back-end e MySQL não têm portas no host, e o MySQL e a rede `edge` não têm saída para a internet.
- Todos os contêineres rodam com `no-new-privileges`. Proxy, frontend e back-end rodam com sistema de arquivos
  somente leitura e capabilities removidas.
- Frontend (Nginx) e back-end rodam como usuário não root.

**IP real do cliente e falsificação de cabeçalhos.**
- O Caddy não confia em proxies à sua frente. Ele descarta `X-Forwarded-*` vindos do cliente e envia o IP real
  da conexão, `X-Forwarded-Proto: https` e `X-Forwarded-Host`. Também remove `Forwarded` e `X-Real-IP`.
- O back-end usa `server.forward-headers-strategy=native` (RemoteIpValve do Tomcat) e só aceita esses cabeçalhos
  quando a conexão vem de `TRUSTED_PROXY_REGEX`, ou seja, do IP fixo do Caddy, `172.31.250.2`.
- O `X-Forwarded-For` é lido da direita para a esquerda, e o cabeçalho `Forwarded` (RFC 7239) é ignorado.
  Um cliente não consegue falsificar o IP usado pelo limitador de login e cadastro.
- Testes: `ProxyConfiavelIntegrationTests`, `ProxyNaoConfiavelIntegrationTests` e a etapa 8 da homologação.
- Se um CDN for colocado à frente, configure `trusted_proxies` no Caddyfile com as faixas do CDN.

**Cabeçalhos HTTP.** HSTS (1 ano), `nosniff`, `X-Frame-Options: DENY`, `Referrer-Policy`, `Permissions-Policy`
e sem `Server`.

**Troca de segredos.** Com o arquivo `444`, use `chmod 600` antes de editar e volte para `444` depois:
- `jwt_secret`: grave o novo valor e rode `docker compose up -d --force-recreate backend`. Todas as sessões são encerradas.
- `datajud_api_key` / `gemini_api_key`: mesmo procedimento.
  ```bash
  read -rs VALOR && chmod 600 secrets/gemini_api_key && printf '%s' "$VALOR" > secrets/gemini_api_key && chmod 444 secrets/gemini_api_key; unset VALOR
  ```
- `mysql_password`: altere primeiro no banco:
  ```bash
  docker compose exec mysql sh -c 'MYSQL_PWD="$(cat /run/secrets/mysql_root_password)" mysql -uroot -e "ALTER USER '\''prosint'\''@'\''%'\'' IDENTIFIED BY '\''<nova>'\''"'
  ```
  Depois grave o mesmo valor no arquivo e recrie o back-end.

## 12. Homologação

`deploy/scripts/homologacao.sh` roda com credenciais fictícias e um banco descartável no projeto
`prosint-homolog`, e não consome APIs externas reais. O mesmo script roda no GitHub Actions
(`.github/workflows/docker-homologacao.yml`). Ele verifica:

1. Sintaxe do Compose (produção e homologação), Caddyfile para `prosint.tech` e `localhost`, e portas publicadas só no proxy.
2. Build das duas imagens. O build do front-end falha se o bundle tiver `localhost:<porta>`.
3. Subida com healthchecks e Flyway: somente `V1`, com sucesso.
4. Rotas React, cache de assets e HTML, redirecionamento HTTP → HTTPS, cabeçalhos, `/api` preservado e
   endpoints internos bloqueados.
5. Cadastro, login e processo; persistência após `down` + `up`; Flyway sem reaplicação.
6. Back-end e MySQL inacessíveis pelo host e sem saída para a internet.
7. `X-Forwarded-For` e `Forwarded` falsificados não contornam o limite de login (429).
8. Backup com checksum e restauração testada em um MySQL descartável.

Para inspecionar a stack depois do teste: `MANTER=1 bash deploy/scripts/homologacao.sh` e abra
`https://localhost:18443` (certificado da CA interna do Caddy). Para remover depois:
```bash
docker compose -p prosint-homolog -f compose.yaml -f compose.homolog.yaml --env-file deploy/homolog/homolog.env down -v
```

## 13. Problemas comuns

| Sintoma | Causa provável / ação |
|---|---|
| Proxy sem certificado, `acme: error` nos logs | DNS ainda não aponta para a VPS ou a porta 80 está bloqueada. Confira `dig` e o firewall do painel da Hostinger. |
| `backend` unhealthy logo após subir | `docker compose logs backend`: segredo obrigatório ausente, falha do Flyway ou da validação Hibernate. |
| `Access denied for user 'prosint'` | `mysql_password` alterado depois da criação do volume (seção 11). |
| 429 em login para todos os usuários | Clientes chegando com o mesmo IP: há um proxy ou CDN à frente sem `trusted_proxies`, ou IPv6 sem suporte no Docker (seção 3). |
| Página antiga após deploy | O HTML é `no-cache`; force a atualização do navegador. Se persistir, confira se o frontend foi recriado (`docker compose ps`). |
