# Refatoração das estatísticas DataJud — AXION

## Resultado

A aba apresenta somente Primeiro julgamento, Primeira baixa e Processos pendentes, além do total da amostra. Os cartões de tempo exibem mediana em dias e quantidade de processos com duração válida. A ausência de mediana aparece como **Indisponível**; zero continua sendo um tempo válido.

O formulário mantém as duas datas opcionais de ajuizamento e o botão Gerar estatística. A geração e recuperação de estatísticas mantêm as rotas e o vínculo ao processo. Foram preservados cadastro, edição, consultas CNJ, filtros internos do DataJud e análise de comunicações DJEN.

## Remoções e regras mantidas

Foram removidos média, mínimo, máximo, amplitude, desvio padrão, percentis 25/50/75, duração dos pendentes e contadores redundantes de eventos dos cálculos, DTOs, modelo JPA e tipos do frontend. Também foram removidos o botão Ver todas as estatísticas, os diálogos de medidas, a janela acessória de parâmetros e seus estados, chamadas e estilos sem uso. O endpoint de parâmetros permanece disponível.

O backend calcula diretamente a mediana de durações ordenadas. A identificação de julgamento/baixa, os formatos de data, a normalização UTC, a rejeição de datas inconsistentes e a seleção do primeiro evento cronologicamente válido foram preservados. Uma baixa identificada com data válida impede a classificação como pendente mesmo quando a duração não pode ser calculada. As quantidades de cada mediana contam somente durações calculáveis.

## Persistência e Gemini

Os nomes e tipos das colunas mantidas não mudaram. Não foi necessária migração de esquema. Colunas obsoletas podem permanecer no banco, sem leitura ou escrita pelo novo modelo; nenhum dado de produção foi acessado ou removido. Registros legados sem subamostra válida retornam mediana indisponível. A compatibilidade foi testada com colunas antigas adicionais em H2.

O Gemini recebe uma lista explícita de indicadores: total, pendentes, mediana de julgamento com quantidade válida e mediana de baixa com quantidade válida. Identificadores do processo, dados pessoais e comunicações não são enviados nessa síntese. O prompt exige explicações desses resultados e limitações, sem recalcular, gerar outros indicadores ou prever duração/êxito. A síntese continua temporária; os resultados estatísticos permanecem persistidos. SinteseDataJudService foi revisado e continua usando a estatística salva mais recente, com conferência do proprietário e do identificador.

## Arquivos desta refatoração

Backend de produção:

- [DataJudResponseDTO.java](backend/src/main/java/br/com/axionweb/backend/dto/datajud/DataJudResponseDTO.java)
- [EstatisticasProcessuaisDTO.java](backend/src/main/java/br/com/axionweb/backend/dto/datajud/EstatisticasProcessuaisDTO.java)
- [TempoEstatisticaDTO.java](backend/src/main/java/br/com/axionweb/backend/dto/datajud/TempoEstatisticaDTO.java)
- [DataJud.java](backend/src/main/java/br/com/axionweb/backend/model/datajud/DataJud.java)
- [TempoEstatistica.java](backend/src/main/java/br/com/axionweb/backend/model/datajud/TempoEstatistica.java)
- [EstatisticaProcessualService.java](backend/src/main/java/br/com/axionweb/backend/service/EstatisticaProcessualService.java)
- [AnaliseService.java](backend/src/main/java/br/com/axionweb/backend/service/AnaliseService.java)
- [DataJudService.java](backend/src/main/java/br/com/axionweb/backend/service/DataJudService.java)
- [GeminiService.java](backend/src/main/java/br/com/axionweb/backend/service/GeminiService.java)

Frontend de produção:

- [DataJudTab.tsx](frontend/src/components/processos/detalhes/DataJudTab.tsx)
- [EstatisticaCard.tsx](frontend/src/components/processos/detalhes/EstatisticaCard.tsx)
- [AISummaryCard.tsx](frontend/src/components/processos/detalhes/AISummaryCard.tsx)
- [estatisticas.ts](frontend/src/types/estatisticas.ts)
- [processoService.ts](frontend/src/services/processoService.ts)
- [ProcessoDetalhes.css](frontend/src/pages/Processos/ProcessoDetalhes.css)

