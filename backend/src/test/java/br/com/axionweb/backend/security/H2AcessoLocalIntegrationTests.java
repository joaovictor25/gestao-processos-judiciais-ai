package br.com.axionweb.backend.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:h2-acesso-local;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles({"test", "dev"})
class H2AcessoLocalIntegrationTests {
    @LocalServerPort
    private int port;

    @Test
    void consoleDeveAbrirSemTokenOuLoginDaAplicacao() throws Exception {
        var resposta = requisitar("/h2/");
        assertThat(resposta.statusCode()).isEqualTo(200);
        assertThat(resposta.body()).contains("H2 Console");
        assertThat(resposta.headers().firstValue("WWW-Authenticate")).isEmpty();
        assertThat(resposta.headers().firstValue("X-Frame-Options")).contains("SAMEORIGIN");
    }

    @Test
    void apiDeveContinuarExigindoJwtMesmoNoPerfilDev() throws Exception {
        for (String caminho : new String[]{"/api/auth/me", "/api/processo/listar-usuario", "/api/dados/analise/1"}) {
            var resposta = requisitar(caminho);
            assertThat(resposta.statusCode()).as(caminho).isEqualTo(401);
            assertThat(resposta.headers().firstValue("WWW-Authenticate")).contains("Bearer");
        }
    }

    private HttpResponse<String> requisitar(String caminho) throws Exception {
        try (var client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(5)).build()) {
            var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + caminho))
                    .timeout(Duration.ofSeconds(10)).GET().build();
            return client.send(request, HttpResponse.BodyHandlers.ofString());
        }
    }
}
