// Fixtures exclusivas da verificação no navegador. Não são importadas pelo frontend.
export function fixturesDetalhes(processoId) {
  const tempo = { quantidade: 40, mediaDias: 130, medianaDias: 119, minimoDias: 0, maximoDias: 280, amplitudeDias: 280, desvioPadraoDias: 50, percentil25Dias: 80, percentil50Dias: 119, percentil75Dias: 170 };
  return {
    estatisticas: [{ id: 100, processoId, quantidadeProcessos: 61, processosBaixados: 40, processosPendentes: 21, processosComJulgamento: 40,
      tempoAtePrimeiroJulgamento: { ...tempo }, tempoAtePrimeiraBaixa: { ...tempo, medianaDias: 168 }, tempoPendente: { ...tempo, quantidade: 21 } }],
    comunicacoes: Array.from({ length: 23 }, (_, index) => ({ id: index + 1, processoId, codigoClasse: '460', dataDisponibilizacao: '2026-09-23', dataDisponibilizacaoFormatada: '23/09/2026',
      idOrgao: 72197, meio: 'DJEN', meioCompleto: 'Diário de Justiça Eletrônico Nacional', siglaTribunal: 'TJPR', status: 'Publicado',
      tipoComunicacao: ['Intimação', 'Decisão', 'Despacho', 'Edital'][index % 4], tipoDocumento: 'Intimação',
      texto: index === 0 ? '<p>Intime-se a parte recorrida para apresentação de contrarrazões no prazo legal.</p><script>window.conteudoInseguro = true</script><img src=x onerror="window.conteudoInseguro=true"><p>Após o decurso do prazo, remetam-se os autos conclusos para julgamento.</p>' : `Comunicação ${index + 1}: manifestação sobre o recurso. ${'Texto integral preservado para consulta. '.repeat(30)}` })),
  };
}
