package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.auth.CadastroUsuarioRequestDTO;
import br.com.axionweb.backend.dto.processo.ProcessoRequestDTO;
import br.com.axionweb.backend.dto.comunicacao.DadosComunicacaoDTO;
import br.com.axionweb.backend.dto.datajud.DadosDataJudDTO;
import br.com.axionweb.backend.model.comunicacao.Comunicacao;
import br.com.axionweb.backend.model.datajud.DataJud;
import br.com.axionweb.backend.model.processo.Assunto;
import br.com.axionweb.backend.model.processo.Processo;
import br.com.axionweb.backend.repository.ComunicacaoRepository;
import br.com.axionweb.backend.repository.DataJudRepository;
import br.com.axionweb.backend.repository.ProcessoRepository;
import br.com.axionweb.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:analise-vinculo;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@WithMockUser(username = "analise@teste.com")
@ActiveProfiles("test")
class AnaliseProcessoIntegrationTests {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioService usuarioService;

    @Autowired
    private AnaliseService analiseService;

    @Autowired
    private ProcessoService processoService;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private DataJudRepository dataJudRepository;

    @Autowired
    private ComunicacaoRepository comunicacaoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private DataJudService dataJudService;

    @MockitoBean
    private ComunicacaoService comunicacaoService;

    @BeforeEach
    void preparar() {
        comunicacaoRepository.deleteAllInBatch();
        dataJudRepository.deleteAllInBatch();
        processoRepository.deleteAllInBatch();
        usuarioRepository.deleteAllInBatch();
        usuarioService.cadastrar(new CadastroUsuarioRequestDTO("Teste", "analise@teste.com", "senha123"));

        when(dataJudService.montarParametroDataJud(any())).thenCallRealMethod();
        when(comunicacaoService.montarParametroComunicacao(any())).thenCallRealMethod();
        when(dataJudService.buscarProcessos(any())).thenReturn(List.of());
        when(comunicacaoService.consumirApiComunicacao(any())).thenReturn(List.of());
    }

    @Test
    void deveSalvarEConsultarCadaAnalisePeloSeuProcesso() {
        Processo primeiro = cadastrarProcesso("0000001");
        when(dataJudService.buscarProcessos(any()))
                .thenReturn(List.of(dadosDataJud()), List.of(dadosDataJud(), dadosDataJud()));
        when(comunicacaoService.consumirApiComunicacao(any()))
                .thenReturn(List.of(comunicacao("Primeiro A"), comunicacao("Primeiro B")),
                        List.of(comunicacao("Segundo")));

        assertThat(analiseService.analisar(primeiro.getId())).isEqualTo("200");
        Processo segundo = cadastrarProcesso("0000002");
        analiseService.analisar(segundo.getId());
        Processo semAnalise = cadastrarProcesso("0000003");

        assertThat(dataJudRepository.findByProcessoIdOrderByIdAsc(primeiro.getId()))
                .singleElement().satisfies(estatistica -> {
                    assertThat(estatistica.getProcesso().getId()).isEqualTo(primeiro.getId());
                    assertThat(estatistica.getQuantidadeProcessos()).isEqualTo(1);
                    assertThat(estatistica.getProcessosPendentes()).isEqualTo(1);
                });
        assertThat(dataJudRepository.findByProcessoIdOrderByIdAsc(segundo.getId()))
                .singleElement().satisfies(estatistica -> {
                    assertThat(estatistica.getProcesso().getId()).isEqualTo(segundo.getId());
                    assertThat(estatistica.getQuantidadeProcessos()).isEqualTo(2);
                });
        assertThat(comunicacaoRepository.findByProcessoIdOrderByIdAsc(primeiro.getId()))
                .allSatisfy(item -> assertThat(item.getProcesso().getId()).isEqualTo(primeiro.getId()))
                .extracting(Comunicacao::getTexto).containsExactly("Primeiro A", "Primeiro B");
        assertThat(comunicacaoRepository.findByProcessoIdOrderByIdAsc(segundo.getId()))
                .singleElement().satisfies(item -> {
                    assertThat(item.getProcesso().getId()).isEqualTo(segundo.getId());
                    assertThat(item.getTexto()).isEqualTo("Segundo");
                });
        assertThat(dataJudRepository.findByProcessoIdOrderByIdAsc(semAnalise.getId())).isEmpty();
        assertThat(comunicacaoRepository.findByProcessoIdOrderByIdAsc(semAnalise.getId())).isEmpty();
        assertThat(dataJudRepository.count()).isEqualTo(2);
        assertThat(comunicacaoRepository.count()).isEqualTo(3);
    }

