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
| GET | `/api/dados/estatisticas/{processoId}/parametros` | Descreve os parâmetros da consulta atual, com valores e indicação de uso, sem consultar a API externa; exige propriedade do processo |
| GET | `/api/dados/comunicacoes/{processoId}` | Lista somente as comunicações salvas do processo |
| POST | `/api/dados/comunicacoes/{processoId}/pesquisar` | Pesquisa publicações no DJEN por texto e período e substitui as comunicações salvas, sem acionar IA ou DataJud |
| POST | `/api/dados/comunicacoes/{processoId}/analise-ia` | Analisa os textos salvos do processo nas categorias escolhidas, sem gravar o resultado |
| POST | `/estatisticas/calcular` | Calcula estatísticas dos dados enviados, com autenticação; não consulta registros de outros usuários |

O JSON de cadastro/edição de processo preserva os campos existentes: `numeroProcesso`, `clienteVinculado`, `posicaoCliente`, `tribunal`, `orgaoJulgador`, `grau`, `comarca`, `uf`, `classeProcessual`, `assunto`, `areaAtuacao`, `status`, `valorCausa`, `nomeMagistrado`, `dataInicio`, `dataFim`. Não envie `id`, `usuario` ou `usuarioId`: o DTO não utiliza esses campos. Edições não transferem a propriedade do processo. As respostas não serializam a entidade Usuario.

As buscas consideram processo e e-mail autenticado no banco. Um ID inexistente e um ID de outro usuário produzem o mesmo `404`, inclusive para usuários `ADMIN`. A verificação ocorre antes das chamadas externas ou de qualquer exclusão. O backend aplica essas regras independentemente do cliente.

A análise completa é gravada em uma transação: acrescenta uma execução estatística e substitui as comunicações anteriores somente daquele processo. Se houver falha na consulta ou na gravação, os resultados anteriores permanecem intactos. Uma resposta válida sem resultados no endpoint legado acrescenta uma execução com amostra zero e/ou remove as comunicações antigas; o POST exclusivo de estatísticas retorna 422 para amostras vazias. A gravação usa bloqueio por processo para evitar mistura entre execuções concorrentes. Estatísticas e sínteses anteriores mantêm seus IDs e não são removidas por novas consultas.

A consulta conjunta retorna `processoId`, `estatisticas` e `comunicacoes`; ela não executa novamente as APIs nem inclui a análise da IA. As sínteses de DataJud e de comunicações são geradas sob demanda e persistidas por seus endpoints próprios. A rota legada de análise por categorias continua disponível para compatibilidade.

Para as páginas separadas do futuro frontend, use `/api/processo/listar-usuario` para a lista de processos e, ao selecionar um processo, `/api/dados/estatisticas/{processoId}` ou `/api/dados/comunicacoes/{processoId}`. As duas consultas mantêm respostas em arrays: após uma análise bem-sucedida, há uma estatística consolidada e apenas as comunicações da análise atual, ordenadas por ID. Se o processo pertence à conta e ainda não tem resultados, retornam `200` com `[]`; processo inexistente ou de outra conta retorna `404`. Todas exigem o Bearer token. Essas consultas apenas leem o banco, sem recalcular estatísticas nem consultar as APIs externas.

### Preenchimento do cadastro pelo CNJ

`GET /api/processo/consultar-datajud?numeroProcesso=0000832-35.2018.4.01.3202` aceita os 20 dígitos, com ou sem pontuação, e exige `Authorization: Bearer <token>`. O backend valida o dígito verificador e identifica o tribunal de origem pelo CNJ. Envia uma única consulta `match` por `numeroProcesso`, em contexto `bool.filter`, ao endpoint público desse tribunal e confere novamente a igualdade exata de cada número retornado. Solicita apenas os campos da capa, sem movimentos, pontuação de relevância, ordenação por data ou paginação; `track_total_hits: 100` limita a contagem necessária para sinalizar truncamento. A chave do DataJud permanece no backend.

A resposta `ConsultaCnjResponseDTO` contém `numeroProcesso` formatado, `tribunalConsultado`, `registros` e `limiteAtingido`. Cada registro contém os seguintes dados compatíveis com o cadastro:

