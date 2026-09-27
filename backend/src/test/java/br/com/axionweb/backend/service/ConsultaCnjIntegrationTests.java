package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.auth.CadastroUsuarioRequestDTO;
import br.com.axionweb.backend.repository.*;
import br.com.axionweb.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:consulta-cnj;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConsultaCnjIntegrationTests {
    private static final String CNJ = "00008323520184013202";
    private static final String ROTA = "/api/processo/consultar-datajud";
    private static final String REGISTRO = """
            {"numeroProcesso":"00008323520184013202","tribunal":"TRF1","grau":"G1",
             "orgaoJulgador":{"nome":"Órgão de teste","codigo":123,"codigoMunicipioIBGE":5128},
             "classe":{"nome":"Procedimento comum","codigo":7},
             "assuntos":[{"nome":"Dano material","codigo":10439},{"nome":"Dano moral","codigo":10433}],
             "dataAjuizamento":"2018-10-29T00:00:00.000Z","campoExtra":"ignorado"}
            """;
    @Autowired private MockMvc mvc;
    @Autowired private DataJudService dataJudService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ProcessoRepository processos;
    @Autowired private DataJudRepository estatisticas;
    @Autowired private ComunicacaoRepository comunicacoes;
    @Autowired private JwtService jwtService;
    private MockRestServiceServer datajud;
    private String bearer;

    @BeforeEach
    void preparar() {
        comunicacoes.deleteAllInBatch(); estatisticas.deleteAllInBatch(); processos.deleteAllInBatch(); usuarios.deleteAllInBatch();
        usuarioService.cadastrar(new CadastroUsuarioRequestDTO("Consulta CNJ", "cnj@teste.com", "senha123"));
        bearer = "Bearer " + jwtService.gerarToken("cnj@teste.com");
        var builder = RestClient.builder().baseUrl("https://datajud.teste");
        datajud = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(dataJudService, "restDataJud", builder.build());
    }

    @Test
    void consultaNumeroExatoConverteCamposEAssuntosSemCadastrarOuAnalisar() throws Exception {
        datajud.expect(requestTo("https://datajud.teste/api_publica_trf1/_search"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content().json("""
                        {"size":100,"track_total_hits":true,"query":{"match":{"numeroProcesso":"00008323520184013202"}},
                         "_source":["numeroProcesso","tribunal","grau","orgaoJulgador","classe","assuntos","dataAjuizamento"],
                         "sort":[{"dataHoraUltimaAtualizacao":"desc"}]}
                        """))
                .andRespond(withSuccess(resposta(REGISTRO), MediaType.APPLICATION_JSON));
        mvc.perform(get(ROTA).param("numeroProcesso", "0000832-35.2018.4.01.3202").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroProcesso").value("0000832-35.2018.4.01.3202"))
                .andExpect(jsonPath("$.tribunalConsultado").value("TRF1"))
                .andExpect(jsonPath("$.limiteAtingido").value(false))
                .andExpect(jsonPath("$.registros.length()").value(1))
                .andExpect(jsonPath("$.registros[0].grau").value("G1"))
                .andExpect(jsonPath("$.registros[0].orgaoJulgador.codigo").value("123"))
                .andExpect(jsonPath("$.registros[0].classeProcessual.nome").value("Procedimento comum"))
                .andExpect(jsonPath("$.registros[0].classeProcessual.codigo").value("7"))
                .andExpect(jsonPath("$.registros[0].assuntos.length()").value(2))
                .andExpect(jsonPath("$.registros[0].assuntos[1].codigo").value("10433"))
                .andExpect(jsonPath("$.registros[0].dataInicio").value("2018-10-29"))
                .andExpect(jsonPath("$.registros[0].comarca").doesNotExist())
                .andExpect(jsonPath("$.registros[0].clienteVinculado").doesNotExist());
        assertThat(processos.count()).isZero(); assertThat(estatisticas.count()).isZero(); assertThat(comunicacoes.count()).isZero();
        datajud.verify();
    }

    @Test
    void preservaRegistrosDeGrausDiferentesEIgnoraNumeroDiferente() throws Exception {
        String outroGrau = REGISTRO.replace("\"G1\"", "\"G2\"").replace("2018-10-29T00:00:00.000Z", "20181029000000");
        String outroNumero = REGISTRO.replace(CNJ, "00000010020258260001");
        datajud.expect(anything()).andRespond(withSuccess(resposta(REGISTRO, outroGrau, REGISTRO, outroNumero), MediaType.APPLICATION_JSON));
        mvc.perform(get(ROTA).param("numeroProcesso", CNJ).header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.registros.length()").value(2))
                .andExpect(jsonPath("$.registros[1].grau").value("G2"))
                .andExpect(jsonPath("$.registros[1].dataInicio").value("2018-10-29"));
        datajud.verify();
    }

    @Test
    void toleraCamposAusentesSemInventarDados() throws Exception {
        datajud.expect(anything()).andRespond(withSuccess(resposta("{\"numeroProcesso\":\"" + CNJ + "\",\"dataAjuizamento\":\"20180231000000\"}"), MediaType.APPLICATION_JSON));
        mvc.perform(get(ROTA).param("numeroProcesso", CNJ).header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.registros[0].tribunal").value("TRF1"))
                .andExpect(jsonPath("$.registros[0].assuntos").isEmpty())
                .andExpect(jsonPath("$.registros[0].dataInicio").isEmpty())
                .andExpect(jsonPath("$.registros[0].orgaoJulgador").isEmpty());
        datajud.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"hits\":{\"hits\":[]}}", "{\"hits\":{\"hits\":[{\"_source\":{\"numeroProcesso\":\"outro\"}}]}}"})
    void retorna404SeNaoExisteCorrespondenciaExata(String json) throws Exception {
        datajud.expect(anything()).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
        mvc.perform(get(ROTA).param("numeroProcesso", CNJ).header("Authorization", bearer))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.codigo").value("CNJ_NAO_ENCONTRADO"));
        datajud.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"timed_out\":true,\"hits\":{\"hits\":[]}}", "resposta inválida"})
    void respostaExternaInvalidaNaoViraProcessoNaoEncontrado(String json) throws Exception {
        datajud.expect(anything()).andRespond(withSuccess(json, MediaType.APPLICATION_JSON));
        mvc.perform(get(ROTA).param("numeroProcesso", CNJ).header("Authorization", bearer))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.codigo").value("CNJ_CONSULTA_INDISPONIVEL"));
        datajud.verify();
    }

    @Test
    void informaLimiteDeConsultasSemExporCorpoExterno() throws Exception {
        datajud.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("detalhe externo interno"));
        mvc.perform(get(ROTA).param("numeroProcesso", CNJ).header("Authorization", bearer))
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.codigo").value("CNJ_CONSULTA_LIMITADA"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("detalhe externo interno"))));
        datajud.verify();
    }

    @Test
    void exigeAutenticacaoEValidaAntesDeConsultarApi() throws Exception {
        mvc.perform(get(ROTA).param("numeroProcesso", CNJ)).andExpect(status().isUnauthorized());
        mvc.perform(get(ROTA).param("numeroProcesso", "123").header("Authorization", bearer))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("CNJ_INVALIDO"));
        mvc.perform(get(ROTA).header("Authorization", bearer)).andExpect(status().isBadRequest());
        datajud.verify();
    }

    private String resposta(String... registros) {
        return "{\"timed_out\":false,\"hits\":{\"total\":{\"value\":" + registros.length + "},\"hits\":[" +
                java.util.Arrays.stream(registros).map(registro -> "{\"_source\":" + registro + "}")
                        .collect(java.util.stream.Collectors.joining(",")) + "]}}";
    }
}
