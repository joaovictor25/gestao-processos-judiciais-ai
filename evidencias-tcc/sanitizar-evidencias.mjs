// Mantém resultados e diagnósticos; remove valores de credenciais, JWT e cabeçalhos.
import { readFileSync, writeFileSync, readdirSync } from 'node:fs';
import { resolve, relative } from 'node:path';
const dir = import.meta.dirname, root = resolve(dir, '..');
const walk = p => readdirSync(p,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(resolve(p,e.name)):[resolve(p,e.name)]);
const segredos = new Set();
for (const p of walk(resolve(root,'backend/src/main'))) {
  if (!/\.(java|properties)$/.test(p)) continue;
  const content = readFileSync(p,'utf8');
  for (const m of content.matchAll(/(?:AIza[\w-]{30,}|AQ\.[\w-]{30,}|APIKey\s+[\w=+/.-]{20,})/g)) segredos.add(m[0]);
}
let redacoes = 0;
for (const p of walk(resolve(dir,'evidencias'))) {
  if (!/\.(log|txt|xml|json|csv|md)$/.test(p)) continue;
  const buffer = readFileSync(p);
  let content = buffer.toString(buffer[0]===255&&buffer[1]===254?'utf16le':'utf8').replace(/^\uFEFF/,'');
  for (const secret of segredos) {
    if (content.includes(secret)) { redacoes += content.split(secret).length-1; content = content.split(secret).join('[CREDENCIAL OMITIDA]'); }
  }
  content = content.replace(/eyJ[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}\.[A-Za-z0-9_-]{8,}/g,()=>{redacoes++;return '[JWT DE TESTE OMITIDO]';});
  writeFileSync(p,content,'utf8');
}
console.log(JSON.stringify({ arquivosRevisados:'evidencias/** (textos)', ocorrenciasRedigidasNestaPassagem:redacoes, valoresSensiveisExibidos:0 }));
