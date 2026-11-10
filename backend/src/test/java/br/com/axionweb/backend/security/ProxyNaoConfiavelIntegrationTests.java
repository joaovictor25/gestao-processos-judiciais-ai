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

/** Conexão direta de um endereço que não é o proxy: todos os cabeçalhos encaminhados são ignorados. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:proxyNaoConfiavel;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop",
        "server.forward-headers-strategy=native",
        "server.tomcat.remoteip.internal-proxies=172\\\\.31\\\\.250\\\\.2",
        "server.tomcat.remoteip.remote-ip-header=X-Forwarded-For",
        "server.tomcat.remoteip.protocol-header=X-Forwarded-Proto"})
@ActiveProfiles("test")
class ProxyNaoConfiavelIntegrationTests {
    static final AtomicReference<String[]> ULTIMA = new AtomicReference<>();

    @TestConfiguration
    static class CapturaOrigem {
        @Bean
        FilterRegistrationBean<Filter> capturarOrigem() {
            var registro = new FilterRegistrationBean<Filter>((request, response, cadeia) -> {
                ULTIMA.set(new String[]{request.getRemoteAddr(), request.getScheme()});
                cadeia.doFilter(request, response);
            });
            registro.setOrder(Ordered.HIGHEST_PRECEDENCE);
            return registro;
        }
    }

    @LocalServerPort int porta;

    @Test
    void cabecalhosDeClienteNaoConfiavelSaoIgnorados() throws Exception {
        var requisicao = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + "/api/auth/me"))
                .header("X-Forwarded-For", "203.0.113.9").header("X-Forwarded-Proto", "https")
                .header("Forwarded", "for=203.0.113.10;proto=https").GET().build();
        assertThat(HttpClient.newHttpClient().send(requisicao, HttpResponse.BodyHandlers.discarding()).statusCode()).isEqualTo(401);
        assertThat(ULTIMA.get()).containsExactly("127.0.0.1", "http");
    }
}
