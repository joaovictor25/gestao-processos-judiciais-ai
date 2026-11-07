# Gestão de processos judiciais com IA

Aplicação para cadastrar usuários e processos, consultar publicações do DJEN, obter estatísticas do DataJud e gerar sínteses com Gemini.

Este guia explica como baixar e executar o projeto **na sua máquina, em ambiente de desenvolvimento**. A aplicação usa dois programas, que ficam abertos em terminais separados:

| Programa | Função | Endereço padrão |
| --- | --- | --- |
| Backend — Java/Spring Boot | Autenticação, banco e integrações | `http://localhost:8080` |
| Frontend — React/Vite | Telas acessadas pelo navegador | **http://localhost:5173** |

**Sobre os dados:** o banco padrão é H2 em memória. Ele é criado automaticamente, mas **reiniciar o backend apaga os usuários, processos e resultados cadastrados nessa execução**. Não é necessário instalar MySQL para seguir este guia.

## 1. Instalar os requisitos

| Ferramenta | Versão | Download |
| --- | --- | --- |
| Java **JDK** | 25 ou superior; para uma instalação nova, use JDK 25 LTS | [Eclipse Temurin](https://adoptium.net/temurin/releases/?version=25) — selecione JDK, seu sistema e sua arquitetura |
| Node.js com npm | 22.12 ou superior; Node 24 LTS é uma opção compatível | [Node.js](https://nodejs.org/en/download) |
| Git | Necessário se escolher a opção de clonar | [Git](https://git-scm.com/install/) |

Instale o **JDK completo**, que inclui `java` e `javac`. O Maven já é fornecido pelo projeto através do Maven Wrapper (`mvnw` e `mvnw.cmd`).

Após instalar, feche e abra o terminal para atualizar o `PATH`. Confira:

**Windows — PowerShell:**

```powershell
java -version
javac -version
node --version
npm.cmd --version
git --version
```

**Linux/macOS — terminal:**

```sh
java -version
javac -version
node --version
npm --version
git --version
```

`java` e `javac` devem indicar a versão 25 ou superior. Se algum comando não for encontrado, corrija a instalação antes de continuar; `git --version` pode ser ignorado se você optar pelo ZIP. No Windows, este guia usa `npm.cmd` para funcionar também quando o PowerShell bloqueia scripts `.ps1`.

## 2. Baixar o projeto

Escolha **uma** das opções.

### Opção A — clonar com Git

No terminal, entre na pasta em que deseja guardar o projeto e execute:

```sh
git clone https://github.com/joaovictor25/gestao-processos-judiciais-ai.git
cd gestao-processos-judiciais-ai
```

### Opção B — baixar o ZIP

1. Abra o [repositório no GitHub](https://github.com/joaovictor25/gestao-processos-judiciais-ai).
2. Na branch `main`, clique em **Code → Download ZIP**.
3. Extraia o ZIP. Não execute os arquivos de dentro do arquivo compactado.
4. Abra um terminal na pasta extraída, normalmente `gestao-processos-judiciais-ai-main`.

Nos próximos passos, **raiz do projeto** significa a pasta que contém `README.md`, `backend` e `frontend`. Se o caminho tiver espaços, use aspas ao navegar, por exemplo: `cd "C:\Meus projetos\gestao-processos-judiciais-ai"`.

## 3. Iniciar o backend — terminal 1

Abra um terminal na **raiz do projeto**. Para a primeira execução, não é necessário configurar chaves: cadastro e login funcionam com a configuração padrão. A IA é configurada separadamente no passo 6.

**Windows — PowerShell:**

```powershell
cd backend
.\mvnw.cmd -version
.\mvnw.cmd spring-boot:run
```

**Linux/macOS:**

```sh
cd backend
sh ./mvnw -version
sh ./mvnw spring-boot:run
```

O comando `-version` mostra o Java utilizado pelo Maven; confira que é 25 ou superior. Usar `sh ./mvnw` permite executar o wrapper mesmo quando o ZIP não preserva a permissão de execução do arquivo.

Na primeira vez, o Maven baixa suas dependências. Aguarde até aparecerem mensagens semelhantes a:

```text
Tomcat started on port 8080 (http)
Started BackendApplication
```

O perfil utilizado deve ser `dev`. A mensagem `JWT_SECRET ausente no perfil dev` é esperada: o programa cria uma chave temporária para autenticação.

**Mantenha esse terminal aberto.** Ele ficará ocupado enquanto o backend estiver rodando. Se aparecer `BUILD FAILURE` e o comando encerrar, consulte a seção de problemas comuns antes de iniciar o frontend.

## 4. Iniciar o frontend — terminal 2

Abra **outro terminal na raiz do projeto**, sem fechar o backend.

**Windows — PowerShell:**

```powershell
cd frontend
Copy-Item .env.example .env
npm.cmd ci
npm.cmd run dev
```

**Linux/macOS:**

```sh
cd frontend
cp .env.example .env
npm ci
npm run dev
```

A cópia do `.env.example` é feita na primeira instalação. Se já tiver um `.env` configurado, mantenha o arquivo existente. `npm ci` instala as versões registradas no `package-lock.json`.

O arquivo `frontend/.env` deve conter:

```dotenv
VITE_API_URL=http://localhost:8080
```

Aguarde a mensagem do Vite com o endereço:

```text
Local: http://localhost:5173/
```

**Mantenha também esse terminal aberto.** No desenvolvimento, o Vite encaminha as chamadas `/api` ao backend indicado em `VITE_API_URL`.

## 5. Acessar e criar sua conta

1. Abra **http://localhost:5173** no navegador.
2. Clique em **Criar conta**, ou acesse diretamente **http://localhost:5173/cadastro**.
3. Preencha nome, e-mail, senha com pelo menos 6 caracteres e confirmação da senha.
4. Após o cadastro, faça login com o e-mail e a senha informados.
5. O sistema abrirá o dashboard. Use **Processos** para acessar os cadastros.

Não há usuário ou senha padrão. A aplicação usa a conta que você acabou de criar. Uma instalação nova começa sem processos cadastrados.

Cadastro, login e acesso à sua conta permitem conferir a instalação sem configurar Gemini. Consultas DataJud/DJEN exigem conexão com os respectivos serviços; sínteses de IA exigem a chave do próximo passo. A disponibilidade de uma API externa não determina se os dois programas locais iniciaram corretamente.

## 6. Habilitar as sínteses com Gemini — opcional

1. Obtenha uma chave no [Google AI Studio](https://aistudio.google.com/app/apikey), seguindo a [documentação de chaves da API Gemini](https://ai.google.dev/gemini-api/docs/api-key?hl=en).
2. No **terminal 1**, pare o backend com `Ctrl+C`.
3. Ainda na pasta `backend`, configure a chave e inicie novamente. Substitua `COLE_SUA_CHAVE_AQUI` pela sua chave real.

**Windows — PowerShell:**

```powershell
$env:GEMINI_API_KEY = "COLE_SUA_CHAVE_AQUI"
.\mvnw.cmd spring-boot:run
```

**Linux/macOS:**

```sh
export GEMINI_API_KEY="COLE_SUA_CHAVE_AQUI"
sh ./mvnw spring-boot:run
```

Esses comandos configuram a variável apenas nesse terminal e nos processos iniciados por ele. Ao abrir um terminal novo, configure-a novamente. O backend **não carrega automaticamente um arquivo `.env`**.

A chave deve ficar no backend; não a coloque em `frontend/.env`, em variáveis `VITE_*` ou em arquivos enviados ao Git. O modelo configurado no projeto é `gemini-3.5-flash-lite`; `GEMINI_MODEL` permite selecionar outro modelo compatível e disponível na sua conta.

Como o backend foi reiniciado, crie novamente sua conta e os dados no H2. Para gerar uma síntese, primeiro obtenha estatísticas ou publicações no processo. Sem chave, as funções de IA informam que não estão configuradas; o restante da instalação pode ser utilizado.

## Encerrar e executar novamente

Para encerrar, pressione `Ctrl+C` em **cada um dos dois terminais**. Se o Windows perguntar se deseja finalizar o arquivo em lotes, confirme.

Nas próximas execuções, abra dois terminais nas pastas indicadas:

| Pasta do terminal | Windows — PowerShell | Linux/macOS |
| --- | --- | --- |
| `backend` | `.\mvnw.cmd spring-boot:run` | `sh ./mvnw spring-boot:run` |
| `frontend` | `npm.cmd run dev` | `npm run dev` |

Não é necessário copiar o `.env` ou reinstalar dependências toda vez. Execute `npm ci` novamente quando atualizar o `package-lock.json`. Se usar Gemini, configure `GEMINI_API_KEY` no terminal do backend antes de iniciá-lo.

## Problemas comuns

| Problema | O que conferir |
| --- | --- |
| `java`, `javac`, `node` ou `npm` não encontrado | Instale a ferramenta, confira o `PATH` e abra um terminal novo. |
| `release version 25 not supported` ou `UnsupportedClassVersionError` | O Java usado é antigo. Confira `java -version`, `javac -version` e a saída de `mvnw -version`; todos devem apontar para JDK 25 ou superior. |
| `JAVA_HOME` inválido | A variável deve apontar para a pasta do JDK que contém `bin`, e não para a própria pasta `bin`. Veja o exemplo abaixo. |
| PowerShell bloqueia `npm.ps1` | Use `npm.cmd ci` e `npm.cmd run dev`, como nos passos acima. |
| `package.json` não encontrado | Execute os comandos npm dentro da pasta `frontend`. |
| `mvnw` ou `.mvn/wrapper` não encontrado | Extraia todo o projeto e execute o wrapper dentro da pasta `backend`. |
| `Port 8080 was already in use` ou porta 5173 ocupada | Verifique se já há uma execução aberta. Encerre a execução anterior ou use as portas alternativas abaixo. |
| O navegador mostra erro de conexão ao cadastrar ou entrar | Confira se o backend mostrou `Started BackendApplication`, se os dois terminais continuam abertos e se `frontend/.env` aponta para a porta correta. Reinicie o Vite após editar o `.env`. |
| Ao abrir `localhost:8080`, aparece 401 | A porta 8080 é a API e exige autenticação nas rotas privadas. A interface fica em **http://localhost:5173**. |
| O login parou de funcionar após reiniciar o backend | O H2 em memória foi recriado. Cadastre a conta novamente. |
| `JWT_SECRET` exigido na inicialização | Confira se você ativou um perfil diferente de `dev` por uma variável anterior. O guia usa o perfil padrão `dev`; MySQL/produção exigem configuração própria. |
| Falha ao baixar dependências | Confira internet, proxy e acesso aos repositórios Maven/npm. A primeira execução precisa baixar arquivos. |
| Erro ou espera ao consultar DataJud/DJEN/Gemini | Confira conexão, chave/modelo do Gemini quando aplicável e a resposta exibida. Os provedores podem limitar ou recusar consultas. |

### Ajustar `JAVA_HOME`, se necessário

No PowerShell, substitua o caminho abaixo pela **pasta real do JDK instalado**, que deve conter `bin\javac.exe`:

```powershell
$env:JAVA_HOME = "C:\caminho\do\jdk-25"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
java -version
javac -version
```

No Linux/macOS, use o caminho real que contém `bin/javac`:

```sh
export JAVA_HOME="/caminho/do/jdk-25"
export PATH="$JAVA_HOME/bin:$PATH"
java -version
javac -version
```

O ajuste acima vale para o terminal atual. Faça-o no terminal que iniciará o backend e confira novamente `mvnw -version`.

### Usar portas alternativas

Se a porta 8080 estiver ocupada, inicie o backend na 8081, dentro de `backend`:

```powershell
# Windows — PowerShell
$env:SERVER_PORT = "8081"
.\mvnw.cmd spring-boot:run
```

```sh
# Linux/macOS
SERVER_PORT=8081 sh ./mvnw spring-boot:run
```

Depois, altere `frontend/.env` para `VITE_API_URL=http://localhost:8081` e reinicie o frontend.

Se apenas a porta 5173 estiver ocupada, execute `npm.cmd run dev -- --port 5174` no Windows ou `npm run dev -- --port 5174` no Linux/macOS, dentro de `frontend`. Nesse caso, abra `http://localhost:5174`.

## Documentação complementar e validação

- [Backend](backend/README.md): autenticação, H2, configuração, MySQL, fila DataJud e testes.
- [Frontend](frontend/README.md): ambiente, build e verificações locais.

O fluxo de instalação foi validado no Windows com JDK 26, Node.js 24.11.1 e npm 11.6.2, em uma pasta nova contendo os arquivos versionados, sem `node_modules`, `target` ou configuração local anterior. Foram conferidos a instalação via `npm ci`, o build, a inicialização padrão, cadastro, login, consulta autenticada e logout pelo proxy do frontend. O Maven utilizou o cache de dependências da máquina de validação. Os comandos Linux/macOS utilizam o wrapper incluído; não foram executados nesses sistemas nesta validação. As APIs externas e a geração real com Gemini não foram acionadas.
