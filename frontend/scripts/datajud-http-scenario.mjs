// Opt-in: backend isolado deve apontar DataJud e Gemini para http://127.0.0.1:8092.
// Para este cenário de histórico, use app.datajud.cache-ms=0 e intervalo-ms=0.
// O frontend, autenticação, HTTP do backend, cálculo e persistência são reais.
import assert from 'node:assert/strict';
import { createServer } from 'node:http';

export async function verificarDataJudHttp({ origin, processoId, command, evaluate, waitFor, fill, click, checks, viewport, screenshot }) {
  const falhas = [];
  const chamadas = [];
  let amostraVazia = false;
  let erroIA = false;
  let respostaInvalida = false;
  let primeiraConsulta = true;
  const movimento = (nome, dataHora) => ({ nome, dataHora });
  const processo = movimentos => ({ dataAjuizamento: '20250101000000', movimentos });
  const amostra = [
    processo([movimento('Provimento', '2025-01-11T00:00:00Z'), movimento('Baixa Definitiva', '2025-01-21T00:00:00Z')]),
    processo([movimento('Provimento', '2025-01-21T00:00:00Z'), movimento('Baixa Definitiva', '2025-02-10T00:00:00Z')]),
    processo([movimento('Baixa Definitiva', '2024-12-31T00:00:00Z')]),
    processo([movimento('Provimento', '2025-01-31T00:00:00Z')]),
    processo([]),
  ];
  const indicadores = {
    quantidadeProcessos: 5, processosPendentes: 2,
    tempoAtePrimeiroJulgamento: { quantidade: 3, medianaDias: 20 },
    tempoAtePrimeiraBaixa: { quantidade: 2, medianaDias: 30 },
  };
  const servidor = createServer(async (req, res) => {
    try {
      assert.equal(req.method, 'POST');
      const chunks = [];
      for await (const chunk of req) chunks.push(chunk);
      const body = JSON.parse(Buffer.concat(chunks).toString('utf8'));
      chamadas.push(req.url);
      let resposta;
      if (req.url.endsWith('/_search')) {
        assert.deepEqual(body._source, ['numeroProcesso', 'dataAjuizamento', 'movimentos.nome', 'movimentos.dataHora']);
        if (primeiraConsulta) {
          assert.ok(JSON.stringify(body).includes('2025-01-01'));
          assert.ok(JSON.stringify(body).includes('2025-01-31'));
          primeiraConsulta = false;
        }
        resposta = { timed_out: false, hits: { hits: (amostraVazia ? [] : amostra).map(_source => ({ _source })) } };
      } else if (req.url.endsWith(':generateContent')) {
        if (erroIA) { res.writeHead(503); res.end('{}'); return; }
        assert.deepEqual(JSON.parse(body.contents[0].parts[0].text), indicadores);
        assert.ok(body.systemInstruction.parts[0].text.includes('Não recalcule estatísticas'));
        resposta = { candidates: [{ finishReason: 'STOP', content: { parts: [{ text: JSON.stringify({
          sintese: `A amostra contém 5 processos. A mediana até o primeiro julgamento é de ${respostaInvalida ? 999 : 20} dias, com 3 durações válidas. A mediana até a primeira baixa é de 30 dias, com 2 durações válidas. Há 2 processos sem baixa identificada. Os dados descrevem somente a amostra e não preveem prazos individuais.`,
        }) }] } }] };
      } else {
        res.writeHead(501); res.end(); return;
      }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify(resposta));
    } catch (erro) {
      falhas.push(erro.message);
      res.writeHead(500); res.end('{}');
    }
  });
  await new Promise((resolve, reject) => { servidor.once('error', reject); servidor.listen(8092, '127.0.0.1', resolve); });
  try {
    await click('#tab-datajud');
    await waitFor('Boolean(document.querySelector("#datajud-inicio"))');
    await waitFor('document.querySelector(".datajud-sidebar button[type=submit]")?.disabled === false');
    await fill('datajud-inicio', '2025-01-01');
    await fill('datajud-fim', '2025-01-31');
    await click('.datajud-sidebar button[type=submit]');
    await waitFor('document.querySelectorAll(".statistic-card").length === 3');
    assert.deepEqual(await evaluate('[...document.querySelectorAll(".statistic-value")].map(n => n.textContent)'), ['20 dias', '30 dias', '2 processos']);
    assert.deepEqual(await evaluate('[...document.querySelectorAll(".statistic-sample")].map(n => n.textContent)'), ['3 processos com duração válida', '2 processos com duração válida']);
    const token = await evaluate('sessionStorage.getItem("axion.auth.token")');
    const ler = async () => {
      const resposta = await fetch(origin + '/api/dados/estatisticas/' + processoId, { headers: { Authorization: 'Bearer ' + token } });
      assert.equal(resposta.status, 200);
      return resposta.json();
    };
    const salvas = await ler();
    assert.equal(salvas.length, 1);
    assert.ok(salvas[0].id > 0);
    for (const [campo, valor] of Object.entries(indicadores)) assert.deepEqual(salvas[0][campo], valor);
    assert.equal(salvas[0].parametros.dataInicio, '2025-01-01');
    assert.equal(salvas[0].parametros.dataFim, '2025-01-31');
    assert.equal(salvas[0].sintese, null);
    await command('Page.reload');
    await waitFor('Boolean(document.querySelector("#tab-datajud"))');
    await click('#tab-datajud');
    await waitFor('document.querySelectorAll(".statistic-card").length === 3');
    assert.deepEqual(await evaluate('[...document.querySelectorAll(".statistic-value")].map(n => n.textContent)'), ['20 dias', '30 dias', '2 processos']);
    await click('.ai-summary-action button');
    await waitFor('Boolean(document.querySelector(".datajud-ai-result"))');
    assert.equal(await evaluate('document.querySelector(".datajud-ai-result").textContent.includes("5 processos")'), true);
    const sintetizadas = await ler();
    assert.ok(sintetizadas[0].sintese.geradaEm);
    const api = async (rota, body) => {
      const resposta = await fetch(origin + '/api/dados/estatisticas/' + processoId + rota, {
        method: body === undefined ? 'GET' : 'POST',
        headers: { Authorization: 'Bearer ' + token, 'Content-Type': 'application/json' },
        body: body === undefined ? undefined : JSON.stringify(body),
      });
      assert.equal(resposta.status, 200); return resposta.json();
    };
    await waitFor('document.querySelectorAll(".datajud-history li").length === 1');
    await click('.ai-summary-action button');
    await waitFor('!document.querySelector(".ai-summary-action button").disabled');
    assert.equal((await api('/sinteses')).itens.length, 1, 'Regeneração não duplica histórico');
    const regeneradas = await ler();
    erroIA = true;
    await click('.ai-summary-action button');
    await waitFor('Boolean(document.querySelector(".ai-summary-card [role=alert]"))');
    assert.deepEqual(await ler(), regeneradas, 'Falha externa preserva texto, data e indicadores');
    erroIA = false; respostaInvalida = true;
    await click('.ai-summary-action button');
    await waitFor('Boolean(document.querySelector(".ai-summary-card [role=alert]"))');
    assert.deepEqual(await ler(), regeneradas, 'Número inventado não substitui a síntese salva');
    respostaInvalida = false;
    await fill('datajud-inicio', ''); await fill('datajud-fim', '');
    await click('.datajud-sidebar button[type=submit]');
    await waitFor('!document.querySelector(".datajud-sidebar button[type=submit]").disabled && !document.querySelector(".datajud-ai-result")');
    assert.equal((await ler()).length, 2);
    await click('.datajud-history li button');
    await waitFor('Boolean(document.querySelector(".datajud-history-text"))');
    assert.equal(await evaluate('document.querySelector(".datajud-history-modal").textContent.includes("01/01/2025 a 31/01/2025")'), true);
    assert.equal(await evaluate('Boolean(document.querySelector(".datajud-ai-result"))'), false, 'Modal não restaura síntese antiga como atual');
    if (viewport) { await viewport(320, 720); await screenshot('datajud-historico-modal-mobile'); }
    await command('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27 });
    await waitFor('!document.querySelector(".datajud-history-modal")');
    await click('.ai-summary-action button');
    await waitFor('Boolean(document.querySelector(".datajud-ai-result"))');
    assert.equal(await evaluate('document.querySelector(".datajud-ai-result").textContent.includes("Sem recorte de data")'), true);
    if (viewport) { await screenshot('datajud-sintese-mobile'); await viewport(1672, 1000); await screenshot('datajud-sintese-desktop'); }
    for (let i = 0; i < 5; i++) {
      const [execucao] = await api('/gerar', {});
      await api('/sintese-ia', { estatisticaId: execucao.id });
    }
    await command('Page.reload');
    await waitFor('Boolean(document.querySelector("#tab-datajud"))'); await click('#tab-datajud');
    await waitFor('document.querySelectorAll(".datajud-history li").length === 5 && Boolean(document.querySelector(".datajud-ai-result"))');
    await click('.datajud-history-pages button');
    await waitFor('document.querySelectorAll(".datajud-history li").length === 2');
    assert.equal((await api('/sinteses?pagina=1')).itens.length, 2);
    const antesVazia = await ler();
    amostraVazia = true;
    await fill('datajud-inicio', '2025-01-01');
    await fill('datajud-fim', '2025-01-31');
    await click('.datajud-sidebar button[type=submit]');
    await waitFor('Boolean(document.querySelector(".datajud-tab [role=alert]"))');
    assert.deepEqual(await ler(), antesVazia, 'Consulta vazia preserva estatísticas e sínteses');
    assert.equal(chamadas.filter(url => url.endsWith('/_search')).length, 8);
    assert.equal(chamadas.filter(url => url.endsWith(':generateContent')).length, 10);
    assert.deepEqual(falhas, []);
    checks.push('Fluxo HTTP local completo: persistência, regeneração, falha externa, número inventado, novo recorte, modal, Escape, mobile 320px, reload, histórico 5+2 e preservação após consulta vazia');
    await click('#tab-comunicacoes');
  } finally {
    await new Promise(resolve => servidor.close(resolve));
  }
}
