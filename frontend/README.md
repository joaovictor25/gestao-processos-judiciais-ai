# Frontend

Interface em React, TypeScript e Vite. Para instalar os requisitos e iniciar frontend e backend, siga o [guia de instalação na raiz](../README.md).

## Executar em desenvolvimento

Requisito: Node.js 22.12 ou superior, com npm. O fluxo foi validado com Node.js 24.11.1 e npm 11.6.2.

Inicie primeiro o backend e aguarde a mensagem `Started BackendApplication`. Em outro terminal, **dentro da pasta `frontend`**, execute os comandos abaixo. Copie o arquivo de exemplo somente na primeira instalação.

Windows — PowerShell:

```powershell
Copy-Item .env.example .env
npm.cmd ci
npm.cmd run dev
```

Linux/macOS:

```sh
cp .env.example .env
npm ci
npm run dev
```

Abra **http://localhost:5173**. Crie sua conta em `/cadastro` e faça login. Não há login fictício nem credenciais padrão. O H2 do backend é temporário: após reiniciar o backend, crie a conta novamente.

`npm ci` instala as versões do `package-lock.json`. Nas próximas execuções, basta `npm.cmd run dev` no Windows ou `npm run dev` no Linux/macOS. Reinstale dependências quando o lockfile mudar.

## Ambiente e conexão com a API

O arquivo `frontend/.env` deve conter:

```dotenv
VITE_API_URL=http://localhost:8080
```

Durante o desenvolvimento, o navegador envia as chamadas para `/api`, na mesma origem do frontend. O proxy do Vite as encaminha para o backend indicado em `VITE_API_URL`. Se o backend estiver em outra porta, altere o arquivo e reinicie o Vite.

O dashboard consulta dados da conta autenticada. Não é necessário configurar uma opção de demonstração. Uma conta nova começa sem processos.

Variáveis `VITE_*` são públicas no navegador. Configure a chave Gemini somente no ambiente do backend, seguindo o [guia principal](../README.md#6-habilitar-as-sínteses-com-gemini--opcional).

## Gerar o build e testar a prévia local

`build` verifica o TypeScript e gera os arquivos estáticos em `dist/`. `preview` permite conferir esses arquivos localmente na porta **4173**, com o backend ainda em execução na **8080**.

Para essa prévia, crie ou ajuste `.env.production.local` com `VITE_API_URL=`. O valor vazio faz o bundle usar `/api`, que o proxy de `preview` encaminha para o backend local. Execute dentro de `frontend`:

Windows — PowerShell:

```powershell
Set-Content -Path .env.production.local -Value 'VITE_API_URL='
npm.cmd run build
npm.cmd run preview
```

Linux/macOS:

```sh
printf '%s\n' 'VITE_API_URL=' > .env.production.local
npm run build
npm run preview
```

Abra **http://localhost:4173**. Não abra `dist/index.html` diretamente pelo explorador de arquivos. Encerre a prévia com `Ctrl+C`. O arquivo `.env.production.local` é local e está ignorado pelo Git; ele não altera a configuração de desenvolvimento usada por `npm run dev`.

Uma variável `VITE_API_URL` definida diretamente no ambiente do terminal tem prioridade sobre os arquivos `.env`. Se o bundle ainda usar uma URL antiga, confira esse ambiente, ajuste a configuração e gere o build novamente.

### Publicar o frontend em outro ambiente

O valor de `VITE_API_URL` é incorporado ao JavaScript **durante o build**. Mudar o `.env` depois de gerar `dist/` não altera o bundle já gerado.

Se a aplicação publicada usar `/api`, o servidor de hospedagem precisa encaminhar essa rota ao backend. Os proxies de `dev` e `preview` não são incluídos em `dist/`. Se usar uma URL de API de outra origem, configure o CORS correspondente no backend.

O servidor também precisa encaminhar rotas da interface, como `/login`, `/dashboard` e `/processos/1`, para `index.html`, preservando os arquivos estáticos e a rota da API. `npm run preview` serve para conferência local do build.

## Verificações adicionais

Dentro de `frontend`, use `npm.cmd` no Windows e `npm` no Linux/macOS:

| Comando | Verificação |
| --- | --- |
| `npm run typecheck` | Verifica o TypeScript |
| `npm run build` | Verifica os tipos e produz `dist/` |
| `node --test scripts/seguranca-tcc.test.mjs` | Verifica padrões de credenciais nas fontes e no bundle; execute o build antes |
| `npm audit` | Consulta os avisos de segurança das dependências instaladas |

Na validação desta revisão, `npm ci` e o build concluíram, mas a auditoria apontou dois alertas de severidade alta em `vite` e `source-map-js`. As dependências não foram alteradas nesta revisão de documentação. Esses alertas devem ser tratados antes de expor a aplicação em um ambiente público.

O script `scripts/browser-check.mjs` contém cenários opcionais de navegador e usa Chrome/Edge instalado. Ele cria contas, cadastra e exclui processos; utilize um backend isolado para esses testes. Os artefatos gerados ficam em `artifacts/`, ignorados pelo Git.

## Funcionamento das telas

- **Cadastro e login:** usam a autenticação real do backend; o token fica no `sessionStorage` da aba.
- **Dashboard e processos:** exibem dados da conta autenticada.
- **Consulta pelo CNJ:** depende do backend e da disponibilidade do DataJud; os campos podem ser revisados antes de cadastrar o processo.
- **DataJud:** permite solicitar estatísticas, acompanhar a fila e recuperar resultados salvos. O acompanhamento pode ser retomado ao reabrir a aba.
- **Comunicações:** pesquisa publicações por texto e período; as sínteses com IA dependem da configuração Gemini no backend.
- **Recuperação de senha:** ainda não possui um fluxo implementado; a tela informa essa limitação.

As configurações da fila e do banco estão no [README do backend](../backend/README.md#fila-de-consultas-datajud).
