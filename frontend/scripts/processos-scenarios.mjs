import assert from 'node:assert/strict';

// CRUD usa o Spring Boot/H2 real. Falhas HTTP são interceptadas no navegador.
export async function verificarProcessos({ origin, command, evaluate, waitFor, fill, click, pathIs, viewport, screenshot, checks, listen }) {
  const token = await evaluate('sessionStorage.getItem("axion.auth.token")');
  const headers = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };
  const api = async (path, options = {}) => {
    const response = await fetch(`${origin}${path}`, { headers, ...options });
    assert.ok(response.ok, `API de verificação: ${path} retornou ${response.status}`);
    return response.json();
  };
  const select = async (id, value) => evaluate(`(() => { const field = document.getElementById(${JSON.stringify(id)}); field.value = ${JSON.stringify(value)}; field.dispatchEvent(new Event('change', { bubbles: true })); })()`);
  const go = async path => { await command('Page.navigate', { url: `${origin}${path}` }); await pathIs(path); };
  let interceptarAtualizacao = false;
  let interceptarLista = false;
  const failures = [];
  const removeListener = listen(message => {
    if (message.method !== 'Fetch.requestPaused') return;
    const { requestId, request } = message.params;
    const responder = async () => {
      if ((interceptarAtualizacao && request.method === 'PUT') || (interceptarLista && request.url.includes('/listar-usuario'))) {
        await command('Fetch.fulfillRequest', { requestId, responseCode: 500, responseHeaders: [{ name: 'Content-Type', value: 'application/json' }], body: Buffer.from(JSON.stringify({ mensagem: 'java.lang.Exception detalhe que não pode aparecer' })).toString('base64') });
      } else await command('Fetch.continueRequest', { requestId });
    };
    // Navegação/StrictMode podem cancelar uma requisição antes do continueRequest.
    void responder().catch(error => { if (!error.message.includes('Invalid InterceptionId')) failures.push(error.message); });
  });

  try {
    await command('Fetch.enable', { patterns: [
      { urlPattern: '*/api/processo/atualizar/*' }, { urlPattern: '*/api/processo/listar-usuario' },
    ] });
    await viewport(1440, 1000);
    await click('.sidebar-nav a[href="/processos"]');
    await pathIs('/processos');
    await waitFor('document.body.textContent.includes("Nenhum processo cadastrado.")');
    await screenshot('processos-vazio');
    checks.push('Listagem real vazia com ação para cadastrar o primeiro processo');
    await click('.process-state a[href="/processos/novo"]');
    await pathIs('/processos/novo');
    await waitFor('Boolean(document.querySelector(".process-form button[type=submit]"))');
    await click('button[type="submit"]');
    await waitFor('document.querySelectorAll(".field-error").length === 10');
    assert.equal(await evaluate('document.activeElement.id'), 'numeroProcesso');
    assert.equal(await evaluate('document.querySelector(".sidebar-nav a.active").textContent'), 'Processos');
    checks.push('Dez campos obrigatórios validados e menu Processos ativo no cadastro');
    const dados = {
      numeroProcesso: '0051611-36.2025.8.16.0182', clienteVinculado: 'João da Silva',
      orgaoNome: '3ª Turma Recursal dos Juizados Especiais', orgaoCodigo: '72197', comarca: 'Curitiba',
      classeNome: 'Recurso Inominado Cível', classeCodigo: '460', assuntoNome: 'Indenização por Dano Material', assuntoCodigo: '10439',
      nomeMagistrado: 'Magistrado de teste', dataInicio: '2026-09-24', dataFim: '2026-09-23', valorCausa: '2500000',
    };
    for (const [field, value] of Object.entries(dados)) await fill(field, value);
    for (const [field, value] of Object.entries({ posicaoCliente: 'AUTOR', tribunal: 'TJPR', grau: 'G1', uf: 'PR', areaAtuacao: 'Cível', status: 'Em andamento' })) await select(field, value);
    assert.equal(await evaluate('document.getElementById("valorCausa").value'), 'R$ 25.000,00');
    await click('button[type="submit"]');
    await waitFor('document.querySelector("#dataFim-error")?.textContent.includes("posterior")');
    await fill('dataFim', '2026-09-27');
    await evaluate('window.scrollTo(0, 0)');
    await screenshot('novo-processo-desktop');
    await viewport(390, 844);
    await screenshot('novo-processo-mobile');
    await viewport(1440, 1000);
    await click('button[type="submit"]');
    await pathIs('/processos');
    await waitFor('document.body.textContent.includes("Processo cadastrado com sucesso.") && document.querySelector(".process-table tbody tr")');
    const criados = await api('/api/processo/listar-usuario');
    assert.equal(criados.length, 1);
    const criado = criados[0];
    assert.equal(criado.valorCausa, 25000);
    assert.equal(criado.dataInicio, '2026-09-24');
    assert.equal(criado.dataFim, '2026-09-27');
    assert.deepEqual(criado.orgaoJulgador, { nome: dados.orgaoNome, codigo: dados.orgaoCodigo });
    assert.deepEqual(criado.classeProcessual, { nome: dados.classeNome, codigo: dados.classeCodigo });
    assert.deepEqual(criado.assunto, { nome: dados.assuntoNome, codigo: dados.assuntoCodigo });
    assert.equal(criado.usuarioId, undefined);
    checks.push('Cadastro real persiste moeda, datas e classificações aninhadas no formato dos DTOs');

    await waitFor('document.body.textContent.includes("Não analisado")');
    await click('.process-row-actions a');
    await pathIs(`/processos/${criado.id}`);
    await waitFor('document.querySelector(".details-header-actions a")');
    await click('.details-header-actions a');
    await pathIs(`/processos/${criado.id}/editar`);
    await waitFor('document.getElementById("clienteVinculado")?.value === "João da Silva"');
    assert.equal(await evaluate('document.getElementById("orgaoCodigo").value'), '72197');
    assert.equal(await evaluate('document.getElementById("dataFim").value'), '2026-09-27');
    await screenshot('editar-processo-desktop');
    await fill('clienteVinculado', 'Alteração cancelada');
    await click('.process-form-actions .button--secondary');
    await pathIs('/processos');
    assert.equal((await api(`/api/processo/${criado.id}`)).clienteVinculado, 'João da Silva');
    checks.push('Edição carrega os dados existentes e Cancelar não grava alterações');

    await waitFor('Boolean(document.querySelector(".process-row-actions a"))');
    await click('.process-row-actions a');
    await pathIs(`/processos/${criado.id}`);
    await waitFor('document.querySelector(".details-header-actions a")');
    await click('.details-header-actions a');
    await pathIs(`/processos/${criado.id}/editar`);
    await waitFor('Boolean(document.getElementById("clienteVinculado"))');
    await fill('clienteVinculado', 'João Atualizado');
    await fill('valorCausa', '9876543');
    await fill('dataFim', '');
    await select('status', 'Concluído');
    interceptarAtualizacao = true;
    await click('button[type="submit"]');
    await waitFor('Boolean(document.querySelector(".process-save-error"))');
    assert.equal(await evaluate('document.getElementById("clienteVinculado").value'), 'João Atualizado');
    assert.equal(await evaluate('document.body.textContent.includes("java.lang")'), false);
    assert.equal((await api(`/api/processo/${criado.id}`)).clienteVinculado, 'João da Silva');
    interceptarAtualizacao = false;
    await click('button[type="submit"]');
    await pathIs('/processos');
    await waitFor('document.body.textContent.includes("Processo atualizado com sucesso.")');
    const editado = await api(`/api/processo/${criado.id}`);
    assert.equal(editado.clienteVinculado, 'João Atualizado');
    assert.equal(editado.valorCausa, 98765.43);
    assert.equal(editado.dataFim, null);
    assert.equal(editado.status, 'Concluído');
    assert.equal(editado.numeroProcesso, criado.numeroProcesso);
    checks.push('PUT real atualiza processo; falha preserva formulário e permite nova tentativa');

    for (let index = 0; index < 12; index++) {
      const { id: _id, ...payload } = criado;
      await api('/api/processo/cadastrar', { method: 'POST', body: JSON.stringify({ ...payload,
        numeroProcesso: `100000${index % 10}-00.2026.8.26.000${index % 10}`,
        clienteVinculado: `Cliente ${index + 1}`, posicaoCliente: index % 2 ? 'RÉU' : 'AUTOR',
        status: ['Em andamento', 'Suspenso', 'Arquivado'][index % 3],
      }) });
    }
    await go('/processos');
    await waitFor('document.querySelectorAll(".process-table tbody tr").length === 10 && !document.body.textContent.includes("Consultando…")');
    assert.equal(await evaluate('document.querySelector(".pagination-count").textContent'), '1–10 de 13 processos');
    assert.equal(await evaluate('document.querySelectorAll(".process-summary strong")[0].textContent'), '13');
    assert.equal(await evaluate('document.querySelectorAll(".process-summary strong")[1].textContent'), '4');
    assert.equal(await evaluate('document.querySelectorAll(".process-summary strong")[2].textContent'), '13');
    await click('button[aria-label="Página 2"]');
    assert.equal(await evaluate('document.querySelectorAll(".process-table tbody tr").length'), 3);
    await click('.pagination-size button:nth-child(2)');
    assert.equal(await evaluate('document.querySelectorAll(".process-table tbody tr").length'), 13);
    await fill('buscar-processos', 'joao atualizado');
    assert.equal(await evaluate('document.querySelectorAll(".process-table tbody tr").length'), 1);
    await fill('buscar-processos', '00516113620258160182');
    assert.equal(await evaluate('document.querySelectorAll(".process-table tbody tr").length'), 1);
    await fill('buscar-processos', 'inexistente-zz');
    await waitFor('document.body.textContent.includes("Nenhum processo encontrado")');
    await click('.process-state button');
    checks.push('Busca sem acentos e por CNJ sem pontuação; paginação 10/20; cards calculados da API');

    await screenshot('processos-desktop');
    await viewport(820, 1180);
    await screenshot('processos-tablet');
    await viewport(390, 844);
    await screenshot('processos-mobile');
    await viewport(320, 720);
    await screenshot('processos-small-mobile');
    await go(`/processos/${criado.id}/editar`);
    await waitFor('Boolean(document.getElementById("clienteVinculado"))');
    await screenshot('editar-processo-small-mobile');
    await viewport(1440, 1000);
    checks.push('Lista e formulário responsivos em desktop, tablet, 390 px e 320 px');

    const otherEmail = `processo.outro.${Date.now()}@example.com`;
    await api('/api/auth/cadastro', { method: 'POST', body: JSON.stringify({ nome: 'Outra conta', email: otherEmail, senha: 'Teste-local-123' }) });
    const otherLogin = await api('/api/auth/login', { method: 'POST', body: JSON.stringify({ email: otherEmail, senha: 'Teste-local-123' }) });
    const { id: _id, ...otherPayload } = criado;
    const other = await api('/api/processo/cadastrar', { method: 'POST', headers: { 'Content-Type': 'application/json', Authorization: `Bearer ${otherLogin.token}` }, body: JSON.stringify({ ...otherPayload, numeroProcesso: 'PROCESSO-DE-OUTRA-CONTA' }) });
    await go(`/processos/${other.id}/editar`);
    await waitFor('document.body.textContent.includes("Processo não encontrado")');
    assert.equal(await evaluate('document.body.textContent.includes("PROCESSO-DE-OUTRA-CONTA")'), false);
    await go(`/processos/${other.id}`);
    await waitFor('document.body.textContent.includes("Processo não encontrado")');
    assert.equal(await evaluate('document.body.textContent.includes("PROCESSO-DE-OUTRA-CONTA")'), false);
    await go('/processos/invalido/editar');
    await waitFor('document.body.textContent.includes("Processo não encontrado")');
    checks.push('Edição e detalhes rejeitam processo de outra conta sem expor seus dados; ID inválido tratado');

    interceptarLista = true;
    await go('/processos');
    await waitFor('document.body.textContent.includes("Não foi possível carregar os processos")');
    assert.equal(await evaluate('document.body.textContent.includes("java.lang")'), false);
    interceptarLista = false;
    await click('.process-state button');
    await waitFor('document.querySelectorAll(".process-table tbody tr").length === 10');
    checks.push('Erro de listagem com mensagem segura e botão de nova tentativa');
    assert.deepEqual(failures, []);
  } finally {
    await command('Fetch.disable');
    removeListener();
  }
  await go('/dashboard');
}
