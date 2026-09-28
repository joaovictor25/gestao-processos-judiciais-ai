# Backend de gestão de processos

Spring Boot 4.1.1, Java 25, Spring Data JPA e H2. O frontend React/Vite fica separado em [`../frontend`](../frontend/README.md), com cadastro, login e logout integrados à API.

## Executar

No diretório `backend`, com `JAVA_HOME` apontando para um JDK 25 ou superior:

```powershell
.\mvnw.cmd spring-boot:run
```

O perfil padrão é `dev`. Sem `JWT_SECRET`, esse perfil gera uma chave criptográfica aleatória na inicialização, sem imprimi-la. Os tokens deixam de funcionar ao reiniciar. O banco atual também é temporário (`jdbc:h2:mem:banco`): usuários, processos, estatísticas e comunicações precisam ser recriados depois que a aplicação é encerrada.

Para usar uma chave estável, forneça `JWT_SECRET` como uma chave aleatória em Base64 com pelo menos 32 bytes após decodificação. Mantenha-a no ambiente ou em um gerenciador de segredos, fora do Git. `app.jwt.validade` controla a duração do token (padrão `PT1H`); `app.jwt.issuer` define o emissor.

Fora de desenvolvimento, defina explicitamente `SPRING_PROFILES_ACTIVE=prod`, configure `JWT_SECRET` e o banco persistente. A aplicação recusa uma chave ausente fora de `dev`, inválida ou curta. O perfil `prod` não muda sozinho a URL do banco: é necessário configurar o datasource do ambiente. Use HTTPS no acesso externo à API.

O console H2 em `/h2` é habilitado apenas no perfil `dev`, com acesso local sem autenticação da aplicação. As rotas privadas da API continuam exigindo JWT.

### Acessar o H2 pelo navegador

Inicie o backend normalmente pela IDE ou pelo PowerShell, dentro de `backend`. O perfil padrão é `dev`:

```powershell
.\mvnw.cmd spring-boot:run
```

Não é necessário criar administrador nem configurar `DEV_ADMIN_EMAIL` ou `DEV_ADMIN_PASSWORD`.

1. Com o backend iniciado, abra `http://localhost:8080/h2/` (ajuste a porta se necessário).
2. O console abre diretamente. Na tela de conexão do H2, preencha:

   | Campo | Valor padrão do projeto |
   | --- | --- |
   | Driver Class | `org.h2.Driver` |
   | JDBC URL | `jdbc:h2:mem:banco` |
   | User Name | `sa` |
   | Password | Deixe vazio |

3. Clique em **Connect**. `sa` é o usuário de conexão do próprio banco; não é uma conta da aplicação.

Como o H2 atual é em memória, reiniciar o backend apaga os registros anteriores. Fora do perfil `dev`, o console continua desabilitado.

## Cadastro, login e logout

```http
POST /api/auth/cadastro
Content-Type: application/json

{"nome":"João Victor","email":"joao@email.com","senha":"123456"}
```

Retorna `201` com `id`, `nome`, `email` e `role`. Nome e e-mail são normalizados; e-mails duplicados, inclusive com diferença de maiúsculas, retornam `409`. A senha deve ter pelo menos 6 caracteres e no máximo 72 bytes em UTF-8, limite do BCrypt. Ela é armazenada como hash pelo `DelegatingPasswordEncoder`.

```http
POST /api/auth/login
Content-Type: application/json

{"email":"joao@email.com","senha":"123456"}
```

Retorna `200`:

```json
{
  "token": "<JWT assinado>",
  "tipo": "Bearer",
  "usuarioId": 1,
  "nome": "João Victor",
  "email": "joao@email.com",
  "role": "USER"
}
```

As demais rotas da API exigem `Authorization: Bearer <token>`. O JWT usa assinatura HS256 e contém apenas identificação e datas, sem senha. Cada login gera um identificador aleatório (`jti`), garantindo tokens diferentes mesmo para logins do mesmo usuário no mesmo segundo. A validação verifica assinatura, algoritmo, emissor, expiração, subject e revogação, e carrega o usuário do banco em cada requisição. Não é criada sessão de servidor.

`GET /api/auth/me` retorna os dados públicos da conta autenticada. Não há refresh token nesta implementação.

Para sair, envie o token do login atual, sem corpo na requisição:

```http
POST /api/auth/logout
Authorization: Bearer <token>
```

