# Resultados quantitativos

| Indicador | Valor |
| --- | --- |
| planejados | 385 |
| executados | 370 |
| aprovados | 367 |
| reprovados | 3 |
| naoExecutados | 15 |

Requisitos identificados: 12. Backend: linha de base 312/312 aprovada; execução ampliada 321/322, com 1 falha. Frontend: 2 testes de padrões de segurança e 44 checkpoints únicos de navegador aprovados. O fluxo interrompido, se presente no CSV, é contado separadamente como reprovado. Build é verificação auxiliar, não somado como cenário funcional.

A contagem de testes é de invocações/checkpoints definidos, não de asserções nem percentual de cobertura de código. A linha de base não é somada novamente ao total final. Repetições de login nos fluxos do navegador foram deduplicadas. Casos IA preparados e pendências explícitas integram não executados.

Defeito funcional backend reproduzido: 1 (item nulo DJEN). Achados por inspeção: credencial Gemini embutida e ausência de timeout DJEN explícito (2); Authorization DataJud literal é observação de configuração, sem afirmar segredo privado. Problemas do executor/ambiente são registrados separadamente, sem inflar falhas do produto.

Correção de teste: sincronização com novo documento após reload. Correção de ambiente: pausa 429/worker/cache explícitos no servidor isolado; build e Node liberados para subprocessos. Correções de produção: 0. Resultados originais preservados. IA real: 0 chamadas, 0 respostas, 0 avaliações humanas; não há medida observada de qualidade do Gemini.

Fonte calculável: evidencias/resultados/resumo.json e matriz CSV.

## Oito grupos solicitados

| Grupo principal | Aprovados | Reprovados | Não executados |
| --- | --- | --- | --- |
| Complementares: ambiente, migração e dashboard | 6 | 0 | 1 |
| 1 — Autenticação e segurança | 74 | 1 | 0 |
| 2 — Gerenciamento de processos | 30 | 0 | 0 |
| 3 — DataJud | 127 | 0 | 1 |
| 4 — Estatística | 17 | 0 | 0 |
| 5 — DJEN | 42 | 2 | 1 |
| 6 — Síntese de comunicações | 12 | 0 | 6 |
| 7 — Explicação estatística | 27 | 0 | 5 |
| 8 — Persistência | 32 | 0 | 1 |

Classificação pela exigência principal da matriz, sem duplicar cenários transversais. Grupos 6 e 7 aprovados referem-se a respostas simuladas; a qualidade do Gemini real continua não avaliada. Inspeções estáticas S01/S02 estão nos grupos 1/5 e não são falhas JUnit.
