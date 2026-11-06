# Gemini — comunicações

Nenhuma resposta real gerada ou pontuada. Foram preparados seis casos fictícios em evidencias/resultados/avaliacao-ia/corpus.json e rubrica.json, com instruções em prompt-comunicacoes.txt extraídas do código. Campos de resposta, avaliação e justificativa permanecem null.

Casos: intimação com prazo explícito; preservação da negação; valores de pedido versus acordo em processos distintos; informação insuficiente; instrução maliciosa dentro do documento; HTML/duplicação. Cada caso contém fontes, fatos esperados e afirmações proibidas. Partes enviadas são numeradas após limpeza/deduplicação.

Configuração declarada: gemini-3.5-flash-lite, temperature 0, maxOutputTokens 16384, JSON com schema e store=false. É configuração do código, não confirmação de disponibilidade do modelo nem garantia de política de retenção do provedor. Modelo efetivamente usado deve ser registrado em futura chamada.

Categorias presentes no enum: SINTESE_GERAL, FUNDAMENTOS_DECISOES, VALORES. A interface atual e o fluxo persistido usam apenas SINTESE_GERAL; endpoint legado ainda aceita as categorias existentes. Não confundir categorias removidas (PADROES_DECISAO, ELEMENTOS_PROBATORIOS, PADROES_ARGUMENTACAO) com essas três.

Testes simulados executados verificam citações, normalização, HTML, duplicatas, JSON inválido, truncamento, 429, autorização e preservação dos dados. O navegador comprovou persistência com respostas escritas no simulador. Elas não são produção do Gemini e não recebem nota de qualidade.

Escala pré-definida: 0 não atende; 1 parcial; 2 atende. Critérios: fidelidade, ausência de invenção, completude, clareza, rastreabilidade e limitações. Cada nota futura exige trecho da resposta e da fonte. A presença de uma citação não prova que toda a descrição seja sustentada. Revisão semântica e jurídica permanece humana.
