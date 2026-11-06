# Ambiente de testes

Data local: 05/11/2026, America/Sao_Paulo. Windows 11 amd64. Commit base: `22adb3fb731a8d6a38cb3822af88aff6f8b8b772`; checkout já modificado. Manifesto SHA-256 e estado Git em evidencias/.

Java declarado no pom: 25; executado: Oracle Java 26+35-2893. Maven Wrapper 3.9.16. Spring Boot 4.1.1. Node v24.11.1.

| Dependência frontend resolvida | Versão |
| --- | --- |
| react | 19.3.0 |
| react-dom | 19.3.0 |
| react-router-dom | 7.18.4 |
| typescript | 7.0.2 |
| vite | 8.3.1 |

Dependências observadas no classpath Surefire: h2-2.4.240.jar, hibernate-core-7.4.5.Final.jar, junit-jupiter-api-6.0.3.jar, mysql-connector-j-9.7.0.jar, spring-boot-4.1.1.jar. MySQL é driver disponível, não servidor utilizado; não foi medida versão de servidor MySQL. JPA/Hibernate foi executado sobre H2 em memória, com create-drop.

Backend HTTP de navegador: loopback 18081; frontend Vite: 15173; provedores simulados DataJud: 8092, DJEN/Gemini: 8093. O teste de fila Java usa porta efêmera. Perfil test com parâmetros explícitos; spring-boot:run não carrega automaticamente src/test/resources. JWT e chave Gemini locais são fictícios.

Build e Node test inicialmente encontraram spawn EPERM no sandbox; repetidos com permissão de subprocessos e aprovados. Postman não iniciou por manifesto Private Cloud não legível. Chrome headless usa DevTools; nenhum pacote de navegador foi instalado.

Ferramentas adicionais verificadas: npm 11.6.2; Chrome 156.0.8078.80. Fonte: evidencias/execucoes/ferramentas.json.
