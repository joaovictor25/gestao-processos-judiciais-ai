# Axion — frontend

Frontend em React, Vite e TypeScript, com React Router DOM, Context API, Fetch nativo e CSS puro. Autenticação, dashboard e gestão de processos seguem as imagens fornecidas. O backend permanece em `../backend`.

## Executar

Requisito: Node.js 22.12 ou superior. A implementação foi verificada com Node 24.11.1.

Inicie o Spring Boot na porta 8080 em um terminal, na pasta `backend`:

```powershell
.\mvnw.cmd spring-boot:run
```

Em outro terminal, na pasta `frontend`:

```powershell
npm install
Copy-Item .env.example .env
npm run dev
```

Acesse `http://localhost:5173`. Crie uma conta na tela de cadastro e faça login; não há credenciais nem login fictício embutidos. O banco padrão do backend é H2 em memória: as contas precisam ser recriadas após reiniciar o backend.

```powershell
npm run build
npm run preview
```

`build` verifica o TypeScript e gera `dist/`. `preview` serve o resultado em `http://localhost:4173`. `npm run typecheck` executa apenas a verificação de tipos.

Para testar a autenticação no `preview` sem configurar CORS, crie `.env.production.local` com `VITE_API_URL=` antes do build. Assim, o navegador usa `/api` e o proxy local do preview encaminha ao backend padrão na porta 8080. Um build com URL absoluta exige o CORS descrito abaixo, inclusive no preview.

## Ambiente e API

```dotenv
VITE_API_URL=http://localhost:8080
VITE_DASHBOARD_DEMO=true
```

Durante o desenvolvimento, as chamadas usam `/api` na mesma origem do frontend. O proxy do Vite encaminha para `VITE_API_URL`, evitando a necessidade de alterar o CORS do backend. A URL não aparece em componentes. Reinicie o Vite após mudar o arquivo `.env`.

