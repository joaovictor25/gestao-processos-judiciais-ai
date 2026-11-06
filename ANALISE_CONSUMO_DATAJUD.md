# Análise do consumo da API DataJud

Análise realizada em 03/11/2026 sobre o código presente no workspace, incluindo suas alterações locais preexistentes. Nenhum arquivo de produção foi alterado nesta investigação.

O fluxo contém situações capazes de multiplicar requisições. Entretanto, o trecho de log fornecido mostra uma execução recusada na primeira chamada, sem repetição, e não comprova que a aplicação tenha excedido a cota do provedor.

## Interpretação do log

- `limite=400`: quantidade máxima de processos aceitos na amostra, não quantidade de requisições.
- `pagina=1, tentativa=1`: a primeira página falhou na primeira tentativa.
- `httpExterno=429, repetir=false`: a API externa recusou a chamada; o serviço não executou retry nessa ocorrência.
- `duracaoMs=15426`: aproximadamente 15,4 segundos aguardando essa chamada.
- As mensagens seguintes tratam a mesma falha em camadas diferentes; não representam novas consultas.
- A inicialização do `DispatcherServlet` é uma mensagem normal e não explica a recusa externa.

Não é possível estabelecer o consumo total anterior, a origem exata da recusa ou o período de bloqueio a partir desse trecho. O log registra a classe Java `TooManyRequests`, mas não o tipo de erro estruturado do provedor nem seu eventual `Retry-After`.

## Achados e evidências

### 1. A paginação não tem teto de requisições quando há descarte local de datas

Referência: `backend/src/main/java/br/com/axionweb/backend/service/DataJudService.java:68`.

O `while` encerra por quantidade de processos aceitos, página incompleta, cursor inválido ou erro. Cada registro ainda passa por `dentroDoPeriodo`; registros descartados não contam para os 400. Enquanto o servidor retornar páginas completas com cursores crescentes, o código continua consultando sem um máximo próprio de páginas, registros examinados ou duração total.

O próprio código contempla respostas com datas fora do intervalo em consultas com apenas uma das datas preenchidas: os filtros alternativos ISO/compacto podem se sobrepor, e a conferência final ocorre localmente. A incidência real depende das representações e do mapeamento dos dados no provedor; não foi medida no DataJud real.

Há um agravante em `Math.min(100, limite - processos.size())`: ao aceitar 399 registros, todas as páginas seguintes passam a pedir apenas um registro. Cada registro rejeitado pode consumir outra chamada.

Reprodução por HTTP local usando o serviço e a configuração reais do cliente: quatro páginas iniciais forneceram 399 registros válidos e um fora do período; dez páginas subsequentes forneceram um registro inválido cada; a última veio vazia. Resultado: **15 requisições para 399 registros aceitos**. Não houve retry nem cursor repetido. A quantidade de páginas inválidas poderia continuar maior.

Esse problema não explica diretamente o log informado, pois nele `inicio=null` e `fim=null`, e a falha aconteceu antes de qualquer página bem-sucedida.

### 2. O controle de execuções não impede duplicações simultâneas

Referência: `backend/src/main/java/br/com/axionweb/backend/service/ExecucaoAnaliseService.java:16`.

`ativas.merge(id, 1, Integer::sum)` apenas contabiliza operações. A ação é executada mesmo se o processo já estiver em análise. A trava do banco em `AnaliseService` ocorre depois das chamadas externas e protege a gravação, sem evitar consultas duplicadas.

Reprodução local: duas ações do mesmo processo entraram simultaneamente em `executar`. Ambas concluíram; o contador foi limpo normalmente. Portanto, o contador funciona como informação para o dashboard, não como exclusão mútua.

No frontend, a trava `pedido.current` pertence à montagem da aba DataJud. Sair da aba desmonta o componente e aborta o `fetch`; retornar cria outra trava vazia. Não há mecanismo no serviço síncrono que propague esse cancelamento para encerrar a busca externa. Assim, navegar e solicitar novamente pode sobrepor trabalho ainda em andamento. Essa sequência de navegação foi identificada por leitura do código; não foi executada em navegador nesta investigação.

