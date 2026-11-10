package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.auth.CadastroUsuarioRequestDTO;
import br.com.axionweb.backend.dto.datajud.*;
import br.com.axionweb.backend.dto.processo.ProcessoRequestDTO;
import br.com.axionweb.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:sinteses;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "sintese@teste.com")
class SinteseDataJudIntegrationTests {
    private static final String TEXTO = "Na amostra de 2 processos, a mediana até o primeiro julgamento foi de 10,5 dias e até a primeira baixa de 20,5 dias. As medianas descrevem conjuntos de registros válidos que podem ser distintos. Em 0 processos, não foi identificada baixa definitiva. Os resultados não permitem prever prazos individuais.";
    @Autowired AnaliseService analises;
    @Autowired SinteseDataJudService sinteses;
    @Autowired ProcessoService processos;
    @Autowired UsuarioService usuarios;
    @Autowired DataJudRepository estatisticas;
    @Autowired ProcessoRepository processoRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired ComunicacaoRepository comunicacoes;
    @Autowired MockMvc mvc;
    @MockitoBean LLMService llm;
    @MockitoBean DataJudService datajud;
    private Long processoId;

    @BeforeEach
    void preparar() {
        comunicacoes.deleteAllInBatch(); estatisticas.deleteAllInBatch(); processoRepository.deleteAllInBatch(); usuarioRepository.deleteAllInBatch();
        usuarios.cadastrar(new CadastroUsuarioRequestDTO("Síntese", "sintese@teste.com", "senha123"));
        processoId = processos.cadastrar(new ProcessoRequestDTO("0000001", null, null, "TJPR", null,
                "G1", null, null, null, null, null, null, null, null, null, null)).getId();
        when(datajud.montarParametroDataJud(any())).thenCallRealMethod();
        when(datajud.buscarProcessos(any())).thenReturn(List.of(dado(11, 21), dado(12, 22)));
        when(llm.sintetizarEstatisticas(any())).thenReturn(TEXTO);
    }

