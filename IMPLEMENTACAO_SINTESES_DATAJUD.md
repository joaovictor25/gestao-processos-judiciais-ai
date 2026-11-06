# Sínteses estatísticas e histórico DataJud

Implementação de 03/11/2026, limitada ao DataJud. Alterações que já existiam no checkout foram preservadas.

## Inspeção e arquitetura reutilizada

- Interface: `DataJudTab`, `AISummaryCard`, `EstatisticaCard`, `useDadosProcesso`, `processoService`, componentes `Button`, `Icon` e estados de carregamento/erro.
- Backend: `AnaliseController`, `AnaliseService`, `DataJudService`, `EstatisticaProcessualService`, `LLMService`/`GeminiService`, entidade `DataJud` e `DataJudRepository`.
- Autorização: `ProcessoService.buscarPorId` restringe a leitura ao proprietário autenticado. A execução é também consultada por `id` **e** `processoId`.
- Banco encontrado no projeto: H2 em memória, JPA/Hibernate, sem Flyway/Liquibase. A entidade existente foi estendida; não foram criadas tabelas paralelas.
- O cálculo estatístico e os filtros DataJud não foram alterados. A baixa não identificada não é apresentada como certeza de pendência processual.

## Backend e persistência

Cada consulta bem-sucedida acrescenta uma linha em `datajud_estatistica`, preservando os indicadores e sínteses anteriores. Essa regra também vale para o endpoint legado de análise, sem alterar a substituição das comunicações feita por ele.

A linha registra vínculo com o processo, instante da execução, tribunal, código da classe, grau, limite da consulta, datas opcionais, indicadores originais, texto da síntese, instante da síntese, estado e versão otimista. Regenerar atualiza texto/data da mesma linha. Uma resposta atrasada que conflite com uma síntese já salva recebe HTTP 409.

Somente os indicadores são enviados ao Gemini. O prompt solicita um parágrafo em português brasileiro, preferencialmente de 50–70 palavras, contextualizando as medianas e as limitações sem recalcular, prever resultados ou recomendar estratégias. O backend valida formato, tamanho (25–110 palavras, até 1200 caracteres), números em formato brasileiro, associações explícitas entre marcos/durações e contagens, unidades e padrões evidentes de afirmações proibidas. Uma falha não apaga os indicadores nem substitui texto/data existentes.

Estados persistidos: `SEM_SINTESE` e `CONCLUIDA`. A operação é síncrona e o carregamento é exibido na interface; não se mantém um registro incompleto marcado como concluído nem um estado permanente de execução em andamento.

## Interface e histórico

- Síntese atual em destaque abaixo dos indicadores, com data e período da execução.
- Geração lateral com bloqueio de cliques duplicados, carregamento, erro, nova tentativa e indicação de síntese salva/ausente.
- Uma nova estatística começa sem síntese e mantém as anteriores no histórico.
- Histórico lateral ordenado da geração mais recente para a mais antiga, cinco itens por página, com “Ver mais” e retorno à página anterior.
- Modal com data, período, tamanho da amostra e texto completo. Aceita Escape e botão Fechar; devolve o foco ao acionador e não muda os indicadores atuais.
- Sem datas: “Sem recorte de data”. Em execuções legadas sem metadados: “Período não registrado nesta execução”, sem inventar um filtro.
- Cancelamento e vinculação ao ID da execução impedem a aplicação de respostas antigas após troca de seleção, versão, processo ou aba.

## Contratos HTTP

Todas as rotas exigem autenticação e acesso ao processo.

| Método/rota | Alteração |
| --- | --- |
| `POST /api/dados/estatisticas/{processoId}/gerar` | Preserva execuções anteriores e retorna lista com a nova execução persistida. |
| `GET /api/dados/estatisticas/{processoId}` | Mantém lista por ID crescente; inclui parâmetros, data, estado e síntese salva. A atual é o último item. |
| `POST /api/dados/estatisticas/{processoId}/sintese-ia` | Reutilizado. Recebe `{ "estatisticaId": 123 }`, gera/regenera e salva. Retorna ID, texto e `geradaEm`. |
| `GET /api/dados/estatisticas/{processoId}/sinteses?pagina=0` | Novo. Retorna `itens`, `pagina`, `temProxima`; cinco sínteses concluídas por página. |
| `GET /api/dados/estatisticas/{processoId}/{estatisticaId}` | Novo. Detalha uma execução e sua síntese para leitura histórica. |

Erros: 401 sem autenticação; 404 sem acesso ou sem vínculo processo/execução; 400 para página inválida; 422 sem amostra válida; 409 para conflito concorrente; 502 para resposta inválida/serviço indisponível; 429 para limite da IA; 503 para configuração ausente.

## Banco persistente

O SQL `backend/schema/20261103_datajud_sinteses.sql` acrescenta colunas e índice, preservando linhas e colunas antigas. Deve ser aplicado **uma vez** ao esquema existente antes de iniciar a atualização. Os metadados históricos desconhecidos ficam nulos.

