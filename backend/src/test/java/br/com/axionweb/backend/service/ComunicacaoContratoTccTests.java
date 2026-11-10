package br.com.axionweb.backend.service;

import br.com.axionweb.backend.model.processo.Processo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import java.io.IOException;
import java.time.LocalDate;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** Contrato local do consumidor DJEN; nenhuma conexão ao CNJ. */
class ComunicacaoContratoTccTests {
    private ComunicacaoService service;
    private MockRestServiceServer servidor;

    @BeforeEach void preparar() {
        var builder = RestClient.builder().baseUrl("http://djen.invalid");
        servidor = MockRestServiceServer.bindTo(builder).build();
        service = new ComunicacaoService();
        service.restComunicacao = builder.build();
    }
    @AfterEach void verificar() { servidor.verify(); }

    @Test void enviaTextoCodificadoTribunalPeriodoMeioEPaginaSemPercorrerTotal() {
        var processo = new Processo(); processo.setTribunal("TJPR");
        var parametros = service.montarParametroComunicacao(processo, "dano & reparação",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));
        servidor.expect(requestTo("http://djen.invalid/api/v1/comunicacao?texto=dano%20%26%20repara%C3%A7%C3%A3o&siglaTribunal=TJPR&dataDisponibilizacaoInicio=2025-01-01&dataDisponibilizacaoFim=2025-01-31&meio=D&pagina=1&itensPorPagina=80"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"success\",\"count\":160,\"items\":[{\"texto\":\"<p>Documento fictício</p>\",\"siglaTribunal\":\"TJPR\"}]}", MediaType.APPLICATION_JSON));
        var dados = service.consumirApiComunicacao(parametros);
        assertThat(dados).hasSize(1); // count não dispara paginação automática.
        assertThat(dados.getFirst().getTexto()).isEqualTo("<p>Documento fictício</p>");
        assertThat(dados.getFirst().getSiglaTribunal()).isEqualTo("TJPR");
    }

    @Test void omiteDatasAusentesERespeitaPaginaExplicitamenteSolicitada() {
        var processo = new Processo(); processo.setTribunal("TJPR");
        var parametros = service.montarParametroComunicacao(processo, null, null, null);
        parametros.setPagina(2); parametros.setItensPorPagina(20);
        servidor.expect(requestTo("http://djen.invalid/api/v1/comunicacao?siglaTribunal=TJPR&meio=D&pagina=2&itensPorPagina=20"))
                .andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));
        assertThat(service.consumirApiComunicacao(parametros)).isEmpty();
    }

    @ParameterizedTest @ValueSource(strings = {"{}", "{\"items\":null}", "json-invalido"})
    void rejeitaRespostaSemListaOuJsonInvalido(String resposta) {
        servidor.expect(anything()).andRespond(withSuccess(resposta, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.consumirApiComunicacao(parametros())).isInstanceOf(RestClientException.class);
    }

    @ParameterizedTest @ValueSource(ints = {429, 500, 503})
    void propagaFalhaHttpSemConfundirComListaVazia(int status) {
        servidor.expect(anything()).andRespond(withStatus(HttpStatus.valueOf(status)));
        assertThatThrownBy(() -> service.consumirApiComunicacao(parametros()))
                .isInstanceOf(org.springframework.web.client.RestClientResponseException.class)
                .satisfies(erro -> assertThat(((org.springframework.web.client.RestClientResponseException) erro).getStatusCode().value()).isEqualTo(status));
    }

    @Test void propagaFalhaDeTransporteSemRetornarAmostraVazia() {
        servidor.expect(anything()).andRespond(withException(new IOException("timeout simulado")));
        assertThatThrownBy(() -> service.consumirApiComunicacao(parametros())).isInstanceOf(RestClientException.class);
    }

    @Test void registroNuloDeveSerRejeitadoComoFalhaDeIntegracaoControlada() {
        servidor.expect(anything()).andRespond(withSuccess("{\"items\":[null]}", MediaType.APPLICATION_JSON));
        // Requisito de robustez: resposta estruturalmente inválida não deve escapar como NPE.
        assertThatThrownBy(() -> service.consumirApiComunicacao(parametros()))
                .isInstanceOf(RestClientException.class)
                .isNotInstanceOf(NullPointerException.class)
                .hasMessageContaining("comunicação nula");
    }

    private br.com.axionweb.backend.dto.comunicacao.ParametrosComunicacaoDTO parametros() {
        var processo = new Processo(); processo.setTribunal("TJPR");
        return service.montarParametroComunicacao(processo, "teste fictício", null, null);
    }
}