    @Test
    void persisteExecucoesParametrosOriginaisESintesesSeparadas() {
        var primeira = gerar("2024-01-01", "2024-12-31");
        assertThat(primeira.executadaEm()).isNotNull();
        assertThat(primeira.parametros().dataInicio()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(primeira.quantidadeProcessos()).isEqualTo(2);
        assertThat(primeira.tempoAtePrimeiroJulgamento().getMedianaDias()).isEqualTo(10.5);
        assertThat(primeira.statusSintese()).isEqualTo("SEM_SINTESE");
        assertThat(sinteses.historico(processoId, 0).itens()).isEmpty();
        var salva = sinteses.gerar(processoId, primeira.id());
        var segunda = gerar("2025-01-01", null);
        assertThat(segunda.id()).isNotEqualTo(primeira.id());
        assertThat(segunda.sintese()).isNull();
        assertThat(sinteses.consultar(processoId, primeira.id()).sintese()).isEqualTo(salva);
        assertThat(sinteses.consultar(processoId, primeira.id()).parametros()).isEqualTo(primeira.parametros());
        assertThat(estatisticas.count()).isEqualTo(2);
        assertThat(sinteses.historico(processoId, 0).itens()).extracting(HistoricoSintesesDTO.Item::estatisticaId).containsExactly(primeira.id());
        verify(llm).sintetizarEstatisticas(argThat(dto -> dto.id().equals(primeira.id())));
    }

    @Test
    void regeneracaoAtualizaMesmaExecucaoEReordenaHistoricoPaginado() {
        Long primeira = null;
        for (int i = 0; i < 7; i++) {
            var execucao = gerar(null, null);
            if (primeira == null) primeira = execucao.id();
            sinteses.gerar(processoId, execucao.id());
        }
        var antiga = sinteses.consultar(processoId, primeira).sintese();
        when(llm.sintetizarEstatisticas(any())).thenReturn(TEXTO.replace("Na amostra", "Nesta amostra"));
        var regenerada = sinteses.gerar(processoId, primeira);
        assertThat(regenerada.sintese()).startsWith("Nesta amostra");
        assertThat(regenerada.geradaEm()).isAfterOrEqualTo(antiga.geradaEm());
        var pagina = sinteses.historico(processoId, 0);
        assertThat(pagina.itens()).hasSize(5);
        assertThat(pagina.itens().getFirst().estatisticaId()).isEqualTo(primeira);
        assertThat(pagina.itens()).extracting(HistoricoSintesesDTO.Item::geradaEm).isSortedAccordingTo(java.util.Comparator.reverseOrder());
        assertThat(pagina.temProxima()).isTrue();
        assertThat(sinteses.historico(processoId, 1).itens()).hasSize(2);
        assertThat(sinteses.historico(processoId, 1).temProxima()).isFalse();
        assertThat(estatisticas.count()).isEqualTo(7);
    }

    @Test
    void falhasPreservamSinteseDataEIndicadoresEPermitemNovaTentativa() {
        var execucao = gerar(null, null);
        var salva = sinteses.gerar(processoId, execucao.id());
        when(llm.sintetizarEstatisticas(any())).thenReturn(TEXTO.replace("10,5", "999"));
        assertThatThrownBy(() -> sinteses.gerar(processoId, execucao.id())).isInstanceOf(ResponseStatusException.class);
        assertThat(sinteses.consultar(processoId, execucao.id()).sintese()).isEqualTo(salva);
        when(llm.sintetizarEstatisticas(any())).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY));
        assertThatThrownBy(() -> sinteses.gerar(processoId, execucao.id())).isInstanceOf(ResponseStatusException.class);
        assertThat(sinteses.consultar(processoId, execucao.id()).sintese()).isEqualTo(salva);
        assertThat(sinteses.consultar(processoId, execucao.id()).tempoAtePrimeiroJulgamento().getMedianaDias()).isEqualTo(10.5);
        var semSintese = gerar(null, null);
        assertThatThrownBy(() -> sinteses.gerar(processoId, semSintese.id())).isInstanceOf(ResponseStatusException.class);
        assertThat(sinteses.consultar(processoId, semSintese.id()).statusSintese()).isEqualTo("SEM_SINTESE");
        assertThat(sinteses.historico(processoId, 0).itens()).hasSize(1);
        doReturn(TEXTO).when(llm).sintetizarEstatisticas(any());
        assertThat(sinteses.gerar(processoId, semSintese.id()).sintese()).isEqualTo(TEXTO);
    }

    @Test
    void endpointsAutorizamProcessoEExecucaoAntesDeExporHistorico() throws Exception {
        var execucao = gerar(null, null);
        sinteses.gerar(processoId, execucao.id());
        var outro = processos.cadastrar(new ProcessoRequestDTO("0000002", null, null, "TJPR", null,
                null, null, null, null, null, null, null, null, null, null, null));
        String base = "/api/dados/estatisticas/" + processoId;
        for (String rota : List.of(base + "/sinteses", base + "/" + execucao.id())) {
            mvc.perform(get(rota).with(user("sintese@teste.com"))).andExpect(status().isOk());
            mvc.perform(get(rota).with(anonymous())).andExpect(status().isUnauthorized());
            mvc.perform(get(rota).with(user("outro@teste.com"))).andExpect(status().isNotFound());
        }
        mvc.perform(get(base + "/sinteses?pagina=-1").with(user("sintese@teste.com"))).andExpect(status().isBadRequest());
        mvc.perform(get(base + "/sinteses").with(user("sintese@teste.com"))).andExpect(jsonPath("$.itens[0].estatisticaId").value(execucao.id()));
        mvc.perform(get(base + "/" + execucao.id()).with(user("sintese@teste.com"))).andExpect(jsonPath("$.sintese.sintese").value(TEXTO));
        mvc.perform(get("/api/dados/estatisticas/" + outro.getId() + "/" + execucao.id()).with(user("sintese@teste.com"))).andExpect(status().isNotFound());
        mvc.perform(post("/api/dados/estatisticas/" + outro.getId() + "/sintese-ia")
                .with(user("sintese@teste.com"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"estatisticaId\":" + execucao.id() + "}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void chamadaDaIaOcorreSemTransacaoEFalhaNaoRegistraAtividade() {
        var execucao = gerar(null, null);
        String atividadeAntes = processoRepository.findById(processoId).orElseThrow().getUltimaAtividadeTitulo();
        when(llm.sintetizarEstatisticas(any())).thenAnswer(chamada -> {
            assertThat(org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive())
                    .as("nenhuma transação ou conexão retida durante a chamada externa").isFalse();
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY);
        });
        assertThatThrownBy(() -> sinteses.gerar(processoId, execucao.id())).isInstanceOf(ResponseStatusException.class);
        assertThat(processoRepository.findById(processoId).orElseThrow().getUltimaAtividadeTitulo()).isEqualTo(atividadeAntes);
        assertThat(sinteses.consultar(processoId, execucao.id()).statusSintese()).isEqualTo("SEM_SINTESE");
        doReturn(TEXTO).when(llm).sintetizarEstatisticas(any());
        sinteses.gerar(processoId, execucao.id());
        assertThat(processoRepository.findById(processoId).orElseThrow().getUltimaAtividadeTitulo())
                .isEqualTo("Síntese das estatísticas DataJud concluída");
    }

    @Test
    void respostaAtrasadaNaoSobrescreveRegeneracaoJaSalva() throws Exception {
        var execucao = gerar(null, null);
        var iniciou = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        var chamadas = new java.util.concurrent.atomic.AtomicInteger();
        when(llm.sintetizarEstatisticas(any())).thenAnswer(invocacao -> {
            if (chamadas.getAndIncrement() == 0) {
                iniciou.countDown();
                assertThat(liberar.await(15, TimeUnit.SECONDS)).isTrue();
                return TEXTO;
            }
            return TEXTO.replace("Na amostra", "Nesta amostra");
        });
        var contexto = SecurityContextHolder.getContext();
        try (var executor = Executors.newSingleThreadExecutor()) {
            var atrasada = executor.submit(() -> {
                SecurityContextHolder.setContext(contexto);
                try { return sinteses.gerar(processoId, execucao.id()); }
                finally { SecurityContextHolder.clearContext(); }
            });
            try {
                assertThat(iniciou.await(10, TimeUnit.SECONDS)).isTrue();
                var salva = sinteses.gerar(processoId, execucao.id());
                liberar.countDown();
                assertThatThrownBy(() -> atrasada.get(10, TimeUnit.SECONDS))
                        .hasCauseInstanceOf(org.springframework.dao.OptimisticLockingFailureException.class);
                assertThat(sinteses.consultar(processoId, execucao.id()).sintese()).isEqualTo(salva);
            } finally { liberar.countDown(); }
        }
    }

    private DataJudResponseDTO gerar(String inicio, String fim) {
        return analises.gerarEstatisticas(processoId, new GerarEstatisticasRequestDTO(
                inicio == null ? null : LocalDate.parse(inicio), fim == null ? null : LocalDate.parse(fim))).getFirst();
    }

    private DadosDataJudDTO dado(int julgamento, int baixa) {
        var dado = new DadosDataJudDTO(); dado.setDataAjuizamento("20250101000000");
        var j = new MovimentoDataJudDTO(); j.setNome("Provimento"); j.setDataHora("2025-01-" + julgamento + "T00:00:00Z");
        var b = new MovimentoDataJudDTO(); b.setNome("Baixa Definitiva"); b.setDataHora("2025-01-" + baixa + "T00:00:00Z");
        dado.setMovimentos(List.of(j, b)); return dado;
    }
}