Retorna `204 No Content` e revoga esse token. Novas requisições com ele retornam `401`, inclusive outra tentativa de logout. Tokens ausentes, inválidos ou expirados também retornam `401`. Outros logins do usuário e de outras contas continuam funcionando; um novo login gera um novo token válido. O logout preserva usuários, processos e resultados salvos. Requisições que já passaram pela autenticação antes da revogação podem concluir normalmente.

O futuro frontend deve chamar esse endpoint ao clicar em **Sair**, remover sua cópia do token após o sucesso e redirecionar para o login. Caso receba `401`, também pode limpar o token local, pois ele não permite mais acesso.

A revogação fica na tabela `token_revogado`, com o hash SHA-256 do conteúdo assinado e sua expiração; o JWT completo não é armazenado. Tokens emitidos antes da inclusão de `jti` também podem ser revogados. Registros expirados são removidos durante os próximos logouts. A revogação sobrevive ao reinício quando o banco é persistente; no H2 em memória atual, ela é apagada junto com os demais dados.

## Processos e resultados

O vínculo persistido é `usuarios.id <- processos.usuario_id <- datajud_estatistica.processo_id / comunicacao.processo_id`. As chaves estrangeiras são obrigatórias. DataJud e Comunicação não duplicam `usuario_id`.

| Método | Rota | Comportamento |
| --- | --- | --- |
| POST | `/api/processo/cadastrar` | Cadastra com o proprietário identificado pelo JWT |
| GET | `/api/processo/listar-usuario` | Lista somente os processos da conta autenticada |
| GET | `/api/processo/{id}` | Busca um processo da conta |
| GET | `/api/processo/buscar-num-processo?numeroProcesso=...` | Busca por número e proprietário |
| GET | `/api/processo/consultar-datajud?numeroProcesso=...` | Consulta a capa pública do CNJ no DataJud para auxiliar o cadastro; exige JWT |
| PUT | `/api/processo/atualizar/{id}` | Atualiza somente se pertencer à conta |
| DELETE | `/api/processo/deletar/{id}` | Exclui o processo e seus resultados, após validar o proprietário |
| POST | `/api/dados/analisar/{id}` | Executa o fluxo DataJud, estatísticas e Comunicações |
| GET | `/api/dados/analisar/{id}` | Solicitação anterior, mantida por compatibilidade; prefira POST |
| GET | `/api/dados/analise/{processoId}` | Consulta estatísticas e comunicações já salvas para o processo |
| GET | `/api/dados/estatisticas/{processoId}` | Lista somente as estatísticas salvas do processo |
| GET | `/api/dados/comunicacoes/{processoId}` | Lista somente as comunicações salvas do processo |
| POST | `/api/dados/comunicacoes/{processoId}/pesquisar` | Pesquisa publicações no DJEN por texto e período e substitui as comunicações salvas, sem acionar IA ou DataJud |
| POST | `/api/dados/comunicacoes/{processoId}/analise-ia` | Analisa os textos salvos do processo nas categorias escolhidas, sem gravar o resultado |
| POST | `/estatisticas/calcular` | Calcula estatísticas dos dados enviados, com autenticação; não consulta registros de outros usuários |

O JSON de cadastro/edição de processo preserva os campos existentes: `numeroProcesso`, `clienteVinculado`, `posicaoCliente`, `tribunal`, `orgaoJulgador`, `grau`, `comarca`, `uf`, `classeProcessual`, `assunto`, `areaAtuacao`, `status`, `valorCausa`, `nomeMagistrado`, `dataInicio`, `dataFim`. Não envie `id`, `usuario` ou `usuarioId`: o DTO não utiliza esses campos. Edições não transferem a propriedade do processo. As respostas não serializam a entidade Usuario.

As buscas consideram processo e e-mail autenticado no banco. Um ID inexistente e um ID de outro usuário produzem o mesmo `404`, inclusive para usuários `ADMIN`. A verificação ocorre antes das chamadas externas ou de qualquer exclusão. O backend aplica essas regras independentemente do cliente.

A análise é gravada em uma transação: uma nova solicitação substitui todas as estatísticas e comunicações anteriores somente daquele processo. A consulta às APIs e a preparação dos dados acontecem antes da substituição; se houver falha na consulta ou na gravação, o resultado anterior permanece intacto. Uma resposta válida sem resultados substitui as estatísticas por valores zerados e/ou remove as comunicações antigas, conforme a fonte que retornou vazia. A gravação usa bloqueio por processo para evitar que solicitações simultâneas acumulem ou misturem resultados. Os IDs dos registros de estatísticas e comunicações podem mudar na substituição; o ID do processo permanece o mesmo. Registros de histórico acumulados pela versão anterior são removidos na próxima análise bem-sucedida daquele processo.

