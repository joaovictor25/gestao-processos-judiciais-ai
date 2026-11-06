// Backend isolado: app.djen.base-url e app.gemini.base-url = http://127.0.0.1:8093.
// Navegador, backend, autenticação e banco reais; apenas DJEN/Gemini são locais.
import assert from 'node:assert/strict';
import { createServer } from 'node:http';

export async function verificarComunicacoesHttp({ origin, command, evaluate, waitFor, fill, click, pathIs, viewport, screenshot, checks }) {
  const falhas = [], chamadas = [];
  let falharIA = false;
  const textoA = 'Foi determinada a apresentação do contrato.';
  const textoB = 'Foi concedido prazo para manifestação da parte.';
  const servidor = createServer(async (req, res) => {
    try {
      chamadas.push(req.url);
      res.setHeader('Content-Type', 'application/json');
      if (req.method === 'GET' && req.url.includes('/comunicacao')) {
        const texto = new URL(req.url, 'http://localhost').searchParams.get('texto');
        res.end(JSON.stringify({ status: 'success', count: 1, items: [{ texto: texto === 'contrato' ? textoA : textoB, siglaTribunal: 'TJPR', data_disponibilizacao: '2026-01-10', tipoComunicacao: 'Intimação' }] }));
        return;
      }
      if (req.url.endsWith(':generateContent')) {
        const chunks = []; for await (const chunk of req) chunks.push(chunk);
        const body = JSON.parse(Buffer.concat(chunks).toString('utf8'));
        assert.ok(body.systemInstruction.parts[0].text.includes('no máximo 120 palavras'));
        if (falharIA) { res.writeHead(503); res.end('{}'); return; }
        const texto = body.contents[0].parts[0].text.includes(textoA) ? textoA : textoB;
        const conteudo = { categorias: [{ tipo: 'SINTESE_GERAL', itens: [{ titulo: 'Síntese', descricao: texto, evidencias: [{ comunicacao: 1, trecho: texto }] }], limitacao: null }] };
        res.end(JSON.stringify({ candidates: [{ finishReason: 'STOP', content: { parts: [{ text: JSON.stringify(conteudo) }] } }] }));
        return;
      }
      res.writeHead(501); res.end('{}');
    } catch (erro) { falhas.push(erro.message); res.writeHead(500); res.end('{}'); }
  });
  await new Promise(resolve => servidor.listen(8093, '127.0.0.1', resolve));
  const token = await evaluate('sessionStorage.getItem("axion.auth.token")');
  const api = async (path, method = 'GET', body) => {
    const r = await fetch(`${origin}${path}`, { method, headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }, body: body && JSON.stringify(body) });
    assert.ok(r.ok, `${method} ${path}: ${r.status}`);
    const text = await r.text(); return text && JSON.parse(text);
  };
  let processo;
  try {
    processo = await api('/api/processo/cadastrar', 'POST', { numeroProcesso: 'TESTE-SINTESE-COMUNICACOES', clienteVinculado: 'Teste local', posicaoCliente: 'AUTOR', tribunal: 'TJPR' });
    const rota = `/processos/${processo.id}`, base = `/api/dados/comunicacoes/${processo.id}`;
    await command('Page.navigate', { url: `${origin}${rota}` }); await pathIs(rota);
    await waitFor('document.querySelector("#tab-comunicacoes")');
    await click('#tab-comunicacoes');
    await waitFor('document.body.textContent.includes("Nenhuma comunicação foi encontrada")');
    assert.equal(await evaluate('document.querySelectorAll(".communication-ai-choice").length'), 0);
    assert.equal(await evaluate('document.querySelector(".communication-ai-options button").disabled'), true);
    await fill('publication-text', 'contrato'); await fill('publication-start', '2026-01-01');
    await click('.publication-search button[type=submit]');
    await waitFor('document.querySelectorAll(".communications-table tbody tr").length === 1 && !document.querySelector(".communication-ai-options button").disabled');
    assert.equal(chamadas.filter(u => u.endsWith(':generateContent')).length, 0);
    await click('.communication-ai-options button'); await click('.communication-ai-options button');
    await waitFor('document.body.textContent.includes("Síntese gerada")');
    await waitFor('document.querySelector(".datajud-history li button")');
    assert.equal(chamadas.filter(u => u.endsWith(':generateContent')).length, 1);
    const primeira = await api(`${base}/pesquisas/atual`);
    assert.equal(primeira.sintese, textoA);
    assert.equal(primeira.textoBusca, 'contrato'); assert.equal(primeira.dataInicio, '2026-01-01');
    checks.push('Síntese única sem categorias: uma chamada IA por busca, texto curto e filtros salvos');
    await command('Page.reload'); await waitFor('document.querySelector("#tab-comunicacoes")'); await click('#tab-comunicacoes');
    await waitFor('document.body.textContent.includes("Síntese gerada")');
    assert.equal(chamadas.filter(u => u.endsWith(':generateContent')).length, 1);
    await fill('publication-text', 'manifestação'); await click('.publication-search button[type=submit]');
    await waitFor('!document.querySelector(".communication-ai-options button").disabled');
    falharIA = true; await click('.communication-ai-options button');
    await waitFor('document.querySelector(".communication-ai-options [role=alert]")');
    assert.equal((await api(`${base}/sinteses`)).itens.length, 1);
    assert.equal((await api(base))[0].texto, textoB);
    falharIA = false; await click('.communication-ai-options > button');
    await waitFor('document.body.textContent.includes("Síntese gerada")');
    await waitFor('document.querySelectorAll(".datajud-history li").length === 2');
    checks.push('Reabrir a aba recupera síntese salva; falha de IA preserva histórico e publicações, permitindo nova tentativa');
    await click('.datajud-history li:last-child button');
    await waitFor('document.querySelector(".datajud-history-modal")?.open && document.querySelector(".datajud-history-modal").textContent.includes("contrato")');
    await evaluate('[...document.querySelectorAll(".datajud-history-modal summary")].find(e => e.textContent.includes("Publicações desta busca")).click()');
    await waitFor('document.querySelector(".datajud-history-modal article")');
    assert.ok(await evaluate(`document.querySelector('.datajud-history-modal article').textContent.includes(${JSON.stringify(textoA)})`));
    assert.equal(await evaluate(`document.querySelector('.datajud-history-modal article').textContent.includes(${JSON.stringify(textoB)})`), false);
    await viewport(320, 720); await screenshot('comunicacoes-historico-mobile');
    await command('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27 });
    await waitFor('!document.querySelector(".datajud-history-modal")');
    await viewport(1440, 900); await screenshot('comunicacoes-sintese-historico');
    assert.deepEqual(await api(`/api/dados/estatisticas/${processo.id}`), []);
    checks.push('Histórico mantém a busca original e seus documentos após nova pesquisa; modal móvel e DataJud preservados');
    assert.deepEqual(falhas, []);
  } finally {
    if (processo) {
      const r = await fetch(`${origin}/api/processo/deletar/${processo.id}`, { method: 'DELETE', headers: { Authorization: `Bearer ${token}` } });
      assert.ok(r.ok);
    }
    await new Promise(resolve => servidor.close(resolve));
  }
}