### 3. Não há pausa compartilhada após 429 nem limite global de consumo

Referências: `DataJudService.java:91`, `ExecucaoAnaliseService.java:16` e `frontend/src/components/processos/detalhes/DataJudTab.tsx:46`.

O serviço corretamente não repete automaticamente um HTTP 429. Porém, não armazena uma janela de espera nem consulta `Retry-After`. Ao terminar o erro, o frontend libera o botão. Outra ação do usuário chega novamente ao provedor, inclusive se a anterior acabou de ser limitada.

Reprodução por HTTP local: o servidor respondeu 429 e `Retry-After: 60`. Duas chamadas consecutivas a `buscarProcessos` produziram duas requisições, sem aplicar a espera informada.

Não foi encontrado controle global que coordene usuários, processos e a consulta automática de CNJ. A consulta por CNJ usa o mesmo cliente externo e também precisaria participar de um controle compartilhado de consumo.

### 4. Consultas idênticas são refeitas integralmente

Referência: `backend/src/main/java/br/com/axionweb/backend/service/AnaliseService.java:131`.

Cada solicitação de geração chama novamente `buscarProcessos`, sem reutilização por filtros nem agrupamento de consultas iguais em andamento. Há persistência de estatísticas, mas ela não funciona como cache da geração. Processos cadastrados com os mesmos tribunal, classe, grau e período podem disparar buscas externas equivalentes.

Reprodução por HTTP local: uma amostra de 400 registros fez quatro chamadas de 100; repetir os mesmos parâmetros elevou o total a oito chamadas.

### 5. As páginas de 100 e os retries ampliam o custo, mas não indicam loop por si só

Referências: `DataJudService.java:38` e `DataJudService.java:102`.

Sem descarte local, uma amostra completa de 400 registros exige quatro chamadas sequenciais. Havendo falhas de rede ou HTTP 502/503/504, cada página admite até três tentativas, com esperas de 500 ms e 1.000 ms. Quatro páginas podem consumir até 12 chamadas quando cada uma precisa das três tentativas. O HTTP 429 não participa dessa repetição.

Aumentar o tamanho da página pode reduzir chamadas, mas amplia a resposta com movimentações e pode agravar latência e pressão de memória. A decisão exige medir tamanho e tempo de resposta; não é recomendável trocar automaticamente 100 por 400 como única correção.

## Comportamentos que não mostraram consultas externas desnecessárias

- Abrir a aba DataJud consulta estatísticas persistidas; a geração depende do envio do formulário.
- O botão de geração tem proteção contra cliques repetidos durante a mesma montagem.
- O cliente `apiRequest` faz um `fetch`, sem retry automático.
- O `StrictMode` pode reexecutar efeitos de leitura em desenvolvimento, mas a geração não está em um efeito.
- A busca automática de CNJ valida o número, espera 600 ms e mantém cache local durante a montagem do formulário; o callback de preenchimento é estável.
- O dashboard atualiza leituras internas, sem chamar o DataJud.
- O cálculo das estatísticas usa as movimentações já recebidas; não há uma consulta externa por processo ou por movimento.
- A síntese com IA usa as estatísticas persistidas, sem consultar novamente o DataJud.

A rota legada `GET /api/dados/analisar/{id}` ainda executa consultas externas e grava dados. Ela não é usada pelo frontend atual para solicitar análise, mas merece remoção ou migração controlada para evitar que uma leitura aparente dispare trabalho caro.

## O que o HTTP 429 permite concluir