A consulta conjunta retorna `processoId`, `estatisticas` e `comunicacoes`; ela não executa novamente as APIs nem inclui a análise da IA. O resultado da análise das comunicações é gerado sob demanda pelo endpoint próprio e não é persistido. A síntese por IA das estatísticas DataJud continua reservada para uma futura etapa.

Para as páginas separadas do futuro frontend, use `/api/processo/listar-usuario` para a lista de processos e, ao selecionar um processo, `/api/dados/estatisticas/{processoId}` ou `/api/dados/comunicacoes/{processoId}`. As duas consultas mantêm respostas em arrays: após uma análise bem-sucedida, há uma estatística consolidada e apenas as comunicações da análise atual, ordenadas por ID. Se o processo pertence à conta e ainda não tem resultados, retornam `200` com `[]`; processo inexistente ou de outra conta retorna `404`. Todas exigem o Bearer token. Essas consultas apenas leem o banco, sem recalcular estatísticas nem consultar as APIs externas.

### Preenchimento do cadastro pelo CNJ

`GET /api/processo/consultar-datajud?numeroProcesso=0000832-35.2018.4.01.3202` aceita os 20 dígitos, com ou sem pontuação, e exige `Authorization: Bearer <token>`. O backend valida o dígito verificador e identifica o tribunal de origem pelo CNJ. Envia a consulta `match` por `numeroProcesso` ao endpoint público desse tribunal e confere novamente a igualdade exata de cada número retornado. A chave do DataJud permanece no backend.

A resposta `ConsultaCnjResponseDTO` contém `numeroProcesso` formatado, `tribunalConsultado`, `registros` e `limiteAtingido`. Cada registro contém os seguintes dados compatíveis com o cadastro:

| DataJud | Campo do formulário / contrato |
| --- | --- |
| `tribunal` | Tribunal / `tribunal` (na ausência, usa o tribunal consultado) |
| `grau` | Grau / `grau` |
| `orgaoJulgador.nome`, `.codigo` | Órgão julgador e código / `orgaoJulgador` |
| `classe.nome`, `.codigo` | Classe processual e código / `classeProcessual` |
| `assuntos[].nome`, `.codigo` | Assunto e código / opções para `assunto` |
| `dataAjuizamento` | Data de distribuição / `dataInicio`, como `YYYY-MM-DD` |

Os códigos são devolvidos como texto, compatíveis com o cadastro. Datas ISO e `yyyyMMddHHmmss` são convertidas sem deslocar o dia por fuso; dados ausentes ou datas inválidas permanecem vazios. O formulário preserva informações digitadas manualmente. Se houver vários registros (por exemplo, graus diferentes), o usuário escolhe qual utilizar; se houver vários assuntos, escolhe o assunto do cadastro. Registros idênticos são deduplicados. A consulta traz até 100 registros, do mais recentemente atualizado para o mais antigo; `limiteAtingido` sinaliza resultados além desse limite.

Cliente, posição do cliente, comarca, UF, área de atuação, status, valor da causa, magistrado e data final continuam manuais: não possuem correspondência direta nos atributos públicos utilizados. O código de município IBGE não é tratado como nome da comarca, e movimentos não são convertidos em status por suposição.

Este endpoint **não cadastra processo, não executa análise estatística e não grava comunicações**. O usuário revisa o formulário e confirma em **Cadastrar processo**. A busca é feita no tribunal identificado no número; não varre todos os tribunais. Um resultado vazio significa que não foi encontrado registro público nessa consulta, não que o processo judicial inexista.

Erros desta consulta usam `{ status, codigo, mensagem }`: `400 / CNJ_INVALIDO`, `400 / CNJ_TRIBUNAL_NAO_SUPORTADO`, `404 / CNJ_NAO_ENCONTRADO`, `429 / CNJ_CONSULTA_LIMITADA` e `502 / CNJ_CONSULTA_INDISPONIVEL`. O frontend permite continuar manualmente quando a fonte externa não responde ou não encontra o CNJ.

