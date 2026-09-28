import assert from 'node:assert/strict';
import { fixturesDetalhes } from './detalhes-fixtures.mjs';

export async function verificarDetalhes({ origin, command, evaluate, waitFor, fill, click, pathIs, viewport, screenshot, checks, listen }) {
  const token = await evaluate('sessionStorage.getItem("axion.auth.token")');
  const response = await fetch(`${origin}/api/processo/listar-usuario`, { headers: { Authorization: `Bearer ${token}` } });
  assert.equal(response.status, 200);
  const [processo] = await response.json();
  const path = `/processos/${processo.id}`;
  const go = async url => { await command('Page.navigate', { url: `${origin}${url}` }); await pathIs(url); };
  await viewport(1672, 1000);
  await go(path);
  await waitFor('document.querySelector(".general-details-grid")');
  assert.equal(await evaluate('document.querySelector("[role=tab][aria-selected=true]").textContent'), 'Informações gerais');
  assert.equal(await evaluate('document.querySelectorAll(".general-details-main > section").length'), 4);
  assert.equal(await evaluate('document.querySelector(".details-identity h1").textContent'), processo.numeroProcesso);
  assert.equal(await evaluate('document.querySelector(".sidebar-nav a.active").textContent'), 'Processos');
  assert.equal(await evaluate('document.querySelector(".details-header-actions button")'), null);
  await screenshot('detalhes-gerais-desktop');
  await viewport(390, 844);
  await screenshot('detalhes-gerais-mobile');
  await viewport(320, 720);
  await screenshot('detalhes-gerais-small-mobile');
  await viewport(1672, 1000);
  await click('#tab-datajud');
  await waitFor('document.body.textContent.includes("Nenhuma análise DataJud foi realizada")');
  await screenshot('detalhes-datajud-vazio');
  await click('#tab-comunicacoes');
  await waitFor('document.body.textContent.includes("Nenhuma comunicação foi encontrada")');
  assert.equal(await evaluate('document.querySelector(".communication-ai-options .button").disabled'), true);
  assert.equal(await evaluate('location.pathname'), path);
  checks.push('Detalhes reais: quatro blocos, mesma rota nas três abas e estados vazios da API');

  const fixtures = fixturesDetalhes(processo.id);
  let erroEstatisticas = false;
  let erroComunicacoes = false;
  let pendente;
  let analises = 0;
  let estatisticasLidas = 0;
  let pedidoIA;
  let chamadasIA = 0;
  let pedidoPesquisa;
  let corpoPesquisa;
  let corpoIA;
  let pesquisas = 0;
  const failures = [];
  const remover = listen(message => {
    if (message.method !== 'Fetch.requestPaused') return;
    const { requestId, request } = message.params;
    const responder = async () => {
      if (request.url.includes('/api/dados/analisar/')) { analises++; pendente = requestId; return; }
      if (request.url.includes('/pesquisar')) { pesquisas++; pedidoPesquisa = requestId; corpoPesquisa = JSON.parse(request.postData); return; }
      if (request.url.includes('/analise-ia')) { chamadasIA++; pedidoIA = requestId; corpoIA = JSON.parse(request.postData); return; }
      const stats = request.url.includes('/api/dados/estatisticas/');
      if (stats) estatisticasLidas++;
      const erro = stats ? erroEstatisticas : erroComunicacoes;
      await command('Fetch.fulfillRequest', { requestId, responseCode: erro ? 500 : 200, responseHeaders: [{ name: 'Content-Type', value: 'application/json' }], body: Buffer.from(JSON.stringify(erro ? {} : stats ? fixtures.estatisticas : fixtures.comunicacoes)).toString('base64') });
    };
    void responder().catch(error => { if (!error.message.includes('Invalid InterceptionId')) failures.push(error.message); });
  });
  try {
    await command('Fetch.enable', { patterns: [{ urlPattern: '*/api/dados/estatisticas/*' }, { urlPattern: '*/api/dados/comunicacoes/*' }, { urlPattern: '*/api/dados/analisar/*' }] });
    await click('#tab-datajud');
    await waitFor('document.querySelectorAll(".statistic-card").length === 3');
    assert.deepEqual(await evaluate('[...document.querySelectorAll(".statistic-value")].map(node => node.textContent)'), ['119 dias', '168 dias', '21 processos']);
    assert.equal(await evaluate('document.querySelector(".ai-summary-action button").disabled'), true);
    await screenshot('detalhes-datajud-desktop');
    await click('.statistic-card .text-button');
    await waitFor('document.querySelector(".statistics-dialog").open');
    assert.equal(await evaluate('document.querySelectorAll(".statistics-measures > div").length'), 10);
    await screenshot('detalhes-estatisticas-modal');
    await command('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27 });
    await waitFor('!document.querySelector(".statistics-dialog").open');
    checks.push('DataJud com fixture: três indicadores e modal com todas as medidas; IA indisponível identificada');

    const anteriores = estatisticasLidas;
    await click('.datajud-refresh button');
    await waitFor('document.querySelector(".datajud-refresh button").disabled');
    await click('.datajud-refresh button');
    assert.equal(analises, 1);
    assert.ok(pendente);
    fixtures.estatisticas[0].tempoAtePrimeiroJulgamento.medianaDias = 0;
    fixtures.estatisticas[0].tempoAtePrimeiraBaixa.quantidade = 0;
    fixtures.estatisticas[0].processosPendentes = 0;
    await command('Fetch.fulfillRequest', { requestId: pendente, responseCode: 200, responseHeaders: [{ name: 'Content-Type', value: 'text/plain' }], body: Buffer.from('200').toString('base64') });
    pendente = undefined;
    await waitFor('document.querySelector(".statistic-value")?.textContent === "0 dias"');
    assert.ok(estatisticasLidas > anteriores);
    assert.deepEqual(await evaluate('[...document.querySelectorAll(".statistic-value")].map(node => node.textContent)'), ['0 dias', '—', '0 processos']);
    await click('.statistic-card:nth-child(2) .text-button');
    await waitFor('document.querySelector(".statistics-dialog").open');
    assert.equal(await evaluate('document.querySelectorAll(".statistics-measures dd")[2].textContent'), '—');
    await click('.statistics-dialog > .button');
    checks.push('Nova análise recarrega estatísticas; bloqueio duplicado; zero válido distinto de amostra ausente');
    await viewport(820, 1180);
    await screenshot('detalhes-datajud-tablet');
    await viewport(320, 720);
    await screenshot('detalhes-datajud-small-mobile');
    await viewport(1672, 1000);

    await click('.datajud-refresh button');
    await waitFor('document.querySelector(".datajud-refresh button").disabled');
    assert.ok(pendente);
    await command('Fetch.fulfillRequest', { requestId: pendente, responseCode: 502,
      responseHeaders: [{ name: 'Content-Type', value: 'application/json' }],
      body: Buffer.from(JSON.stringify({ codigo: 'CONSULTA_DJEN_FALHOU', mensagem: 'java.lang.Exception dado interno' })).toString('base64') });
    pendente = undefined;
    await waitFor('document.body.textContent.includes("A consulta de comunicações (DJEN) falhou")');
    assert.equal(await evaluate('document.body.textContent.includes("java.lang")'), false);
    assert.equal(await evaluate('document.querySelector(".statistic-value").textContent'), '0 dias');
    checks.push('Falha DJEN explica que a análise DataJud não foi salva, preserva resultados e oculta detalhes internos');
    await click('.datajud-refresh button');
    await waitFor('document.querySelector(".datajud-refresh button").disabled');
    assert.ok(pendente);
    await command('Fetch.fulfillRequest', { requestId: pendente, responseCode: 502,
      responseHeaders: [{ name: 'Content-Type', value: 'application/json' }],
      body: Buffer.from(JSON.stringify({ codigo: 'CONSULTA_DATAJUD_LIMITADA' })).toString('base64') });
    pendente = undefined;
    await waitFor('document.body.textContent.includes("O DataJud atingiu o limite de consultas (HTTP 429)")');
    assert.equal(await evaluate('document.querySelector(".statistic-value").textContent'), '0 dias');
    checks.push('Limite externo HTTP 429 recebe mensagem específica sem apagar indicadores existentes');

    erroEstatisticas = true;
    await click('#tab-geral'); await click('#tab-datajud');
    await waitFor('document.body.textContent.includes("Não foi possível carregar o DataJud")');
    erroEstatisticas = false;
    await click('.datajud-panel .process-state button');
    await waitFor('document.querySelectorAll(".statistic-card").length === 3');
    await click('#tab-comunicacoes');
    await waitFor('document.querySelectorAll(".communications-table tbody tr").length === 20');
    assert.equal(await evaluate('document.querySelector(".communication-ai-options .button").disabled'), true);
    assert.deepEqual(await evaluate('[...document.querySelectorAll(".communications-sidebar > * > h2")].map(item => item.textContent)'), ['Pesquisar por publicações', 'Análise das comunicações']);
    assert.equal(await evaluate('document.querySelectorAll(".communication-ai-options input[type=text], .communication-ai-options input[type=date]").length'), 0);
    assert.equal(await evaluate('document.querySelector(".publication-search .button").disabled'), true);
    await fill('publication-text', '   ');
    assert.equal(await evaluate('document.querySelector(".publication-search .button").disabled'), true);
    await fill('publication-text', '  contrato & indenização  ');
    await fill('publication-start', '2026-09-02');
    await fill('publication-end', '2026-09-01');
    assert.equal(await evaluate('document.querySelector(".publication-search .button").disabled'), true);
    await fill('publication-start', '2026-01-01');
    await click('.publication-search .button');
    await waitFor('document.querySelector(".publication-search .button").disabled');
    assert.equal(chamadasIA, 0);
    assert.equal(pesquisas, 1);
    assert.deepEqual(corpoPesquisa, { texto: 'contrato & indenização', dataDisponibilizacaoInicio: '2026-01-01', dataDisponibilizacaoFim: '2026-09-01' });
    await command('Fetch.fulfillRequest', { requestId: pedidoPesquisa, responseCode: 200,
      responseHeaders: [{ name: 'Content-Type', value: 'application/json' }], body: Buffer.from(JSON.stringify(fixtures.comunicacoes)).toString('base64') });
    pedidoPesquisa = undefined;
    await waitFor('document.querySelector(".communications-results h2")?.textContent === "Publicações da última pesquisa"');
    await screenshot('publicacoes-pesquisa-desktop');
    await viewport(320, 720);
    await screenshot('publicacoes-pesquisa-mobile');
    await viewport(1672, 1000);
    await click('.communication-ai-choice:first-of-type');
    await click('.communication-ai-choice:last-of-type');
    assert.equal(await evaluate('document.querySelectorAll(".communication-ai-choice input:checked").length'), 2);
    await click('.communication-ai-options .button');
    await waitFor('Boolean(document.querySelector(".communication-ai-options .button:disabled"))');
    assert.equal(chamadasIA, 1);
    assert.ok(pedidoIA);
    assert.equal(pesquisas, 1);
    assert.deepEqual(corpoIA, { tiposAnalise: ['SINTESE_GERAL', 'VALORES'], comunicacaoIds: fixtures.comunicacoes.map(item => item.id) });
    assert.equal(await evaluate('document.getElementById("publication-text").matches(":disabled")'), true);
    await command('Fetch.fulfillRequest', { requestId: pedidoIA, responseCode: 200,
      responseHeaders: [{ name: 'Content-Type', value: 'application/json' }], body: Buffer.from(JSON.stringify({
        comunicacoesConsideradas: 23, duplicadasIgnoradas: 0,
        comunicacoes: fixtures.comunicacoes.map(({ id, processoId, ...item }, indice) => ({ ...item, numero: indice + 1 })),
        categorias: [
          { tipo: 'SINTESE_GERAL', limitacao: null, itens: [{ titulo: 'Síntese do conjunto', descricao: 'Foram encontradas intimações.', contexto: null, decisaoRelacionada: null, evidencias: [{ comunicacao: 1, trecho: 'Intime-se a parte' }] }] },
          { tipo: 'VALORES', limitacao: 'Não foram identificadas informações suficientes nas comunicações analisadas para esta categoria.', itens: [] },
        ],
      })).toString('base64') });
    pedidoIA = undefined;
    await waitFor('document.querySelectorAll(".communication-ai-category").length === 2');
    assert.equal(await evaluate('document.querySelector(".communication-ai-item li").textContent.includes("Comunicação 1")'), true);
    assert.equal(await evaluate('document.querySelector(".communication-ai-disclaimer").textContent.includes("não constituem previsão")'), true);
    await click('.communication-ai-choice:last-of-type');
    await waitFor('!document.querySelector(".communication-ai-result")');
    checks.push('IA das comunicações: seleção múltipla, bloqueio durante envio, resultado por categoria, evidência e ausência de dados suficientes');
    checks.push('Pesquisa em caixa separada acima da IA; texto obrigatório, datas opcionais, busca sem acionar IA e análise sem repetir pesquisa; botão do topo removido');
    assert.deepEqual(await evaluate('[...document.querySelectorAll(".communications-table th")].map(node => node.textContent)'), ['Data', 'Tipo', 'Meio', 'Documento', 'Status', 'Texto', 'Ações']);
    assert.equal(await evaluate('document.querySelector(".pagination-count").textContent'), '1–20 de 23 comunicações');
    await click('button[aria-label="Página 2"]');
    assert.equal(await evaluate('document.querySelectorAll(".communications-table tbody tr").length'), 3);
    await click('.pagination-size button:nth-child(2)');
    assert.equal(await evaluate('document.querySelectorAll(".communications-table tbody tr").length'), 23);
    await fill('communication-search', 'contrarrazoes');
    assert.equal(await evaluate('document.querySelectorAll(".communications-table tbody tr").length'), 1);
    await click('.communication-view');
    await waitFor('document.querySelector(".communication-modal")?.open');
    assert.equal(await evaluate('document.activeElement.id'), 'communication-details-title');
    assert.equal(await evaluate('document.querySelector(".communication-full-text").textContent.includes("Após o decurso do prazo")'), true);
    assert.equal(await evaluate('Boolean(window.conteudoInseguro)'), false);
    assert.equal(await evaluate('document.querySelector(".communication-full-text img, .communication-full-text script")'), null);
    assert.equal(await evaluate('location.pathname'), path);
    await command('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27 });
    await waitFor('!document.querySelector(".communication-modal")');
    assert.equal(await evaluate('document.activeElement.classList.contains("communication-view")'), true);
    await fill('communication-search', '');
    await click('.pagination-size button:first-child');
    await click('.communication-view');
    await waitFor('document.querySelector(".communication-modal")?.open');
    await screenshot('detalhes-comunicacoes-desktop');
    await viewport(820, 1180);
    await screenshot('detalhes-comunicacoes-tablet');
    await viewport(390, 844);
    await screenshot('detalhes-comunicacoes-mobile');
    await viewport(320, 720);
    await screenshot('detalhes-comunicacoes-small-mobile');
    await click('.communication-modal > button');
    await waitFor('!document.querySelector(".communication-modal")');
    await viewport(1672, 1000);
    checks.push('Comunicações com fixtures: busca textual, paginação e modal seguro com fechamento por Esc e retorno de foco sem mudar rota');
    await fill('communication-search', 'sem-correspondencia-zz');
    await waitFor('document.body.textContent.includes("Nenhuma comunicação corresponde")');
    await fill('communication-search', '');
    await fill('publication-text', 'sem resultado');
    assert.equal(await evaluate('document.querySelector(".communication-ai-options .button").disabled'), true);
    await fill('publication-start', '');
    await fill('publication-end', '');
    await click('.publication-search .button');
    await waitFor('document.querySelector(".publication-search .button").disabled');
    assert.deepEqual(corpoPesquisa, { texto: 'sem resultado' });
    await command('Fetch.fulfillRequest', { requestId: pedidoPesquisa, responseCode: 200,
      responseHeaders: [{ name: 'Content-Type', value: 'application/json' }], body: Buffer.from('[]').toString('base64') });
    pedidoPesquisa = undefined;
    await waitFor('document.body.textContent.includes("Nenhuma publicação encontrada para os filtros informados")');
    assert.equal(await evaluate('document.querySelector(".communication-ai-options .button").disabled'), true);
    assert.equal(await evaluate('document.querySelectorAll(".communications-table tbody tr").length'), 0);
    await click('.publication-search .button');
    await waitFor('document.querySelector(".publication-search .button").disabled');
    await command('Fetch.fulfillRequest', { requestId: pedidoPesquisa, responseCode: 502,
      responseHeaders: [{ name: 'Content-Type', value: 'application/json' }], body: Buffer.from(JSON.stringify({ codigo: 'CONSULTA_DJEN_LIMITADA', mensagem: 'detalhe interno' })).toString('base64') });
    pedidoPesquisa = undefined;
    await waitFor('document.body.textContent.includes("O DJEN limitou as consultas de publicações")');
    assert.equal(await evaluate('document.body.textContent.includes("detalhe interno")'), false);
    checks.push('Pesquisa sem datas, alteração dos filtros desabilita IA, pesquisa vazia limpa a tabela e limite DJEN recebe mensagem segura');
    erroComunicacoes = true;
    await click('#tab-geral'); await click('#tab-comunicacoes');
    await waitFor('document.body.textContent.includes("Não foi possível carregar as comunicações")');
    erroComunicacoes = false;
    await click('.communications-main .process-state button');
    await waitFor('document.querySelectorAll(".communications-table tbody tr").length === 20');
    await evaluate('document.getElementById("tab-comunicacoes").focus()');
    await command('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Home', code: 'Home', windowsVirtualKeyCode: 36 });
    await waitFor('document.querySelector("#tab-geral").getAttribute("aria-selected") === "true"');
    assert.equal(await evaluate('document.activeElement.id'), 'tab-geral');
    checks.push('Recuperação de erros nas duas abas, busca vazia e navegação das abas por teclado');
    checks.push('Detalhes em desktop, tablet e celulares 390/320 px sem overflow da página');
    assert.deepEqual(failures, []);
  } finally {
    if (pendente) { try { await command('Fetch.failRequest', { requestId: pendente, errorReason: 'Aborted' }); } catch { /* Cancelada. */ } }
    if (pedidoIA) { try { await command('Fetch.failRequest', { requestId: pedidoIA, errorReason: 'Aborted' }); } catch { /* Cancelada. */ } }
    if (pedidoPesquisa) { try { await command('Fetch.failRequest', { requestId: pedidoPesquisa, errorReason: 'Aborted' }); } catch { /* Cancelada. */ } }
    await command('Fetch.disable'); remover();
  }
  if (process.env.ANALISE_LIVE === '1') {
    // Opt-in explícito: consulta CNJ de verdade usando apenas um processo de teste.
    const payload = { numeroProcesso: 'DIAGNOSTICO-ANALISE-REAL', clienteVinculado: 'Verificação', tribunal: 'TJPR', grau: 'G2',
      classeProcessual: { codigo: '460', nome: 'Recurso Inominado Cível' }, assunto: { codigo: '10439', nome: 'Dano material' },
      orgaoJulgador: null, dataInicio: '2025-03-15' };
    const headers = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };
    const criado = await fetch(`${origin}/api/processo/cadastrar`, { method: 'POST', headers, body: JSON.stringify(payload) });
    assert.ok(criado.ok);
    const processoReal = await criado.json();
    await go(`/processos/${processoReal.id}`);
    await waitFor('document.querySelector(".general-details-grid")');
    await click('#tab-datajud');
    await waitFor('document.body.textContent.includes("Nenhuma análise DataJud foi realizada")');
    await click('.datajud-panel .process-state button');
    for (let index = 0; index < 120; index++) {
      if (await evaluate('document.querySelector(".process-success, [role=alert]") !== null')) break;
      await new Promise(resolve => setTimeout(resolve, 500));
    }
    const erro = await evaluate('document.querySelector("[role=alert]")?.textContent');
    assert.equal(erro, undefined, `Falha na análise externa real: ${erro}`);
    await waitFor('document.querySelectorAll(".statistic-card").length === 3');
    const consulta = await fetch(`${origin}/api/dados/analise/${processoReal.id}`, { headers });
    assert.ok(consulta.ok);
    const salvo = await consulta.json();
    assert.equal(salvo.estatisticas.length, 1);
    assert.equal(salvo.estatisticas[0].processoId, processoReal.id);
    await screenshot('analise-real-datajud');
    await click('#tab-comunicacoes');
    if (salvo.comunicacoes.length) {
      await waitFor('Boolean(document.querySelector(".pagination-count"))');
      assert.equal(await evaluate('document.querySelector(".pagination-count").textContent.includes("de ' + salvo.comunicacoes.length + ' comunicações")'), true);
    } else {
      await waitFor('document.body.textContent.includes("Nenhuma comunicação foi encontrada para este processo.")');
    }
    console.log(JSON.stringify({ verificacaoReal: { processoId: processoReal.id, estatisticasSalvas: salvo.estatisticas.length,
      amostra: salvo.estatisticas[0].quantidadeProcessos, comunicacoesSalvas: salvo.comunicacoes.length } }));
    checks.push('Consulta REAL DataJud/DJEN pelo botão: processo sem órgão opcional, estatísticas gravadas e resultados exibidos');
  }
  await go('/dashboard');
}
