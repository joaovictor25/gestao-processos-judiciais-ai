# Backend de gestão de processos

Spring Boot 4.1.1, Java 25, Spring Data JPA e H2. O repositório atual contém apenas o backend; não há frontend React/Vite para adaptar.

## Executar

No diretório `backend`, com `JAVA_HOME` apontando para um JDK 25 ou superior:

```powershell
.\mvnw.cmd spring-boot:run
```

O perfil padrão é `dev`. Sem `JWT_SECRET`, esse perfil gera uma chave criptográfica aleatória na inicialização, sem imprimi-la. Os tokens deixam de funcionar ao reiniciar. O banco atual também é temporário (`jdbc:h2:mem:banco`): usuários, processos, estatísticas e comunicações precisam ser recriados depois que a aplicação é encerrada.

Para usar uma chave estável, forneça `JWT_SECRET` como uma chave aleatória em Base64 com pelo menos 32 bytes após decodificação. Mantenha-a no ambiente ou em um gerenciador de segredos, fora do Git. `app.jwt.validade` controla a duração do token (padrão `PT1H`); `app.jwt.issuer` define o emissor.

Fora de desenvolvimento, defina explicitamente `SPRING_PROFILES_ACTIVE=prod`, configure `JWT_SECRET` e o banco persistente. A aplicação recusa uma chave ausente fora de `dev`, inválida ou curta. O perfil `prod` não muda sozinho a URL do banco: é necessário configurar o datasource do ambiente. Use HTTPS no acesso externo à API.

O console H2 em `/h2` é habilitado apenas no perfil `dev`, com acesso local sem autenticação da aplicação. As rotas privadas da API continuam exigindo JWT.

### Acessar o H2 pelo navegador

Inicie o backend normalmente pela IDE ou pelo PowerShell, dentro de `backend`. O perfil padrão é `dev`:

```powershell
.\mvnw.cmd spring-boot:run
```

Não é necessário criar administrador nem configurar `DEV_ADMIN_EMAIL` ou `DEV_ADMIN_PASSWORD`.

1. Com o backend iniciado, abra `http://localhost:8080/h2/` (ajuste a porta se necessário).
2. O console abre diretamente. Na tela de conexão do H2, preencha:

   | Campo | Valor padrão do projeto |
   | --- | --- |
   | Driver Class | `org.h2.Driver` |
   | JDBC URL | `jdbc:h2:mem:banco` |
   | User Name | `sa` |
   | Password | Deixe vazio |

3. Clique em **Connect**. `sa` é o usuário de conexão do próprio banco; não é uma conta da aplicação.

Como o H2 atual é em memória, reiniciar o backend apaga os registros anteriores. Fora do perfil `dev`, o console continua desabilitado.

## Cadastro e login

```http
POST /api/auth/cadastro
Content-Type: application/json

{"nome":"João Victor","email":"joao@email.com","senha":"123456"}
```

Retorna `201` com `id`, `nome`, `email` e `role`. Nome e e-mail são normalizados; e-mails duplicados, inclusive com diferença de maiúsculas, retornam `409`. A senha deve ter pelo menos 6 caracteres e no máximo 72 bytes em UTF-8, limite do BCrypt. Ela é armazenada como hash pelo `DelegatingPasswordEncoder`.

```http
POST /api/auth/login
Content-Type: application/json

{"email":"joao@email.com","senha":"123456"}
```

Retorna `200`:

```json
{
  "token": "<JWT assinado>",
  "tipo": "Bearer",
  "usuarioId": 1,
  "nome": "João Victor",
  "email": "joao@email.com",
  "role": "USER"
}
```

As demais rotas da API exigem `Authorization: Bearer <token>`. O JWT usa assinatura HS256 e contém apenas identificação e datas, sem senha. A validação verifica assinatura, algoritmo, emissor, expiração e subject, e carrega o usuário do banco em cada requisição. Não é criada sessão de servidor.

`GET /api/auth/me` retorna os dados públicos da conta autenticada. Um cliente faz logout descartando seu token; o token já emitido continua válido até expirar. Não há refresh token nem lista de revogação nesta implementação.

## Processos e resultados

O vínculo persistido é `usuarios.id <- processos.usuario_id <- datajud_estatistica.processo_id / comunicacao.processo_id`. As chaves estrangeiras são obrigatórias. DataJud e Comunicação não duplicam `usuario_id`.

| Método | Rota | Comportamento |
| --- | --- | --- |
| POST | `/api/processo/cadastrar` | Cadastra com o proprietário identificado pelo JWT |
| GET | `/api/processo/listar-usuario` | Lista somente os processos da conta autenticada |
| GET | `/api/processo/{id}` | Busca um processo da conta |
| GET | `/api/processo/buscar-num-processo?numeroProcesso=...` | Busca por número e proprietário |
| PUT | `/api/processo/atualizar/{id}` | Atualiza somente se pertencer à conta |
| DELETE | `/api/processo/deletar/{id}` | Exclui o processo e seus resultados, após validar o proprietário |
| POST | `/api/dados/analisar/{id}` | Executa o fluxo DataJud, estatísticas e Comunicações |
| GET | `/api/dados/analisar/{id}` | Solicitação anterior, mantida por compatibilidade; prefira POST |
| GET | `/api/dados/analise/{processoId}` | Consulta estatísticas e comunicações já salvas para o processo |
| GET | `/api/dados/estatisticas/{processoId}` | Lista somente as estatísticas salvas do processo |
| GET | `/api/dados/comunicacoes/{processoId}` | Lista somente as comunicações salvas do processo |
| POST | `/estatisticas/calcular` | Calcula estatísticas dos dados enviados, com autenticação; não consulta registros de outros usuários |