Removido: `frontend/src/components/processos/detalhes/ParametrosDataJud.tsx`.

Testes e cenários:

- [EstatisticaProcessualServiceTests.java](backend/src/test/java/br/com/axionweb/backend/service/EstatisticaProcessualServiceTests.java)
- [AnaliseConsultasHttpIntegrationTests.java](backend/src/test/java/br/com/axionweb/backend/service/AnaliseConsultasHttpIntegrationTests.java)
- [AnaliseProcessoIntegrationTests.java](backend/src/test/java/br/com/axionweb/backend/service/AnaliseProcessoIntegrationTests.java)
- [AnaliseComunicacaoIntegrationTests.java](backend/src/test/java/br/com/axionweb/backend/service/AnaliseComunicacaoIntegrationTests.java)
- [ConsultaDataJudTests.java](backend/src/test/java/br/com/axionweb/backend/service/ConsultaDataJudTests.java)
- [DashboardIntegrationTests.java](backend/src/test/java/br/com/axionweb/backend/service/DashboardIntegrationTests.java)
- [AutenticacaoIsolamentoIntegrationTests.java](backend/src/test/java/br/com/axionweb/backend/security/AutenticacaoIsolamentoIntegrationTests.java)
- [detalhes-fixtures.mjs](frontend/scripts/detalhes-fixtures.mjs)
- [detalhes-scenarios.mjs](frontend/scripts/detalhes-scenarios.mjs)
- [datajud-http-scenario.mjs](frontend/scripts/datajud-http-scenario.mjs)

Documentação: [backend/README.md](backend/README.md), [frontend/README.md](frontend/README.md) e este relatório.

O repositório já continha alterações locais em outros arquivos antes desta tarefa; elas foram preservadas. O ajuste em DashboardIntegrationTests restringe-se à preparação de autenticação e à ordem das verificações, pois MockMvc limpa o contexto de segurança ao encerrar a requisição. Nenhuma regra do dashboard foi alterada.

## Validação

- Backend: `.\mvnw.cmd -o -B -ntp test` — compilação concluída e 262 testes aprovados, sem falhas, erros ou testes ignorados.
- Build do frontend: `npm.cmd run build` — TypeScript e Vite concluídos.
- Navegador: 34 verificações aprovadas, sem exceções JavaScript. Inclui autenticação, cadastro/edição, isolamento entre contas, DataJud, comunicações e tamanhos desktop, tablet e celular (390/320 px).
- Fluxo HTTP local: navegador e backend reais, H2 temporário e respostas DataJud/Gemini controladas em um servidor HTTP local. Verifica filtros, cálculo, contrato exato, gravação, recuperação após reload, payload do Gemini e preservação após consulta vazia.
- Cobertura Java: medianas pares, ímpares, fracionárias e zero; amostras vazias; datas ausentes/inválidas; movimentos repetidos e fora de ordem; contagens; persistência; registros legados; endpoints; permissões; falhas externas; conteúdo enviado à IA.
- Evidências locais: [log do Maven](backend/target/datajud-refactor-tests-final.log), [log do navegador](frontend/artifacts/datajud-browser-check.log), [captura desktop](frontend/artifacts/detalhes-datajud-desktop.png) e [captura mobile](frontend/artifacts/detalhes-datajud-small-mobile.png). Logs e capturas estão em diretórios ignorados pelo Git.

## Limites da validação

MySQL de produção e os serviços públicos DataJud/Gemini não foram utilizados; a evidência de integração é local com H2 e respostas controladas. A resposta textual real do modelo depende do provedor e das instruções enviadas. O CLI Postman não iniciou devido a uma configuração gerenciada da máquina; a validação foi realizada com a suíte existente e o cenário HTTP local. Os servidores temporários foram encerrados ao final da verificação.
