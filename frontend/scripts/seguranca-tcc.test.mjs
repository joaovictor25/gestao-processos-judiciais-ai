import { test } from 'node:test';
import assert from 'node:assert/strict';
import { readdirSync, readFileSync, existsSync } from 'node:fs';
import { resolve, relative } from 'node:path';

const root = resolve(import.meta.dirname, '..');
function files(dir) {
  return readdirSync(dir, { withFileTypes: true }).flatMap(e =>
    e.isDirectory() ? files(resolve(dir, e.name)) : [resolve(dir, e.name)]);
}
// Não imprime conteúdo correspondente: somente caminho e tipo do achado.
function findings(paths) {
  const rules = [
    ['chave privada PEM', /-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----/],
    ['chave Google literal', /AIza[\w-]{30,}|AQ\.[\w-]{30,}/],
    ['variável pública sensível', /VITE_[A-Z_]*(?:SECRET|PASSWORD|PRIVATE_KEY|API_KEY)/],
    ['chamada direta ao provedor', /https:\/\/(?:generativelanguage\.googleapis\.com|api-publica\.datajud\.cnj\.jus\.br)/],
  ];
  return paths.filter(p => /\.(?:tsx?|jsx?|mjs|json|html|css|map)$|\.env/.test(p)).flatMap(p => {
    const text = readFileSync(p, 'utf8');
    return rules.filter(([, pattern]) => pattern.test(text)).map(([rule]) => `${relative(root, p)}: ${rule}`);
  });
}
test('fontes e configuração pública não contêm padrões de segredo nem acesso direto aos provedores', () => {
  const envs = readdirSync(root).filter(n => n.startsWith('.env')).map(n => resolve(root, n));
  assert.deepEqual(findings([...files(resolve(root, 'src')), ...envs, resolve(root, 'vite.config.ts')]), []);
});
test('bundle compilado não contém padrões de segredo nem acesso direto aos provedores', () => {
  assert.ok(existsSync(resolve(root, 'dist/index.html')), 'Execute npm run build antes deste teste.');
  assert.deepEqual(findings(files(resolve(root, 'dist'))), []);
});