O JSON de cadastro/edição de processo preserva os campos existentes: `numeroProcesso`, `clienteVinculado`, `posicaoCliente`, `tribunal`, `orgaoJulgador`, `grau`, `comarca`, `uf`, `classeProcessual`, `assunto`, `areaAtuacao`, `status`, `valorCausa`, `nomeMagistrado`, `dataInicio`, `dataFim`. Não envie `id`, `usuario` ou `usuarioId`: o DTO não utiliza esses campos. Edições não transferem a propriedade do processo. As respostas não serializam a entidade Usuario.

As buscas consideram processo e e-mail autenticado no banco. Um ID inexistente e um ID de outro usuário produzem o mesmo `404`, inclusive para usuários `ADMIN`. A verificação ocorre antes das chamadas externas ou de qualquer exclusão. O backend aplica essas regras independentemente do cliente.

A análise é gravada em uma transação: uma nova solicitação substitui todas as estatísticas e comunicações anteriores somente daquele processo. A consulta às APIs e a preparação dos dados acontecem antes da substituição; se houver falha na consulta ou na gravação, o resultado anterior permanece intacto. Uma resposta válida sem resultados substitui as estatísticas por valores zerados e/ou remove as comunicações antigas, conforme a fonte que retornou vazia. A gravação usa bloqueio por processo para evitar que solicitações simultâneas acumulem ou misturem resultados. Os IDs dos registros de estatísticas e comunicações podem mudar na substituição; o ID do processo permanece o mesmo. Registros de histórico acumulados pela versão anterior são removidos na próxima análise bem-sucedida daquele processo.

A consulta conjunta retorna `processoId`, `estatisticas` e `comunicacoes`; ela não executa novamente as APIs. Ainda não existe entidade/resultado de LLM no projeto. Os pontos reservados para essa futura etapa foram preservados; nenhum resultado de IA foi simulado ou criado.

Para as páginas separadas do futuro frontend, use `/api/processo/listar-usuario` para a lista de processos e, ao selecionar um processo, `/api/dados/estatisticas/{processoId}` ou `/api/dados/comunicacoes/{processoId}`. As duas consultas mantêm respostas em arrays: após uma análise bem-sucedida, há uma estatística consolidada e apenas as comunicações da análise atual, ordenadas por ID. Se o processo pertence à conta e ainda não tem resultados, retornam `200` com `[]`; processo inexistente ou de outra conta retorna `404`. Todas exigem o Bearer token. Essas consultas apenas leem o banco, sem recalcular estatísticas nem consultar as APIs externas.

## Erros

Os erros possuem `status`, `mensagem` e `campos` (erros de validação). Senhas, valores rejeitados e stacktraces não são devolvidos.

| Status | Situação |
| --- | --- |
| 400 | DTO/JSON/parâmetros inválidos |
| 401 | Credenciais incorretas, token ausente, inválido ou expirado |
| 403 | Permissão insuficiente |
| 404 | Processo inexistente ou pertencente a outra conta |
| 409 | E-mail já cadastrado ou conflito de integridade |
| 502 | Falha ao consultar um serviço externo |

## Dados anteriores e migração

No ambiente encontrado, o H2 é exclusivamente em memória. A aplicação recria o esquema na próxima inicialização e não há arquivo de banco persistente a migrar. Nenhuma rotina foi criada para apagar ou atribuir automaticamente processos existentes a uma conta arbitrária.

Se outro ambiente sobrescrever a configuração para utilizar um banco persistente, faça backup e migre antes de iniciar esta versão: crie `usuarios`, adicione `processos.usuario_id` inicialmente anulável, cadastre os usuários com hashes válidos, atribua cada processo a seu proprietário confirmado, confira que não há vínculos nulos/inválidos e então aplique a FK e `NOT NULL`. Preserve os IDs dos processos para manter os vínculos existentes de estatísticas e comunicações. Não dependa de `ddl-auto=update` para inferir proprietários de dados antigos.

## Verificação

```powershell
.\mvnw.cmd -B -ntp test
```

Os testes de integração usam H2 separado e respostas simuladas somente para DataJud e Comunicação. Exercitam cadastro, hash de senha, login real, JWT, validação, isolamento entre contas, tentativas de alterar o proprietário pelo JSON, resultados e exclusão, além das transações e chaves estrangeiras. A chave do perfil `test` é exclusiva dos testes e não é distribuída nos recursos principais.

A implementação de JWT utiliza os componentes Nimbus fornecidos pelo [Spring Security](https://docs.spring.io/spring-security/reference/servlet/oauth2/) e suas versões gerenciadas pelo Spring Boot, sem biblioteca JWT paralela.
