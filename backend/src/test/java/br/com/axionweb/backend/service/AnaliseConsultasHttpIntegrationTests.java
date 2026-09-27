package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.auth.CadastroUsuarioRequestDTO;
import br.com.axionweb.backend.dto.processo.ProcessoRequestDTO;
import br.com.axionweb.backend.model.processo.Assunto;
import br.com.axionweb.backend.model.processo.ClasseProcessual;
import br.com.axionweb.backend.repository.*;
import br.com.axionweb.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Serviços, cálculo, controller e H2 reais; somente as respostas HTTP externas são controladas.
@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:analise-http;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "consulta@teste.com")
class AnaliseConsultasHttpIntegrationTests {
    private static final String DATAJUD = """
            {"timed_out":false,"hits":{"hits":[{"_source":{
              "dataAjuizamento":"20250101000000","dataHoraUltimaAtualizacao":"2025-01-21T00:00:00Z",
              "movimentos":[{"nome":"Provimento","dataHora":"2025-01-11T00:00:00Z"},
                            {"nome":"Baixa Definitiva","dataHora":"2025-01-21T00:00:00Z"}]
            }}]}}
            """;
    private static final String DJEN = """
            {"status":"success","count":1,"items":[{"texto":"Comunicação de teste","siglaTribunal":"TJPR"}]}
            """;
    @Autowired private MockMvc mvc;
    @Autowired private DataJudService dataJudService;
    @Autowired private ComunicacaoService comunicacaoService;
    @Autowired private UsuarioService usuarioService;
    @Autowired private ProcessoService processoService;
    @Autowired private DataJudRepository estatisticas;
    @Autowired private ComunicacaoRepository comunicacoes;
    @Autowired private ProcessoRepository processos;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private JwtService jwtService;
    private MockRestServiceServer datajud;
    private MockRestServiceServer djen;
    private Long processoId;
    private String bearer;

    @BeforeEach
    void preparar() {
        comunicacoes.deleteAllInBatch(); estatisticas.deleteAllInBatch(); processos.deleteAllInBatch(); usuarios.deleteAllInBatch();
        usuarioService.cadastrar(new CadastroUsuarioRequestDTO("Consulta", "consulta@teste.com", "senha123"));
        bearer = "Bearer " + jwtService.gerarToken("consulta@teste.com");
        var classe = new ClasseProcessual(); classe.setNome("Recurso Inominado Cível");
        var assunto = new Assunto(); assunto.setNome("Dano material");
        processoId = processoService.cadastrar(new ProcessoRequestDTO("0000001", "Teste", "AUTOR", "TJPR", null,
                null, null, null, classe, assunto, null, null, null, null, null, null)).getId();
        var builderDatajud = RestClient.builder().baseUrl("https://datajud.teste");
        var builderDjen = RestClient.builder().baseUrl("https://djen.teste");
        datajud = MockRestServiceServer.bindTo(builderDatajud).build();
        djen = MockRestServiceServer.bindTo(builderDjen).build();
        ReflectionTestUtils.setField(dataJudService, "restDataJud", builderDatajud.build());
        ReflectionTestUtils.setField(comunicacaoService, "restComunicacao", builderDjen.build());
    }

    @Test
    void deveGravarEstatisticasEExporParaFrontendComCamposOpcionaisNulos() throws Exception {
        sucesso();
        mvc.perform(get("/api/dados/estatisticas/{id}", processoId).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].processoId").value(processoId))
                .andExpect(jsonPath("$[0].quantidadeProcessos").value(1))
                .andExpect(jsonPath("$[0].tempoAtePrimeiroJulgamento.medianaDias").value(10.0))
                .andExpect(jsonPath("$[0].tempoAtePrimeiraBaixa.medianaDias").value(20.0));
        mvc.perform(get("/api/dados/comunicacoes/{id}", processoId).header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].processoId").value(processoId));
        assertThat(estatisticas.findByProcessoIdOrderByIdAsc(processoId)).hasSize(1);
        assertThat(comunicacoes.findByProcessoIdOrderByIdAsc(processoId)).hasSize(1);
        datajud.verify(); djen.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"DATAJUD", "DJEN"})
    void falhaExternaDeveInformarEtapaEPreservarResultadoAnterior(String servico) throws Exception {
        sucesso();
        Long anterior = estatisticas.findByProcessoIdOrderByIdAsc(processoId).getFirst().getId();
        datajud.reset(); djen.reset();
        if (servico.equals("DATAJUD")) {
            // HTTP 200 com corpo inválido também é falha, não uma amostra vazia.
            datajud.expect(anything()).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        } else {
            datajud.expect(anything()).andRespond(withSuccess(DATAJUD, MediaType.APPLICATION_JSON));
            djen.expect(anything()).andRespond(withServerError().body("detalhe interno que não deve ser exposto"));
        }
        mvc.perform(post("/api/dados/analisar/{id}", processoId).header("Authorization", bearer).accept("text/plain, application/json"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.codigo").value("CONSULTA_" + servico + "_FALHOU"))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("detalhe interno"))));
        assertThat(estatisticas.findByProcessoIdOrderByIdAsc(processoId)).singleElement()
                .satisfies(item -> assertThat(item.getId()).isEqualTo(anterior));
        assertThat(comunicacoes.findByProcessoIdOrderByIdAsc(processoId)).hasSize(1);
        datajud.verify(); djen.verify();
    }

    @Test
    void consultasVaziasValidasDevemGravarAmostraZero() throws Exception {
        datajud.expect(anything()).andRespond(withSuccess("{\"hits\":{\"hits\":[]}}", MediaType.APPLICATION_JSON));
        djen.expect(anything()).andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));
        mvc.perform(post("/api/dados/analisar/{id}", processoId).header("Authorization", bearer)).andExpect(status().isOk());
        assertThat(estatisticas.findByProcessoIdOrderByIdAsc(processoId)).singleElement()
                .satisfies(item -> assertThat(item.getQuantidadeProcessos()).isZero());
    }

    @ParameterizedTest
    @ValueSource(strings = {"DATAJUD", "DJEN"})
    void limiteDeConsultasDeveSerIdentificadoSemGravarResultadosParciais(String servico) throws Exception {
        if (servico.equals("DATAJUD")) {
            datajud.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        } else {
            datajud.expect(anything()).andRespond(withSuccess(DATAJUD, MediaType.APPLICATION_JSON));
            djen.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        }
        mvc.perform(post("/api/dados/analisar/{id}", processoId).header("Authorization", bearer))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.codigo").value("CONSULTA_" + servico + "_LIMITADA"));
        assertThat(estatisticas.findByProcessoIdOrderByIdAsc(processoId)).isEmpty();
        assertThat(comunicacoes.findByProcessoIdOrderByIdAsc(processoId)).isEmpty();
        datajud.verify(); djen.verify();
    }

    private void sucesso() throws Exception {
        datajud.expect(anything()).andRespond(withSuccess(DATAJUD, MediaType.APPLICATION_JSON));
        djen.expect(anything()).andRespond(withSuccess(DJEN, MediaType.APPLICATION_JSON));
        mvc.perform(post("/api/dados/analisar/{id}", processoId).header("Authorization", bearer).accept("text/plain, application/json"))
                .andExpect(status().isOk()).andExpect(content().string("200"));
    }
}
