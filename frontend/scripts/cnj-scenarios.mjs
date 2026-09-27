import assert from 'node:assert/strict';

// Só o HTTP da consulta CNJ é interceptado. Autenticação, cadastro, leitura e
// exclusão de dados usam o backend/H2 temporário da verificação principal.
export async function verificarCnj({ origin, command, evaluate, waitFor, fill, click, pathIs, viewport, screenshot, checks, listen }) {
  const cnjA = '00008323520184013202'; // Exemplo oficial do DataJud.
  const gerarCnj = tribunal => {
    const base = `000000120258${tribunal}0001`;
    const dv = String(98n - BigInt(`${base}00`) % 97n).padStart(2, '0');
    return base.slice(0, 7) + dv + base.slice(7);
  };
  const cnjB = gerarCnj('16'), cnjC = gerarCnj('26');
  const registro = {
    tribunal: 'TRF1', grau: 'G1', orgaoJulgador: { nome: 'Órgão de teste', codigo: '123' },
    classeProcessual: { nome: 'Procedimento comum', codigo: '7' },
    assuntos: [{ nome: 'Dano material', codigo: '10439' }], dataInicio: '2018-10-29',
  };
  const resposta = (registros = [registro]) => ({ numeroProcesso: cnjA, tribunalConsultado: 'TRF1', registros, limiteAtingido: false });
  const recebidas = [];
  const removeListener = listen(message => {
    if (message.method === 'Fetch.requestPaused') recebidas.push({ id: message.params.requestId,
      numero: new URL(message.params.request.url).searchParams.get('numeroProcesso') });
  });
  const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
  const go = async path => { await command('Page.navigate', { url: `${origin}${path}` }); await pathIs(path); };
  const novo = async () => { await go('/processos/novo'); await waitFor('Boolean(document.getElementById("numeroProcesso"))'); };
  const select = async (id, valor) => evaluate(`(() => { const field = document.getElementById(${JSON.stringify(id)}); field.value = ${JSON.stringify(valor)}; field.dispatchEvent(new Event('change', { bubbles: true })); })()`);
  const valor = id => evaluate(`document.getElementById(${JSON.stringify(id)}).value`);
  const aguardar = async numero => {
    for (let i = 0; i < 100; i++) {
      const chamada = recebidas.findLast(item => item.numero === numero && !item.respondida);
      if (chamada) return chamada;
      await delay(50);
    }
    throw new Error(`Consulta CNJ não foi enviada: ${numero}`);
  };
  const responder = async (chamada, corpo, status = 200) => {
    chamada.respondida = true;
    await command('Fetch.fulfillRequest', { requestId: chamada.id, responseCode: status,
      responseHeaders: [{ name: 'Content-Type', value: 'application/json' }], body: Buffer.from(JSON.stringify(corpo)).toString('base64') });
  };
  const token = await evaluate('sessionStorage.getItem("axion.auth.token")');
  const api = async (path, method = 'GET') => {
    const response = await fetch(`${origin}${path}`, { method, headers: { Authorization: `Bearer ${token}` } });
    assert.ok(response.ok, `API: ${method} ${path}: ${response.status}`);
    const text = await response.text();
    return text ? JSON.parse(text) : null;
  };
  let criado;
  try {
    await command('Fetch.enable', { patterns: [{ urlPattern: '*/api/processo/consultar-datajud?*' }] });
    await novo();
    await fill('numeroProcesso', '0000832');
    await delay(800);
    assert.equal(recebidas.length, 0);
    await fill('numeroProcesso', '0000832-36.2018.4.01.3202');
    await delay(800);
    assert.equal(recebidas.length, 0);
    await waitFor('document.body.textContent.includes("dígitos verificadores")');
    checks.push('CNJ incompleto ou com dígito inválido não chama a API; cadastro manual permanece disponível');

    await fill('numeroProcesso', '0000832-35.2018.4.01.3202');
    const primeira = await aguardar(cnjA);
    await fill('clienteVinculado', 'Cliente manual');
    await fill('classeNome', 'Classe digitada durante a consulta');
    await responder(primeira, resposta());
    await waitFor('document.getElementById("orgaoCodigo").value === "123"');
    assert.equal(await valor('tribunal'), 'TRF1');
    assert.equal(await valor('grau'), 'G1');
    assert.equal(await valor('dataInicio'), '2018-10-29');
    assert.equal(await valor('assuntoCodigo'), '10439');
    assert.equal(await valor('classeNome'), 'Classe digitada durante a consulta');
    assert.equal(await valor('classeCodigo'), '');
    for (const campo of ['comarca', 'uf', 'areaAtuacao', 'status', 'valorCausa', 'nomeMagistrado', 'dataFim']) assert.equal(await valor(campo), '');
    checks.push('CNJ completo normalizado: preenche campos compatíveis e preserva dados digitados durante a consulta');
    await viewport(1440, 1000);
    await screenshot('cnj-preenchido-desktop');

    await fill('numeroProcesso', cnjB);
    assert.equal(await valor('orgaoCodigo'), '');
    assert.equal(await valor('dataInicio'), '');
    assert.equal(await valor('clienteVinculado'), 'Cliente manual');
    const antiga = await aguardar(cnjB);
    await fill('numeroProcesso', cnjC);
    await responder(await aguardar(cnjC), resposta([{ ...registro, tribunal: 'TJSP', orgaoJulgador: { nome: 'Órgão novo', codigo: '999' } }]));
    await waitFor('document.getElementById("orgaoCodigo").value === "999"');
    try { await responder(antiga, resposta([{ ...registro, tribunal: 'TJPR' }])); }
    catch (error) { if (!/Invalid InterceptionId|Invalid parameters/.test(error.message)) throw error; }
    await delay(300);
    assert.equal(await valor('tribunal'), 'TJSP');
    assert.equal(await valor('orgaoCodigo'), '999');
    assert.equal(await valor('classeNome'), 'Classe digitada durante a consulta');
    await fill('orgaoNome', 'Órgão corrigido manualmente');
    assert.equal(await valor('orgaoCodigo'), '');
    checks.push('Trocar CNJ limpa dados automáticos, cancela resposta antiga e preserva campos manuais; nome editado não mantém código importado antigo');

    await novo();
    await fill('numeroProcesso', cnjA);
    await responder(await aguardar(cnjA), resposta([registro, { ...registro, grau: 'G2', assuntos: [...registro.assuntos, { nome: 'Dano moral', codigo: '10433' }] }]));
    await waitFor('Boolean(document.getElementById("cnj-registro"))');
    assert.equal(await valor('grau'), '');
    await select('cnj-registro', '1');
    assert.equal(await valor('grau'), 'G2');
    assert.equal(await valor('assuntoNome'), '');
    await select('cnj-assunto', '1');
    assert.equal(await valor('assuntoNome'), 'Dano moral');
    assert.equal(await valor('assuntoCodigo'), '10433');
    await viewport(320, 720);
    await screenshot('cnj-opcoes-small-mobile');
    await select('cnj-registro', '0');
    assert.equal(await valor('grau'), 'G1');
    assert.equal(await valor('assuntoCodigo'), '10439');
    checks.push('Múltiplos graus e assuntos exigem seleção explícita; mudança de registro atualiza os campos automáticos; tela cabe em 320 px');

    await novo();
    await fill('numeroProcesso', cnjA);
    await responder(await aguardar(cnjA), { codigo: 'CNJ_NAO_ENCONTRADO', mensagem: 'java.lang.Exception conteúdo externo' }, 404);
    await waitFor('document.body.textContent.includes("Nenhum registro público desse CNJ")');
    assert.equal(await evaluate('document.body.textContent.includes("java.lang")'), false);
    await fill('clienteVinculado', 'Cliente da consulta CNJ');
    await click('.cnj-feedback button');
    await responder(await aguardar(cnjA), { codigo: 'CNJ_CONSULTA_LIMITADA' }, 429);
    await waitFor('document.body.textContent.includes("limitou")');
    const total = recebidas.length;
    await delay(1000);
    assert.equal(recebidas.length, total);
    assert.equal(await valor('clienteVinculado'), 'Cliente da consulta CNJ');
    assert.deepEqual(await api('/api/processo/listar-usuario'), []);
    await click('.cnj-feedback button');
    await responder(await aguardar(cnjA), resposta());
    await waitFor('document.getElementById("classeCodigo").value === "7"');
    checks.push('Não encontrado e limite 429 exibem mensagem segura, permitem tentar novamente e não cadastram nem repetem a consulta sozinhos');

    await select('posicaoCliente', 'AUTOR');
    await select('areaAtuacao', 'Cível');
    await select('status', 'Em andamento');
    await click('button[type="submit"]');
    await pathIs('/processos');
    const lista = await api('/api/processo/listar-usuario');
    assert.equal(lista.length, 1);
    criado = lista[0];
    assert.equal(criado.classeProcessual.codigo, '7');
    assert.equal(criado.assunto.codigo, '10439');
    assert.equal(criado.orgaoJulgador.codigo, '123');
    assert.equal(criado.dataInicio, '2018-10-29');
    assert.equal(criado.clienteVinculado, 'Cliente da consulta CNJ');
    assert.deepEqual(await api(`/api/dados/estatisticas/${criado.id}`), []);
    assert.deepEqual(await api(`/api/dados/comunicacoes/${criado.id}`), []);
    await go(`/processos/${criado.id}/editar`);
    await waitFor('Boolean(document.getElementById("numeroProcesso"))');
    await fill('numeroProcesso', cnjB);
    await delay(800);
    assert.equal(recebidas.length, total + 1);
    assert.equal(await evaluate('Boolean(document.querySelector(".cnj-feedback, .cnj-result"))'), false);
    checks.push('Cadastro real persiste os campos importados somente no envio; editar processo existente não dispara consulta automática');
  } finally {
    await command('Fetch.disable');
    removeListener();
    if (criado) await api(`/api/processo/deletar/${criado.id}`, 'DELETE');
    await go('/dashboard');
    await viewport(1440, 900);
  }
}
