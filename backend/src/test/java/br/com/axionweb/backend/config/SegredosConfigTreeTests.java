package br.com.axionweb.backend.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Simula os Docker secrets montados em /run/secrets: um arquivo por segredo, com o nome exato da
 * variável (target no compose.yaml) e quebra de linha final, como gerado por "openssl rand ... > arquivo".
 */
class SegredosConfigTreeTests {
    @Configuration(proxyBeanMethods = false)
    static class Vazia {}

    @TempDir Path segredos;

    @Test
    void arquivosDoConfigTreeAlimentamPlaceholdersDoPerfilProdSemQuebraDeLinha() throws Exception {
        escrever("JWT_SECRET", "c29tZS1yYW5kb20tdGVzdC1vbmx5LWtleS0zMi1ieXRlcyE=\n");
        escrever("MYSQL_PASSWORD", "senha-ficticia\n");
        escrever("DATAJUD_API_KEY", "chave-datajud-ficticia\n");
        escrever("GEMINI_API_KEY", "chave-gemini-ficticia\n");
        try (var contexto = iniciar("--spring.profiles.active=prod")) {
            var ambiente = contexto.getEnvironment();
            // RequiredEnvironmentInitializer (perfil prod) já executou e aceitou os segredos.
            assertThat(ambiente.getProperty("app.jwt.secret")).isEqualTo("c29tZS1yYW5kb20tdGVzdC1vbmx5LWtleS0zMi1ieXRlcyE=");
            assertThat(ambiente.getProperty("spring.datasource.password")).isEqualTo("senha-ficticia");
            assertThat(ambiente.getProperty("app.datajud.api-key")).isEqualTo("chave-datajud-ficticia");
            assertThat(ambiente.getProperty("app.gemini.api-key")).isEqualTo("chave-gemini-ficticia");
            // Cabeçalhos encaminhados: somente o proxy confiável (padrão: loopback).
            assertThat(ambiente.getProperty("server.forward-headers-strategy")).isEqualTo("native");
            assertThat(ambiente.getProperty("server.tomcat.remoteip.internal-proxies")).isEqualTo("127\\.0\\.0\\.1");
        }
    }

    @Test
    void segredoObrigatorioAusenteImpedeInicioSemExporValores() throws Exception {
        escrever("MYSQL_PASSWORD", "senha-ficticia-nao-exibir\n");
        escrever("DATAJUD_API_KEY", "chave-datajud-nao-exibir\n");
        assertThatThrownBy(() -> iniciar("--spring.profiles.active=prod").close())
                .hasStackTraceContaining("JWT_SECRET e obrigatoria")
                .satisfies(erro -> assertThat(stack(erro)).doesNotContain("senha-ficticia-nao-exibir", "chave-datajud-nao-exibir"));
    }

    private org.springframework.context.ConfigurableApplicationContext iniciar(String... argumentos) {
        var app = new SpringApplication(Vazia.class);
        app.setWebApplicationType(WebApplicationType.NONE);
        var args = new java.util.ArrayList<>(java.util.List.of(argumentos));
        args.add("--spring.config.import=configtree:" + segredos.toAbsolutePath().toString().replace('\\', '/') + "/");
        args.add("--MYSQL_URL=jdbc:mysql://mysql:3306/prosint");
        args.add("--MYSQL_USER=prosint");
        return app.run(args.toArray(String[]::new));
    }

    private void escrever(String nome, String valor) throws Exception { Files.writeString(segredos.resolve(nome), valor); }

    private static String stack(Throwable erro) {
        var saida = new java.io.StringWriter();
        erro.printStackTrace(new java.io.PrintWriter(saida));
        return saida.toString();
    }
}
