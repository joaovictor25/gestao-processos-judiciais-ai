# Consultas DataJud e geração em segundo plano

O botão Gerar estatística cria uma solicitação persistida. Cada tentativa consulta uma única resposta de até 400 processos, preservando tribunal, classe, grau e período. Não há paginação nem chamadas para repor registros descartados por data. O cálculo usa a quantidade válida efetivamente recebida. Respostas com timeout, shards com falha, registros repetidos ou estrutura inválida não são salvas nem entram no cache.

## Contrato

Todas as rotas exigem autenticação e propriedade do processo. Um processo de outra conta retorna 404 antes de consultar o provedor.

| Método e rota | Resposta |
| --- | --- |
| `POST /api/dados/estatisticas/{id}/solicitacoes` | Corpo `{ "dataInicio": null, "dataFim": null }`. Retorna 202 e `Location` para acompanhar; 200 se reutilizou geração recente concluída. |
| `GET /api/dados/estatisticas/{id}/solicitacoes/{uuid}` | Estado da solicitação e, quando concluída, `resultados` com a estatística salva. Não consulta o DataJud. |
| `GET /api/dados/estatisticas/{id}/solicitacoes/atual` | Solicitação ativa do processo, ou 204. Permite retomar o acompanhamento ao recarregar ou voltar à aba. |

O DTO contém `id`, `processoId`, `status`, `tentativas`, `proximaTentativa`, `codigoErro`, `dataInicio`, `dataFim` e `resultados`. Estados: `AGUARDANDO`, `CONSULTANDO`, `REAGENDADA`, `CONCLUIDA`, `FALHOU`. O snapshot dos parâmetros é fixado na submissão. A mesma solicitação ativa é reutilizada em cliques repetidos; filtros diferentes para o mesmo processo em execução retornam 409. Fila cheia retorna 429 local, sem chamar o provedor.

A rota síncrona `/gerar` permanece para compatibilidade e usa uma tentativa; o botão usa exclusivamente `/solicitacoes`. Todas as chamadas DataJud, inclusive análise antiga e consulta de CNJ, passam pelo mesmo controle. As rotas síncronas informam indisponibilidade quando o controle exige espera; não ocupam threads dormindo nem contornam a pausa.

## Coordenação e recuperação

`datajud_controle` serializa a admissão às chamadas por transações curtas. Há uma consulta externa em voo, com espaçamento padrão de 1,5 s entre inícios (no máximo aproximadamente 40/min). Esse valor é conservador e não promete disponibilidade do provedor. A reserva expira em 90 s para recuperar interrupções do processo; o cliente HTTP usa timeout de conexão de 10 s e resposta de 55 s. Os servidores precisam de relógios sincronizados e devem apontar ao mesmo banco para compartilhar os limites.

HTTP 429 provoca pausa global de pelo menos 30 s ou o `Retry-After`, se maior. O cabeçalho aceita segundos ou data HTTP. A fila também recupera HTTP 502/503/504 e falhas de conexão/leitura. Há no máximo três tentativas, com intervalo crescente a partir de 5 s e pequena variação aleatória, dentro de cinco minutos desde a submissão. Esperar a vez não consome uma tentativa HTTP. Se `Retry-After` exceder esse prazo, a solicitação termina com falha e a pausa global continua sendo respeitada. Não existe repetição infinita.

O cache de respostas válidas dura cinco minutos e é identificado pelo usuário autenticado, endpoint e corpo normalizado. Tem até 200 entradas, cada uma limitada a 4 milhões de caracteres. Consultas equivalentes de processos da mesma conta podem reutilizar a resposta; o resultado calculado continua pertencendo ao processo autorizado. Gerações concluídas equivalentes do mesmo processo reutilizam a estatística recente, sem duplicar o histórico. Erros nunca são armazenados como resultados válidos.

O worker revalida a conta e o acesso ao processo em cada execução. A estatística e o estado `CONCLUIDA` são gravados na mesma transação. Reservas de solicitações interrompidas podem ser recuperadas após 90 s. As solicitações terminais são removidas após sete dias; as estatísticas permanecem. Consultas externas ficam fora das transações de gravação, evitando manter conexões do banco ocupadas enquanto o provedor responde.

O log inclui endpoint, status externo, duração e `error.type` quando ele tem formato seguro. Não registra corpo, `reason`, credenciais ou dados dos processos. `error.type=es_rejected_execution_exception`, por exemplo, permite diferenciar rejeição de recursos de uma mensagem genérica. A mensagem da interface não presume que todo 429 comprova excesso de chamadas da aplicação.

## Configuração e banco existente

Os padrões e variáveis `DATAJUD_*` estão em `src/main/resources/application.properties`. É possível ajustar intervalo, pausa, tentativas, prazo, cache e limites da fila (100 globais; 5 por usuário). Para desativar somente o consumidor da fila, use `DATAJUD_WORKER_ENABLED=false`.

No perfil MySQL, aplique `schema/20261104_datajud_fila.sql` antes de iniciar a nova versão. O script adiciona três tabelas sem alterar estatísticas existentes. Não foi executado em um banco real durante esta implementação. O perfil de desenvolvimento H2 em memória cria as tabelas automaticamente, mas perde fila, cache e dados ao reiniciar; persistência entre reinicializações exige um banco persistente. Múltiplas instâncias com bancos H2 separados não compartilham coordenação.

## Verificação local

`DataJudFilaHttpIntegrationTests` usa um servidor HTTP em loopback com o RestClient real, controller, autenticação e H2. Cobre uma resposta de 400, duplicação, cache e expiração, 429 com Retry-After, limite de tentativas, preservação dos resultados, fila limitada, isolamento, concorrência, coordenação entre instâncias, recuperação de reserva e prazo. A suíte de filtros/validação continua em `ConsultaDataJudTests` e a compatibilidade em `AnaliseConsultasHttpIntegrationTests`.

O Postman CLI disponível no ambiente estava bloqueado por uma configuração corporativa de manifesto. Foi usado o servidor HTTP local em Java como alternativa, sem chamadas à API real. Os testes demonstram comportamento contra respostas simuladas; não comprovam a causa do 429 real nem a disponibilidade do CNJ.

O cenário de navegador usa `BROWSER_SCOPE=datajud-fila` em `frontend/scripts/browser-check.mjs`. Inicie um backend isolado apontando `app.datajud.base-url` a `http://127.0.0.1:8092`, com worker de 100 ms e intervalo/pausa/retry-base em zero apenas nesse teste. O script inicia o provedor simulado e verifica clique duplicado, Retry-After de 5 s, recarregamento durante a espera, conclusão automática, reutilização e falha persistente preservando indicadores. Configure o Vite para esse backend isolado e `FRONTEND_TEST_URL` para a porta do Vite. Não use essas configurações de teste em produção.

Referências: [tutorial DataJud — size](https://www.cnj.jus.br/wp-content/uploads/2023/05/tutorial-api-publica-datajud-beta.pdf), [HTTP 429 e Retry-After](https://www.rfc-editor.org/rfc/rfc6585.html#section-4), [rejeições do Elasticsearch](https://www.elastic.co/docs/troubleshoot/elasticsearch/rejected-requests).
