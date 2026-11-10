package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.comunicacao.TipoAnaliseComunicacao;
import br.com.axionweb.backend.exception.AnaliseComunicacaoException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@ExtendWith(OutputCaptureExtension.class)
class SegredosLogsTests {
    private static final String CHAVE = "chave-gemini-secreta-teste-7f3a";

    @Test
    void falhaDoGeminiNaoRegistraChaveNemCorpoExternoNosLogs(CapturedOutput saida) {
        var gemini = new GeminiService(CHAVE, "https://gemini.teste", "gemini-teste", JsonMapper.builder().build());
        var builder = RestClient.builder().baseUrl("https://gemini.teste");
        var servidor = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(gemini, "restGemini", builder.build());
        // Provedores podem ecoar a chave recebida no corpo do erro.
        servidor.expect(header("X-goog-api-key", CHAVE)).andRespond(withStatus(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON).body("{\"error\":{\"message\":\"API key not valid: " + CHAVE + "\"}}"));

        assertThatThrownBy(() -> gemini.analisarComunicacoes(List.of("Texto de comunicação."), List.of(TipoAnaliseComunicacao.SINTESE_GERAL)))
                .isInstanceOfSatisfying(AnaliseComunicacaoException.class, e -> assertThat(e.getMessage()).doesNotContain(CHAVE));
        servidor.verify();
        assertThat(saida.getAll()).contains("Análise Gemini falhou").doesNotContain(CHAVE).doesNotContain("API key not valid");
    }
}