    @Test
    void novaAnaliseDeveSubstituirResultadosAnterioresEPreservarOutrosProcessos() {
        Processo primeiro = cadastrarProcesso("0000001");
        Processo segundo = cadastrarProcesso("0000002");
        when(dataJudService.buscarProcessos(any()))
                .thenReturn(List.of(dadosDataJud()), List.of(dadosDataJud(), dadosDataJud()), List.of());
        when(comunicacaoService.consumirApiComunicacao(any()))
                .thenReturn(List.of(comunicacao("Primeiro original")),
                        List.of(comunicacao("Segundo original")), List.of(comunicacao("Primeiro nova")));

        analiseService.analisar(primeiro.getId());
        analiseService.analisar(segundo.getId());
        Long idEstatisticaSegundo = dataJudRepository.findByProcessoIdOrderByIdAsc(segundo.getId()).getFirst().getId();
        Long idComunicacaoSegundo = comunicacaoRepository.findByProcessoIdOrderByIdAsc(segundo.getId()).getFirst().getId();
        analiseService.analisar(primeiro.getId());

        assertThat(dataJudRepository.findByProcessoIdOrderByIdAsc(primeiro.getId()))
                .extracting(DataJud::getQuantidadeProcessos).containsExactly(1, 0);
        assertThat(comunicacaoRepository.findByProcessoIdOrderByIdAsc(primeiro.getId()))
                .extracting(Comunicacao::getTexto).containsExactly("Primeiro nova");
        assertThat(dataJudRepository.findByProcessoIdOrderByIdAsc(segundo.getId()))
                .singleElement().satisfies(item -> {
                    assertThat(item.getId()).isEqualTo(idEstatisticaSegundo);
                    assertThat(item.getQuantidadeProcessos()).isEqualTo(2);
                });
        assertThat(comunicacaoRepository.findByProcessoIdOrderByIdAsc(segundo.getId()))
                .singleElement().satisfies(item -> {
                    assertThat(item.getId()).isEqualTo(idComunicacaoSegundo);
                    assertThat(item.getTexto()).isEqualTo("Segundo original");
                });
        assertThat(dataJudRepository.count()).isEqualTo(3);
        assertThat(comunicacaoRepository.count()).isEqualTo(2);
    }

