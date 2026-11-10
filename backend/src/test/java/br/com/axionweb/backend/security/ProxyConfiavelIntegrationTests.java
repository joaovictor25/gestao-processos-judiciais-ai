package br.com.axionweb.backend.security;

import jakarta.servlet.Filter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tomcat real com a mesma estratégia do perfil prod (RemoteIpValve). A conexão do teste parte de
 * 127.0.0.1, que aqui representa o IP fixo do Caddy na rede Docker.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:proxyConfiavel;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop",
        "server.forward-headers-strategy=native",
        "server.tomcat.remoteip.internal-proxies=127\\\\.0\\\\.0\\\\.1",
        "server.tomcat.remoteip.remote-ip-header=X-Forwarded-For",
        "server.tomcat.remoteip.protocol-header=X-Forwarded-Proto",
        "app.auth.limite.login-por-ip=2"})
@ActiveProfiles("test")
class ProxyConfiavelIntegrationTests {
    record Origem(String ip, String esquema, boolean seguro) {}
    static final AtomicReference<Origem> ULTIMA = new AtomicReference<>();

    @TestConfiguration
    static class CapturaOrigem {
        @Bean
        FilterRegistrationBean<Filter> capturarOrigem() {
            var registro = new FilterRegistrationBean<Filter>((request, response, cadeia) -> {
                ULTIMA.set(new Origem(request.getRemoteAddr(), request.getScheme(), request.isSecure()));
                cadeia.doFilter(request, response);
            });
            registro.setOrder(Ordered.HIGHEST_PRECEDENCE);
            return registro;
        }
    }

    @LocalServerPort int porta;
    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void proxyConfiavelInformaIpRealEHttpsMasForwardedEValoresAEsquerdaSaoIgnorados() throws Exception {
        enviar("/api/auth/me", "GET", null, "X-Forwarded-For", "203.0.113.7", "X-Forwarded-Proto", "https");
        assertThat(ULTIMA.get()).isEqualTo(new Origem("203.0.113.7", "https", true));

        // Valor injetado pelo cliente à esquerda e cabeçalho RFC 7239: prevalece o IP anexado pelo proxy.
        enviar("/api/auth/me", "GET", null, "Forwarded", "for=198.51.100.1;proto=https",
                "X-Forwarded-For", "198.51.100.2, 203.0.113.8");
        assertThat(ULTIMA.get()).isEqualTo(new Origem("203.0.113.8", "http", false));
    }

    @Test
    void limitadorDeLoginNaoEContornadoPorCabecalhosFalsificados() throws Exception {
        String corpo = "{\"email\":\"falso@teste.com\",\"senha\":\"errada\"}";
        assertThat(enviar("/api/auth/login", "POST", corpo, "X-Forwarded-For", "198.51.100.10, 203.0.113.50").statusCode()).isEqualTo(401);
        assertThat(enviar("/api/auth/login", "POST", corpo, "Forwarded", "for=198.51.100.11",
                "X-Forwarded-For", "198.51.100.12, 203.0.113.50").statusCode()).isEqualTo(401);
        var limitada = enviar("/api/auth/login", "POST", corpo, "X-Forwarded-For", "198.51.100.13, 203.0.113.50");
        assertThat(limitada.statusCode()).isEqualTo(429);
        assertThat(limitada.headers().firstValue("Retry-After")).isPresent();
        assertThat(enviar("/api/auth/login", "POST", corpo, "X-Forwarded-For", "203.0.113.51").statusCode())
                .as("outro cliente real não é afetado").isEqualTo(401);
    }

    private HttpResponse<String> enviar(String caminho, String metodo, String corpo, String... cabecalhos) throws Exception {
        var requisicao = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + caminho))
                .method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(corpo))
                .header("Content-Type", "application/json").headers(cabecalhos).build();
        return http.send(requisicao, HttpResponse.BodyHandlers.ofString());
    }
}