O [termo de uso publicado pelo CNJ, versão 1.2](https://formularios.cnj.jus.br/wp-content/uploads/2023/11/Termos-de-uso-api-publica-V1.2.pdf), itens 3.13 e 3.14, estabelece 120 requisições por minuto e prevê limitações adicionais por usuário ou chave. Esse valor não garante que todas as chamadas abaixo dele serão aceitas. O conteúdo foi consultado pelo índice de busca; a abertura direta do PDF retornou 403 nesta sessão.

O Elasticsearch também usa 429 para rejeitar operações por saturação e proteção de memória, conforme sua documentação de [requisições rejeitadas](https://www.elastic.co/docs/troubleshoot/elasticsearch/rejected-requests) e [circuit breakers](https://www.elastic.co/docs/troubleshoot/elasticsearch/circuit-breaker-errors). Essa é uma causa alternativa possível para o DataJud, não uma causa confirmada para a ocorrência analisada.

A mensagem atual do frontend transforma qualquer 429 do DataJud em "atingiu o limite de consultas". Ela não diferencia limitação de frequência de uma possível rejeição por capacidade. Registrar apenas campos permitidos do erro estruturado, `Retry-After` e identificadores de correlação ajudaria nessa distinção, sem registrar credenciais ou o corpo completo da resposta.

## Verificação executada

1. `backend/mvnw.cmd -o -Dtest=ConsultaDataJudTests,AnaliseConsultasHttpIntegrationTests test`: **61 testes, zero falhas, zero erros** (33 do serviço e 28 da integração). As verificações incluem paginação, retries, tratamento de 429 e preservação dos resultados anteriores.
2. Reprodução adicional com um servidor HTTP em `127.0.0.1`, usando `DataJudService` e `RestComunicacaoConfig`, para contagem de chamadas, repetição dos mesmos filtros, descartes por datas e `Retry-After`.
3. Reprodução concorrente de `ExecucaoAnaliseService` com duas ações do mesmo processo.

| Cenário local | Chamadas externas simuladas | Resultado |
| --- | ---: | --- |
| 400 registros aceitos, sem datas | 4 | Amostra completa |
| Mesma consulta executada duas vezes | 8 no total | Sem reutilização |
| Descarte por datas perto de completar a amostra | 15 | 399 aceitos; páginas finais com tamanho 1 |
| Duas solicitações com resposta 429 e Retry-After de 60 s | 2 | Sem retry automático; sem pausa entre solicitações |
| Duas ações simultâneas do mesmo processo | Não se aplica | Ambas entram no controle de execução |

O programa temporário de reprodução está em `backend/target/DataJudRequestAudit.java`, diretório ignorado pelo Git e removível por `mvn clean`. As chamadas HTTP foram restritas ao servidor local. Nenhuma consulta de processo foi enviada ao DataJud real. Os testes não estabelecem a disponibilidade atual ou a cota efetiva do provedor.

O Postman CLI não iniciou por erro ao ler um manifesto gerenciado de Private Cloud. A consulta pública ao Orbit falhou por erro de conexão. Por isso, a investigação usou código local, testes existentes, reprodução HTTP local e documentação oficial, sem buscas autenticadas no workspace Postman e sem uma simulação Postman.

## Ordem recomendada de correção

1. Limitar páginas, registros examinados e duração total por geração; manter páginas de coleta com tamanho mínimo adequado mesmo quando faltar um registro aceito. Se o orçamento terminar, informar explicitamente a condição e preservar os dados anteriores; não salvar uma amostra parcial como se a coleta tivesse sido concluída.
2. Impedir ou agrupar execuções iguais em andamento antes da primeira chamada externa. Incluir a geração DataJud e a análise completa; coordenar instâncias se houver mais de uma.
3. Aplicar pausa após 429, respeitar Retry-After quando presente e centralizar o controle de frequência e concorrência para todas as entradas DataJud, inclusive CNJ. Refletir a espera no frontend.
4. Reutilizar consultas por filtros normalizados com validade explícita e autorização do processo preservada. Permitir atualização deliberada sem repetir a consulta a cada tentativa idêntica.
5. Instrumentar quantidade de páginas, tentativas, registros recebidos/aceitos/descartados, tempo total e tipo estruturado da falha. Medir o consumo agregado na janela do erro antes de atribuir a recusa à aplicação ou ao provedor.

As causas de amplificação foram confirmadas nos cenários locais descritos. A causa específica do 429 das 23:21:28 permanece indeterminada sem o histórico agregado de chamadas e metadados adicionais da resposta externa.
