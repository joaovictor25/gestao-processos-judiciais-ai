package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.auth.CadastroUsuarioRequestDTO;
import br.com.axionweb.backend.dto.processo.ProcessoRequestDTO;
import br.com.axionweb.backend.dto.datajud.*;
import br.com.axionweb.backend.exception.DataJudAguardarException;
import br.com.axionweb.backend.repository.*;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:datajud-fila;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop", "app.datajud.cache-ms=300000"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "fila@teste.com")
@ExtendWith(OutputCaptureExtension.class)
class DataJudFilaHttpIntegrationTests {
    record Resposta(int status, String corpo, String retryAfter) { }
    private static final Queue<Resposta> respostas = new ConcurrentLinkedQueue<>();
    private static final List<String> pedidos = new CopyOnWriteArrayList<>();
    private static final ExecutorService servidorThreads = Executors.newCachedThreadPool();
    private static volatile CountDownLatch entrou, liberar;
    private static final HttpServer servidor;
    static {
        try {
            servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            servidor.setExecutor(servidorThreads);
            servidor.createContext("/", troca -> {
                pedidos.add(new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                if (entrou != null) entrou.countDown();
                try { if (liberar != null) liberar.await(10, TimeUnit.SECONDS); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                var resposta = respostas.poll();
                if (resposta == null) resposta = new Resposta(501, "{}", null);
                troca.getResponseHeaders().set("Content-Type", "application/json");
                if (resposta.retryAfter != null) troca.getResponseHeaders().set("Retry-After", resposta.retryAfter);
                byte[] corpo = resposta.corpo.getBytes(StandardCharsets.UTF_8);
                troca.sendResponseHeaders(resposta.status, corpo.length);
                try (var out = troca.getResponseBody()) { out.write(corpo); }
            });
            servidor.start();
        } catch (java.io.IOException erro) { throw new ExceptionInInitializerError(erro); }
    }
    @DynamicPropertySource static void url(DynamicPropertyRegistry r) { r.add("app.datajud.base-url", () -> "http://127.0.0.1:" + servidor.getAddress().getPort()); }
    @AfterAll static void parar() { servidor.stop(0); servidorThreads.shutdownNow(); }
    @Autowired MockMvc mvc;
    @Autowired GeracaoEstatisticaService fila;
    @Autowired DataJudAcessoService acesso;
    @Autowired DataJudService datajud;
    @Autowired AnaliseService analises;
    @Autowired UsuarioService usuarios;
    @Autowired ProcessoService processos;
    @Autowired DataJudRepository estatisticas;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @Autowired org.springframework.transaction.PlatformTransactionManager manager;
    Long processoId;

    @BeforeEach void preparar() {
        respostas.clear(); pedidos.clear(); entrou = null; liberar = null;
        jdbc.update("delete from datajud_solicitacao"); jdbc.update("delete from datajud_cache");
        jdbc.update("update datajud_controle set proxima_consulta=0, ocupado_ate=0, reserva=null");
        ReflectionTestUtils.setField(acesso, "intervalo", 0L); ReflectionTestUtils.setField(acesso, "pausa429", 0L);
        ReflectionTestUtils.setField(acesso, "retryBase", 0L); ReflectionTestUtils.setField(fila, "retryBase", 0L);
        ReflectionTestUtils.setField(fila, "filaUsuario", 5);
        if (jdbc.queryForObject("select count(*) from usuarios where email='fila@teste.com'", Integer.class) == 0)
            usuarios.cadastrar(new CadastroUsuarioRequestDTO("Fila", "fila@teste.com", "senha123"));
        processoId = novoProcesso();
    }
    Long novoProcesso() { return processos.cadastrar(new ProcessoRequestDTO("0000001", "Teste", "AUTOR", "TJPR", null, null, null, null, null, null, null, null, null, null, null, null)).getId(); }
    String rota() { return "/api/dados/estatisticas/" + processoId + "/solicitacoes"; }
    String solicitar() throws Exception {
        var retorno = executar(post(rota()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("AGUARDANDO")).andReturn();
        return mapper.readTree(retorno.getResponse().getContentAsString()).path("id").asText();
    }
    void sucesso(int quantidade) { respostas.add(new Resposta(200, amostra(quantidade), null)); }
    static String amostra(int quantidade) {
        return "{\"timed_out\":false,\"hits\":{\"hits\":[" + IntStream.range(0, quantidade).mapToObj(i ->
                "{\"_id\":\"id-" + i + "\",\"_source\":{\"numeroProcesso\":\"" + i + "\",\"dataAjuizamento\":\"20250101000000\",\"movimentos\":[{\"nome\":\"Baixa Definitiva\",\"dataHora\":\"2025-01-21T00:00:00Z\"}]}}")
                .collect(java.util.stream.Collectors.joining(",")) + "]}}";
    }

    private org.springframework.test.web.servlet.ResultActions executar(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder pedido) throws Exception {
        try { return mvc.perform(pedido); }
        finally {
            var contexto = org.springframework.security.core.context.SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated("fila@teste.com", null, List.of()));
            org.springframework.security.core.context.SecurityContextHolder.setContext(contexto);
        }
    }
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder get(String path) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("fila@teste.com"));
    }
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder post(String path) {
        return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("fila@teste.com"));
    }

    @Test void gera400EmUmaChamadaComContratoAssincronoERecuperaAcompanhamento() throws Exception {
        sucesso(400); String id = solicitar(); assertThat(pedidos).isEmpty();
        executar(get(rota() + "/atual")).andExpect(jsonPath("$.id").value(id));
        fila.processarProxima();
        executar(get(rota() + "/" + id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONCLUIDA"))
                .andExpect(jsonPath("$.resultados[0].quantidadeProcessos").value(400))
                .andExpect(jsonPath("$.resultados[0].tempoAtePrimeiraBaixa.medianaDias").value(20.0));
        assertThat(pedidos).hasSize(1);
        var corpo = mapper.readTree(pedidos.getFirst()); assertThat(corpo.path("size").asInt()).isEqualTo(400);
        assertThat(corpo.has("search_after")).isFalse();
        executar(get(rota() + "/atual")).andExpect(status().isNoContent());
    }

    @Test void cliquesRepetidosEConsultaRecenteReutilizamMesmoTrabalho() throws Exception {
        sucesso(3); String id = solicitar();
        executar(post(rota()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.id").value(id));
        fila.processarProxima();
        executar(post(rota()).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        assertThat(pedidos).hasSize(1); assertThat(estatisticas.findByProcessoIdOrderByIdAsc(processoId)).hasSize(1);
    }

    @Test void cacheReutilizaConsultaEquivalenteEmOutroProcessoDoMesmoUsuario() throws Exception {
        sucesso(3); solicitar(); fila.processarProxima();
        processoId = novoProcesso(); solicitar(); fila.processarProxima();
        assertThat(pedidos).hasSize(1); assertThat(estatisticas.findByProcessoIdOrderByIdAsc(processoId)).hasSize(1);
        jdbc.update("update datajud_cache set expira_em=0"); jdbc.update("update datajud_solicitacao set atualizada_em=0");
        sucesso(2); solicitar(); fila.processarProxima(); assertThat(pedidos).hasSize(2);
    }

    @Test void erro429RespeitaRetryAfterEConcluiSemOutroClique(CapturedOutput output) throws Exception {
        respostas.add(new Resposta(429, "{\"error\":{\"type\":\"es_rejected_execution_exception\",\"reason\":\"SEGREDO_NAO_REGISTRAR\"}}", "1"));
        sucesso(4); String id = solicitar(); long antes = System.currentTimeMillis(); fila.processarProxima();
        var atual = fila.consultar(processoId, id);
        assertThat(atual.status()).isEqualTo("REAGENDADA"); assertThat(atual.tentativas()).isEqualTo(1);
        assertThat(atual.proximaTentativa().toEpochMilli()).isGreaterThanOrEqualTo(antes + 1000);
        fila.processarProxima(); assertThat(pedidos).hasSize(1);
        Thread.sleep(1100); fila.processarProxima();
        assertThat(fila.consultar(processoId, id).status()).isEqualTo("CONCLUIDA"); assertThat(pedidos).hasSize(2);
        assertThat(output).contains("es_rejected_execution_exception").doesNotContain("SEGREDO_NAO_REGISTRAR");
    }

    @Test void erroPersistenteEncerraAposTresTentativasEPreservaResultados() throws Exception {
        sucesso(2); analises.gerarEstatisticas(processoId, new GerarEstatisticasRequestDTO(null, null));
        Long anterior = estatisticas.findByProcessoIdOrderByIdAsc(processoId).getFirst().getId();
        jdbc.update("delete from datajud_cache");
        for (int i=0; i<3; i++) respostas.add(new Resposta(429, "{}", "0"));
        String id = solicitar(); for (int i=0; i<5; i++) fila.processarProxima();
        assertThat(fila.consultar(processoId, id).status()).isEqualTo("FALHOU");
        assertThat(fila.consultar(processoId, id).codigoErro()).isEqualTo("CONSULTA_DATAJUD_LIMITADA");
        assertThat(pedidos).hasSize(4);
        assertThat(estatisticas.findByProcessoIdOrderByIdAsc(processoId)).singleElement().extracting(d -> d.getId()).isEqualTo(anterior);
    }

    @Test void retryAfterSuperiorAoPrazoEncerraSemAnteciparNovaConsulta() throws Exception {
        respostas.add(new Resposta(429, "{}", "3600")); String id = solicitar(); fila.processarProxima();
        assertThat(fila.consultar(processoId, id).status()).isEqualTo("FALHOU");
        assertThat(jdbc.queryForObject("select proxima_consulta from datajud_controle", Long.class)).isGreaterThan(System.currentTimeMillis() + 3_500_000);
        assertThat(pedidos).hasSize(1);
    }

    @Test void concorrenciaEntreInstanciasCompartilhaPausaEIntervalo() {
        ReflectionTestUtils.setField(acesso, "intervalo", 2000L);
        sucesso(1);
        var p = new ParametrosDataJudDTO(); p.setTribunal("TJPR"); p.setQuantidadeResultado(400);
        datajud.buscarProcessos(p);
        var outraInstancia = new DataJudAcessoService(manager, mapper);
        ReflectionTestUtils.setField(outraInstancia, "em", ReflectionTestUtils.getField(acesso, "em"));
        assertThatThrownBy(() -> outraInstancia.consultar("/outro", Map.of("size", 1), () -> { throw new AssertionError("Não deveria chamar HTTP"); }))
                .isInstanceOf(DataJudAguardarException.class);
        assertThat(pedidos).hasSize(1);
    }

    @Test void chamadasSimultaneasNaoUltrapassamUmaConsultaEmVoo() throws Exception {
        entrou = new CountDownLatch(1); liberar = new CountDownLatch(1); sucesso(1);
        var p = new ParametrosDataJudDTO(); p.setTribunal("TJPR"); p.setQuantidadeResultado(400);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var primeira = executor.submit(() -> datajud.buscarProcessos(p));
            assertThat(entrou.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> datajud.buscarProcessos(p)).isInstanceOf(DataJudAguardarException.class);
            liberar.countDown(); assertThat(primeira.get(5, TimeUnit.SECONDS)).hasSize(1);
        } finally { liberar.countDown(); }
        assertThat(pedidos).hasSize(1);
    }

    @Test void falhaTemporariaAumentaIntervaloEntreTentativas() throws Exception {
        ReflectionTestUtils.setField(fila, "retryBase", 1000L);
        respostas.add(new Resposta(503, "{}", null)); respostas.add(new Resposta(503, "{}", null));
        String id = solicitar(); long antes = System.currentTimeMillis(); fila.processarProxima();
        assertThat(fila.consultar(processoId, id).proximaTentativa().toEpochMilli()).isGreaterThanOrEqualTo(antes + 1000);
        jdbc.update("update datajud_solicitacao set proxima_tentativa=0"); antes = System.currentTimeMillis(); fila.processarProxima();
        assertThat(fila.consultar(processoId, id).proximaTentativa().toEpochMilli()).isGreaterThanOrEqualTo(antes + 2000);
    }

    @Test void respostasInvalidasNaoEntramNoCacheNemCriamEstatisticas() throws Exception {
        respostas.add(new Resposta(200, "{\"timed_out\":true,\"hits\":{\"hits\":[]}}", null));
        String id = solicitar(); fila.processarProxima();
        assertThat(fila.consultar(processoId, id).status()).isEqualTo("FALHOU");
        assertThat(estatisticas.findByProcessoIdOrderByIdAsc(processoId)).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from datajud_cache", Integer.class)).isZero();
    }

    @Test void autorizacaoValidacaoEFilaLimitadaNaoConsultamDataJud() throws Exception {
        String id = solicitar();
        executar(get(rota() + "/" + id).with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("outra@teste.com"))).andExpect(status().isNotFound());
        executar(post(rota()).contentType(MediaType.APPLICATION_JSON).content("{\"dataInicio\":\"2025-01-01\"}"))
                .andExpect(status().isConflict());
        executar(post(rota()).contentType(MediaType.APPLICATION_JSON).content("{\"dataInicio\":\"2025-02-01\",\"dataFim\":\"2025-01-01\"}"))
                .andExpect(status().isBadRequest());
        ReflectionTestUtils.setField(fila, "filaUsuario", 1); processoId = novoProcesso();
        executar(post(rota()).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isTooManyRequests());
        assertThat(pedidos).isEmpty();
    }

    @Test void recuperaReservaExpiradaSemDuplicarEstatistica() throws Exception {
        sucesso(1); String id = solicitar();
        jdbc.update("update datajud_solicitacao set status='CONSULTANDO', reserva='reserva-antiga', ocupada_ate=0, tentativas=1 where id=?", id);
        fila.processarProxima(); fila.processarProxima();
        assertThat(fila.consultar(processoId, id).status()).isEqualTo("CONCLUIDA");
        assertThat(pedidos).hasSize(1); assertThat(estatisticas.findByProcessoIdOrderByIdAsc(processoId)).hasSize(1);
    }

    @Test void prazoDaFilaExpiraSemChamadaExterna() throws Exception {
        String id = solicitar(); jdbc.update("update datajud_solicitacao set prazo_final=0 where id=?", id); fila.processarProxima();
        assertThat(fila.consultar(processoId, id).codigoErro()).isEqualTo("DATAJUD_PRAZO_EXCEDIDO"); assertThat(pedidos).isEmpty();
    }

    @Test void interpretaRetryAfterEmSegundosEDataHttp() {
        Instant agora = Instant.parse("2026-11-04T00:00:00Z");
        assertThat(DataJudAcessoService.retryAfterMillis("120", agora)).isEqualTo(120000);
        assertThat(DataJudAcessoService.retryAfterMillis("Wed, 04 Nov 2026 00:01:00 GMT", agora)).isEqualTo(60000);
        assertThat(DataJudAcessoService.retryAfterMillis("invalido", agora)).isZero();
    }

    @Test void consultaCnjRespeitaPausaExternaEReutilizaCacheAposRecuperacao(CapturedOutput output) throws Exception {
        String rotaCnj = "/api/processo/consultar-datajud?numeroProcesso=00008323520184013202";
        respostas.add(new Resposta(429, "{\"error\":{\"type\":\"search_phase_execution_exception\",\"reason\":\"SEGREDO_CNJ\"}}", "45"));
        var recusada = executar(get(rotaCnj)).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("CNJ_CONSULTA_LIMITADA"))
                .andExpect(header().exists("Retry-After")).andReturn();
        assertThat(Long.parseLong(recusada.getResponse().getHeader("Retry-After"))).isBetween(40L, 45L);
        for (int i = 0; i < 3; i++) {
            executar(get(rotaCnj)).andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.codigo").value("CNJ_CONSULTA_AGUARDANDO"))
                    .andExpect(header().exists("Retry-After"));
        }
        assertThat(pedidos).hasSize(1);
        assertThat(output).contains("search_phase_execution_exception").doesNotContain("SEGREDO_CNJ");

        jdbc.update("update datajud_controle set proxima_consulta=0");
        respostas.add(new Resposta(200, "{\"hits\":{\"hits\":[{\"_source\":{\"numeroProcesso\":\"00008323520184013202\",\"tribunal\":\"TRF1\",\"grau\":\"G1\"}}]}}", null));
        executar(get(rotaCnj)).andExpect(status().isOk()).andExpect(jsonPath("$.registros[0].grau").value("G1"));
        // Mesmo sob uma nova pausa global, a resposta já obtida não precisa de HTTP externo.
        jdbc.update("update datajud_controle set proxima_consulta=?", System.currentTimeMillis() + 30000);
        executar(get(rotaCnj)).andExpect(status().isOk());
        assertThat(pedidos).hasSize(2);
        var consulta = mapper.readTree(pedidos.getFirst());
        assertThat(consulta.path("query").path("bool").path("filter").path("match").path("numeroProcesso").asText()).isEqualTo("00008323520184013202");
        assertThat(consulta.path("track_total_hits").asInt()).isEqualTo(100);
        assertThat(consulta.has("sort")).isFalse();
        assertThat(consulta.has("search_after")).isFalse();
        assertThat(estatisticas.findByProcessoIdOrderByIdAsc(processoId)).isEmpty();
    }

    @Test void esperaLocalDoCnjNaoEConfundidaComRecusaExterna() throws Exception {
        jdbc.update("update datajud_controle set ocupado_ate=?, reserva='em-andamento'", System.currentTimeMillis() + 10000);
        executar(get("/api/processo/consultar-datajud?numeroProcesso=00008323520184013202"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("CNJ_CONSULTA_AGUARDANDO"))
                .andExpect(header().string("Retry-After", "1"));
        assertThat(pedidos).isEmpty();
    }

    @Test void indisponibilidadeDoCnjTambemInformaPrazoSemAlegarLimite() throws Exception {
        respostas.add(new Resposta(503, "{}", "20"));
        executar(get("/api/processo/consultar-datajud?numeroProcesso=00008323520184013202"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.codigo").value("CNJ_CONSULTA_INDISPONIVEL"))
                .andExpect(header().string("Retry-After", "20"));
        assertThat(pedidos).hasSize(1);
    }
}