| DataJud | Campo do formulário / contrato |
| --- | --- |
| `tribunal` | Tribunal / `tribunal` (na ausência, usa o tribunal consultado) |
| `grau` | Grau / `grau` |
| `orgaoJulgador.nome`, `.codigo` | Órgão julgador e código / `orgaoJulgador` |
| `classe.nome`, `.codigo` | Classe processual e código / `classeProcessual` |
| `assuntos[].nome`, `.codigo` | Assunto e código / opções para `assunto` |
| `dataAjuizamento` | Data de distribuição / `dataInicio`, como `YYYY-MM-DD` |

Os códigos são devolvidos como texto, compatíveis com o cadastro. Datas ISO e `yyyyMMddHHmmss` são convertidas sem deslocar o dia por fuso; dados ausentes ou datas inválidas permanecem vazios. O formulário preserva informações digitadas manualmente. Se houver vários registros (por exemplo, graus diferentes), o usuário escolhe qual utilizar; se houver vários assuntos, escolhe o assunto do cadastro. Registros idênticos são deduplicados. A consulta traz até 100 registros, sem garantir ordem cronológica; `limiteAtingido` considera também `hits.total.relation = gte` para sinalizar a contagem limitada.

Cliente, posição do cliente, comarca, UF, área de atuação, status, valor da causa, magistrado e data final continuam manuais: não possuem correspondência direta nos atributos públicos utilizados. O código de município IBGE não é tratado como nome da comarca, e movimentos não são convertidos em status por suposição.

Este endpoint **não cadastra processo, não executa análise estatística e não grava comunicações**. O usuário revisa o formulário e confirma em **Cadastrar processo**. A busca é feita no tribunal identificado no número; não varre todos os tribunais. Um resultado vazio significa que não foi encontrado registro público nessa consulta, não que o processo judicial inexista.

Erros desta consulta usam `{ status, codigo, mensagem }`: `400 / CNJ_INVALIDO`, `400 / CNJ_TRIBUNAL_NAO_SUPORTADO`, `404 / CNJ_NAO_ENCONTRADO`, `429 / CNJ_CONSULTA_LIMITADA`, `429 / CNJ_CONSULTA_AGUARDANDO` e `502 / CNJ_CONSULTA_INDISPONIVEL`. `AGUARDANDO` significa que o controle local impediu uma nova chamada (intervalo, consulta em andamento ou pausa compartilhada), sem atribuir outra recusa ao DataJud. Falhas temporárias e esperas locais incluem `Retry-After` em segundos, arredondado para cima; preservam o maior prazo entre a configuração local e o cabeçalho externo. O frontend mostra a contagem regressiva e impede novas tentativas, inclusive ao mudar de CNJ, durante essa pausa; sem cabeçalho, usa 30 segundos para HTTP 429. Não repete automaticamente a chamada ao terminar o prazo e permite continuar o cadastro manualmente.

