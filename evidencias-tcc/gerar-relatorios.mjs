import { readFileSync, writeFileSync, readdirSync, mkdirSync, existsSync } from 'node:fs';
import { resolve, relative } from 'node:path';
import { createHash } from 'node:crypto';
const dir = import.meta.dirname, root = resolve(dir, '..');
const read = p => readFileSync(resolve(root, p), 'utf8');
const write = (p, text) => writeFileSync(resolve(dir, p), text);
const json = (p, value) => write(p, JSON.stringify(value, null, 2));
const walk = p => readdirSync(p, { withFileTypes: true }).flatMap(e => e.isDirectory() ? walk(resolve(p, e.name)) : [resolve(p, e.name)]);
const sha = p => createHash('sha256').update(readFileSync(p)).digest('hex');
const decode = s => (s ?? '').replace(/&quot;/g, '"').replace(/&apos;/g, "'").replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&amp;/g, '&');
const attrs = s => Object.fromEntries([...s.matchAll(/([\w.-]+)="([^"]*)"/g)].map(m => [m[1], decode(m[2])]));
function junit(run) {
  const logBuffer = readFileSync(resolve(dir, `evidencias/logs/backend-${run}.log`));
  const log = logBuffer.toString(logBuffer[0] === 255 && logBuffer[1] === 254 ? 'utf16le' : 'utf8');
  const executed = new Set([...log.matchAll(/\[INFO\] Running\s+([\w.]+)/g)].map(m => m[1]));
  return walk(resolve(dir, `evidencias/resultados/${run}`)).filter(p => p.endsWith('.xml')).flatMap(p => {
    const xml = readFileSync(p, 'utf8'); const suite = attrs(xml.match(/<testsuite\s+([^>]+)/)[1]);
    if (!executed.has(suite.name)) return []; // Não contabilizar XML residual de execução anterior.
    return [...xml.matchAll(/<testcase\b([^>]*?)(?:\/>|>([\s\S]*?)<\/testcase>)/g)].map(m => ({ ...attrs(m[1]),
      status: /<(failure|error)\b/.test(m[2] ?? '') ? 'reprovado' : /<skipped\b/.test(m[2] ?? '') ? 'não executado' : 'aprovado',
      failure: decode((m[2] ?? '').match(/<(?:failure|error)\b[^>]*>([\s\S]*?)<\/(?:failure|error)>/)?.[1] ?? ''),
      timestamp: suite.timestamp ?? '2026-11-05; horário no log da execução', evidence: relative(dir, p).replaceAll('\\', '/') }));
  });
}
const baseline = junit('baseline'), final = junit('final');
for (const [nome, casos] of [['baseline',baseline],['final',final]]) {
  const buf = readFileSync(resolve(dir,`evidencias/logs/backend-${nome}.log`));
  const log = buf.toString(buf[0]===255&&buf[1]===254?'utf16le':'utf8');
  const totais = [...log.matchAll(/Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)/g)].at(-1);
  if (!totais || Number(totais[1])!==casos.length || Number(totais[2])+Number(totais[3])!==casos.filter(t=>t.status==='reprovado').length)
    throw new Error('XML e resumo Maven divergem: '+nome);
}
const segurancaLog = readFileSync(resolve(dir,'evidencias/logs/frontend-seguranca-retry.log'),'utf8');
if (!/tests 2/.test(segurancaLog) || !/pass 2/.test(segurancaLog) || !/fail 0/.test(segurancaLog))
  throw new Error('Revisar resultado Node antes de atribuir aprovação.');