Referências oficiais: [atributos públicos](https://datajud-wiki.cnj.jus.br/api-publica/glossario/), [consulta por número](https://datajud-wiki.cnj.jus.br/api-publica/exemplos/exemplo1/), [endpoints por tribunal](https://datajud-wiki.cnj.jus.br/api-publica/endpoints/) e [numeração CNJ — Resolução 65](https://atos.cnj.jus.br/atos/detalhar/119).

### Pesquisa de publicações e análise por IA

`POST /api/dados/comunicacoes/{processoId}/pesquisar` exige JWT do proprietário e recebe os filtros preenchidos na tela:

```json
{"texto":"indenização por dano moral","dataDisponibilizacaoInicio":"2026-01-01","dataDisponibilizacaoFim":"2026-09-01"}
```

O texto é obrigatório e não aceita apenas espaços. As datas são opcionais e independentes; o fim não pode ser anterior ao início. Os três valores vêm da requisição, sem usar o assunto ou as datas do cadastro como padrão. Datas ausentes são omitidas da consulta externa. O tribunal vem do processo autorizado. A pesquisa consulta a primeira página do DJEN, com `meio=D` e até 200 publicações, e retorna uma lista de `ComunicacaoSalvaDTO` com IDs. Uma pesquisa bem-sucedida substitui somente as comunicações desse processo, inclusive quando retorna lista vazia; estatísticas DataJud são preservadas. Falhas externas preservam a pesquisa anterior e retornam `CONSULTA_DJEN_FALHOU` ou `CONSULTA_DJEN_LIMITADA`. A pesquisa não depende de chave Gemini nem aciona IA.

`POST /api/dados/comunicacoes/{processoId}/analise-ia` exige JWT do proprietário e um corpo como:

```json
{"tiposAnalise":["SINTESE_GERAL","ELEMENTOS_PROBATORIOS","VALORES"],"comunicacaoIds":[101,102]}
```

Os IDs devem corresponder à lista completa retornada pela pesquisa, na mesma ordem. A análise usa esses documentos salvos, sem consultar o DJEN novamente. Se outra pesquisa substituir os documentos antes da análise, retorna `409 / PESQUISA_DESATUALIZADA` e não aciona a IA. Os exemplos de IDs acima são ilustrativos.

Categorias aceitas: `SINTESE_GERAL`, `FUNDAMENTOS_DECISOES`, `PADROES_DECISAO`, `ELEMENTOS_PROBATORIOS`, `PADROES_ARGUMENTACAO`, `VALORES`. A resposta `AnaliseComunicacaoDTO` contém a quantidade de textos distintos analisados, duplicados ignorados, somente as categorias solicitadas e `comunicacoes`, com as fontes normalizadas e numeradas utilizadas na análise. Cada item contém título, descrição, contexto e eventual decisão relacionada, além de trechos literais com o número da comunicação de origem. Categorias sem achados retornam lista vazia e uma limitação explícita.

O backend confirma o proprietário antes de ler as comunicações e usa apenas o campo `texto`. O HTML é convertido em texto legível com jsoup, incluindo a decodificação de entidades, antes de remover textos vazios e duplicados. Cada documento é enviado em uma parte separada, e as citações são conferidas contra esse mesmo texto, com normalização de espaços Unicode e acentos equivalentes. O conteúdo original salvo no banco é preservado. Rejeita conjuntos acima de 500.000 caracteres de texto legível sem fazer análise parcial. A resposta do Gemini é validada: categorias extras, ausência de categorias, formato incompleto e citações cujo trecho não ocorre no documento indicado resultam em erro, sem exibição de conclusões não verificadas. Essa verificação de trechos auxilia a revisão, mas não substitui a conferência profissional do significado de cada achado. A análise não altera comunicações, estatísticas nem o processo e não persiste o resultado gerado.

O schema JSON enviado à API define a estrutura e os tipos das categorias. Os limites de até cinco itens por categoria e duas evidências por item são solicitados no prompt e conferidos pelo backend, sem limites de arrays aninhados no schema. Respostas interrompidas por `MAX_TOKENS` retornam `RESPOSTA_INCOMPLETA`; os demais motivos de rejeição são registrados no log sem os textos dos documentos.

Configure a chave **no ambiente do processo backend** com `GEMINI_API_KEY`. Ela não deve entrar no repositório ou no frontend. O modelo padrão é `gemini-flash-latest`; `GEMINI_MODEL` e `GEMINI_BASE_URL` são opcionais. Sem chave, o endpoint retorna `503 / CONFIGURACAO_AUSENTE`. Falha externa, limite HTTP 429 e resposta sem evidências verificáveis recebem códigos específicos, sem corpo externo ou chave na resposta. A chamada usa `systemInstruction`, saída JSON e não habilita ferramentas ou busca externa, conforme a [referência oficial de generateContent](https://ai.google.dev/api/generate-content). O alias `gemini-flash-latest` pode mudar para outra versão do modelo; para fixar uma versão, configure `GEMINI_MODEL` com um [identificador específico](https://ai.google.dev/gemini-api/docs/models).

## Erros

Os erros gerais possuem `status`, `mensagem` e `campos` (erros de validação). As falhas identificadas das consultas de análise retornam `status`, `mensagem` e `codigo` (`CONSULTA_DATAJUD_FALHOU`, `CONSULTA_DJEN_FALHOU`, `CONSULTA_DATAJUD_LIMITADA` ou `CONSULTA_DJEN_LIMITADA`). Senhas, valores rejeitados e stacktraces não são devolvidos.

| Status | Situação |
| --- | --- |
| 400 | DTO/JSON/parâmetros inválidos |
| 401 | Credenciais incorretas, token ausente, inválido, expirado ou revogado |
| 403 | Permissão insuficiente |
| 404 | Processo inexistente ou pertencente a outra conta |
| 409 | E-mail já cadastrado ou conflito de integridade |
| 502 | Falha ao consultar um serviço externo |

### Análise não aparece no banco ou no frontend

O `POST /api/dados/analisar/{id}` deve terminar com HTTP 200 antes de os resultados serem consultados. Uma falha de DataJud **ou** DJEN cancela a transação inteira. O log identifica o serviço e o status HTTP externo; por exemplo, `serviço=DATAJUD, httpExterno=429` indica que o DataJud limitou as consultas. Nesse caso, aguarde antes de tentar novamente. O backend responde 502 com o código `CONSULTA_DATAJUD_LIMITADA`, que o frontend traduz para uma mensagem específica.

Campos opcionais ausentes (órgão julgador, códigos, nomes ou grau) são ignorados na montagem dos filtros; quando o código está ausente e o nome está preenchido, a consulta utiliza o nome. Isso também atende processos antigos com campos `null`. O tribunal precisa estar preenchido e ser suportado. Respostas externas inválidas ou consultas DataJud incompletas não são interpretadas como amostra vazia e não apagam resultados anteriores.

Confira no H2 a mesma URL JDBC usada pela aplicação (`jdbc:h2:mem:banco` na configuração padrão). Os resultados ficam em `datajud_estatistica` e `comunicacao`, vinculados por `processo_id`. Reiniciar o backend apaga os dados desse banco em memória; após reiniciar, cadastre/entre na conta e execute novamente a análise.

## Dados anteriores e migração

No ambiente encontrado, o H2 é exclusivamente em memória. A aplicação recria o esquema na próxima inicialização e não há arquivo de banco persistente a migrar. Nenhuma rotina foi criada para apagar ou atribuir automaticamente processos existentes a uma conta arbitrária.

Se outro ambiente sobrescrever a configuração para utilizar um banco persistente, faça backup e migre antes de iniciar esta versão: crie `usuarios`, adicione `processos.usuario_id` inicialmente anulável, cadastre os usuários com hashes válidos, atribua cada processo a seu proprietário confirmado, confira que não há vínculos nulos/inválidos e então aplique a FK e `NOT NULL`. Preserve os IDs dos processos para manter os vínculos existentes de estatísticas e comunicações. Não dependa de `ddl-auto=update` para inferir proprietários de dados antigos.

Para o logout, o esquema persistente também precisa da entidade `TokenRevogado`: tabela `token_revogado` com ID gerado, `token_hash` obrigatório de 64 caracteres e `expira_em` obrigatório, com os índices definidos na entidade. As revogações precisam ser preservadas enquanto os tokens correspondentes ainda estiverem válidos.

## Verificação

```powershell
.\mvnw.cmd -B -ntp test
```

Os testes de integração usam H2 separado e respostas simuladas somente para DataJud e Comunicação. Exercitam cadastro, hash de senha, login real, JWT, logout com bloqueio do token revogado, preservação de outros logins, novo login após sair, validação, isolamento entre contas, tentativas de alterar o proprietário pelo JSON, resultados e exclusão, além das transações e chaves estrangeiras. A chave do perfil `test` é exclusiva dos testes e não é distribuída nos recursos principais.

A implementação de JWT utiliza os componentes Nimbus fornecidos pelo [Spring Security](https://docs.spring.io/spring-security/reference/servlet/oauth2/) e suas versões gerenciadas pelo Spring Boot, sem biblioteca JWT paralela.
