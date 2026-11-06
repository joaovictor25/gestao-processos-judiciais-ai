# Gemini — estatísticas

Cinco entradas controladas preparadas no corpus: medianas distintas/fracionárias, dados indisponíveis, zero válido, amostra de 400 e amostra vazia. Esta última é controle negativo que deve ser recusado antes do modelo. Nenhuma resposta real ou nota humana disponível.

Prompt em prompt-estatisticas.txt. Apenas quantidadeProcessos, processosPendentes, tempoAtePrimeiroJulgamento e tempoAtePrimeiraBaixa são enviados. Configuração declarada: gemini-3.5-flash-lite; temperature 0; maxOutputTokens 8192; JSON/schema; store=false.

18 invocações existentes do validador foram executadas. Os testes incluem número não fornecido, mediana trocada, formato, previsão, percentual, recomendação e indisponibilidade. As integrações verificam retorno inválido/vazio/truncado e preservação da síntese anterior.

Critérios futuros: preservação numérica e associação ao marco; conceito de mediana; interpretação de contagens; limitações; ausência de previsão e informação inventada; clareza. Avaliar com a rubrica e registrar divergências individualmente. O validador baseado em padrões não verifica toda a linguagem natural nem obriga que todos os números apareçam; não equivale a uma medida de qualidade do Gemini. Não há percentuais de acurácia calculados.
