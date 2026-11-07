# Backend

API em Java 25 e Spring Boot 4.1.1, com Spring Security, JPA e H2. Para baixar o projeto e iniciar os dois serviços, siga primeiro o [guia de instalação na raiz](../README.md).

## Executar

Todos os comandos desta seção são executados **dentro da pasta `backend`**.

Windows — PowerShell:

```powershell
.\mvnw.cmd -version
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```sh
sh ./mvnw -version
sh ./mvnw spring-boot:run
```

O Maven deve utilizar JDK 25 ou superior. O wrapper baixa a distribuição Maven e as dependências necessárias. Aguarde `Started BackendApplication`; a porta padrão é **8080**. Encerre com `Ctrl+C`.

O perfil padrão é `dev`. O banco H2 é criado automaticamente em memória e não exige instalação externa. Cadastro e login funcionam sem chave Gemini. As sínteses de IA exigem `GEMINI_API_KEY`.

## Configuração do ambiente

Configure variáveis **no mesmo terminal que iniciará o backend**. Este projeto não carrega automaticamente arquivos `.env` no Spring Boot.

| Variável | Quando usar | Padrão |
| --- | --- | --- |
| `GEMINI_API_KEY` | Para gerar sínteses de IA | Vazio; IA indisponível até configurar |
| `GEMINI_MODEL` | Para selecionar um modelo compatível disponível na sua conta | `gemini-3.5-flash-lite` |
| `GEMINI_BASE_URL` | Para alterar o endereço do provedor, inclusive em testes locais | `https://generativelanguage.googleapis.com` |
| `JWT_SECRET` | Para manter uma chave de autenticação estável; obrigatória fora de `dev` | Chave aleatória por inicialização no perfil `dev` |
| `SERVER_PORT` | Quando precisar usar outra porta | `8080` |
| `SPRING_PROFILES_ACTIVE` | Para selecionar explicitamente outro perfil | Sem valor, utiliza o perfil padrão `dev` |

Exemplo com Gemini, ainda dentro de `backend`; substitua o texto pela sua chave:

```powershell
# Windows — PowerShell
$env:GEMINI_API_KEY = "COLE_SUA_CHAVE_AQUI"
.\mvnw.cmd spring-boot:run
```

```sh
# Linux/macOS
export GEMINI_API_KEY="COLE_SUA_CHAVE_AQUI"
sh ./mvnw spring-boot:run
```

As variáveis definidas assim valem para esse terminal e seus processos. Reinicie o backend após alterar a configuração. Credenciais não devem ser colocadas no código ou em arquivos versionados.

### Autenticação e dados locais

- Crie a conta pela tela de cadastro do frontend. Não existe conta padrão ou administrador pré-cadastrado.
- As rotas privadas exigem o JWT obtido no login. O frontend envia o token automaticamente.
- `JWT_SECRET` deve ser Base64 válido, com pelo menos 32 bytes após decodificação. No perfil `dev`, omitir a variável gera uma chave temporária e um aviso esperado no log.
- Reiniciar o backend com H2 em memória apaga usuários, processos, estatísticas, fila e cache. Com a chave JWT temporária, também invalida os tokens anteriores.
- O perfil `prod` não configura automaticamente um banco persistente. Ele exige configuração explícita da chave e do datasource.

### Acessar o console H2

Com o backend em execução no perfil `dev`, abra **http://localhost:8080/h2/**. Use:

| Campo | Valor |
| --- | --- |
| Driver Class | `org.h2.Driver` |
| JDBC URL | `jdbc:h2:mem:banco` |
| User Name | `sa` |
| Password | Deixe vazio |

Clique em **Connect**. Se alterou a porta ou o datasource, use os valores correspondentes. `sa` é o usuário do banco; o login da aplicação é a conta criada no frontend. O console só está habilitado no perfil `dev`, com acesso local.

## Usar um MySQL existente

O caminho de instalação inicial é o H2 do guia principal. O perfil opcional `mysql` foi preparado para um **banco com o esquema da aplicação já existente**: ele usa `ddl-auto=validate`, portanto não cria automaticamente as tabelas.

Os scripts abaixo são migrações para esse esquema. Não constituem uma instalação completa de um banco vazio e não são executados automaticamente pelo programa:

