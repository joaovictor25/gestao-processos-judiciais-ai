// Backend isolado com DataJud em 127.0.0.1:8092, worker de 100 ms e pausa/retry-base de 0.
// Somente o provedor é simulado; navegador, HTTP, autenticação, fila e banco são reais.
import assert from 'node:assert/strict';
import { createServer } from 'node:http';

export async function verificarDataJudFila({ origin, command, evaluate, waitFor, fill, click, checks, screenshot }) {
  const chamadas = [];
  const falhas = [];
  let persistente = false;
  const servidor = createServer(async (req, res) => {
    try {
      if (!req.url.endsWith('/_search')) { res.writeHead(501); res.end(); return; }
      const chunks = []; for await (const chunk of req) chunks.push(chunk);
      const consulta = JSON.parse(Buffer.concat(chunks).toString());
      chamadas.push({ instante: Date.now(), consulta });
      assert.equal(consulta.size, 400); assert.equal(consulta.search_after, undefined);
      if (chamadas.length === 1 || persistente) {
        res.writeHead(429, { 'Content-Type': 'application/json', 'Retry-After': persistente ? '0' : '5' });
        res.end(JSON.stringify({ error: { type: 'es_rejected_execution_exception', reason: 'INTERNO_NAO_EXIBIR' } }));
        return;
      }
      res.writeHead(200, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ timed_out: false, hits: { hits: Array.from({ length: 400 }, (_, i) => ({
        _id: `id-${i}`, _source: { numeroProcesso: String(i), dataAjuizamento: '20250101000000',
          movimentos: [{ nome: 'Baixa Definitiva', dataHora: '2025-01-21T00:00:00Z' }] },
      })) } }));
    } catch (error) { falhas.push(error.message); res.writeHead(500); res.end('{}'); }
  });
  await new Promise((resolve, reject) => { servidor.once('error', reject); servidor.listen(8092, '127.0.0.1', resolve); });
  try {
    const token = await evaluate('sessionStorage.getItem("axion.auth.token")');
    const cadastro = await fetch(origin + '/api/processo/cadastrar', { method: 'POST',
      headers: { Authorization: 'Bearer ' + token, 'Content-Type': 'application/json' },
      body: JSON.stringify({ numeroProcesso: '0000001', cliente: 'Teste da fila', posicaoCliente: 'AUTOR', tribunal: 'TJPR' }) });
    assert.equal(cadastro.status, 200);
    const processo = await cadastro.json();
    await command('Page.navigate', { url: origin + '/processos/' + processo.id });
    await waitFor('Boolean(document.querySelector("#tab-datajud"))'); await click('#tab-datajud');
    await waitFor('document.querySelector(".datajud-sidebar button[type=submit]")?.disabled === false');
    await click('.datajud-sidebar button[type=submit]'); await click('.datajud-sidebar button[type=submit]');
    await waitFor('document.body.textContent.includes("Aguardando disponibilidade do DataJud")');
    assert.equal(chamadas.length, 1);
    // Não confundir a aba do documento antigo com a aba montada após o reload.
    await evaluate('document.querySelector("#tab-datajud").dataset.tccAntesReload = "true"');
    await command('Page.reload');
    await waitFor('Boolean(document.querySelector("#tab-datajud:not([data-tcc-antes-reload])"))'); await click('#tab-datajud');
    await waitFor('document.body.textContent.includes("Amostra consultada: 400 processos")');
    assert.equal(chamadas.length, 2);
    assert.ok(chamadas[1].instante - chamadas[0].instante >= 5000, 'Respeita Retry-After');
    checks.push('Fila real: clique duplicado, HTTP 429, espera de 5 s, reload e conclusão automática com 400 processos');
    await waitFor('document.querySelector(".datajud-sidebar button[type=submit]")?.disabled === false');
    await click('.datajud-sidebar button[type=submit]');
    await waitFor('document.querySelector(".datajud-sidebar button[type=submit]")?.disabled === false');
    assert.equal(chamadas.length, 2);
    checks.push('Nova solicitação equivalente reutiliza resultado salvo sem chamada externa');
    persistente = true;
    await fill('datajud-inicio', '2025-01-01');
    await click('.datajud-sidebar button[type=submit]');
    await waitFor('document.body.textContent.includes("continuou recusando as consultas após as tentativas automáticas")');
    assert.equal(chamadas.length, 5);
    assert.equal(await evaluate('document.body.textContent.includes("Amostra consultada: 400 processos")'), true);
    assert.equal(await evaluate('document.body.textContent.includes("INTERNO_NAO_EXIBIR")'), false);
    assert.equal(await evaluate('document.querySelector(".datajud-sidebar button[type=submit]").disabled'), false);
    checks.push('429 persistente encerra após três tentativas, preserva indicadores e oculta o corpo externo');
    await screenshot('datajud-fila-429');
    assert.deepEqual(falhas, []);
  } finally { await new Promise(resolve => servidor.close(resolve)); }
}
