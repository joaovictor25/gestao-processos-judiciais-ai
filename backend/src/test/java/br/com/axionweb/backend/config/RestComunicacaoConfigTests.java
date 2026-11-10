package br.com.axionweb.backend.config;

import br.com.axionweb.backend.dto.comunicacao.ParametrosComunicacaoDTO;
import br.com.axionweb.backend.service.ComunicacaoService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestComunicacaoConfigTests {

    @Test
    void clienteDjenInterrompeLeituraQueExcedeTimeoutConfigurado() throws Exception {
        var servidor = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        servidor.createContext("/api/v1/comunicacao", resposta -> {
            try {
                Thread.sleep(500);
                byte[] corpo = "{\"items\":[]}".getBytes(StandardCharsets.UTF_8);
                resposta.sendResponseHeaders(200, corpo.length);
                resposta.getResponseBody().write(corpo);
            } catch (InterruptedException erro) {
                Thread.currentThread().interrupt();
            } finally {
                resposta.close();
            }
        });
        servidor.start();

        try {
            var config = new RestComunicacaoConfig();
            var cliente = config.restComunicacao(
                    "http://127.0.0.1:" + servidor.getAddress().getPort(), 1_000, 50);
            var service = new ComunicacaoService();
            ReflectionTestUtils.setField(service, "restComunicacao", cliente);
            var parametros = new ParametrosComunicacaoDTO();
            parametros.setSiglaTribunal("TJPR");
            parametros.setMeio("D");
            parametros.setPagina(1);
            parametros.setItensPorPagina(1);

            assertThatThrownBy(() -> service.consumirApiComunicacao(parametros))
                    .isInstanceOf(RestClientException.class)
                    .hasMessageContaining("I/O error")
                    .hasRootCauseInstanceOf(HttpTimeoutException.class);
        } finally {
            servidor.stop(0);
        }
    }
}
