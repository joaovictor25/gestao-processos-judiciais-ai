// Fixtures exclusivas da verificação no navegador. Não são importadas pelo frontend.
export function fixturesDetalhes(processoId) {
  const tempo = { quantidade: 40, medianaDias: 119 };
  return {
    estatisticas: [{ id: 100, processoId, quantidadeProcessos: 61, processosPendentes: 21,
      executadaEm: '2026-11-03T12:00:00Z', statusSintese: 'SEM_SINTESE', sintese: null,
      parametros: { tribunal: 'TJPR', classeCodigo: '460', grau: 'G1', limiteResultados: 400, dataInicio: null, dataFim: null },
      tempoAtePrimeiroJulgamento: { ...tempo }, tempoAtePrimeiraBaixa: { ...tempo, medianaDias: 168 } }],
    comunicacoes: Array.from({ length: 23 }, (_, index) => ({ id: index + 1, processoId, codigoClasse: '460', dataDisponibilizacao: '2026-09-23', dataDisponibilizacaoFormatada: '23/09/2026',
      idOrgao: 72197, meio: 'DJEN', meioCompleto: 'Diário de Justiça Eletrônico Nacional', siglaTribunal: 'TJPR', status: 'Publicado',
      tipoComunicacao: ['Intimação', 'Decisão', 'Despacho', 'Edital'][index % 4], tipoDocumento: 'Intimação',
      texto: index === 0 ? '<p>Intime-se a parte recorrida para apresentação de contrarrazões no prazo legal.</p><script>window.conteudoInseguro = true</script><img src=x onerror="window.conteudoInseguro=true"><p>Após o decurso do prazo, remetam-se os autos conclusos para julgamento.</p>' : `Comunicação ${index + 1}: manifestação sobre o recurso. ${'Texto integral preservado para consulta. '.repeat(30)}` })),
  };
}
