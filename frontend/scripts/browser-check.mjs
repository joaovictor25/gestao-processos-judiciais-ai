// Verificação opcional com Chrome/Edge instalado: nenhuma dependência de testes adicional.
// Use um backend com banco temporário; este script cadastra uma conta de verificação.
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { existsSync } from 'node:fs';
import { mkdir, writeFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { verificarProcessos } from './processos-scenarios.mjs';
import { verificarDetalhes } from './detalhes-scenarios.mjs';
import { verificarCnj } from './cnj-scenarios.mjs';

const origin = process.env.FRONTEND_TEST_URL || 'http://127.0.0.1:5173';
const executable = process.env.BROWSER_PATH || [
  'C:/Program Files/Google/Chrome/Application/chrome.exe',
  'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
].find(existsSync);
assert.ok(executable, 'Defina BROWSER_PATH com o caminho do Chrome ou Edge.');
const artifacts = resolve('artifacts');
await mkdir(artifacts, { recursive: true });
const child = spawn(executable, ['--headless=new', '--no-first-run', '--no-default-browser-check',
  '--disable-background-networking', '--remote-debugging-port=9229',
  `--user-data-dir=${resolve('.browser-test')}`, 'about:blank'], { windowsHide: true, stdio: 'ignore' });
const delay = ms => new Promise(resolveDelay => setTimeout(resolveDelay, ms));
let socket;
let sequence = 0;
const pending = new Map();
const exceptions = [];
const checks = [];
const requests = [];
const listeners = new Set();

function command(method, params = {}) {
  return new Promise((resolveCommand, reject) => {
    const id = ++sequence;
    const timeout = setTimeout(() => { pending.delete(id); reject(new Error(`Tempo excedido: ${method}`)); }, 20000);
    pending.set(id, { resolve: result => { clearTimeout(timeout); resolveCommand(result); }, reject });
    socket.send(JSON.stringify({ id, method, params }));
  });
}
async function evaluate(expression) {
  const result = await command('Runtime.evaluate', { expression, returnByValue: true, awaitPromise: true, userGesture: true });
  if (result.exceptionDetails) throw new Error(result.exceptionDetails.text + ': ' + result.exceptionDetails.exception?.description);
  return result.result.value;
}
async function waitFor(expression) {
  for (let attempt = 0; attempt < 100; attempt++) {
    if (await evaluate(`Boolean(${expression})`)) return;
    await delay(100);
  }
  throw new Error(`Condição não atendida: ${expression}`);
}
async function fill(id, value) {
  await evaluate(`(() => { const input = document.getElementById(${JSON.stringify(id)}); input.focus();
    Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value').set.call(input, ${JSON.stringify(value)});
    input.dispatchEvent(new Event('input', { bubbles: true })); })()`);
}
async function click(selector) { await evaluate(`document.querySelector(${JSON.stringify(selector)}).click()`); }
async function pathIs(path) { await waitFor(`location.pathname === ${JSON.stringify(path)} && !document.querySelector('.session-screen .spinner')`); }
async function viewport(width, height) { await command('Emulation.setDeviceMetricsOverride', { width, height, deviceScaleFactor: 1, mobile: false }); await delay(180); }
async function screenshot(name) {
  await evaluate('document.fonts.ready');
  await delay(200);
  const overflow = await evaluate('document.documentElement.scrollWidth > document.documentElement.clientWidth');
  assert.equal(overflow, false, `Rolagem horizontal indevida: ${name}`);
  const screenshot = await command('Page.captureScreenshot', { format: 'png', captureBeyondViewport: false });
  await writeFile(resolve(artifacts, `${name}.png`), Buffer.from(screenshot.data, 'base64'));
}

try {
  child.on('error', error => { console.error(error.message); });
  let page;
  for (let attempt = 0; attempt < 100; attempt++) {
    try {
      const response = await fetch('http://127.0.0.1:9229/json/new?about:blank', { method: 'PUT' });
      page = await response.json();
      break;
    } catch { await delay(100); }
  }
  assert.ok(page?.webSocketDebuggerUrl, 'O navegador não iniciou.');
  socket = new WebSocket(page.webSocketDebuggerUrl);
  await new Promise((resolveOpen, reject) => { socket.addEventListener('open', resolveOpen); socket.addEventListener('error', reject); });
  socket.addEventListener('message', event => {
    const message = JSON.parse(event.data);
    for (const listener of listeners) listener(message);
    if (message.id) {
      const operation = pending.get(message.id);
      pending.delete(message.id);
      if (message.error) operation?.reject(new Error(message.error.message));
      else operation?.resolve(message.result);
    }
    if (message.method === 'Runtime.exceptionThrown') exceptions.push(message.params.exceptionDetails.text);
    if (message.method === 'Network.requestWillBeSent' && message.params.request.url.includes('/api/')) requests.push(new URL(message.params.request.url).pathname);
  });
  await command('Page.enable');
  await command('Runtime.enable');
  await command('Network.enable');
  await viewport(1440, 900);
  await command('Page.navigate', { url: `${origin}/dashboard` });
  await pathIs('/login');
  await waitFor('Boolean(document.getElementById("email"))');
  checks.push('Rota protegida redireciona visitante ao login');
  await screenshot('login-desktop');
  await viewport(390, 844);
  await screenshot('login-mobile');
  await viewport(1440, 900);
  await click('a[href="/cadastro"]');
  await pathIs('/cadastro');
  await screenshot('cadastro-desktop');
  await viewport(390, 844);
  await screenshot('cadastro-mobile');
  await click('button[type="submit"]');
  await waitFor('document.querySelectorAll(".field-error").length === 4');
  assert.equal(await evaluate('document.activeElement.id'), 'nome');
  await fill('nome', 'Pessoa de Teste');
  await fill('email', 'email-invalido');
  await fill('senha', '123456');
  await fill('confirmacao', '654321');
  await click('button[type="submit"]');
  await waitFor('document.body.textContent.includes("As senhas não coincidem") && document.body.textContent.includes("e-mail válido")');
  checks.push('Validação local e foco no primeiro campo inválido');

  const email = `frontend.${Date.now()}@example.com`;
  const password = 'Teste-local-123';
  await fill('email', email);
  await fill('senha', password);
  await fill('confirmacao', password);
  await click('button[aria-label="Mostrar senha"]');
  assert.equal(await evaluate('document.getElementById("senha").type'), 'text');
  await click('button[type="submit"]');
  await pathIs('/login');
  await waitFor('document.body.textContent.includes("Conta criada!")');
  checks.push('Cadastro real e confirmação de sucesso');

  await fill('email', email);
  await fill('senha', 'senha-incorreta');
  await click('button[type="submit"]');
  await waitFor('document.body.textContent.includes("E-mail ou senha incorretos")');
  checks.push('Credenciais incorretas recebem mensagem segura');
  await fill('senha', password);
  await click('button[type="submit"]');
  await pathIs('/dashboard');
  await waitFor('document.querySelector(".sidebar-user strong")?.textContent === "Pessoa de Teste"');
  checks.push('Login real e identidade fornecida pelo backend');

  await verificarCnj({ origin, command, evaluate, waitFor, fill, click, pathIs, viewport, screenshot, checks,
    listen: handler => { listeners.add(handler); return () => listeners.delete(handler); } });
  await verificarProcessos({ origin, command, evaluate, waitFor, fill, click, pathIs, viewport, screenshot, checks,
    listen: handler => { listeners.add(handler); return () => listeners.delete(handler); } });
  await verificarDetalhes({ origin, command, evaluate, waitFor, fill, click, pathIs, viewport, screenshot, checks,
    listen: handler => { listeners.add(handler); return () => listeners.delete(handler); } });

  await waitFor('Boolean(document.querySelector(".sidebar-close"))');
  await viewport(1440, 900);
  assert.equal(await evaluate('getComputedStyle(document.querySelector(".sidebar-close")).display'), 'none');
  await screenshot('dashboard-desktop');
  await viewport(820, 1180);
  await screenshot('dashboard-tablet');
  await viewport(390, 844);
  await screenshot('dashboard-mobile');
  await evaluate('window.scrollTo(0, 700)');
  await screenshot('dashboard-mobile-content');
  await evaluate('window.scrollTo(0, 0)');
  await viewport(320, 720);
  await screenshot('dashboard-small-mobile');
  await viewport(390, 844);
  await click('.menu-toggle');
  await waitFor('document.querySelector(".menu-toggle").getAttribute("aria-expanded") === "true"');
  await screenshot('dashboard-mobile-menu');
  await click('.sidebar-nav a[href="/processos"]');
  await pathIs('/processos');
  assert.equal(await evaluate('document.querySelector(".menu-toggle").getAttribute("aria-expanded")'), 'false');
  await click('.menu-toggle');
  await click('.sidebar-nav a[href="/dashboard"]');
  await pathIs('/dashboard');
  checks.push('Desktop, tablet e celular sem overflow; navegação móvel funcional');

  await viewport(1440, 900);
  await click('.demo-control button');
  assert.equal(await evaluate('document.querySelector(".metric-value").textContent'), '—');
  await screenshot('dashboard-empty');
  await click('.demo-control button');
  checks.push('Demonstração identificada e estados vazios');

  await command('Page.reload');
  await pathIs('/dashboard');
  await waitFor('document.querySelector(".sidebar-user strong")?.textContent === "Pessoa de Teste"');
  assert.ok(requests.includes('/api/auth/me'));
  checks.push('Sessão restaurada por /api/auth/me após atualizar a página');

  const token = await evaluate('sessionStorage.getItem("axion.auth.token")');
  await click('.logout-button');
  await pathIs('/login');
  assert.equal(await evaluate('sessionStorage.getItem("axion.auth.token")'), null);
  const denied = await fetch(`${origin}/api/auth/me`, { headers: { Authorization: `Bearer ${token}` } });
  assert.equal(denied.status, 401);
  checks.push('Logout real limpa token e revogação bloqueia reutilização');

  await click('a[href="/cadastro"]');
  await pathIs('/cadastro');
  await fill('nome', 'Pessoa de Teste');
  await fill('email', email);
  await fill('senha', password);
  await fill('confirmacao', password);
  await click('button[type="submit"]');
  await waitFor('document.querySelector("#email-error")?.textContent.includes("já está cadastrado")');
  checks.push('Cadastro duplicado exibe conflito no campo e-mail');

  await click('a[href="/login"]');
  await pathIs('/login');
  await fill('email', email);
  await fill('senha', password);
  await command('Network.emulateNetworkConditions', { offline: true, latency: 0, downloadThroughput: -1, uploadThroughput: -1 });
  await click('button[type="submit"]');
  await waitFor('document.body.textContent.includes("Não foi possível conectar ao servidor")');
  await command('Network.emulateNetworkConditions', { offline: false, latency: 0, downloadThroughput: -1, uploadThroughput: -1 });
  checks.push('Falha de rede recebe feedback sem detalhes internos');
  await click('button[type="submit"]');
  await pathIs('/dashboard');
  checks.push('Novo login após logout funciona');
  await evaluate('sessionStorage.setItem("axion.auth.token", "token-invalido")');
  await command('Page.reload');
  await pathIs('/login');
  assert.equal(await evaluate('sessionStorage.getItem("axion.auth.token")'), null);
  checks.push('Token inválido na inicialização limpa a sessão e protege o painel');
  assert.deepEqual(exceptions, [], 'Exceções JavaScript no navegador');
  await writeFile(resolve(artifacts, 'browser-check.json'), JSON.stringify({ checks, exceptions, endpoints: [...new Set(requests)] }, null, 2));
  console.log(JSON.stringify({ passed: checks.length, checks, exceptions, screenshots: artifacts }, null, 2));
} catch (error) {
  console.error(JSON.stringify({ exceptions, page: await evaluate('({ path: location.pathname, text: document.body.innerText.slice(0, 1800) })') }));
  throw error;
} finally {
  if (socket?.readyState === WebSocket.OPEN) {
    try { await command('Browser.close'); } catch { /* O navegador pode fechar antes de responder. */ }
    socket.close();
  }
  child.kill();
}
