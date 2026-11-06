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
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;
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
    void parametrosMostramFiltrosReaisSemConsultarApisExternas() throws Exception {
        mvc.perform(get("/api/dados/estatisticas/{id}/parametros", processoId).header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(10))
                .andExpect(jsonPath("$[0].valor").value("TJPR"))
                .andExpect(jsonPath("$[0].utilizado").value(true))
                .andExpect(jsonPath("$[1].valor").isEmpty())
                .andExpect(jsonPath("$[1].utilizado").value(false))
                .andExpect(jsonPath("$[1].descricao").value("Código da classe não informado. A busca não filtra por classe nem usa o nome como alternativa."))
                .andExpect(jsonPath("$[2].utilizado").value(false))
                .andExpect(jsonPath("$[3].utilizado").value(false))
                .andExpect(jsonPath("$[4].utilizado").value(false))
                .andExpect(jsonPath("$[5].valor").value("400"))
                .andExpect(jsonPath("$[6].utilizado").value(false))
                .andExpect(jsonPath("$[7].utilizado").value(false))
                .andExpect(jsonPath("$[8].utilizado").value(false))
                .andExpect(jsonPath("$[9].utilizado").value(false));
        datajud.verify(); djen.verify();
    }

    @Test
    void parametrosPriorizamCodigoEIgnoramDatasPreenchidas() throws Exception {
        var processo = processos.findById(processoId).orElseThrow();
        processo.getClasseProcessual().setCodigo(" 460 ");
        processo.setGrau(" G2 ");
        processo.getAssunto().setCodigo("10439");
        var orgao = new br.com.axionweb.backend.model.processo.OrgaoJulgador();
        orgao.setCodigo("72197"); processo.setOrgaoJulgador(orgao);
        processo.setComarca("Curitiba"); processo.setUf("PR");
        processo.setDataInicio(java.time.LocalDate.of(2025, 1, 1));
        processos.save(processo);
        mvc.perform(get("/api/dados/estatisticas/{id}/parametros", processoId).header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$[1].valor").value("460"))
                .andExpect(jsonPath("$[1].descricao").value("Busca pelo código cadastrado."))
                .andExpect(jsonPath("$[2].utilizado").value(false))
                .andExpect(jsonPath("$[3].utilizado").value(false))
                .andExpect(jsonPath("$[4].valor").value("G2"))
                .andExpect(jsonPath("$[4].utilizado").value(true))
                .andExpect(jsonPath("$[6].utilizado").value(false))
                .andExpect(jsonPath("$[8].utilizado").value(false));
        datajud.verify(); djen.verify();
    }

    @Test
    void parametrosNaoExpoemProcessoDeOutraConta() throws Exception {
        mvc.perform(get("/api/dados/estatisticas/{id}/parametros", processoId)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("outra-conta@teste.com")))
                .andExpect(status().isNotFound());
        datajud.verify(); djen.verify();
    }

    @Test
    void deveGravarEstatisticasEExporParaFrontendComCamposOpcionaisNulos() throws Exception {
        sucesso();
        mvc.perform(get("/api/dados/estatisticas/{id}", processoId).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].processoId").value(processoId))
                .andExpect(jsonPath("$[0].quantidadeProcessos").value(1))
                .andExpect(jsonPath("$[0].length()").value(10))
                .andExpect(jsonPath("$[0].processosPendentes").value(0))
                .andExpect(jsonPath("$[0].tempoAtePrimeiroJulgamento.length()").value(2))
                .andExpect(jsonPath("$[0].tempoAtePrimeiroJulgamento.quantidade").value(1))
                .andExpect(jsonPath("$[0].tempoAtePrimeiraBaixa.length()").value(2))
                .andExpect(jsonPath("$[0].tempoAtePrimeiraBaixa.quantidade").value(1))
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

    @ParameterizedTest
    @ValueSource(strings = {"20250230000000", "20250101120000"})
    void persisteEConsultaAusenciaDeDuracaoParaDatasInconsistentes(String inicio) throws Exception {
        String resposta = DATAJUD.replace("20250101000000", inicio)
                .replace("2025-01-11T00:00:00Z", "2025-01-01T00:00:00Z")
                .replace("2025-01-21T00:00:00Z", "2025-01-01T00:00:00Z");
        datajud.expect(anything()).andRespond(withSuccess(resposta, MediaType.APPLICATION_JSON));
        djen.expect(anything()).andRespond(withSuccess(DJEN, MediaType.APPLICATION_JSON));
        mvc.perform(post("/api/dados/analisar/{id}", processoId).header("Authorization", bearer))
                .andExpect(status().isOk());
        assertThat(estatisticas.findByProcessoIdOrderByIdAsc(processoId)).hasSize(1);
        mvc.perform(get("/api/dados/estatisticas/{id}", processoId).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantidadeProcessos").value(1))
                .andExpect(jsonPath("$[0].tempoAtePrimeiroJulgamento.quantidade").value(0))
                .andExpect(jsonPath("$[0].tempoAtePrimeiroJulgamento.medianaDias").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$[0].tempoAtePrimeiraBaixa.quantidade").value(0))
                .andExpect(jsonPath("$[0].tempoAtePrimeiraBaixa.medianaDias").value(org.hamcrest.Matchers.nullValue()));
        datajud.verify(); djen.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"dataInicio\":\"2025-01-01\"}", "{\"dataFim\":\"2025-01-31\"}", "{\"dataInicio\":\"2025-01-01\",\"dataFim\":\"2025-01-31\"}"})
    void geraDataJudComPeriodoOpcionalSemAlterarComunicacoes(String filtro) throws Exception {
        sucesso();
        var comunicacaoId = comunicacoes.findByProcessoIdOrderByIdAsc(processoId).getFirst().getId();
        datajud.reset(); djen.reset();
        var expectation = datajud.expect(anything());
        if (filtro.contains("dataInicio")) expectation.andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath("$.query.bool.filter[0].bool.should[0].range.dataAjuizamento.gte").value("2025-01-01T00:00:00.000Z"));
        if (filtro.contains("dataFim")) expectation.andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath("$.query.bool.filter[0].bool.should[0].range.dataAjuizamento.lte").value("2025-01-31T23:59:59.999Z"));
        if (filtro.equals("{}")) expectation.andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath("$.query.bool.filter.length()").value(0));
        expectation.andRespond(withSuccess(DATAJUD, MediaType.APPLICATION_JSON));
        mvc.perform(post("/api/dados/estatisticas/{id}/gerar", processoId).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(filtro))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].tempoAtePrimeiraBaixa.medianaDias").value(20.0));
        assertThat(estatisticas.findByProcessoIdOrderByIdAsc(processoId)).hasSize(2);
        assertThat(comunicacoes.findByProcessoIdOrderByIdAsc(processoId)).singleElement()
                .satisfies(item -> assertThat(item.getId()).isEqualTo(comunicacaoId));
        datajud.verify(); djen.verify();
    }

    @Test
    void rejeitaPeriodoInvertidoAntesDeConsultarApi() throws Exception {
        mvc.perform(post("/api/dados/estatisticas/{id}/gerar", processoId).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"dataInicio\":\"2025-02-01\",\"dataFim\":\"2025-01-01\"}"))
                .andExpect(status().isBadRequest());
        datajud.verify(); djen.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 500, 504})
    void falhaNaGeracaoPreservaEstatisticasAnteriores(int httpExterno) throws Exception {
        sucesso();
        Long anterior = estatisticas.findByProcessoIdOrderByIdAsc(processoId).getFirst().getId();
        datajud.reset(); djen.reset();
        for (int tentativa = 0; tentativa < 1; tentativa++) {
            datajud.expect(anything()).andRespond(withStatus(HttpStatus.valueOf(httpExterno)));
        }
        mvc.perform(post("/api/dados/estatisticas/{id}/gerar", processoId).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.codigo").value(httpExterno == 429 ? "CONSULTA_DATAJUD_LIMITADA" : "CONSULTA_DATAJUD_FALHOU"));
        assertThat(estatisticas.findByProcessoIdOrderByIdAsc(processoId)).singleElement()
                .satisfies(item -> assertThat(item.getId()).isEqualTo(anterior));
        mvc.perform(get("/api/dados/estatisticas/{id}", processoId).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(anterior.intValue()))
                .andExpect(jsonPath("$[0].tempoAtePrimeiraBaixa.medianaDias").value(20.0));
        datajud.verify(); djen.verify();
    }

    @Test
    void gerarUsaConsultaSimplificadaECalculaEstatisticasDoRetorno() throws Exception {
        var processo = processos.findById(processoId).orElseThrow();
        processo.getClasseProcessual().setCodigo("460");
        processo.getAssunto().setCodigo("10439");
        processo.setGrau("TR");
        var orgao = new br.com.axionweb.backend.model.processo.OrgaoJulgador();
        orgao.setCodigo("72197"); processo.setOrgaoJulgador(orgao);
        processo.setDataInicio(java.time.LocalDate.of(2020, 1, 1));
        processo.setDataFim(java.time.LocalDate.of(2021, 1, 1));
        processos.save(processo);
        datajud.expect(anything())
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath("$.query.bool.filter.length()").value(2))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath("$.query.bool.filter[0].term['classe.codigo']").value("460"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath("$.query.bool.filter[1].term['grau.keyword']").value("TR"))
                .andRespond(withSuccess(DATAJUD, MediaType.APPLICATION_JSON));
        mvc.perform(post("/api/dados/estatisticas/{id}/gerar", processoId).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantidadeProcessos").value(1))
                .andExpect(jsonPath("$[0].tempoAtePrimeiroJulgamento.medianaDias").value(10.0))
                .andExpect(jsonPath("$[0].tempoAtePrimeiraBaixa.medianaDias").value(20.0));
        assertThat(estatisticas.findByProcessoIdOrderByIdAsc(processoId)).hasSize(1);
        datajud.verify(); djen.verify();
    }

    @Test
    void buscaSemProcessosPreservaResultadoAnteriorEInformaAusenciaDeAmostra() throws Exception {
        sucesso();
        Long anterior = estatisticas.findByProcessoIdOrderByIdAsc(processoId).getFirst().getId();
        datajud.reset(); djen.reset();
        datajud.expect(anything()).andRespond(withSuccess("{\"hits\":{\"hits\":[]}}", MediaType.APPLICATION_JSON));
        mvc.perform(post("/api/dados/estatisticas/{id}/gerar", processoId).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("DATAJUD_SEM_PROCESSOS"));
        mvc.perform(get("/api/dados/estatisticas/{id}", processoId).header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(anterior.intValue()));
        datajud.verify(); djen.verify();
    }

    @Test
    void limiteDataJudNaPrimeiraGeracaoNaoCriaEstatisticaVazia() throws Exception {
        datajud.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        mvc.perform(post("/api/dados/estatisticas/{id}/gerar", processoId).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.codigo").value("CONSULTA_DATAJUD_LIMITADA"));
        assertThat(estatisticas.findByProcessoIdOrderByIdAsc(processoId)).isEmpty();
        mvc.perform(get("/api/dados/estatisticas/{id}", processoId).header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        datajud.verify(); djen.verify();
    }

    @Test
    void naoGeraEstatisticasDeOutraConta() throws Exception {
        mvc.perform(post("/api/dados/estatisticas/{id}/gerar", processoId)
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("outra@teste.com"))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
        datajud.verify(); djen.verify();
    }

    @Test
    void botaoSemDatasEnviaMesmaConsultaDoGetAntigoECalculaMesmosResultados() throws Exception {
        var consultaAntiga = new java.util.concurrent.atomic.AtomicReference<String>();
        datajud.expect(anything()).andExpect(request -> consultaAntiga.set(
                ((org.springframework.mock.http.client.MockClientHttpRequest) request).getBodyAsString()))
                .andRespond(withSuccess(DATAJUD, MediaType.APPLICATION_JSON));
        djen.expect(anything()).andRespond(withSuccess(DJEN, MediaType.APPLICATION_JSON));
        mvc.perform(get("/api/dados/analisar/{id}", processoId).header("Authorization", bearer))
                .andExpect(status().isOk());
        Long idAntigo = estatisticas.findByProcessoIdOrderByIdAsc(processoId).getFirst().getId();
        datajud.verify(); djen.verify(); datajud.reset(); djen.reset();
        datajud.expect(anything())
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content().json(consultaAntiga.get()))
                .andRespond(withSuccess(DATAJUD, MediaType.APPLICATION_JSON));
        mvc.perform(post("/api/dados/estatisticas/{id}/gerar", processoId).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tempoAtePrimeiroJulgamento.medianaDias").value(10.0))
                .andExpect(jsonPath("$[0].tempoAtePrimeiraBaixa.medianaDias").value(20.0));
        assertThat(estatisticas.findByProcessoIdOrderByIdAsc(processoId)).hasSize(2).last()
                .satisfies(item -> assertThat(item.getId()).isNotEqualTo(idAntigo));
        mvc.perform(get("/api/dados/estatisticas/{id}", processoId).header("Authorization", bearer))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].quantidadeProcessos").value(1));
        datajud.verify(); djen.verify();
    }

    @Test
    void leEstatisticaAntigaSemAlterarColunasLegadasENormalizaMedianaSemAmostra() throws Exception {
        sucesso();
        // Simula o esquema anterior apenas no H2 isolado desta classe.
        jdbc.execute("ALTER TABLE datajud_estatistica ADD COLUMN IF NOT EXISTS primeira_baixa_media_dias DOUBLE");
        jdbc.execute("ALTER TABLE datajud_estatistica ADD COLUMN IF NOT EXISTS pendente_mediana_dias DOUBLE");
        jdbc.update("""
                UPDATE datajud_estatistica SET primeira_baixa_media_dias=123,
                    pendente_mediana_dias=456, julgamento_quantidade=0, julgamento_mediana_dias=0
                WHERE processo_id=?
                """, processoId);
        mvc.perform(get("/api/dados/estatisticas/{id}", processoId).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].length()").value(10))
                .andExpect(jsonPath("$[0].tempoAtePrimeiroJulgamento.quantidade").value(0))
                .andExpect(jsonPath("$[0].tempoAtePrimeiroJulgamento.medianaDias").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$[0].tempoAtePrimeiraBaixa.medianaDias").value(20.0))
                .andExpect(jsonPath("$[0].tempoPendente").doesNotExist())
                .andExpect(jsonPath("$[0].tempoAtePrimeiraBaixa.mediaDias").doesNotExist());
        assertThat(jdbc.queryForObject("SELECT primeira_baixa_media_dias FROM datajud_estatistica WHERE processo_id=?",
                Double.class, processoId)).isEqualTo(123.0);
        assertThat(jdbc.queryForObject("SELECT pendente_mediana_dias FROM datajud_estatistica WHERE processo_id=?",
                Double.class, processoId)).isEqualTo(456.0);
        jdbc.update("UPDATE datajud_estatistica SET julgamento_quantidade=NULL, julgamento_mediana_dias=NULL WHERE processo_id=?", processoId);
        mvc.perform(get("/api/dados/estatisticas/{id}", processoId).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tempoAtePrimeiroJulgamento").value(org.hamcrest.Matchers.nullValue()));
        datajud.verify(); djen.verify();
    }

    private void sucesso() throws Exception {
        datajud.expect(anything()).andRespond(withSuccess(DATAJUD, MediaType.APPLICATION_JSON));
        djen.expect(anything()).andRespond(withSuccess(DJEN, MediaType.APPLICATION_JSON));
        mvc.perform(post("/api/dados/analisar/{id}", processoId).header("Authorization", bearer).accept("text/plain, application/json"))
                .andExpect(status().isOk()).andExpect(content().string("200"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"dataInicio\":\"2025-01-01\",\"dataFim\":\"2025-01-31\"}"})
    void geraEstatisticasDaAmostraEmRespostaUnica(String filtro) throws Exception {
        datajud.expect(anything()).andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath("$.size").value(400))
                .andRespond(withSuccess(paginaDataJud(0, 400), MediaType.APPLICATION_JSON));
        mvc.perform(post("/api/dados/estatisticas/{id}/gerar", processoId).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(filtro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].quantidadeProcessos").value(400))
                .andExpect(jsonPath("$[0].tempoAtePrimeiraBaixa.quantidade").value(400))
                .andExpect(jsonPath("$[0].tempoAtePrimeiraBaixa.quantidade").value(400))
                .andExpect(jsonPath("$[0].tempoAtePrimeiraBaixa.medianaDias").value(20.0));
        assertThat(estatisticas.findByProcessoIdOrderByIdAsc(processoId)).singleElement()
                .satisfies(item -> assertThat(item.getQuantidadeProcessos()).isEqualTo(400));
        datajud.verify(); djen.verify();
    }

    @Test
    void falhaNaConsultaUnicaPreservaEstatisticaAnterior() throws Exception {
        sucesso();
        Long anterior = estatisticas.findByProcessoIdOrderByIdAsc(processoId).getFirst().getId();
        datajud.reset(); djen.reset();
        datajud.expect(anything()).andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT));
        mvc.perform(post("/api/dados/estatisticas/{id}/gerar", processoId).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.codigo").value("CONSULTA_DATAJUD_FALHOU"));
        assertThat(estatisticas.findByProcessoIdOrderByIdAsc(processoId)).singleElement().satisfies(item -> {
            assertThat(item.getId()).isEqualTo(anterior);
            assertThat(item.getQuantidadeProcessos()).isEqualTo(1);
        });
        datajud.verify(); djen.verify();
    }

    private String paginaDataJud(int inicio, int quantidade) {
        return "{\"timed_out\":false,\"hits\":{\"hits\":[" + java.util.stream.IntStream.range(inicio, inicio + quantidade)
                .mapToObj(i -> """
                        {"_id":"id-%03d","sort":["id-%03d"],"_source":{
                        "dataAjuizamento":"20250101000000","dataHoraUltimaAtualizacao":"2025-01-21T00:00:00Z",
                        "movimentos":[{"nome":"Baixa Definitiva","dataHora":"2025-01-21T00:00:00Z"}]}}
                        """.formatted(i, i))
                .collect(java.util.stream.Collectors.joining(",")) + "]}}";
    }
}
