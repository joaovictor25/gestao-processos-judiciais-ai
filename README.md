# Gestão de processos judiciais com IA

Aplicação com backend Spring Boot e frontend React para cadastro de usuários e processos, consulta de publicações, estatísticas do DataJud e sínteses com Gemini.

## Requisitos

- Git.
- JDK 25 ou superior, com `JAVA_HOME` apontando para o JDK.
- Node.js 22.12 ou superior e npm.
- Acesso à internet para baixar dependências e consultar os serviços externos.

O Maven Wrapper acompanha o projeto. O ambiente de desenvolvimento usa H2 em memória, sem instalação de banco de dados.

## Clonar

```sh
git clone https://github.com/joaovictor25/gestao-processos-judiciais-ai.git
cd gestao-processos-judiciais-ai
```

## Iniciar o backend

Em um terminal, entre na pasta `backend`.

Windows (PowerShell):

```powershell
cd backend
# Opcional: necessário para gerar sínteses com IA.
$env:GEMINI_API_KEY = "sua-chave-do-gemini"
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```sh
cd backend
# Opcional: necessário para gerar sínteses com IA.
export GEMINI_API_KEY="sua-chave-do-gemini"
sh ./mvnw spring-boot:run
```

Substitua o exemplo pela sua chave ou omita a linha de configuração para executar sem sínteses de IA. As consultas DataJud e DJEN dependem da disponibilidade desses serviços. O backend atende em `http://localhost:8080`. Mantenha o terminal aberto.

## Iniciar o frontend

Abra outro terminal na raiz do repositório.

Windows (PowerShell):

```powershell
cd frontend
Copy-Item .env.example .env
npm ci
npm run dev
```

Linux/macOS:

```sh
cd frontend
cp .env.example .env
npm ci
npm run dev
```

Abra **http://localhost:5173**, crie uma conta na tela de cadastro e faça login. O frontend encaminha as chamadas da API para o backend configurado em `frontend/.env`.

## Dados e configuração

- O perfil padrão `dev` usa um banco temporário: reiniciar o backend apaga os usuários, processos e resultados salvos.
- Nesse perfil, `JWT_SECRET` é opcional. Sem ele, uma chave é gerada ao iniciar; os tokens anteriores deixam de funcionar após reiniciar.
- A chave Gemini deve ficar no ambiente do backend. Variáveis `VITE_*` são públicas e não devem conter credenciais.
- O backend lê variáveis do processo; não carrega automaticamente um arquivo `.env`.
- Para usar um banco MySQL existente, consulte as configurações e migrações no [README do backend](backend/README.md). Esse perfil valida um esquema já preparado.

## Documentação

- [Backend: autenticação, configuração, banco e testes](backend/README.md).
- [Frontend: ambiente, build e verificações locais](frontend/README.md).