O perfil opcional `mysql` inclui o driver MySQL e lê `MYSQL_URL`, `MYSQL_USER` e `MYSQL_PASSWORD`, além de `JWT_SECRET`. Usa `ddl-auto=validate` e UTC na conexão JPA. Ative com `SPRING_PROFILES_ACTIVE=mysql` após a atualização do esquema. O perfil padrão H2 continua em memória e perde dados ao reiniciar. Nenhum banco de produção foi alterado.

## Arquivos desta implementação

Criados:

- `backend/src/main/java/br/com/axionweb/backend/dto/datajud/ConsultaEstatisticaDTO.java`
- `backend/src/main/java/br/com/axionweb/backend/dto/datajud/HistoricoSintesesDTO.java`
- `backend/src/main/java/br/com/axionweb/backend/service/ValidadorSinteseDataJud.java`
- `backend/src/main/resources/application-mysql.properties`
- `backend/schema/20261103_datajud_sinteses.sql`
- `backend/src/test/java/br/com/axionweb/backend/service/SinteseDataJudIntegrationTests.java`
- `backend/src/test/java/br/com/axionweb/backend/service/ValidadorSinteseDataJudTests.java`
- `backend/src/test/java/br/com/axionweb/backend/service/MigracaoSintesesTests.java`
- `frontend/src/components/processos/detalhes/HistoricoSinteses.tsx`
- Este relatório.

Modificados/reaproveitados (inclusive arquivos que já estavam sem rastreamento no Git):

- Backend: `pom.xml`, `README.md`, `AnaliseController.java`, `DataJudResponseDTO.java`, `SinteseDataJudDTO.java`, `GlobalExceptionHandler.java`, `DataJud.java`, `DataJudRepository.java`, `AnaliseService.java`, `GeminiService.java`, `SinteseDataJudService.java`.
- Testes backend: `AutenticacaoIsolamentoIntegrationTests.java`, `AnaliseComunicacaoIntegrationTests.java`, `AnaliseConsultasHttpIntegrationTests.java`, `AnaliseProcessoIntegrationTests.java`. Expectativas de substituição foram atualizadas para preservação do histórico; os cenários existentes foram mantidos.
- Frontend: `README.md`, `DataJudTab.tsx`, `AISummaryCard.tsx`, `ProcessoDetalhes.css`, `api.ts`, `processoService.ts`, `types/estatisticas.ts`.
- Testes navegador: `browser-check.mjs`, `detalhes-fixtures.mjs`, `detalhes-scenarios.mjs`, `datajud-http-scenario.mjs`.

## Testes executados e resultados

- `backend: .\mvnw.cmd -q test`: **286 testes, zero falhas, zero erros, zero ignorados**, incluindo a suíte existente e 24 testes novos de sínteses, validação e migração. Relatórios em `backend/target/surefire-reports`; log final em `backend/target/datajud-final-tests.log`.
- `backend: .\mvnw.cmd -q -DskipTests package`: empacotamento concluído; JAR usado no teste real do backend local.
- `frontend: npm.cmd run build`: TypeScript e build de produção Vite concluídos com sucesso.
- `frontend: node scripts/browser-check.mjs`, com `BROWSER_SCOPE=detalhes` e `DATAJUD_LOCAL_HTTP=1`: **35 verificações aprovadas**, zero exceções JavaScript. Backend Java e H2 reais; DataJud/Gemini simulados em HTTP local. Inclui persistência, regeneração sem duplicação, falhas, rejeição de número inventado, histórico 5+2, modal, Escape, recarga, cancelamento de resposta atrasada e telas de 320 px sem rolagem horizontal.
- Capturas conferidas: `frontend/artifacts/datajud-sintese-desktop.png`, `datajud-sintese-mobile.png` e `datajud-historico-modal-mobile.png`. Resultado estruturado em `frontend/artifacts/browser-check.json`.
- `git diff --check` nos arquivos alterados da implementação: sem erros de espaço em branco (há avisos normais de conversão LF/CRLF do checkout).

As primeiras rodadas detectaram expectativas antigas de substituição e um ID não propagado após a introdução da versão JPA. Esses pontos foram corrigidos; os resultados acima correspondem às execuções finais. Um build inicialmente bloqueado pelo sandbox foi executado com a permissão de subprocessos necessária.

## Limites da verificação

DataJud e Gemini são simulados; não foram feitas chamadas reais para validar esta entrega. A validação determinística não pretende cobrir toda a semântica possível de um texto. A migração é exercitada em H2 no modo MySQL; um servidor MySQL real não foi utilizado. O Postman CLI não iniciou devido à configuração local de Private Cloud, e os testes utilizaram a infraestrutura existente do projeto.

Cancelar um pedido no navegador descarta sua resposta visual; o backend pode concluir e persistir o resultado. Ao voltar à aba, a leitura recupera o que foi salvo. O histórico persiste conforme o banco configurado.