Em produção, a camada HTTP utiliza `VITE_API_URL` definido **durante o build**. Se o frontend e a API forem hospedados em origens diferentes, configure CORS para a origem exata no Spring Boot. Como alternativa, deixe `VITE_API_URL` vazio no build e encaminhe `/api` para o backend no proxy do servidor. A configuração `server.proxy` do Vite é de desenvolvimento, conforme a [documentação do Vite](https://vite.dev/config/server-options.html#server-proxy). O proxy equivalente de `preview` existe somente para verificação local.

O servidor que hospedar `dist/` deve encaminhar rotas como `/login`, `/cadastro`, `/dashboard` e `/processos/1` para `index.html`, preservando `/api` e os arquivos estáticos. Variáveis `VITE_*` são públicas no navegador; não coloque segredos nelas.

| Método | Endpoint existente | Uso |
| --- | --- | --- |
| POST | `/api/auth/cadastro` | Envia somente `nome`, `email`, `senha` |
| POST | `/api/auth/login` | Envia `email`, `senha`; recebe token e identificação |
| GET | `/api/auth/me` | Recupera a conta ao recarregar a página |
| POST | `/api/auth/logout` | Revoga o token atual; aceita resposta 204 sem corpo |
| GET | `/api/processo/listar-usuario` | Lista os processos da conta autenticada |
| GET | `/api/processo/{id}` | Carrega cadastro completo para detalhes e edição |
| GET | `/api/processo/consultar-datajud?numeroProcesso=...` | Consulta pública do CNJ exato pelo backend, com JWT, para preencher o cadastro |
| POST | `/api/processo/cadastrar` | Cadastra um processo vinculado à conta |
| PUT | `/api/processo/atualizar/{id}` | Atualiza os dados do processo existente |
| GET | `/api/dados/analise/{id}` | Consulta presença de resultados para a listagem |
| GET | `/api/dados/estatisticas/{id}` | Recupera as estatísticas DataJud salvas |
| GET | `/api/dados/comunicacoes/{id}` | Recupera as comunicações salvas |
| POST | `/api/dados/comunicacoes/{id}/analise-ia` | Analisa as categorias escolhidas usando apenas os textos salvos da conta |
| POST | `/api/dados/analisar/{id}` | Executa análise e substitui os resultados desse processo; resposta em texto |

O cadastro público não envia `id`, `role`, proprietário ou outros campos internos. Confirmação de senha é validada apenas no formulário. O nome tem limite de 255 caracteres, e-mail de 254; a senha respeita mínimo de 6 caracteres no cadastro e máximo de 72 bytes UTF-8 do backend.

O JWT fica em `sessionStorage`, somente para a aba atual; senha e dados pessoais não são persistidos pelo frontend. A Context API mantém o usuário retornado pela API. Cada rota protegida exige uma sessão carregada. Uma resposta 401 em chamada autenticada limpa a sessão; falhas de rede não são tratadas como credenciais inválidas. Se o logout falhar por erro de conexão, o usuário recebe feedback e pode tentar novamente, sem exibir um falso sucesso de revogação.

## Telas e escopo

- `/login`: autenticação real, validação, mostrar/ocultar senha, loading e erros seguros.
- `/cadastro`: cadastro real e redirecionamento para login com confirmação de sucesso.
- `/dashboard`: layout protegido, menu responsivo, quatro indicadores e gráfico em CSS com o card de atividades recentes ao lado nas telas maiores.
- `/processos`: listagem real, indicadores da carteira, busca por número/cliente, paginação 10/20/50 e ações Ver/Analisar.
- `/processos/novo`: formulário completo, dividido em cinco seções, validação, moeda brasileira e cadastro real.
- `/processos/:id/editar`: mesmo formulário com dados carregados, atualização por PUT e tratamento de processo inexistente ou indisponível para a conta.
- `/processos/:id`: página única de detalhes, aberta pelo botão Ver. Cabeçalho compartilhado e abas Informações gerais, DataJud e Comunicações, sem trocar a rota. Editar abre o formulário; Solicitar análise atualiza os resultados salvos.

O formulário adapta os nomes visuais aos DTOs existentes: **Data de distribuição** é enviada como `dataInicio`; órgão, classe e assunto são objetos `{ nome, codigo }`, com códigos em texto. A moeda é convertida para número decimal ao enviar. Campos opcionais vazios não recebem valores fictícios. O frontend não envia proprietário nem papel do usuário nos processos.

Na aba DataJud, os três indicadores mostram mediana até o primeiro julgamento, mediana até a primeira baixa e quantidade de processos pendentes. “Ver todas as estatísticas” abre um diálogo com as medidas da categoria. Zero é preservado como valor válido; ausência de amostra é apresentada com `—`. Em registros antigos com mais de uma estatística, usa-se a última da lista ordenada pelo backend.

Comunicações possui busca somente no texto, paginação 20/50/100 e modal de detalhes aberto pelo botão Ver. O modal fecha pelo X, pelo botão Fechar, pela tecla Esc ou pelo fundo, devolvendo o foco ao botão de origem. HTML recebido é convertido para texto e nunca inserido como HTML executável. Os detalhes mostram o código do órgão disponibilizado pelo DTO, sem deduzir seu nome a partir do processo cadastrado.

Na aba Comunicações, o card lateral **Análise das comunicações** permite escolher uma ou várias categorias. O botão é habilitado apenas com comunicações salvas e ao menos uma categoria marcada. Enquanto a solicitação está em andamento, bloqueia novos envios e mostra carregamento. O resultado aparece na área principal por categoria, com trechos literais que sustentam os achados, limitações da amostra e aviso de que não é previsão judicial ou recomendação jurídica. Alterar a seleção limpa o resultado anterior. A chave do Gemini é configurada somente no backend; veja `backend/README.md`.

### Limites do contrato atual

- A consulta automática de cadastro pesquisa o tribunal de origem do CNJ, conforme os detalhes abaixo; um resultado vazio não prova a inexistência do processo.

- A API não informa datas de atualização das análises. A interface indica “Data não informada”.
- A síntese por IA das estatísticas DataJud ainda está identificada como “Em breve”. A análise das comunicações funciona pelo endpoint separado descrito acima quando o backend possui `GEMINI_API_KEY`.
- A solicitação de análise é síncrona. Os botões ficam bloqueados enquanto a requisição está em andamento na página; não há acompanhamento de tarefas em segundo plano após recarregar ou sair da página. Uma perda de conexão pode impedir a confirmação mesmo se o servidor concluir o trabalho.
- A listagem consulta os resultados de cada processo com até quatro consultas simultâneas, pois não há resumo em lote. “Aguardando análise” representa processos sem resultados salvos; não é uma fila de tarefas do servidor. Resultado vazio não permite distinguir uma consulta sem ocorrências de uma análise nunca executada.
- Busca e paginação são locais, sobre as listas retornadas pela API. Nenhuma leitura das abas executa automaticamente uma nova análise externa.

O backend atual não possui número ou UF da OAB em `Usuario` nem nos DTOs. Esses campos aparecem desabilitados e identificados como futuros; nenhum valor é coletado e descartado silenciosamente. O perfil mostra somente nome e papel reais do `AuthContext`, sem OAB fictícia. Telefone não integra o contrato de cadastro. Recuperação de senha tem um aviso de indisponibilidade, pois não existe endpoint correspondente.

Os indicadores agregados e o feed de atividades não têm endpoint próprio. Os exemplos ficam em `src/mocks/dashboard.ts`, identificados como demonstração na interface, sem chamadas a endpoints inventados nem associação à conta autenticada. “Ocultar exemplos” apresenta estados vazios. Para iniciar sem exemplos, defina `VITE_DASHBOARD_DEMO=false`. Para a futura integração, `ProcessoResumo` respeita os campos de `ProcessoResponseDTO`; `AtividadeRecente` e `DashboardResumo` são modelos de apresentação, não DTOs atribuídos à API.

### Consulta automática no cadastro de processo

Em `/processos/novo`, digitar ou colar um CNJ completo e com dígito verificador válido inicia a consulta após 600 ms sem alteração. Aceita número com ou sem pontuação. O frontend chama somente o backend; não recebe a chave de acesso do DataJud.

Os dados compatíveis preenchem **tribunal, grau, órgão julgador e código, classe processual e código, assunto e código e data de distribuição**. Dados já digitados são preservados, inclusive durante a espera. Trocar o CNJ limpa apenas valores importados automaticamente e cancela a requisição anterior, impedindo que uma resposta atrasada preencha outro processo. Nome e código de cada classificação são tratados juntos para não misturar uma edição manual com um código importado antigo.

Se o DataJud retornar vários registros para o mesmo CNJ, aparece uma seleção de grau/órgão. Se retornar vários assuntos, aparece a seleção de assunto. Cliente, posição, comarca, UF, área, status, valor, magistrado e data final continuam manuais. Informações ausentes permanecem vazias.

A busca não salva nem analisa o processo. Revise e complete o formulário antes de clicar em **Cadastrar processo**. Não encontrado, limite de consultas, indisponibilidade e espera acima de 30 segundos exibem orientação e permitem o preenchimento manual. Não há repetição automática após erro; o botão **Tentar novamente** inicia outra tentativa. Resultados bem-sucedidos ficam em memória apenas durante a abertura desse formulário. A edição de processos existentes não dispara essa consulta.

## Organização e manutenção

```text
src/
  assets/                  # Imagem de referência local
  components/
    forms/                 # Input, Select e validação simples
    layout/                # AuthLayout, AppLayout, Header e Sidebar
    ui/                    # Button e ícones SVG
    processos/             # Formulário, tabela, paginação e estados reutilizáveis
      detalhes/            # Cabeçalho, abas, estatísticas e comunicações
  contexts/AuthContext.tsx
  mocks/dashboard.ts
  pages/                   # Login, Cadastro, Dashboard e Processos
  routes/ProtectedRoute.tsx
  services/                # api.ts, authService.ts e processoService.ts
  styles/                  # theme.css e globals.css
  types/                   # Contratos e modelos de apresentação
  App.tsx
  main.tsx
```

As cores e os tokens de identidade estão em **`src/styles/theme.css`**. Os demais estilos usam CSS Variables. Formulários e painel têm estilos próprios; não há bibliotecas de UI, ícones, gráficos ou formulários. O gráfico utiliza `conic-gradient` com legenda textual; ícones são SVG inline. A fotografia fornecida é um asset local, enquadrado em uma janela SVG, sem dependência de serviços externos.

Para adicionar uma página, crie o componente em `pages` e uma rota sob `AppLayout` em `App.tsx`. Chamadas HTTP passam pelos services. O formulário de cadastro/edição compartilha validação e conversão em `processoFormModel.ts`; as abas compartilham o ciclo de carregamento e cancelamento em `useDadosProcesso.ts`.

Dependências de execução: `react`, `react-dom`, `react-router-dom`. Desenvolvimento: `vite`, `typescript`, `@vitejs/plugin-react`, `@types/react`, `@types/react-dom`, `@types/node`. O `package-lock.json` registra as versões instaladas.

## Verificação no navegador

O script opcional `scripts/browser-check.mjs` usa Node e o protocolo DevTools de Chrome/Edge instalado, sem acrescentar dependências. **Use um backend isolado**, pois ele cria contas e processos de verificação. Exemplo no Windows:

```powershell
# Terminal 1, em backend
.\mvnw.cmd -B -ntp spring-boot:run '-Dspring-boot.run.arguments=--server.port=8081 --spring.datasource.url=jdbc:h2:mem:frontend-verificacao'

# Terminal 2, em frontend
$env:VITE_API_URL = 'http://127.0.0.1:8081'
npm run dev -- --host 127.0.0.1

# Terminal 3, em frontend
node scripts/browser-check.mjs
```

Opcionalmente defina `BROWSER_PATH` e `FRONTEND_TEST_URL`. O script verifica autenticação real, CRUD de processos, isolamento entre contas, máscaras e datas, cancelamento, erros, busca e paginação. Também testa as três abas, atualização após análise, medidas sem amostra, teclado, modal de comunicação e texto seguro. As operações de cadastro/consulta/edição usam Spring Boot/H2 reais; chamadas de análise externa, resultados preenchidos e falhas HTTP são interceptados **apenas nos testes**. As fixtures estão em `scripts/detalhes-fixtures.mjs`; nenhum componente de produção importa esses dados. Não são chamadas as APIs externas DataJud/DJEN durante essa verificação.

O script captura telas em desktop, tablet e celulares (incluindo 320 px), verifica ausência de rolagem horizontal da página e registra exceções JavaScript. As imagens e o relatório ficam em `artifacts/`, ignorados pelo Git. O perfil temporário do navegador também é ignorado.

Para verificar também uma análise **real** com as integrações CNJ, execute o script com `ANALISE_LIVE=1` (PowerShell: `$env:ANALISE_LIVE = '1'`). Essa opção acrescenta uma consulta DataJud/DJEN para um processo de teste sem órgão julgador, verifica a gravação e a exibição nas abas. Depende da disponibilidade externa e da configuração de acesso do backend.

Falhas identificadas das integrações mostram qual consulta falhou (DataJud ou DJEN). Como a análise completa é transacional, uma falha de comunicações também impede a substituição das estatísticas DataJud. O frontend reconhece somente códigos de erro conhecidos, sem apresentar conteúdo técnico arbitrário da resposta.

A consulta automática de cadastro acrescenta o endpoint autenticado `GET /api/processo/consultar-datajud` ao backend Java. O contrato, o mapeamento dos campos e as referências oficiais estão em `backend/README.md`. Os cenários em `scripts/cnj-scenarios.mjs` verificam o preenchimento, cancelamento, preservação de campos manuais, seleção de registros/assuntos, tratamento de falhas e cadastro real; apenas a resposta HTTP do DataJud é controlada no navegador.
