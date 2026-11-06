package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.datajud.ParametrosDataJudDTO;
import br.com.axionweb.backend.model.processo.Assunto;
import br.com.axionweb.backend.model.processo.ClasseProcessual;
import br.com.axionweb.backend.model.processo.OrgaoJulgador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import java.time.LocalDate;
import java.util.stream.IntStream;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class ConsultaDataJudTests {
    private DataJudService service;
    private MockRestServiceServer servidor;

    @BeforeEach
    void preparar() {
        var builder = RestClient.builder().baseUrl("https://datajud.teste");
        servidor = MockRestServiceServer.bindTo(builder).build();
        service = new DataJudService();
        ReflectionTestUtils.setField(service, "restDataJud", builder.build());
        var acesso = org.mockito.Mockito.mock(DataJudAcessoService.class);
        org.mockito.Mockito.when(acesso.consultar(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyMap(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(i -> ((java.util.function.Supplier<?>) i.getArgument(2)).get());
        ReflectionTestUtils.setField(service, "acesso", acesso);
    }

    @Test
    void camposOpcionaisNulosNaoDevemImpedirConsulta() {
        var parametros = parametros();
        servidor.expect(requestTo("https://datajud.teste/api_publica_tjpr/_search"))
                .andExpect(content().json("""
                        {"size":400,"track_total_hits":false,"query":{"bool":{"filter":[]}},
                         "sort":[{"id.keyword":"asc"}],
                         "_source":["numeroProcesso","dataAjuizamento","movimentos.nome","movimentos.dataHora"]}
                        """))
                .andRespond(withSuccess("{\"hits\":{\"hits\":[]}}", MediaType.APPLICATION_JSON));

        assertThat(service.buscarProcessos(parametros)).isEmpty();
        servidor.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void classeSemCodigoNaoUsaNomeNemAssuntoOuOrgao(String codigo) {
        var parametros = parametros();
        var classe = new ClasseProcessual(); classe.setNome(" Recurso Inominado Cível ");
        classe.setCodigo(codigo);
        var assunto = new Assunto(); assunto.setNome("Dano material");
        var orgao = new OrgaoJulgador(); orgao.setCodigo("72197");
        parametros.setClasseProcessual(classe); parametros.setAssunto(assunto); parametros.setOrgaoJulgador(orgao);
        servidor.expect(requestTo("https://datajud.teste/api_publica_tjpr/_search"))
                .andExpect(content().json("""
                        {"size":400,"query":{"bool":{"filter":[]}}}
                        """))
                .andExpect(jsonPath("$.query.bool.filter.length()").value(0))
                .andRespond(withSuccess("{\"hits\":{\"hits\":[]}}", MediaType.APPLICATION_JSON));
        assertThat(service.buscarProcessos(parametros)).isEmpty();
        servidor.verify();
    }

    @Test
    void cadastroCompletoMontaSomenteClasseEGrauSemHerdarPeriodo() {
        var processo = new br.com.axionweb.backend.model.processo.Processo();
        processo.setTribunal(" tjpr ");
        processo.setGrau(" tr ");
        var classe = new ClasseProcessual(); classe.setCodigo(" 460 "); classe.setNome("Recurso Inominado Cível");
        processo.setClasseProcessual(classe);
        var assunto = new Assunto(); assunto.setCodigo("10439"); processo.setAssunto(assunto);
        var orgao = new OrgaoJulgador(); orgao.setCodigo("72197"); processo.setOrgaoJulgador(orgao);
        processo.setComarca("Curitiba"); processo.setUf("PR");
        processo.setDataInicio(LocalDate.of(2025, 1, 1)); processo.setDataFim(LocalDate.of(2025, 12, 31));
        var parametros = service.montarParametroDataJud(processo);
        servidor.expect(requestTo("https://datajud.teste/api_publica_tjpr/_search"))
                .andExpect(jsonPath("$.query.bool.filter.length()").value(2))
                .andExpect(jsonPath("$.query.bool.filter[0].term['classe.codigo']").value("460"))
                .andExpect(jsonPath("$.query.bool.filter[1].term['grau.keyword']").value("TR"))
                .andRespond(withSuccess("{\"hits\":{\"hits\":[]}}", MediaType.APPLICATION_JSON));
        assertThat(service.buscarProcessos(parametros)).isEmpty();
        assertThat(parametros.getQuantidadeResultados()).isEqualTo(400);
        assertThat(parametros.getDataInicio()).isNull();
        assertThat(parametros.getDataFim()).isNull();
        servidor.verify();
    }

    @Test
    void grauContinuaAplicadoQuandoClasseNaoTemCodigo() {
        var parametros = parametros();
        var classe = new ClasseProcessual(); classe.setNome("Recurso Inominado Cível");
        parametros.setClasseProcessual(classe); parametros.setGrau(" g2 ");
        servidor.expect(anything())
                .andExpect(jsonPath("$.query.bool.filter.length()").value(1))
                .andExpect(jsonPath("$.query.bool.filter[0].term['grau.keyword']").value("G2"))
                .andRespond(withSuccess("{\"hits\":{\"hits\":[]}}", MediaType.APPLICATION_JSON));
        assertThat(service.buscarProcessos(parametros)).isEmpty();
        servidor.verify();
    }

    @Test
    void respostaInvalidaNaoPodeSerConfundidaComAmostraVazia() {
        servidor.expect(anything()).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.buscarProcessos(parametros())).isInstanceOf(RestClientException.class);
    }

    @Test
    void timeoutNaRespostaNaoPodeSubstituirResultadosPorAmostraParcial() {
        servidor.expect(anything()).andRespond(withSuccess("{\"timed_out\":true,\"hits\":{\"hits\":[]}}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.buscarProcessos(parametros())).isInstanceOf(RestClientException.class);
    }

    @Test
    void registroSemSourceNaoPodeVirarEstatisticaVazia() {
        servidor.expect(anything()).andRespond(withSuccess("{\"hits\":{\"hits\":[{}]}}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.buscarProcessos(parametros())).isInstanceOf(RestClientException.class);
    }

    @Test
    void falhaEmShardNaoPodeSerConfundidaComAmostraCompleta() {
        servidor.expect(anything()).andRespond(withSuccess(
                "{\"timed_out\":false,\"_shards\":{\"total\":3,\"successful\":2,\"failed\":1},\"hits\":{\"hits\":[]}}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.buscarProcessos(parametros())).isInstanceOf(RestClientException.class)
                .hasMessageContaining("falha em shards");
        servidor.verify();
    }

    @Test
    void propagaTimeoutParaAgendadorSemRepeticaoLocal() {
        servidor.expect(anything()).andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withException(
                new java.net.SocketTimeoutException("Read timed out")));
        assertThatThrownBy(() -> service.buscarProcessos(parametros())).isInstanceOf(RestClientException.class);
        servidor.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"sem", "inicio", "fim", "ambas"})
    void consultaUnicaComOsMesmosFiltrosComDatasOpcionais(String datas) {
        var parametros = parametros();
        parametros.setQuantidadeResultado(120);
        var classe = new ClasseProcessual(); classe.setCodigo("460"); parametros.setClasseProcessual(classe);
        var assunto = new Assunto(); assunto.setCodigo("10439"); parametros.setAssunto(assunto);
        var orgao = new OrgaoJulgador(); orgao.setCodigo("72197"); parametros.setOrgaoJulgador(orgao);
        parametros.setGrau("TR");
        boolean inicio = datas.equals("inicio") || datas.equals("ambas");
        boolean fim = datas.equals("fim") || datas.equals("ambas");
        if (inicio) parametros.setDataInicio(LocalDate.of(2025, 1, 1));
        if (fim) parametros.setDataFim(LocalDate.of(2025, 12, 31));
        {
            var expectativa = servidor.expect(requestTo("https://datajud.teste/api_publica_tjpr/_search"))
                    .andExpect(jsonPath("$.size").value(120))
                    .andExpect(jsonPath("$.query.bool.filter[0].term['classe.codigo']").value("460"))
                    .andExpect(jsonPath("$.query.bool.filter[1].term['grau.keyword']").value("TR"))
                    .andExpect(jsonPath("$.query.bool.filter.length()").value(inicio || fim ? 3 : 2));
            expectativa.andExpect(jsonPath("$.search_after").doesNotExist());
            if (inicio) expectativa
                    .andExpect(jsonPath("$.query.bool.filter[2].bool.should[0].range.dataAjuizamento.gte").value("2025-01-01T00:00:00.000Z"))
                    .andExpect(jsonPath("$.query.bool.filter[2].bool.should[1].range.dataAjuizamento.gte").value("20250101000000"));
            else expectativa.andExpect(jsonPath("$.query.bool.filter[2].bool.should[*].range.dataAjuizamento.gte").doesNotExist());
            if (fim) expectativa
                    .andExpect(jsonPath("$.query.bool.filter[2].bool.should[0].range.dataAjuizamento.lte").value("2025-12-31T23:59:59.999Z"))
                    .andExpect(jsonPath("$.query.bool.filter[2].bool.should[1].range.dataAjuizamento.lte").value("20251231235959"));
            else expectativa.andExpect(jsonPath("$.query.bool.filter[2].bool.should[*].range.dataAjuizamento.lte").doesNotExist());
            if (inicio || fim) expectativa
                    .andExpect(jsonPath("$.query.bool.filter[2].bool.minimum_should_match").value(1))
                    .andExpect(jsonPath("$.query.bool.filter[2].bool.should[1].range.dataAjuizamento.format").doesNotExist());
            if (!inicio && !fim) expectativa.andExpect(jsonPath("$.query.bool.filter.length()").value(2));
            expectativa.andRespond(withSuccess(pagina(0, 120), MediaType.APPLICATION_JSON));
        }
        assertThat(service.buscarProcessos(parametros)).hasSize(120)
                .extracting(dado -> dado.getNumeroProcesso()).doesNotHaveDuplicates();
        servidor.verify();
    }

    @Test
    void coleta400SemPaginar() {
        servidor.expect(anything()).andExpect(jsonPath("$.size").value(400))
                .andExpect(jsonPath("$.search_after").doesNotExist())
                .andRespond(withSuccess(pagina(0, 400), MediaType.APPLICATION_JSON));
        assertThat(service.buscarProcessos(parametros())).hasSize(400);
        servidor.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 502, 503, 504})
    void propagaFalhaParaCoordenacaoSemLoopLocal(int status) {
        servidor.expect(anything()).andRespond(withStatus(HttpStatus.valueOf(status)));
        assertThatThrownBy(() -> service.buscarProcessos(parametros())).isInstanceOf(RestClientException.class);
        servidor.verify();
    }

    @Test
    void rejeitaRespostaMaiorQueSolicitado() {
        servidor.expect(anything()).andRespond(withSuccess(pagina(0, 401), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.buscarProcessos(parametros())).isInstanceOf(RestClientException.class).hasMessageContaining("tamanho solicitado");
        servidor.verify();
    }

    @Test
    void naoExigeCursorParaAmostraCompleta() {
        servidor.expect(anything()).andRespond(withSuccess(pagina(0, 400).replace("sort", "ignorado"), MediaType.APPLICATION_JSON));
        assertThat(service.buscarProcessos(parametros())).hasSize(400);
        servidor.verify();
    }

    @Test
    void naoContaRegistroRepetidoNaResposta() {
        servidor.expect(anything()).andRespond(withSuccess(pagina(0, 2).replace("id-001", "id-000"), MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.buscarProcessos(parametros())).isInstanceOf(RestClientException.class).hasMessageContaining("repetiu um registro");
        servidor.verify();
    }

    private String pagina(int inicio, int quantidade) {
        return "{\"timed_out\":false,\"hits\":{\"hits\":[" + IntStream.range(inicio, inicio + quantidade)
                .mapToObj(i -> "{\"_id\":\"id-%03d\",\"sort\":[\"id-%03d\"],\"_source\":{\"numeroProcesso\":\"%020d\",\"dataAjuizamento\":\"20250101000000\"}}".formatted(i, i, i))
                .collect(Collectors.joining(",")) + "]}}";
    }

    @ParameterizedTest
    @ValueSource(strings = {"inicio", "fim"})
    void periodoComUmLimiteDescartaDatasSemBuscarReposicao(String limite) {
        var parametros = parametros();
        if (limite.equals("inicio")) parametros.setDataInicio(LocalDate.of(2025, 1, 1));
        else parametros.setDataFim(LocalDate.of(2025, 1, 1));
        String fora = limite.equals("inicio") ? "20241231235959" : "20250102000000";
        servidor.expect(anything()).andRespond(withSuccess(pagina(0, 100).replace("20250101000000", fora), MediaType.APPLICATION_JSON));
        assertThat(service.buscarProcessos(parametros)).isEmpty();
        servidor.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"20250101000000", "20250101235959", "2025-01-01T00:00:00Z", "2025-01-01T23:59:59.999Z", "2025-01-01"})
    void incluiDiasLimiteEmDatasCompactasEIso(String data) {
        var parametros = parametros();
        parametros.setDataInicio(LocalDate.of(2025, 1, 1));
        parametros.setDataFim(LocalDate.of(2025, 1, 1));
        servidor.expect(anything()).andRespond(withSuccess(pagina(0, 1).replace("20250101000000", data), MediaType.APPLICATION_JSON));
        assertThat(service.buscarProcessos(parametros)).hasSize(1);
        servidor.verify();
    }

    @ParameterizedTest
    @ValueSource(strings = {"20241231235959", "20250102000000", "2025-01-01T23:59:59-03:00", "20250230000000", "invalida"})
    void excluiDatasForaDoPeriodoOuInvalidas(String data) {
        var parametros = parametros();
        parametros.setDataInicio(LocalDate.of(2025, 1, 1));
        parametros.setDataFim(LocalDate.of(2025, 1, 1));
        servidor.expect(anything()).andRespond(withSuccess(pagina(0, 1).replace("20250101000000", data), MediaType.APPLICATION_JSON));
        assertThat(service.buscarProcessos(parametros)).isEmpty();
        servidor.verify();
    }

    private ParametrosDataJudDTO parametros() {
        var parametros = new ParametrosDataJudDTO();
        parametros.setTribunal("TJPR");
        parametros.setQuantidadeResultado(400);
        return parametros;
    }
}