1. [Sínteses DataJud](schema/20261103_datajud_sinteses.sql).
2. [Pesquisas e sínteses de comunicações](schema/20261104_comunicacao_sinteses.sql).
3. [Fila e cache DataJud](schema/20261104_datajud_fila.sql).

Antes de atualizar um banco existente, faça backup e confira quais migrações ainda precisam ser aplicadas. Reaplicar scripts que adicionam colunas ou tabelas já existentes pode falhar.

Depois de preparar o esquema, configure no ambiente:

| Variável | Conteúdo |
| --- | --- |
| `SPRING_PROFILES_ACTIVE` | `mysql` |
| `MYSQL_URL` | URL JDBC do banco, por exemplo `jdbc:mysql://localhost:3306/gestao_processos` |
| `MYSQL_USER` | Usuário com acesso ao banco |
| `MYSQL_PASSWORD` | Senha desse usuário |
| `JWT_SECRET` | Chave aleatória em Base64 com pelo menos 32 bytes |
| `GEMINI_API_KEY` | Sua chave, se utilizar sínteses de IA |

Inicie com o mesmo comando `spring-boot:run`. Nesse perfil, o console H2 fica desabilitado. Um erro de validação do esquema precisa ser resolvido no banco; não substitua `validate` por `create` em um banco que contém dados.

## Integrações externas

O backend se conecta ao DataJud/CNJ, ao DJEN e, quando configurado, ao Gemini. O frontend acessa somente a API local; as credenciais de integração permanecem no backend.

A instalação local pode ser validada com cadastro e login sem consultar esses provedores. Consultas e geração de sínteses dependem de internet, disponibilidade, limites do serviço e, no caso de Gemini, chave e modelo disponíveis na sua conta.

## Fila de consultas DataJud

O botão **Gerar estatística** cria uma solicitação no backend. O frontend acompanha o resultado e pode retomar o acompanhamento ao recarregar a página. A execução continua enquanto o backend estiver ativo; com H2 em memória, encerrar o backend perde a fila e seus dados.

Cada tentativa consulta uma resposta de até 400 processos. Há coordenação das chamadas, cache temporário e reagendamento de falhas transitórias, incluindo HTTP 429. O andamento pode indicar espera; cliques repetidos não aceleram a consulta.

As configurações completas estão em [application.properties](src/main/resources/application.properties). Entre as variáveis disponíveis:

| Variável | Padrão | Uso |
| --- | --- | --- |
| `DATAJUD_INTERVALO_MS` | `1500` | Intervalo mínimo entre inícios de consultas |
| `DATAJUD_PAUSA_429_MS` | `30000` | Pausa mínima após limite do provedor |
| `DATAJUD_MAX_TENTATIVAS` | `3` | Limite de tentativas |
| `DATAJUD_PRAZO_GERACAO_MS` | `300000` | Prazo de geração em milissegundos |
| `DATAJUD_CACHE_MS` | `300000` | Duração do cache |
| `DATAJUD_FILA_MAX` | `100` | Limite global da fila |
| `DATAJUD_FILA_POR_USUARIO` | `5` | Limite por usuário |
| `DATAJUD_WORKER_ENABLED` | `true` | Ativa o processamento da fila |

Os padrões são suficientes para iniciar o projeto. Desativar o worker impede o consumo da fila. Para compartilhar a coordenação entre instâncias, todas precisam usar o mesmo banco persistente.

## Testes automatizados

Os testes são uma verificação adicional; não é necessário executá-los para iniciar pelo comando `spring-boot:run`. Dentro de `backend`:

```powershell
# Windows — PowerShell
.\mvnw.cmd -B -ntp test
```

```sh
# Linux/macOS
sh ./mvnw -B -ntp test
```

Os testes utilizam bancos H2 de teste e integrações simuladas. Os relatórios locais ficam em `target/surefire-reports` e são ignorados pelo Git.

**Pendência conhecida nesta versão:** `ComunicacaoContratoTccTests.registroNuloDeveSerRejeitadoComoFalhaDeIntegracaoControlada` falha no tratamento de uma comunicação nula. Na última execução de referência da versão atual, 321 de 322 testes passaram. Essa falha da suíte não impede iniciar a aplicação com `spring-boot:run`; ela precisa ser corrigida para que a suíte completa fique verde.
