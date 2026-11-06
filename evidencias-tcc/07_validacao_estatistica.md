# Validação estatística

EstatisticaProcessualServiceTests possui 17 invocações na execução final, todas aprovadas. Os valores esperados são literais definidos independentemente do serviço.

| Conjunto controlado (dias) | Cálculo independente | Esperado/observado |
| --- | --- | --- |
| 0,10,20,30 | (10+20)/2 | 15 / 15; julgamento e baixa |
| 1,20,2 → ordenado 1,2,20 | Elemento central | 2 / 2; julgamento e baixa |
| 1,2 | (1+2)/2 | 1,5 / 1,5 |
| Evento após 12 horas | Dias completos = 0 | 0 / 0; não confundir com null |
| Cinco registros; somente um com duração válida em cada marco | Julgamento 10 dias; baixa 20 dias | Total 5; sem baixa 3; válidos 1/1; medianas 10/20 |
| Sem registros | Não há mediana definida | Contagens 0; medianas null |
| Ajuizamento inválido, baixa com data válida | Baixa identificada mas duração não calculável | Não contar como sem baixa; excluir da duração |
| Eventos fora de ordem, antes e depois do início | Escolher primeiro evento consistente | Julgamento 2; baixa 3 |

Datas compactas interpretadas em UTC; datas ISO normalizadas por instante; durações em dias completos. Reconhecimento por nomes: Provimento, Não-Provimento, Provimento em Parte, Homologação de Transação; baixa: Baixa Definitiva. Isso limita a validade externa: taxonomias e outros movimentos reais não foram aferidos.

Não há média calculada na implementação atual. Colunas legadas não são evidência de cálculo novo. A contagem processosPendentes representa ausência de baixa identificada, mesmo quando não há dados suficientes para determinar situação jurídica. As subamostras podem sobrepor-se.

Evidências: XML EstatisticaProcessualServiceTests e testes integrados AnaliseConsultasHttpIntegrationTests; o fluxo de fila do navegador verifica 400 registros e preservação após falha.
