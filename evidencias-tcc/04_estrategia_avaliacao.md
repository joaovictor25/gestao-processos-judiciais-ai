# Estratégia reproduzível

## Escopo e critérios definidos

Reutilizar JUnit/AssertJ/MockMvc, H2 e scripts Node/Chrome existentes. Complementar apenas lacunas relevantes. Não usar banco real, dados pessoais reais, chamadas CNJ/Gemini reais, carga ou mudanças de produção. As oito áreas são autenticação, CRUD, DataJud, estatística, DJEN, síntese de comunicações, explicação estatística e persistência de análises.

Uma invocação JUnit aprova somente se todas as suas asserções passam. Parâmetros contam como invocações separadas, identificadas pelo XML. Checkpoint de navegador só aprova quando o script o registra após suas asserções; autenticação repetida entre fluxos conta uma vez no consolidado. Build é verificação auxiliar. Inspeções estáticas são identificadas separadamente. Testes simulados comprovam comportamento contra aquelas respostas, não contra o serviço público atual.

Estatística: comparar valores literais obtidos por ordenação/aritmética independente, sem chamar a função sob teste para gerar o esperado. Não introduzir novas métricas. Gemini: corpus e rubrica congelados antes de qualquer geração; ainda não houve geração real. A pontuação e a justificativa humanas estão pendentes. Não interpretar coerência ou citação como correção jurídica.

## Pré-requisitos

Mesmo checkout (inclusive alterações locais anteriores; conferir manifesto SHA-256), JDK compatível com release 25, Maven Wrapper, Node >=22.12 e dependências do lockfile. Nesta execução foram usados Java 26, Maven 3.9.16 e Node 24.11.1. Chrome/Edge com modo headless para navegador. Dependências já estavam instaladas; não houve instalação. Se necessário em outra máquina, resolver Maven e executar npm ci antes, com acesso aos repositórios de pacotes.

## Backend isolado

Da raiz, executar `./evidencias-tcc/executar-backend.ps1`. O script cria uma pasta nova por execução, guarda código de saída e XML sanitizado; não sobrescreve baseline/final. Uma falha é esperada enquanto F01 (item nulo DJEN) não for corrigida. Não remover ou afrouxar a asserção para obter aprovação.

Equivalente, no diretório backend:

```powershell
.\mvnw.cmd -B -ntp '-Dspring.profiles.active=test' '-Dspring.datasource.url=jdbc:h2:mem:tcc-validacao' '-Dspring.jpa.hibernate.ddl-auto=create-drop' '-Dapp.datajud.base-url=http://127.0.0.1:1' '-Dapp.djen.base-url=http://127.0.0.1:1' '-Dapp.gemini.base-url=http://127.0.0.1:1' '-Dapp.gemini.api-key=teste-local' test
```

Classes de integração podem substituir o H2 por outro nome em memória e provedores por MockRestServiceServer ou servidor loopback efêmero. O worker do perfil test é desabilitado. Os testes apagam somente seus bancos em memória. O teste de console H2 ativa também dev de propósito; essa verificação não significa recomendação de expor console em produção.

## Frontend

No diretório frontend:

```powershell
npm.cmd run build
node --test scripts/seguranca-tcc.test.mjs
```

Para navegador, iniciar três terminais. Terminal 1, raiz:

```powershell
./evidencias-tcc/iniciar-backend-browser.ps1
```

Terminal 2, frontend:

```powershell
$env:VITE_API_URL='http://127.0.0.1:18081'
npm.cmd run dev -- --host 127.0.0.1 --port 15173
```

Terminal 3, frontend; executar os escopos sequencialmente, arquivando `artifacts/browser-check.json` antes do próximo, pois o script sobrescreve esse arquivo:

```powershell
$env:FRONTEND_TEST_URL='http://127.0.0.1:15173'
$env:BROWSER_DEBUG_PORT='19229'
$env:BROWSER_PROFILE='.browser-tcc'
Remove-Item Env:ANALISE_LIVE -ErrorAction SilentlyContinue
Remove-Item Env:DATAJUD_LOCAL_HTTP -ErrorAction SilentlyContinue
$env:BROWSER_SCOPE='comunicacoes' # depois: datajud-fila, detalhes, exclusao
node scripts/browser-check.mjs
```

Portas 8092 e 8093 devem estar livres: os próprios cenários iniciam/encerram provedores simulados. `comunicacoes` e `datajud-fila` exercitam HTTP real até simuladores; `detalhes` também usa interceptação DevTools. Todas as URLs do servidor estão em loopback. Não habilitar ANALISE_LIVE. Não executar esses scripts contra banco do usuário. O JWT do script de inicialização é exclusivamente fictício.

`spring-boot:run` não inclui src/test/resources: por isso timeout de espera 429, cache e worker são passados explicitamente. A tentativa inicial do navegador manteve pausa padrão de 30 s e não atendia ao cenário que aguardava 5 s; foi registrada e repetida com o ambiente correto.

## Evidências e contagem

`evidencias/execucoes/registro.md` descreve as tentativas. Os XML baseline e final conservam resultados por método; properties e stdout/stderr foram retirados para não distribuir ambiente/credenciais. Logs separados preservam resultados, com sanitização de segredos e tokens. Um XML residual de DataJudConsultaUnicaHttpTests, anterior à tarefa, foi excluído do consolidado: a classe não aparece em `[INFO] Running` destas execuções nem tem fonte atual. Essa checagem evita contar artefatos antigos como testes executados.

Inventário, manifesto, ambiente, matriz e resumo são gerados por `node evidencias-tcc/gerar-relatorios.mjs` a partir dos XML/logs selecionados e fontes atuais. Esse gerador não executa testes nem deve ser usado para atribuir aprovação a logs de outra execução. `node evidencias-tcc/preparar-avaliacao.mjs` recria corpus e prompts offline; não executar sobre anotações humanas futuras sem preservá-las.

Não se usa número de arquivos como medida de cobertura. Não foi executada instrumentação de cobertura de linhas; não há percentual correspondente.