    @Test
    void reanaliseSemResultadosDeveZerarEstatisticaERemoverComunicacoesAntigas() {
        Processo processo = cadastrarProcesso("0000001");
        when(dataJudService.buscarProcessos(any())).thenReturn(List.of(dadosDataJud())).thenReturn(List.of());
        when(comunicacaoService.consumirApiComunicacao(any()))
                .thenReturn(List.of(comunicacao("Anterior A"), comunicacao("Anterior B"))).thenReturn(List.of());
        analiseService.analisar(processo.getId());

        analiseService.analisar(processo.getId());

        assertThat(dataJudRepository.findByProcessoIdOrderByIdAsc(processo.getId()))
                .hasSize(2).last().satisfies(item -> {
                    assertThat(item.getQuantidadeProcessos()).isZero();
                    assertThat(item.getProcessosPendentes()).isZero();
                    assertThat(item.getTempoAtePrimeiraBaixa().getQuantidade()).isZero();
                });
        assertThat(comunicacaoRepository.findByProcessoIdOrderByIdAsc(processo.getId())).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"datajud", "comunicacao", "persistencia"})
    void falhaNaReanaliseDevePreservarResultadoAnteriorCompleto(String etapa) {
        Processo processo = cadastrarProcesso("0000001");
        when(dataJudService.buscarProcessos(any())).thenReturn(List.of(dadosDataJud()));
        when(comunicacaoService.consumirApiComunicacao(any()))
                .thenReturn(List.of(comunicacao("Anterior A"), comunicacao("Anterior B")));
        analiseService.analisar(processo.getId());
        Long estatisticaAnteriorId = dataJudRepository.findByProcessoIdOrderByIdAsc(processo.getId()).getFirst().getId();
        List<Long> comunicacoesAnterioresIds = comunicacaoRepository.findByProcessoIdOrderByIdAsc(processo.getId())
                .stream().map(Comunicacao::getId).toList();

        when(dataJudService.buscarProcessos(any())).thenReturn(List.of(dadosDataJud(), dadosDataJud()));
        Class<? extends RuntimeException> erroEsperado = IllegalStateException.class;
        if (etapa.equals("datajud")) {
            when(dataJudService.buscarProcessos(any())).thenThrow(new IllegalStateException("DataJud indisponivel"));
        } else if (etapa.equals("comunicacao")) {
            when(comunicacaoService.consumirApiComunicacao(any()))
                    .thenThrow(new IllegalStateException("Comunicacao indisponivel"));
        } else {
            DadosComunicacaoDTO invalida = comunicacao("Nova invalida");
            invalida.setCodigoClasse("X".repeat(256));
            when(comunicacaoService.consumirApiComunicacao(any()))
                    .thenReturn(List.of(comunicacao("Nova valida"), invalida));
            erroEsperado = DataIntegrityViolationException.class;
        }

        assertThatThrownBy(() -> analiseService.analisar(processo.getId())).isInstanceOf(erroEsperado);

        assertThat(dataJudRepository.findByProcessoIdOrderByIdAsc(processo.getId()))
                .singleElement().satisfies(item -> {
                    assertThat(item.getId()).isEqualTo(estatisticaAnteriorId);
                    assertThat(item.getQuantidadeProcessos()).isEqualTo(1);
                    assertThat(item.getProcessosPendentes()).isEqualTo(1);
                });
        assertThat(comunicacaoRepository.findByProcessoIdOrderByIdAsc(processo.getId()))
                .extracting(Comunicacao::getId).containsExactlyElementsOf(comunicacoesAnterioresIds);
        assertThat(comunicacaoRepository.findByProcessoIdOrderByIdAsc(processo.getId()))
                .extracting(Comunicacao::getTexto).containsExactly("Anterior A", "Anterior B");
    }

    @Test
    void analisesSimultaneasPreservamExecucoesSemMisturarResultados() throws Exception {
        Processo processo = cadastrarProcesso("0000001");
        CyclicBarrier consultasConcluidas = new CyclicBarrier(2);
        AtomicInteger execucoes = new AtomicInteger();
        ThreadLocal<Integer> execucaoAtual = new ThreadLocal<>();
        when(dataJudService.buscarProcessos(any())).thenAnswer(invocacao -> {
            int numero = execucoes.incrementAndGet();
            execucaoAtual.set(numero);
            return numero == 1 ? List.of(dadosDataJud()) : List.of(dadosDataJud(), dadosDataJud());
        });
        when(comunicacaoService.consumirApiComunicacao(any())).thenAnswer(invocacao -> {
            int numero = execucaoAtual.get();
            consultasConcluidas.await(10, TimeUnit.SECONDS);
            return List.of(comunicacao("Execucao " + numero));
        });
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        try (var executor = Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<String> analisar = () -> {
                var contexto = SecurityContextHolder.createEmptyContext();
                contexto.setAuthentication(authentication);
                SecurityContextHolder.setContext(contexto);
                try {
                    return analiseService.analisar(processo.getId());
                } finally {
                    SecurityContextHolder.clearContext();
                    execucaoAtual.remove();
                }
            };
            var primeira = executor.submit(analisar);
            var segunda = executor.submit(analisar);
            assertThat(primeira.get(20, TimeUnit.SECONDS)).isEqualTo("200");
            assertThat(segunda.get(20, TimeUnit.SECONDS)).isEqualTo("200");
        }

        List<DataJud> estatisticas = dataJudRepository.findByProcessoIdOrderByIdAsc(processo.getId());
        assertThat(estatisticas).hasSize(2);
        assertThat(estatisticas).extracting(DataJud::getQuantidadeProcessos).containsExactlyInAnyOrder(1, 2);
        assertThat(comunicacaoRepository.findByProcessoIdOrderByIdAsc(processo.getId()))
                .extracting(Comunicacao::getTexto)
                .containsExactly("Execucao " + estatisticas.getLast().getQuantidadeProcessos());
    }

    @Test
    void deveVincularEstatisticaMesmoQuandoApisNaoRetornamResultados() {
        Processo processo = cadastrarProcesso("0000001");

        analiseService.analisar(processo.getId());

        assertThat(dataJudRepository.findByProcessoIdOrderByIdAsc(processo.getId()))
                .singleElement().satisfies(item -> {
                    assertThat(item.getProcesso().getId()).isEqualTo(processo.getId());
                    assertThat(item.getQuantidadeProcessos()).isZero();
                });
        assertThat(comunicacaoRepository.findByProcessoIdOrderByIdAsc(processo.getId())).isEmpty();
    }

    @Test
    void falhaNaApiDeComunicacaoDeveDesfazerSomenteAnaliseAtual() {
        Processo primeiro = cadastrarProcesso("0000001");
        Processo segundo = cadastrarProcesso("0000002");
        when(comunicacaoService.consumirApiComunicacao(any()))
                .thenReturn(List.of(comunicacao("Analise concluida")))
                .thenThrow(new IllegalStateException("API indisponivel"));
        analiseService.analisar(primeiro.getId());

        assertThatThrownBy(() -> analiseService.analisar(segundo.getId()))
                .isInstanceOf(IllegalStateException.class).hasMessage("API indisponivel");

        assertThat(dataJudRepository.findByProcessoIdOrderByIdAsc(segundo.getId())).isEmpty();
        assertThat(comunicacaoRepository.findByProcessoIdOrderByIdAsc(segundo.getId())).isEmpty();
        assertThat(dataJudRepository.findByProcessoIdOrderByIdAsc(primeiro.getId())).hasSize(1);
        assertThat(comunicacaoRepository.findByProcessoIdOrderByIdAsc(primeiro.getId()))
                .extracting(Comunicacao::getTexto).containsExactly("Analise concluida");
    }

    @Test
    void falhaAoSalvarComunicacaoDeveDesfazerEstatisticaEOutrasComunicacoesDaAnalise() {
        Processo processo = cadastrarProcesso("0000001");
        DadosComunicacaoDTO invalida = comunicacao("Invalida");
        invalida.setCodigoClasse("X".repeat(256));
        when(comunicacaoService.consumirApiComunicacao(any()))
                .thenReturn(List.of(comunicacao("Valida"), invalida));

        assertThatThrownBy(() -> analiseService.analisar(processo.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(dataJudRepository.count()).isZero();
        assertThat(comunicacaoRepository.count()).isZero();
        assertThat(processoRepository.existsById(processo.getId())).isTrue();
    }

    @Test
    void processoInexistenteNaoDeveConsultarApisNemSalvarAnalise() {
        assertThatThrownBy(() -> analiseService.analisar(-1L))
                .isInstanceOf(RuntimeException.class).hasMessage("Processo não encontrado");

        verifyNoInteractions(dataJudService, comunicacaoService);
        assertThat(dataJudRepository.count()).isZero();
        assertThat(comunicacaoRepository.count()).isZero();
    }

    @Test
    void naoDevePermitirRegistrosSemProcesso() {
        assertThatThrownBy(() -> dataJudRepository.saveAndFlush(new DataJud()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> comunicacaoRepository.saveAndFlush(new Comunicacao()))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(dataJudRepository.count()).isZero();
        assertThat(comunicacaoRepository.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"datajud_estatistica", "comunicacao"})
    void bancoDeveRejeitarVinculoComProcessoInexistente(String tabela) {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into " + tabela + " (id, processo_id) values (?, ?)", -1L, -1L))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void excluirProcessoDeveRemoverApenasSeusDadosDeAnalise() {
        Processo primeiro = cadastrarProcesso("0000001");
        Processo segundo = cadastrarProcesso("0000002");
        when(comunicacaoService.consumirApiComunicacao(any()))
                .thenReturn(List.of(comunicacao("Primeiro")), List.of(comunicacao("Segundo")));
        analiseService.analisar(primeiro.getId());
        analiseService.analisar(segundo.getId());

        processoService.excluir(primeiro.getId());

        assertThat(processoRepository.existsById(primeiro.getId())).isFalse();
        assertThat(dataJudRepository.findByProcessoIdOrderByIdAsc(primeiro.getId())).isEmpty();
        assertThat(comunicacaoRepository.findByProcessoIdOrderByIdAsc(primeiro.getId())).isEmpty();
        assertThat(processoRepository.existsById(segundo.getId())).isTrue();
        assertThat(dataJudRepository.findByProcessoIdOrderByIdAsc(segundo.getId())).hasSize(1);
        assertThat(comunicacaoRepository.findByProcessoIdOrderByIdAsc(segundo.getId()))
                .extracting(Comunicacao::getTexto).containsExactly("Segundo");
    }

    private Processo cadastrarProcesso(String numero) {
        Assunto assunto = new Assunto();
        assunto.setNome("Mesmo assunto");
        return processoService.cadastrar(new ProcessoRequestDTO(numero, null, null, "TJSP", null,
                null, null, null, null, assunto, null, null, null, null, null, null));
    }

    private DadosDataJudDTO dadosDataJud() {
        DadosDataJudDTO dados = new DadosDataJudDTO();
        dados.setDataAjuizamento("20250101000000");
        dados.setDataHoraUltimaAtualizacao("2025-01-11T00:00:00Z");
        dados.setMovimentos(List.of());
        return dados;
    }

    private DadosComunicacaoDTO comunicacao(String texto) {
        DadosComunicacaoDTO dados = new DadosComunicacaoDTO();
        dados.setTexto(texto);
        dados.setSiglaTribunal("TJSP");
        return dados;
    }
}
