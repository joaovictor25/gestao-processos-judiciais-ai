# Limitações e pendências

- APIs reais CNJ/Gemini não chamadas; disponibilidade, contrato externo atual e qualidade do modelo não demonstrados.
- H2 em memória não substitui teste MySQL nem comprova durabilidade após reiniciar o servidor. Reload de página verifica recuperação via API no mesmo servidor.
- Amostras artificiais e pequenas não são representativas da população judicial. Sem estimativa de acurácia jurídica, previsão ou similaridade semântica.
- DataJud limitado a 400, DJEN primeira página de 80; não há coleta automática de todas as páginas. Paginação explícita do cliente DJEN não é fluxo completo na interface.
- Validação automática de IA limita-se a formato, padrões numéricos/citações. Completude e fidelidade semântica dependem de avaliação humana ainda não realizada.
- Credencial embutida e timeout DJEN devem ser tratados separadamente; produção preservada. Teste DJEN de item nulo permanece reprovado.
- Postman CLI indisponível por configuração local de Private Cloud; foram usados simuladores já disponíveis.
- Nenhum teste manual, carga, pentest avançado, matriz extensa de navegadores, comparação de modelos ou certificação LGPD foi realizado.
- Checkout contém alterações anteriores; é necessário o mesmo estado dos arquivos, não apenas o commit base, para reproduzir. Manifesto registra hashes sem divulgar conteúdo de configuração.
- Tentativas originais do navegador e compilação foram preservadas. Falha de espera/seleção no script não comprova por si só erro do produto. Consulte logs e o registro de execuções antes de citar resultados.

Pendências específicas aparecem no CSV. Não foram inventadas notas, resultados de APIs reais ou aprovações manuais.
