# Testes funcionais

Linha de base: 312 invocações JUnit aprovadas. Execução ampliada: 322, sendo 321 aprovadas e 1 reprovadas. Casos parametrizados contam por invocação; não são requisitos distintos.

| Classe | Invocações | Reprovadas |
| --- | --- | --- |
| BackendApplicationTests.java | 1 | 0 |
| AutenticacaoIsolamentoIntegrationTests.java | 53 | 0 |
| H2AcessoLocalIntegrationTests.java | 2 | 0 |
| AnaliseComunicacaoIntegrationTests.java | 48 | 0 |
| AnaliseConsultasHttpIntegrationTests.java | 28 | 0 |
| AnaliseProcessoIntegrationTests.java | 15 | 0 |
| ComunicacaoContratoTccTests.java | 10 | 1 |
| ConsultaCnjIntegrationTests.java | 12 | 0 |
| ConsultaDataJudTests.java | 34 | 0 |
| DashboardIntegrationTests.java | 3 | 0 |
| DataJudFilaHttpIntegrationTests.java | 17 | 0 |
| EstatisticaProcessualServiceTests.java | 17 | 0 |
| MigracaoSintesesTests.java | 2 | 0 |
| NumeroCnjTests.java | 35 | 0 |
| SinteseDataJudIntegrationTests.java | 5 | 0 |
| TextoComunicacaoTests.java | 22 | 0 |
| ValidadorSinteseDataJudTests.java | 18 | 0 |

Inventário completo de métodos e parâmetros: evidencias/resultados/inventario-testes.json. Resultados individuais: CSV e XML final. XML baseline preservado separadamente. Foram removidas apenas propriedades de ambiente e saídas de console dos XML compartilháveis; testcase/status/duração/falha foram mantidos.

Novo arquivo Java: ComunicacaoContratoTccTests.java, dez invocações: parâmetros e codificação, paginação explícita sem busca automática, ausência de datas, resposta inválida (3), HTTP 429/500/503 (3), transporte (1), item nulo (1). Nove aprovadas, item nulo reprovado. Dois testes novos Node verificam padrões de exposição no frontend. O script de fila foi ajustado para aguardar o documento novo após reload. Nenhuma lógica de produção foi alterada.

Navegador: 44 checkpoints únicos concluídos; 59 registros antes de remover repetições de autenticação. Evidência por fluxo em browser-*/browser-check.json. Capturas são auxiliares; asserções são a evidência funcional. Não há teste manual nem API real.

Também foi corrigida a espera de reload em exclusao-scenarios.mjs: o teste aguardava elementos do documento antigo. A primeira correção não tratava tabela vazia; a segunda usa marcador no documento e passou. Logs das três tentativas preservados. Nenhum componente de produção foi modificado.