O formulário aguarda 600 ms após um CNJ completo e válido e reutiliza respostas bem-sucedidas durante sua montagem. O backend também reutiliza seu cache por usuário e consulta (`app.datajud.cache-ms`, padrão de 5 minutos). O controle global compartilhado com as estatísticas permite somente uma consulta externa por vez e respeita a pausa após falhas. Um HTTP 429 externo, inclusive `search_phase_execution_exception`, também pode indicar falta de recursos no Elasticsearch do provedor; o log isolado não prova excesso de requisições deste cliente. Reduzir chamadas não garante disponibilidade do DataJud. Referências: [recusas no Elasticsearch](https://www.elastic.co/docs/troubleshoot/elasticsearch/rejected-requests) e [contagem limitada](https://www.elastic.co/docs/solutions/search/the-search-api).

Referências oficiais: [atributos públicos](https://datajud-wiki.cnj.jus.br/api-publica/glossario/), [consulta por número](https://datajud-wiki.cnj.jus.br/api-publica/exemplos/exemplo1/), [endpoints por tribunal](https://datajud-wiki.cnj.jus.br/api-publica/endpoints/) e [numeração CNJ — Resolução 65](https://atos.cnj.jus.br/atos/detalhar/119).

### Pesquisa de publicações e análise por IA

O frontend usa agora **Gerar síntese com IA**, sem seleção de categorias. A pesquisa continua retornando a mesma lista de publicações, mas também grava um registro em `comunicacao_pesquisa` com os filtros efetivamente enviados, tribunal, data e cópia das publicações. Novas pesquisas não apagam esse histórico. Os endpoints abaixo exigem o proprietário do processo:

| Método | Rota | Contrato |
| --- | --- | --- |
| GET | `/api/dados/comunicacoes/{id}/pesquisas/atual` | Metadados da busca correspondente aos registros atuais; `204` quando não houver vínculo registrado |
| POST | `/api/dados/comunicacoes/{id}/sintese-ia` | `{ pesquisaId?, comunicacaoIds: [...] }`; devolve a busca com síntese e data de geração |
| GET | `/api/dados/comunicacoes/{id}/sinteses?pagina=0` | `{ itens, pagina, temProxima }`, cinco sínteses por página, mais recentes primeiro |
| GET | `/api/dados/comunicacoes/{id}/pesquisas/{pesquisaId}` | `{ pesquisa, publicacoes }`, preservando os documentos da busca original |

A síntese usa exclusivamente o texto dos documentos daquela busca, remove vazios e duplicados e valida as citações literais. O prompt solicita um parágrafo de 60 a 90 palavras; o backend rejeita respostas acima de 120 palavras ou 1200 caracteres, sem truncar frases. Textos insuficientes produzem uma limitação breve. O resultado é salvo somente após validação; falhas preservam dados e histórico. Repetir a geração já concluída reutiliza o resultado salvo. `@Version` protege a gravação da mesma busca. Comunicações anteriores ao histórico podem ser sintetizadas mediante conferência dos IDs atuais, com filtros desconhecidos explicitamente marcados, sem inventar metadados. A exclusão do processo remove seu histórico por chave estrangeira em cascata.

Para MySQL com `ddl-auto=validate`, aplicar uma vez a migração aditiva [schema/20261104_comunicacao_sinteses.sql](schema/20261104_comunicacao_sinteses.sql) antes de iniciar a versão nova. Ela não modifica tabelas existentes nem foi executada em produção. O H2 em memória cria a tabela automaticamente, mas continua perdendo todos os dados ao reiniciar.

As rotas e categorias descritas a seguir são mantidas para clientes legados; a tela usa somente a nova síntese.

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

Categorias aceitas: `SINTESE_GERAL`, `FUNDAMENTOS_DECISOES` e `VALORES`. A resposta `AnaliseComunicacaoDTO` contém a quantidade de textos distintos analisados, duplicados ignorados, somente as categorias solicitadas e `comunicacoes`, com as fontes normalizadas e numeradas utilizadas na análise. Cada item contém título, descrição, contexto e eventual decisão relacionada, além de trechos literais com o número da comunicação de origem. Categorias sem achados retornam lista vazia e uma limitação explícita.

`POST /api/dados/estatisticas/{processoId}/sintese-ia` recebe `{ "estatisticaId": 123 }` e retorna `{ "estatisticaId": 123, "sintese": "...", "geradaEm": "2026-11-03T15:00:00Z" }`. Autoriza o proprietário e usa os indicadores imutáveis da execução indicada, inclusive uma execução anterior, sem consultar DataJud/DJEN novamente. Envia à IA somente os quatro campos de indicadores, sem dados pessoais, identificadores ou comunicações. A síntese é persistida nessa execução; regenerar atualiza texto/data sem criar outro item. ID inexistente ou pertencente a outro processo retorna 404; amostra vazia, 422; conflito de versões concorrentes, 409. Respostas vazias, inconsistentes ou truncadas retornam 502; limite externo, 429; configuração ausente, 503. Falhas preservam indicadores, texto e data anteriores.

O prompt solicita um parágrafo em português brasileiro, preferencialmente de 50–70 palavras, explicando medianas, subamostras distintas, ausência de baixa identificada e limitações. O backend aceita 25–110 palavras e até 1200 caracteres, confere números (incluindo `162,5` e `1.162,5`), duração por marco quando explicitada e rejeita padrões evidentes de indicadores novos, previsões, aconselhamento e causalidade. Essa validação determinística é básica, não uma garantia de compreensão semântica completa.

`GET /api/dados/estatisticas/{processoId}/sinteses?pagina=0` retorna `{ "itens": [{ "estatisticaId": 123, "geradaEm": "...", "parametros": { ... } }], "pagina": 0, "temProxima": false }`. São cinco sínteses concluídas por página, ordenadas por data de geração e ID decrescentes. `GET /api/dados/estatisticas/{processoId}/{estatisticaId}` retorna a execução completa para o modal, com os mesmos campos do GET de estatísticas. Todas as leituras verificam o proprietário e o vínculo entre processo e execução.

As respostas de estatísticas mantêm os seis campos anteriores e acrescentam `executadaEm`, `parametros` (tribunal, classeCodigo, grau, limiteResultados, dataInicio, dataFim), `statusSintese` (`SEM_SINTESE` ou `CONCLUIDA`) e `sintese` (objeto acima ou null). O POST `/gerar` retorna uma lista com a nova execução; o GET existente mantém a lista de execuções em ordem crescente de ID, com a atual no final. O endpoint legado de análise também preserva as estatísticas anteriores; o comportamento das comunicações permanece igual.

Persistência: a tabela existente `datajud_estatistica` recebe os campos adicionais e uma versão otimista, sem tabelas paralelas. As execuções antigas continuam acessíveis, com parâmetros/data de execução desconhecidos (null). O estado de carregamento é síncrono na interface: nenhuma falha é gravada como síntese concluída e nenhuma tarefa fica permanentemente marcada como em andamento.

O checkout usa H2 em memória por padrão e não possuía Flyway/Liquibase. Para o MySQL persistente existente, o driver foi incluído e o perfil opcional `mysql` usa `MYSQL_URL`, `MYSQL_USER`, `MYSQL_PASSWORD` e `JWT_SECRET`, com `ddl-auto=validate`. Antes da atualização, faça backup e aplique **uma vez** [schema/20261103_datajud_sinteses.sql](schema/20261103_datajud_sinteses.sql) ao esquema existente; depois inicie com `SPRING_PROFILES_ACTIVE=mysql`. O SQL é aditivo e não deduz dados históricos ausentes. Não foi aplicado a banco de produção. O perfil H2 de desenvolvimento continua temporário.

O backend confirma o proprietário antes de ler as comunicações e usa apenas o campo `texto`. O HTML é convertido em texto legível com jsoup, incluindo a decodificação de entidades, antes de remover textos vazios e duplicados. Cada documento é enviado em uma parte separada, e as citações são conferidas contra esse mesmo texto, com normalização de espaços Unicode e acentos equivalentes. O conteúdo original salvo no banco é preservado. Rejeita conjuntos acima de 500.000 caracteres de texto legível sem fazer análise parcial. A resposta do Gemini é validada: categorias extras, ausência de categorias, formato incompleto e citações cujo trecho não ocorre no documento indicado resultam em erro, sem exibição de conclusões não verificadas. Essa verificação de trechos auxilia a revisão, mas não substitui a conferência profissional do significado de cada achado. A análise não altera comunicações, estatísticas nem o processo e não persiste o resultado gerado.

A conferência das citações aceita diferenças de maiúsculas/minúsculas, espaços, quebras de linha, aspas e pontuação de apresentação (pontos, vírgulas, dois-pontos, ponto e vírgula, exclamação, interrogação e delimitadores). Exige palavras completas em sequência contínua na comunicação indicada, preservando acentos, palavras e números. Separadores internos de valores e datas são preservados: `5.000,00` não equivale a `500,00`, e `03/10/2026` não equivale a `10/03/2026`. Essa tolerância não aceita paráfrases, troca de palavras ou citações encontradas apenas em outra comunicação.

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

A consulta de estatísticas usa o tribunal para definir o endpoint e aplica apenas o código da classe processual e o grau, quando preenchidos. Código da classe ausente ou vazio faz o filtro ser omitido, sem busca alternativa por nome. Assunto, órgão julgador, comarca e UF não restringem a amostra. As datas do cadastro não são herdadas; somente o período informado ao gerar estatísticas é aplicado. Isso também atende processos antigos com campos `null`. O tribunal precisa estar preenchido e ser suportado. Respostas externas inválidas ou consultas DataJud incompletas não são interpretadas como amostra vazia e não apagam resultados anteriores.

Confira no H2 a mesma URL JDBC usada pela aplicação (`jdbc:h2:mem:banco` na configuração padrão). Os resultados ficam em `datajud_estatistica` e `comunicacao`, vinculados por `processo_id`. Reiniciar o backend apaga os dados desse banco em memória; após reiniciar, cadastre/entre na conta e execute novamente a análise.

## Dados anteriores e migração

No ambiente encontrado, o H2 é exclusivamente em memória. A aplicação recria o esquema na próxima inicialização e não há arquivo de banco persistente a migrar. Nenhuma rotina foi criada para apagar ou atribuir automaticamente processos existentes a uma conta arbitrária.

Se outro ambiente sobrescrever a configuração para utilizar um banco persistente, faça backup e migre antes de iniciar esta versão: crie `usuarios`, adicione `processos.usuario_id` inicialmente anulável, cadastre os usuários com hashes válidos, atribua cada processo a seu proprietário confirmado, confira que não há vínculos nulos/inválidos e então aplique a FK e `NOT NULL`. Preserve os IDs dos processos para manter os vínculos existentes de estatísticas e comunicações. Não dependa de `ddl-auto=update` para inferir proprietários de dados antigos.

Para o logout, o esquema persistente também precisa da entidade `TokenRevogado`: tabela `token_revogado` com ID gerado, `token_hash` obrigatório de 64 caracteres e `expira_em` obrigatório, com os índices definidos na entidade. As revogações precisam ser preservadas enquanto os tokens correspondentes ainda estiverem válidos.

## Contrato simplificado das estatísticas DataJud

Os endpoints de geração e recuperação mantêm as rotas e o vínculo ao processo. Cada resultado contém somente identificadores e os seis valores estatísticos:

```json
{
  "id": 100,
  "processoId": 1,
  "quantidadeProcessos": 61,
  "processosPendentes": 21,
  "tempoAtePrimeiroJulgamento": { "quantidade": 38, "medianaDias": 119.0 },
  "tempoAtePrimeiraBaixa": { "quantidade": 35, "medianaDias": 168.0 }
}
```

A quantidade de cada tempo conta somente durações válidas. Sem duração calculável, retorna `{"quantidade":0,"medianaDias":null}`. Um tempo embutido inteiramente ausente em registros legados pode retornar `null`. Medianas antigas com quantidade ausente ou zero são expostas como indisponíveis. O cálculo direto em `POST /estatisticas/calcular` usa os mesmos indicadores, sem os identificadores.

Média, mínimo, máximo, amplitude, desvio padrão, percentis, tempo pendente e contadores redundantes de eventos deixaram de ser calculados, mapeados por JPA e serializados. O Gemini recebe uma lista explícita de campos: total, ausência de baixa identificada e as duas medianas com suas quantidades. O prompt exige explicação objetiva desses resultados e limitações; proíbe recalcular, derivar indicadores, prever duração ou estimar êxito. Estatísticas e sínteses permanecem salvas por execução.

A redução anterior dos indicadores preservou os nomes das colunas. A inclusão do histórico exige o DDL aditivo descrito na seção de sínteses. Colunas antigas, anuláveis no modelo anterior, podem permanecer em MySQL sem leitura ou escrita por esta versão. Não executar `DROP COLUMN`, `TRUNCATE` ou recriação do esquema. Em banco persistente, usar `spring.jpa.hibernate.ddl-auto=validate`. Não há migração destrutiva nem acesso ao banco de produção nesta entrega. A compatibilidade com colunas legadas é exercitada em H2; MySQL de produção não foi usado nos testes.

## Consistência das estatísticas DataJud

O cálculo aceita ajuizamento compacto (`uuuuMMddHHmmss`, validação estrita) ou ISO com offset. Datas com offset são normalizadas para UTC; o formato compacto, que não informa fuso, é interpretado em UTC por convenção de compatibilidade do sistema, sem inferir o fuso do tribunal ou da máquina. Os tempos representam dias completos, não dias úteis.

Datas inexistentes e durações anteriores ao ajuizamento não entram nas medidas, inclusive diferenças negativas inferiores a 24 horas. Para a duração, é selecionado o primeiro movimento válido a partir do ajuizamento, mesmo quando existem eventos anteriores inconsistentes. Registros nulos são ignorados. Uma amostra sem durações válidas mantém quantidade zero e medidas nulas, compatíveis com o banco e com a exibição de ausência de amostra no frontend.

As contagens de movimentos identificados e a quantidade de durações válidas são distintas. Esta etapa preserva os nomes de julgamento reconhecidos e a classificação legada por presença de baixa. A baixa é identificada por nome e data válida mesmo se o ajuizamento estiver ausente ou impedir o cálculo da duração; esse processo não entra na contagem de pendentes. Não são calculados tempos de processos pendentes. Não trata reaberturas, não amplia o limite de 400 resultados e não transforma esses indicadores em previsão. Resultados já salvos só recebem o novo cálculo ao solicitar uma atualização da análise.

## Verificação

Na geração exclusiva de estatísticas, uma busca sem processos retorna HTTP 422 (`DATAJUD_SEM_PROCESSOS`) e preserva as estatísticas anteriores. Uma resposta com registros sem `_source` é rejeitada como falha de integração. O log identifica o processo, o período, a quantidade recebida e o ID gravado na transação, sem registrar credenciais ou o conteúdo dos processos.

O botão exibe diretamente a resposta persistida do POST; ao reabrir a aba, o GET lê o banco. A consulta sem datas tem teste de equivalência com o GET legado `/api/dados/analisar/{id}`. Para testes locais com um servidor HTTP simulado, `app.datajud.base-url` pode ser sobrescrita; o valor padrão continua sendo a API pública do CNJ.

`POST /api/dados/estatisticas/{id}/gerar` recebe `{ "dataInicio": "2025-01-01", "dataFim": "2025-12-31" }`, com ambos os campos opcionais. Aplica `range` sobre `dataAjuizamento`, incluindo os dias inicial e final em UTC. `{}` não aplica datas, nem herda o período do cadastro. O endpoint valida a ordem das datas e a propriedade do processo, acrescenta uma execução estatística numa transação e preserva as execuções anteriores e as comunicações. Falhas externas preservam os resultados anteriores. O limite continua em 400 resultados.

O filtro de período contempla duas representações encontradas no DataJud: ISO (com formato explícito) e compacta `yyyyMMddHHmmss` (sem reinterpretar o valor indexado). Os intervalos são combinados por `bool.should` com `minimum_should_match: 1`. A data da origem também é conferida no backend, para excluir falsos positivos da sobreposição de intervalos abertos e datas inválidas. A paginação continua até reunir até 400 registros dentro do período ou esgotar a busca; páginas sem registros válidos não encerram a coleta. Sem datas, essa restrição não é aplicada.

A coleta de estatísticas usa páginas de até 100 registros com `search_after`, ordenadas pelo identificador único `id.keyword` descrito no [glossário da API pública do CNJ](https://datajud-wiki.cnj.jus.br/api-publica/glossario/). Reúne as páginas até esgotar a busca ou atingir o limite de 400; todos os filtros, inclusive os limites de data opcionais, são reaplicados em cada página. Solicita somente o número, data de ajuizamento e nomes/datas dos movimentos usados no cálculo. Os critérios usam contexto `bool.filter`, com `term` apenas para `classe.codigo` e `grau.keyword`, sem pesquisa por nomes. A consulta dispensa a pontuação de relevância e a contagem total da base. A amostra segue a ordem de `id.keyword` e pode incluir diferentes assuntos e órgãos julgadores. O cálculo e a gravação continuam ocorrendo após a coleta. A síntese por IA considera essa abrangência, sem presumir filtros que não estão registrados nas estatísticas salvas.

Cada página admite até três tentativas em HTTP 502/503/504 ou falha de conexão/leitura, com intervalos de 500 ms e 1 s. HTTP 429 e outros erros não são repetidos. O cliente DataJud tem limite de conexão de 10 s e de resposta de 55 s por tentativa. O log informa página, tentativa, duração e status externo, sem credenciais ou conteúdo processual. Respostas incompletas (`timed_out`, shards com falha), registros repetidos ou falta de cursor para continuar abortam a geração. As estatísticas só são calculadas e substituídas após a coleta completa da amostra; uma falha intermediária preserva as anteriores. Datas vazias continuam válidas, mas indisponibilidade persistente no DataJud pode impedir a geração mesmo após as novas tentativas.

```powershell
.\mvnw.cmd -B -ntp test
```

Os testes de integração usam H2 separado e respostas simuladas somente para DataJud e Comunicação. Exercitam cadastro, hash de senha, login real, JWT, logout com bloqueio do token revogado, preservação de outros logins, novo login após sair, validação, isolamento entre contas, tentativas de alterar o proprietário pelo JSON, resultados e exclusão, além das transações e chaves estrangeiras. A chave do perfil `test` é exclusiva dos testes e não é distribuída nos recursos principais.

A implementação de JWT utiliza os componentes Nimbus fornecidos pelo [Spring Security](https://docs.spring.io/spring-security/reference/servlet/oauth2/) e suas versões gerenciadas pelo Spring Boot, sem biblioteca JWT paralela.