const requisitos = {
  RF01: 'Cadastrar e autenticar usuários, revogar sessão e proteger acesso',
  RF02: 'Gerenciar processos próprios com validação e persistência CRUD',
  RF03: 'Consultar DataJud com filtros, limites, fila e recuperação de falhas',
  RF04: 'Calcular medianas e contagens das subamostras válidas',
  RF05: 'Consultar DJEN por texto/tribunal/período e preparar comunicações',
  RF06: 'Sintetizar comunicações com evidências e limitações',
  RF07: 'Explicar somente indicadores estatísticos fornecidos',
  RF08: 'Persistir análises, histórico e fontes, preservando dados após falha',
  RF09: 'Apresentar dashboard da carteira autenticada',
  RNF01: 'Isolar contas e restringir acesso a dados e análises',
  RNF02: 'Proteger credenciais e minimizar exposição de dados nas integrações',
  RNF03: 'Manter integridade de persistência e inicialização/migração do ambiente'
};
function requirement(t) {
  const c = t.classname, n = t.name;
  if (/OutroUsuario|OutraConta|Proprietario|proprietario|Autorizam|IsolaProprietarios|Visitante|ExigeAutenticacao|bloqueiaOutro/i.test(n)) return 'RNF01';
  if (/Senha|Sigilo|SemExpor|NaoExibeCorpo|SemDadosPessoais|chaveAusente/.test(n)) return 'RNF02';
  if (/EstatisticaProcessualServiceTests/.test(c)) return 'RF04';
  if (/ValidadorSintese/.test(c)) return 'RF07';
  if (/SinteseDataJud|AnaliseProcesso/.test(c)) return 'RF08';
  if (/ComunicacaoContrato/.test(c)) return 'RF05';
  if (/AnaliseComunicacao/.test(c)) {
    if (/Persistida|Historico|Antigas|preserva|Preserva/.test(n)) return 'RF08';
    if (/sintetizaSomenteEstatisticas|sinteseExige|sinteseValida|sinteseRejeita|falhaDaSintese/.test(n)) return 'RF07';
    if (/pesquisa|Pesquisa|filtros|Filtros|Djen|buscaVazia/.test(n)) return 'RF05';
    return 'RF06';
  }
  if (/TextoComunicacao/.test(c)) return 'RF05';
  if (/ConsultaDataJud|ConsultaCnj|NumeroCnj|DataJudFila|AnaliseConsultas/.test(c)) return 'RF03';
  if (/Dashboard/.test(c)) return 'RF09';
  if (/Autenticacao/.test(c)) return /[Pp]rocesso|[Cc]onsultasSeparadas|[Rr]eanalise/.test(n) ? 'RF02' : 'RF01';
  if (/H2Acesso/.test(c)) return 'RF01';
  return 'RNF03';
}
const testFiles = walk(resolve(root, 'backend/src/test')).filter(p => p.endsWith('.java'));
const sources = Object.fromEntries(testFiles.map(p => [p.split(/[\\/]/).at(-1).replace('.java', ''), { path: relative(root, p).replaceAll('\\', '/'), text: readFileSync(p, 'utf8') }]));
const sourceFor = t => sources[t.classname.split('.').at(-1)];
const methodFor = t => t.name.split('(')[0];
function expectations(t) {
  const src = sourceFor(t)?.text ?? ''; const method = methodFor(t);
  const start = src.indexOf('void ' + method + '(');
  const next = src.indexOf('\n    @', start + 1);
  const block = src.slice(start, next < 0 ? undefined : next);
  const assertions = block.split(/\r?\n/).filter(l => /assertThat|andExpect|assertEquals|assertTrue|assertFalse|assertThrows|verify\(/.test(l));
  return assertions.slice(0, 5).map(l => l.trim()).join(' ') || `Contrato verificável em ${sourceFor(t)?.path}#${method}`;
}
const rows = final.map((t, i) => {
  const req = requirement(t), src = sourceFor(t), method = methodFor(t);
  const unit = !src?.text.includes('@SpringBootTest');
  const existing = baseline.some(b => b.classname === t.classname && b.name === t.name);
  return { requisito: req, descricao: requisitos[req], teste: `J-${String(i + 1).padStart(3, '0')}`, cenario: `${t.classname}#${t.name}`,
    precondicoes: unit ? 'JUnit; fixtures locais; nenhuma API real' : 'Spring test; H2 em memória isolado; fixtures locais; provedores simulados quando usados',
    procedimento: `Executado Maven test; ${src?.path}#${method}; argumentos parametrizados identificados no nome JUnit`,
    esperado: expectations(t), observado: t.status === 'aprovado' ? 'Asserções concluídas sem falhas na execução final' : t.failure.slice(0, 1400),
    situacao: t.status, evidencia: t.evidence, observacoes: `${existing ? 'Existente na linha de base' : 'Novo nesta tarefa'}; ${t.time}s; ${t.timestamp}; aprovação limitada às asserções. ${unit ? 'Teste unitário/contrato local.' : 'Integração local; não comprova MySQL de produção.'}` };
});
const browserRuns = ['comunicacoes', 'datajud-fila', 'detalhes', 'exclusao'];
const browser = browserRuns.flatMap(scope => {
  const path = `evidencias/resultados/browser-${scope}/browser-check.json`;
  if (!existsSync(resolve(dir, path))) return [];
  const result = JSON.parse(readFileSync(resolve(dir, path), 'utf8').replace(/^\uFEFF/, ''));
  return result.checks.map((check, i) => ({ scope, check, evidence: path, id: `B-${scope}-${i + 1}` }));
});
// Cada checkpoint repetido de autenticação é contado uma única vez no consolidado.
const uniqueBrowser = [...new Map(browser.map(b => [b.check, b])).values()];
for (const b of uniqueBrowser) {
  const req = /[Ss]íntese|[Hh]istórico/.test(b.check) ? 'RF08' : /Fila|429|equivalente/.test(b.check) ? 'RF03' : /[Cc]adastro|Login|Credenciais|Rota protegida|Validação local/.test(b.check) ? 'RF01' : 'RF02';
  rows.push({ requisito: req, descricao: requisitos[req], teste: b.id, cenario: b.check,
    precondicoes: 'Chrome headless; Vite 15173; Spring 18081; H2 temporário; simuladores loopback',
    procedimento: `node scripts/browser-check.mjs; BROWSER_SCOPE=${b.scope}`, esperado: b.check,
    observado: 'Checkpoint registrado pelo script após asserções; ver JSON e log do fluxo', situacao: 'aprovado', evidencia: b.evidence,
    observacoes: 'Automatizado em navegador; autenticação repetida deduplicada no total; não é teste manual nem API real.' });
}
for (const [i, label] of ['Fontes/configuração pública sem padrões de segredo', 'Bundle sem padrões de segredo'].entries()) rows.push({
  requisito: 'RNF02', descricao: requisitos.RNF02, teste: `F-SEG-${i + 1}`, cenario: label,
  precondicoes: 'Build concluído; Node test; fontes e dist locais', procedimento: 'node --test scripts/seguranca-tcc.test.mjs',
  esperado: 'Nenhum padrão de chave/PEM/variável sensível/provedor direto encontrado', observado: 'Asserções aprovadas', situacao: 'aprovado',
  evidencia: 'evidencias/logs/frontend-seguranca-retry.log', observacoes: 'Heurística limitada; não prova ausência universal de segredos.' });
const corpus = JSON.parse(readFileSync(resolve(dir, 'evidencias/resultados/avaliacao-ia/corpus.json'), 'utf8'));
for (const c of [...corpus.comunicacoes, ...corpus.estatisticas]) rows.push({ requisito: c.grupo === 6 ? 'RF06' : 'RF07',
  descricao: requisitos[c.grupo === 6 ? 'RF06' : 'RF07'], teste: c.id, cenario: c.foco ?? c.fatosEsperados.join('; '),
  precondicoes: 'Autorização futura, configuração adequada e avaliador humano', procedimento: 'Preparado corpus e prompt; nenhuma chamada real executada',
  esperado: c.foco ?? c.fatosEsperados.join('; '), observado: 'Sem resposta real ou pontuação', situacao: 'não executado',
  evidencia: 'evidencias/resultados/avaliacao-ia/corpus.json', observacoes: c.situacao });
const pendencias = [
  ['P-MYSQL', 'RNF03', 'Executar integração no MySQL isolado', 'Nenhuma instância MySQL de teste provisionada; H2 não demonstra compatibilidade integral.'],
  ['P-DATAJUD', 'RF03', 'Consulta real DataJud', 'Não autorizada; testes simulados separados.'],
  ['P-DJEN', 'RF05', 'Consulta real DJEN', 'Não autorizada; testes simulados separados.'],
  ['P-MANUAL', 'RF08', 'Conferência manual da interface e das análises', 'Não houve avaliação humana efetivamente realizada.'],
];
const achados = [
  { id:'S01', requisito:'RNF02', arquivo:'backend/src/main/java/br/com/axionweb/backend/service/GeminiService.java',
    padrao:/@Value\("\$\{app\.gemini\.api-key:([^}]*)\}/, descricao:'Fallback literal de credencial Gemini',
    esperado:'Segredo configurável sem credencial embutida', proposta:'Remover fallback; exigir variável de ambiente; responsável deve avaliar rotação.' },
  { id:'S02', requisito:'RF05', arquivo:'backend/src/main/java/br/com/axionweb/backend/config/RestComunicacaoConfig.java',
    padrao:/public RestClient restComunicacao\([\s\S]*?\n    }/, descricao:'Cliente DJEN sem timeout explícito',
    esperado:'Limites de conexão/leitura explícitos para integração externa', proposta:'Configurar request factory e limites; verificar transporte HTTP com atraso controlado.' }
].map(a=> {
  const source = read(a.arquivo); const match = source.match(a.padrao);
  if (!match) throw new Error('Inspeção mudou; revisar ' + a.id);
  const encontrado = a.id === 'S01' ? match[1].length > 0 : !/setReadTimeout|connectTimeout|requestFactory/.test(match[0]);
  return { id:a.id, requisito:a.requisito, arquivo:a.arquivo, linha:source.slice(0,match.index).split('\n').length,
    descricao:a.descricao, esperado:a.esperado, proposta:a.proposta, encontrado, tipo:'inspeção estática automatizada; não testa validade de credenciais nem duração de timeout' };
});
json('evidencias/resultados/inspecao-seguranca.json',achados);
for (const a of achados) rows.push({ requisito:a.requisito, descricao:requisitos[a.requisito], teste:a.id, cenario:a.descricao,
  precondicoes:'Código local atual; inspeção sem enviar credenciais', procedimento:`Inspecionar ${a.arquivo}:${a.linha}`,
  esperado:a.esperado, observado:a.encontrado ? a.descricao+' identificado; nenhum valor sensível registrado' : 'Padrão não encontrado',
  situacao:a.encontrado?'reprovado':'aprovado', evidencia:'evidencias/resultados/inspecao-seguranca.json', observacoes:a.tipo+'; '+a.proposta });
for (const [id, req, scenario, reason] of pendencias) rows.push({ requisito: req, descricao: requisitos[req], teste: id, cenario: scenario,
  precondicoes: reason, procedimento: 'Não realizado', esperado: 'Validar em ambiente indicado', observado: reason, situacao: 'não executado', evidencia: '12_limitacoes.md', observacoes: reason });
// Fluxo legado interrompido: não contar checkpoints parciais como aprovação completa.
if (existsSync(resolve(dir, 'evidencias/logs/browser-detalhes.log')) && !browser.some(b => b.scope === 'detalhes')) rows.push({
  requisito: 'RF02', descricao: requisitos.RF02, teste: 'B-DETALHES-FLUXO', cenario: 'Fluxo legado completo de cadastro/CRUD/detalhes',
  precondicoes: 'Ambiente isolado e interceptação dos provedores', procedimento: 'BROWSER_SCOPE=detalhes; node scripts/browser-check.mjs',
  esperado: 'Concluir todos os checkpoints', observado: 'Fluxo interrompido; motivo exato no log; checkpoints subsequentes não concluídos',
  situacao: 'reprovado', evidencia: 'evidencias/logs/browser-detalhes.log', observacoes: 'Falha do fluxo não equivale automaticamente a defeito de produção; consultar limitações.' });
const cols = ['requisito','descricao','teste','cenario','precondicoes','procedimento','esperado','observado','situacao','evidencia','observacoes'];
write('05_matriz_requisitos_testes.csv', '\uFEFF' + [cols, ...rows.map(r => cols.map(c => r[c] ?? ''))].map(r => r.map(v => '"' + String(v).replaceAll('"','""') + '"').join(',')).join('\r\n'));
const counts = list => ({ planejados: list.length, executados: list.filter(r => r.situacao !== 'não executado').length,
  aprovados: list.filter(r => r.situacao === 'aprovado').length, reprovados: list.filter(r => r.situacao === 'reprovado').length, naoExecutados: list.filter(r => r.situacao === 'não executado').length });
const summary = { requisitos: Object.keys(requisitos).length, matriz: counts(rows), backendBaseline: { total: baseline.length, aprovados: baseline.filter(t => t.status === 'aprovado').length },
  backendFinal: { total: final.length, aprovados: final.filter(t => t.status === 'aprovado').length, reprovados: final.filter(t => t.status === 'reprovado').length },
  browserCheckpointsUnicos: uniqueBrowser.length, frontendSeguranca: 2, inspecoesEstaticas:achados.length, corpusIA: 11, respostasGeminiReais: 0, avaliacoesHumanas: 0 };
const grupoRequisito = {RF01:1,RF02:2,RF03:3,RF04:4,RF05:5,RF06:6,RF07:7,RF08:8,RF09:0,RNF01:1,RNF02:1,RNF03:0};
summary.porGrupoPrincipal = Object.fromEntries(Array.from({length:9},(_,g)=>[g,counts(rows.filter(r=>grupoRequisito[r.requisito]===g))]));
json('evidencias/resultados/resumo.json', summary);
const inventory = testFiles.map(p => ({ arquivo: relative(root,p).replaceAll('\\','/'), sha256: sha(p),
  origem: p.endsWith('ComunicacaoContratoTccTests.java') ? 'novo' : 'existente antes desta tarefa',
  testesFinal: final.filter(t => t.classname.split('.').at(-1) + '.java' === p.split(/[\\/]/).at(-1)).map(t => ({ metodo: t.name, situacao: t.status, tempoSegundos: t.time })) }));
json('evidencias/resultados/inventario-testes.json', inventory);
const sourcePaths = [...walk(resolve(root,'backend/src/main')), ...walk(resolve(root,'frontend/src')), ...testFiles,
  ...walk(resolve(root,'frontend/scripts')), resolve(root,'backend/pom.xml'), resolve(root,'frontend/package-lock.json')];
json('evidencias/resultados/manifesto-fontes.json', sourcePaths.map(p => ({ arquivo: relative(root,p).replaceAll('\\','/'), sha256: sha(p) })));
// Somente versões extraídas da propriedade classpath; nunca copiar properties completas.
const originalXml = readFileSync(resolve(root,'backend/target/surefire-reports/TEST-br.com.axionweb.backend.BackendApplicationTests.xml'),'utf8');
const versions = [...new Set([...originalXml.matchAll(/(?:hibernate-core|h2|mysql-connector-j|spring-boot|junit-jupiter-api)-([\d.]+(?:[A-Za-z][A-Za-z0-9.-]*|-[A-Za-z0-9.-]+)?)\.jar/g)].map(m => m[0]))].sort();
const lock = JSON.parse(read('frontend/package-lock.json'));
const frontendVersions = Object.fromEntries(['react','react-dom','react-router-dom','typescript','vite'].map(n => [n,lock.packages['node_modules/'+n]?.version]));
const commit = readFileSync(resolve(dir,'evidencias/execucoes/commit.txt'),'utf8').replace(/^\uFEFF/,'').trim();
json('evidencias/execucoes/ambiente.json',{ dataLocal:'2026-11-05', fuso:'America/Sao_Paulo', commit, checkoutModificado:true,
  os:'Windows 11 (10.0), amd64', javaDeclarado:25, javaExecutado:'Oracle 26+35-2893', maven:'3.9.16',node:process.version,frontendVersions, jarsObservados:versions });
const table = (headers, data) => '| '+headers.join(' | ')+' |\n| '+headers.map(()=> '---').join(' | ')+' |\n'+data.map(r=>'| '+r.join(' | ')+' |').join('\n');
write('02_ambiente_testes.md', `# Ambiente de testes\n\nData local: 05/11/2026, America/Sao_Paulo. Windows 11 amd64. Commit base: \`${commit}\`; checkout já modificado. Manifesto SHA-256 e estado Git em evidencias/.\n\nJava declarado no pom: 25; executado: Oracle Java 26+35-2893. Maven Wrapper 3.9.16. Spring Boot 4.1.1. Node ${process.version}.\n\n${table(['Dependência frontend resolvida','Versão'],Object.entries(frontendVersions))}\n\nDependências observadas no classpath Surefire: ${versions.join(', ')}. MySQL é driver disponível, não servidor utilizado; não foi medida versão de servidor MySQL. JPA/Hibernate foi executado sobre H2 em memória, com create-drop.\n\nBackend HTTP de navegador: loopback 18081; frontend Vite: 15173; provedores simulados DataJud: 8092, DJEN/Gemini: 8093. O teste de fila Java usa porta efêmera. Perfil test com parâmetros explícitos; spring-boot:run não carrega automaticamente src/test/resources. JWT e chave Gemini locais são fictícios.\n\nBuild e Node test inicialmente encontraram spawn EPERM no sandbox; repetidos com permissão de subprocessos e aprovados. Postman não iniciou por manifesto Private Cloud não legível. Chrome headless usa DevTools; nenhum pacote de navegador foi instalado.\n`);
write('03_requisitos.md', `# Requisitos identificados\n\nSão ${Object.keys(requisitos).length} requisitos derivados do código atual, não uma especificação retrospectiva aprovada pelo orientador. A matriz vincula cada execução à exigência principal; um teste pode verificar aspectos de outros requisitos.\n\n${table(['ID','Descrição'],Object.entries(requisitos))}\n\nA consulta estatística é por filtros estruturados; pesquisa DJEN é textual. Não foi identificada recuperação por embeddings, ranking semântico ou predição judicial. A persistência das sínteses está implementada. Não há obrigação implementada de percorrer todas as páginas externas.\n`);
write('06_relatorio_testes_funcionais.md', `# Testes funcionais\n\nLinha de base: ${baseline.length} invocações JUnit aprovadas. Execução ampliada: ${final.length}, sendo ${summary.backendFinal.aprovados} aprovadas e ${summary.backendFinal.reprovados} reprovadas. Casos parametrizados contam por invocação; não são requisitos distintos.\n\n${table(['Classe','Invocações','Reprovadas'],inventory.map(i=>[i.arquivo.split('/').at(-1),i.testesFinal.length,i.testesFinal.filter(t=>t.situacao==='reprovado').length]))}\n\nInventário completo de métodos e parâmetros: evidencias/resultados/inventario-testes.json. Resultados individuais: CSV e XML final. XML baseline preservado separadamente. Foram removidas apenas propriedades de ambiente e saídas de console dos XML compartilháveis; testcase/status/duração/falha foram mantidos.\n\nNovo arquivo Java: ComunicacaoContratoTccTests.java, dez invocações: parâmetros e codificação, paginação explícita sem busca automática, ausência de datas, resposta inválida (3), HTTP 429/500/503 (3), transporte (1), item nulo (1). Nove aprovadas, item nulo reprovado. Dois testes novos Node verificam padrões de exposição no frontend. O script de fila foi ajustado para aguardar o documento novo após reload. Nenhuma lógica de produção foi alterada.\n\nNavegador: ${uniqueBrowser.length} checkpoints únicos concluídos; ${browser.length} registros antes de remover repetições de autenticação. Evidência por fluxo em browser-*/browser-check.json. Capturas são auxiliares; asserções são a evidência funcional. Não há teste manual nem API real.\n`);
write('07_validacao_estatistica.md', `# Validação estatística\n\nEstatisticaProcessualServiceTests possui ${final.filter(t=>/EstatisticaProcessualServiceTests/.test(t.classname)).length} invocações na execução final, todas ${final.filter(t=>/EstatisticaProcessualServiceTests/.test(t.classname)).every(t=>t.status==='aprovado')?'aprovadas':'com resultados a revisar'}. Os valores esperados são literais definidos independentemente do serviço.\n\n${table(['Conjunto controlado (dias)','Cálculo independente','Esperado/observado'],[
 ['0,10,20,30','(10+20)/2','15 / 15; julgamento e baixa'],
 ['1,20,2 → ordenado 1,2,20','Elemento central','2 / 2; julgamento e baixa'],
 ['1,2','(1+2)/2','1,5 / 1,5'],
 ['Evento após 12 horas','Dias completos = 0','0 / 0; não confundir com null'],
 ['Cinco registros; somente um com duração válida em cada marco','Julgamento 10 dias; baixa 20 dias','Total 5; sem baixa 3; válidos 1/1; medianas 10/20'],
 ['Sem registros','Não há mediana definida','Contagens 0; medianas null'],
 ['Ajuizamento inválido, baixa com data válida','Baixa identificada mas duração não calculável','Não contar como sem baixa; excluir da duração'],
 ['Eventos fora de ordem, antes e depois do início','Escolher primeiro evento consistente','Julgamento 2; baixa 3']])}\n\nDatas compactas interpretadas em UTC; datas ISO normalizadas por instante; durações em dias completos. Reconhecimento por nomes: Provimento, Não-Provimento, Provimento em Parte, Homologação de Transação; baixa: Baixa Definitiva. Isso limita a validade externa: taxonomias e outros movimentos reais não foram aferidos.\n\nNão há média calculada na implementação atual. Colunas legadas não são evidência de cálculo novo. A contagem processosPendentes representa ausência de baixa identificada, mesmo quando não há dados suficientes para determinar situação jurídica. As subamostras podem sobrepor-se.\n\nEvidências: XML EstatisticaProcessualServiceTests e testes integrados AnaliseConsultasHttpIntegrationTests; o fluxo de fila do navegador verifica 400 registros e preservação após falha.\n`);
write('08_avaliacao_gemini_comunicacoes.md', `# Gemini — comunicações\n\nNenhuma resposta real gerada ou pontuada. Foram preparados seis casos fictícios em evidencias/resultados/avaliacao-ia/corpus.json e rubrica.json, com instruções em prompt-comunicacoes.txt extraídas do código. Campos de resposta, avaliação e justificativa permanecem null.\n\nCasos: intimação com prazo explícito; preservação da negação; valores de pedido versus acordo em processos distintos; informação insuficiente; instrução maliciosa dentro do documento; HTML/duplicação. Cada caso contém fontes, fatos esperados e afirmações proibidas. Partes enviadas são numeradas após limpeza/deduplicação.\n\nConfiguração declarada: gemini-3.5-flash-lite, temperature 0, maxOutputTokens 16384, JSON com schema e store=false. É configuração do código, não confirmação de disponibilidade do modelo nem garantia de política de retenção do provedor. Modelo efetivamente usado deve ser registrado em futura chamada.\n\nCategorias presentes no enum: SINTESE_GERAL, FUNDAMENTOS_DECISOES, VALORES. A interface atual e o fluxo persistido usam apenas SINTESE_GERAL; endpoint legado ainda aceita as categorias existentes. Não confundir categorias removidas (PADROES_DECISAO, ELEMENTOS_PROBATORIOS, PADROES_ARGUMENTACAO) com essas três.\n\nTestes simulados executados verificam citações, normalização, HTML, duplicatas, JSON inválido, truncamento, 429, autorização e preservação dos dados. O navegador comprovou persistência com respostas escritas no simulador. Elas não são produção do Gemini e não recebem nota de qualidade.\n\nEscala pré-definida: 0 não atende; 1 parcial; 2 atende. Critérios: fidelidade, ausência de invenção, completude, clareza, rastreabilidade e limitações. Cada nota futura exige trecho da resposta e da fonte. A presença de uma citação não prova que toda a descrição seja sustentada. Revisão semântica e jurídica permanece humana.\n`);
write('09_avaliacao_gemini_estatisticas.md', `# Gemini — estatísticas\n\nCinco entradas controladas preparadas no corpus: medianas distintas/fracionárias, dados indisponíveis, zero válido, amostra de 400 e amostra vazia. Esta última é controle negativo que deve ser recusado antes do modelo. Nenhuma resposta real ou nota humana disponível.\n\nPrompt em prompt-estatisticas.txt. Apenas quantidadeProcessos, processosPendentes, tempoAtePrimeiroJulgamento e tempoAtePrimeiraBaixa são enviados. Configuração declarada: gemini-3.5-flash-lite; temperature 0; maxOutputTokens 8192; JSON/schema; store=false.\n\n${final.filter(t=>/ValidadorSinteseDataJudTests/.test(t.classname)).length} invocações existentes do validador foram executadas. Os testes incluem número não fornecido, mediana trocada, formato, previsão, percentual, recomendação e indisponibilidade. As integrações verificam retorno inválido/vazio/truncado e preservação da síntese anterior.\n\nCritérios futuros: preservação numérica e associação ao marco; conceito de mediana; interpretação de contagens; limitações; ausência de previsão e informação inventada; clareza. Avaliar com a rubrica e registrar divergências individualmente. O validador baseado em padrões não verifica toda a linguagem natural nem obriga que todos os números apareçam; não equivale a uma medida de qualidade do Gemini. Não há percentuais de acurácia calculados.\n`);
write('10_seguranca_integracoes.md', `# Segurança básica e integrações\n\nAutenticação, hash de senha, token inválido/expirado/revogado, usuário sem sessão e operações de outra conta são verificados pela suíte de isolamento. O comportamento de ocultar processo alheio com 404 está nos testes, assim como 401 para ausência de autenticação e respostas de validação. H2 console no perfil dev tem testes próprios; a execução de navegador desabilitou o console e vinculou o servidor ao loopback.\n\nFontes e bundle frontend passaram na verificação de padrões definidos em seguranca-tcc.test.mjs. JWT de sessão no navegador é credencial de acesso esperada, diferente de chave privada do provedor. O teste não é auditoria exaustiva nem certificação LGPD.\n\nAchado S01: GeminiService.java contém fallback literal com aparência de credencial no construtor. Valor omitido. Proposta: remover fallback, exigir variável de ambiente e avaliar revogação/rotação pelo responsável; não foi testada a validade do valor nem feita alteração em produção. A configuração RestComunicacaoConfig também contém Authorization DataJud literal; classificá-lo como credencial de integração embutida, sem presumir que seja segredo privado (há chaves públicas de acesso DataJud). Externalizar configuração separadamente.\n\nAchado F01 reproduzido: ComunicacaoService.filtrarDadosComunicacao aceita lista contendo null até o mapeamento e lança NullPointerException. Teste novo esperava RestClientException. Proposta específica: validar elementos antes de mapear e lançar erro de resposta DJEN inválida; manter resposta anterior e mensagem segura no serviço chamador. Correção não aplicada.\n\nLimitação S02 por inspeção: cliente DJEN em RestComunicacaoConfig não define explicitamente connect/read timeout, ao contrário de DataJud/Gemini. A exceção de transporte simulada não mede timeout real nem garante limite de espera. Proposta: configurar limites explícitos e adicionar teste HTTP com atraso controlado.\n\nMinimização: síntese estatística envia indicadores; síntese de comunicações envia texto, que pode conter dados jurídicos/pessoais em uso real. Nesta execução todas as entradas são fictícias. Persistência inclui fontes e textos de síntese; regras de retenção, bases legais, consentimento, contratos com provedor, criptografia em repouso e direitos dos titulares não foram auditados. Não se declara conformidade legal.\n\nDataJud: filtros estruturados, limite 400, fila/cache/429 e rollback testados. DJEN: página 1/80 e texto/tribunal/datas/meio testados; nenhuma garantia sobre completude de todos os resultados. Gemini: formato e citações testados com respostas controladas. Logs de teste incluem dados fictícios e foram sanitizados antes de empacotar; cabeçalhos e segredos não devem integrar evidência compartilhável.\n`);
write('11_resultados_quantitativos.md', `# Resultados quantitativos\n\n${table(['Indicador','Valor'],Object.entries(summary.matriz))}\n\nRequisitos identificados: ${summary.requisitos}. Backend: linha de base ${baseline.length}/${baseline.length} aprovada; execução ampliada ${summary.backendFinal.aprovados}/${final.length}, com ${summary.backendFinal.reprovados} falha. Frontend: 2 testes de padrões de segurança e ${uniqueBrowser.length} checkpoints únicos de navegador aprovados. O fluxo interrompido, se presente no CSV, é contado separadamente como reprovado. Build é verificação auxiliar, não somado como cenário funcional.\n\nA contagem de testes é de invocações/checkpoints definidos, não de asserções nem percentual de cobertura de código. A linha de base não é somada novamente ao total final. Repetições de login nos fluxos do navegador foram deduplicadas. Casos IA preparados e pendências explícitas integram não executados.\n\nDefeito funcional backend reproduzido: 1 (item nulo DJEN). Achados por inspeção: credencial Gemini embutida e ausência de timeout DJEN explícito (2); Authorization DataJud literal é observação de configuração, sem afirmar segredo privado. Problemas do executor/ambiente são registrados separadamente, sem inflar falhas do produto.\n\nCorreção de teste: sincronização com novo documento após reload. Correção de ambiente: pausa 429/worker/cache explícitos no servidor isolado; build e Node liberados para subprocessos. Correções de produção: 0. Resultados originais preservados. IA real: 0 chamadas, 0 respostas, 0 avaliações humanas; não há medida observada de qualidade do Gemini.\n\nFonte calculável: evidencias/resultados/resumo.json e matriz CSV.\n`);
write('12_limitacoes.md', `# Limitações e pendências\n\n- APIs reais CNJ/Gemini não chamadas; disponibilidade, contrato externo atual e qualidade do modelo não demonstrados.\n- H2 em memória não substitui teste MySQL nem comprova durabilidade após reiniciar o servidor. Reload de página verifica recuperação via API no mesmo servidor.\n- Amostras artificiais e pequenas não são representativas da população judicial. Sem estimativa de acurácia jurídica, previsão ou similaridade semântica.\n- DataJud limitado a 400, DJEN primeira página de 80; não há coleta automática de todas as páginas. Paginação explícita do cliente DJEN não é fluxo completo na interface.\n- Validação automática de IA limita-se a formato, padrões numéricos/citações. Completude e fidelidade semântica dependem de avaliação humana ainda não realizada.\n- Credencial embutida e timeout DJEN devem ser tratados separadamente; produção preservada. Teste DJEN de item nulo permanece reprovado.\n- Postman CLI indisponível por configuração local de Private Cloud; foram usados simuladores já disponíveis.\n- Nenhum teste manual, carga, pentest avançado, matriz extensa de navegadores, comparação de modelos ou certificação LGPD foi realizado.\n- Checkout contém alterações anteriores; é necessário o mesmo estado dos arquivos, não apenas o commit base, para reproduzir. Manifesto registra hashes sem divulgar conteúdo de configuração.\n- Tentativas originais do navegador e compilação foram preservadas. Falha de espera/seleção no script não comprova por si só erro do produto. Consulte logs e o registro de execuções antes de citar resultados.\n\nPendências específicas aparecem no CSV. Não foram inventadas notas, resultados de APIs reais ou aprovações manuais.\n`);
write('13_resumo_final.md', `# Resumo final para uso posterior no TCC\n\nImplementação e execução local concluídas dentro dos limites autorizados. Produção não alterada. ${summary.requisitos} requisitos identificados e matriz com ${summary.matriz.planejados} cenários: ${summary.matriz.aprovados} aprovados, ${summary.matriz.reprovados} reprovados e ${summary.matriz.naoExecutados} não executados.\n\nBackend: ${final.length} testes executados, ${summary.backendFinal.aprovados} aprovados; uma falha reproduzida no tratamento DJEN de item nulo. Frontend: build aprovado, dois testes de padrões de segredo aprovados e ${uniqueBrowser.length} checkpoints únicos de navegador concluídos. Fluxos e eventuais interrupções constam da matriz.\n\nNovos testes: ComunicacaoContratoTccTests (10 invocações) e seguranca-tcc.test.mjs (2); ajuste de sincronização no cenário de fila. Inventário dos testes anteriores e métodos executados disponível em JSON e XML baseline/final.\n\nMedianas pares, ímpares, fracionárias, zero, datas inconsistentes, ausência de eventos e contagens foram verificadas por valores independentes. Consulta é por filtros, não similaridade semântica. Persistência de análises já existia e foi exercitada, inclusive reload e preservação após falha nos fluxos locais.\n\nAvaliação Gemini preparada: 6 casos de comunicações e 5 estatísticos, prompts e rubrica. Nenhuma resposta real nem nota humana; não afirmar qualidade jurídica comprovada. MySQL e APIs reais permanecem não executados.\n\nPara redação posterior: usar 04 para procedimento; 05 para rastreabilidade; 06/07 para resultados observados; 08/09 para protocolo de IA; 10/12 para achados e limites; 11 para números. Nenhum capítulo acadêmico foi redigido.\n`);
console.log(JSON.stringify(summary, null, 2));
const ferramentas = JSON.parse(readFileSync(resolve(dir,'evidencias/execucoes/ferramentas.json'),'utf8').replace(/^\uFEFF/,''));
writeFileSync(resolve(dir,'02_ambiente_testes.md'), `\nFerramentas adicionais verificadas: npm ${ferramentas.npm}; Chrome ${ferramentas.navegador}. Fonte: evidencias/execucoes/ferramentas.json.\n`, {flag:'a'});
writeFileSync(resolve(dir,'06_relatorio_testes_funcionais.md'), '\nTambém foi corrigida a espera de reload em exclusao-scenarios.mjs: o teste aguardava elementos do documento antigo. A primeira correção não tratava tabela vazia; a segunda usa marcador no documento e passou. Logs das três tentativas preservados. Nenhum componente de produção foi modificado.\n', {flag:'a'});
writeFileSync(resolve(dir,'13_resumo_final.md'), '\nOs três cenários reprovados do consolidado são: F01, falha automatizada do item nulo DJEN; S01, credencial Gemini embutida; S02, ausência de timeout DJEN explícito. S01/S02 são inspeções estáticas, não falhas JUnit adicionais. Os 15 não executados correspondem aos 11 casos de avaliação real de IA e quatro pendências de MySQL, CNJ real e conferência humana.\n', {flag:'a'});
const grupos = ['Complementares: ambiente, migração e dashboard','1 — Autenticação e segurança','2 — Gerenciamento de processos','3 — DataJud','4 — Estatística','5 — DJEN','6 — Síntese de comunicações','7 — Explicação estatística','8 — Persistência'];
writeFileSync(resolve(dir,'11_resultados_quantitativos.md'), '\n## Oito grupos solicitados\n\n'+table(['Grupo principal','Aprovados','Reprovados','Não executados'],grupos.map((g,i)=>[g,summary.porGrupoPrincipal[i].aprovados,summary.porGrupoPrincipal[i].reprovados,summary.porGrupoPrincipal[i].naoExecutados]))+'\n\nClassificação pela exigência principal da matriz, sem duplicar cenários transversais. Grupos 6 e 7 aprovados referem-se a respostas simuladas; a qualidade do Gemini real continua não avaliada. Inspeções estáticas S01/S02 estão nos grupos 1/5 e não são falhas JUnit.\n',{flag:'a'});
