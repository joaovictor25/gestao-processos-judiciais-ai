import assert from 'node:assert/strict';

// Use o backend isolado do browser-check: as exclusões abaixo são reais.
export async function verificarExclusao({ origin, command, evaluate, waitFor, fill, click, pathIs, viewport, screenshot, checks, listen }) {
  const token = await evaluate('sessionStorage.getItem("axion.auth.token")');
  const headers = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };
  const api = (path, options = {}) => fetch(`${origin}/api/processo${path}`, { headers, ...options });
  const lista = async () => { const response = await api('/listar-usuario'); assert.equal(response.status, 200); return response.json(); };
  const registros = [];
  for (let index = 1; index <= 11; index++) {
    const response = await api('/cadastrar', { method: 'POST', body: JSON.stringify({
      numeroProcesso: `0051611-36.2025.8.16.${String(index).padStart(4, '0')}`,
      clienteVinculado: `Cliente Exclusão ${index}`, status: 'Em andamento',
    }) });
    assert.ok(response.ok);
    registros.push(await response.json());
  }
  let deletes = 0;
  const removeListener = listen(message => {
    if (message.method === 'Network.requestWillBeSent' && message.params.request.method === 'DELETE') deletes++;
  });
  const modal = '.process-delete-modal';
  const confirmar = `${modal} .process-delete-button`;
  const abrir = async id => {
    await evaluate(`document.querySelector('[data-processo-id="${id}"] .process-delete-button').focus()`);
    await click(`[data-processo-id="${id}"] .process-delete-button`);
    await waitFor(`document.querySelector('${modal}')?.open`);
  };
  const confirmarExclusao = async id => {
    await abrir(id);
    await click(confirmar);
    await waitFor(`!document.querySelector('${modal}') && !document.querySelector('[data-processo-id="${id}"]')`);
  };
  const redeNormal = { offline: false, latency: 0, downloadThroughput: -1, uploadThroughput: -1 };
  try {
    await command('Page.navigate', { url: `${origin}/processos` });
    await pathIs('/processos');
    await waitFor('document.querySelectorAll(".process-table tbody tr").length === 10');
    await viewport(1440, 900);
    await screenshot('exclusao-desktop');
    await viewport(390, 844);
    await screenshot('exclusao-mobile');
    await abrir(registros[0].id);
    await screenshot('exclusao-confirmacao-mobile');
    assert.equal(await evaluate('document.activeElement.textContent'), 'Cancelar');
    await click(`${modal} button:first-child`);
    await waitFor(`!document.querySelector('${modal}')`);
    assert.equal(deletes, 0);
    assert.equal(await evaluate('document.activeElement.classList.contains("process-delete-button")'), true);
    await abrir(registros[0].id);
    await command('Input.dispatchKeyEvent', { type: 'keyDown', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27 });
    await command('Input.dispatchKeyEvent', { type: 'keyUp', key: 'Escape', code: 'Escape', windowsVirtualKeyCode: 27 });
    await waitFor(`!document.querySelector('${modal}')`);
    assert.equal(deletes, 0);
    checks.push('Botão Excluir ao lado de Ver; confirmação responsiva; Cancelar e Escape não excluem e restauram o foco');

    await viewport(1440, 900);
    await click('button[aria-label="Página 2"]');
    const ultimo = registros.at(-1);
    await abrir(ultimo.id);
    assert.ok(await evaluate(`document.querySelector('${modal}').textContent.includes(${JSON.stringify(ultimo.numeroProcesso)})`));
    await command('Network.emulateNetworkConditions', { ...redeNormal, offline: true });
    await click(confirmar);
    await waitFor(`document.querySelector('${modal} [role="alert"]')?.textContent.includes('Não foi possível conectar')`);
    assert.equal((await lista()).length, 11);
    assert.equal(await evaluate(`Boolean(document.querySelector('[data-processo-id="${ultimo.id}"]'))`), true);
    checks.push('Falha de conexão mantém o processo e permite tentar novamente');

    await command('Network.emulateNetworkConditions', { ...redeNormal, latency: 1000 });
    const antes = deletes;
    await click(confirmar);
    await waitFor(`document.querySelector('${confirmar}')?.disabled`);
    assert.equal(await evaluate(`document.querySelector('${modal} button:first-child').disabled`), true);
    await click(confirmar);
    await waitFor(`!document.querySelector('${modal}')`);
    await command('Network.emulateNetworkConditions', redeNormal);
    assert.equal(deletes - antes, 1);
    assert.equal((await api(`/${ultimo.id}`)).status, 404);
    assert.equal(await evaluate('document.querySelector(".pagination-count").textContent'), '1–10 de 10 processos');
    assert.deepEqual(await evaluate('[...document.querySelectorAll(".process-summary strong")].map(el => el.textContent)'), ['10', '10', '10']);
    await waitFor('document.querySelector(".notice--success")?.textContent.includes("excluído com sucesso")');
    await evaluate('document.documentElement.dataset.tccAntesReload = "true"');
    await command('Page.reload');
    await waitFor('!document.documentElement.dataset.tccAntesReload && document.querySelectorAll(".process-table tbody tr").length === 10');
    assert.equal(await evaluate(`Boolean(document.querySelector('[data-processo-id="${ultimo.id}"]'))`), false);
    checks.push('DELETE real com resposta vazia; uma requisição durante o carregamento; registro ausente após recarregar; contadores e última página atualizados');

    const filtrado = registros[1];
    await fill('buscar-processos', filtrado.clienteVinculado);
    await waitFor('document.querySelectorAll(".process-table tbody tr").length === 1');
    await confirmarExclusao(filtrado.id);
    await waitFor('document.body.textContent.includes("Nenhum processo encontrado")');
    assert.equal((await lista()).length, 9);
    checks.push('Exclusão com busca ativa remove somente o processo selecionado e preserva o filtro');

    // Reduz a conta de verificação a um registro para exercitar o estado vazio.
    const restantes = await lista();
    for (const registro of restantes.slice(1)) assert.ok((await api(`/deletar/${registro.id}`, { method: 'DELETE' })).ok);
    await evaluate('document.documentElement.dataset.tccAntesReload = "true"');
    await command('Page.reload');
    await waitFor('!document.documentElement.dataset.tccAntesReload && document.querySelectorAll(".process-table tbody tr").length === 1');
    await confirmarExclusao(restantes[0].id);
    await waitFor('document.body.textContent.includes("Nenhum processo cadastrado.")');
    assert.deepEqual(await lista(), []);
    assert.deepEqual(await evaluate('[...document.querySelectorAll(".process-summary strong")].map(el => el.textContent)'), ['0', '0', '0']);
    checks.push('Excluir o último processo exibe o estado vazio e zera os contadores');
  } finally {
    removeListener();
    await command('Network.emulateNetworkConditions', redeNormal);
  }
}
