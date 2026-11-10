package br.com.axionweb.backend.persistence;

import br.com.axionweb.backend.dto.auth.CadastroUsuarioRequestDTO;
import br.com.axionweb.backend.dto.auth.LoginRequestDTO;
import br.com.axionweb.backend.dto.processo.ProcessoRequestDTO;
import br.com.axionweb.backend.model.comunicacao.Comunicacao;
import br.com.axionweb.backend.model.comunicacao.PesquisaComunicacao;
import br.com.axionweb.backend.model.datajud.DataJud;
import br.com.axionweb.backend.model.datajud.SolicitacaoEstatistica;
import br.com.axionweb.backend.model.datajud.TempoEstatistica;
import br.com.axionweb.backend.model.processo.Assunto;
import br.com.axionweb.backend.model.processo.ClasseProcessual;
import br.com.axionweb.backend.model.processo.OrgaoJulgador;
import br.com.axionweb.backend.model.processo.Processo;
import br.com.axionweb.backend.model.usuario.TokenRevogado;
import br.com.axionweb.backend.repository.ComunicacaoRepository;
import br.com.axionweb.backend.repository.DataJudRepository;
import br.com.axionweb.backend.repository.PesquisaComunicacaoRepository;
import br.com.axionweb.backend.repository.ProcessoRepository;
import br.com.axionweb.backend.repository.TokenRevogadoRepository;
import br.com.axionweb.backend.repository.UsuarioRepository;
import br.com.axionweb.backend.security.CustomUserDetailsService;
import br.com.axionweb.backend.service.AuthService;
import br.com.axionweb.backend.service.ProcessoService;
import br.com.axionweb.backend.service.UsuarioService;
import jakarta.persistence.EntityManagerFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = "app.datajud.worker-enabled=false")
@ActiveProfiles("test")
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MySqlPersistenceIntegrationTests {

    private static final String EMAIL = "mysql-persistencia@prosint.test";
    private static final String SENHA = "Senha-segura-123";
    private static final String TEXTO_EXTENSO = "Decisão judicial — íntegra da comunicação ⚖️.\n".repeat(4_000);
    private static Long processoId;
    private static Long estatisticaId;
    private static Long pesquisaId;

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.4")
            .withDatabaseName("prosint_test")
            .withUsername("prosint")
            .withPassword("prosint_test_password");

    @DynamicPropertySource
    static void configurarMySql(DynamicPropertyRegistry propriedades) {
        propriedades.add("spring.datasource.url", MYSQL::getJdbcUrl);
        propriedades.add("spring.datasource.username", MYSQL::getUsername);
        propriedades.add("spring.datasource.password", MYSQL::getPassword);
        propriedades.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        propriedades.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.MySQLDialect");
        propriedades.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        propriedades.add("spring.jpa.properties.hibernate.jdbc.time_zone", () -> "UTC");
        propriedades.add("spring.flyway.enabled", () -> "true");
        propriedades.add("spring.flyway.locations", () -> "classpath:db/migration/mysql");
        propriedades.add("spring.flyway.clean-disabled", () -> "true");
        propriedades.add("app.datajud.api-key", () -> "chave-somente-de-teste");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired UsuarioService usuarios;
    @Autowired AuthService autenticacao;
    @Autowired CustomUserDetailsService detalhesUsuario;
    @Autowired ProcessoService processos;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired ProcessoRepository processoRepository;
    @Autowired ComunicacaoRepository comunicacaoRepository;
    @Autowired DataJudRepository dataJudRepository;
    @Autowired PesquisaComunicacaoRepository pesquisaRepository;
    @Autowired TokenRevogadoRepository tokenRepository;
    @Autowired EntityManagerFactory emf;
    @Autowired PlatformTransactionManager gerenciadorTransacoes;

    @AfterEach
    void limparContextoDeSeguranca() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @Order(1)
    void flywayCriaBaselineCompletoEHibernateValidaSchema() {
        Integer migracoes = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success = 1", Integer.class);
        Integer tabelas = jdbc.queryForObject("""
                select count(*) from information_schema.tables
                where table_schema = database() and table_name in (
                    'usuarios', 'processos', 'comunicacao', 'comunicacao_pesquisa',
                    'datajud_estatistica', 'datajud_controle', 'datajud_cache',
                    'datajud_solicitacao', 'token_revogado')
                """, Integer.class);

        assertThat(migracoes).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                select count(*) from flyway_schema_history
                where version = '1' and script = 'V1__baseline_prosint.sql'
                  and type = 'SQL' and success = 1 and checksum is not null
                """, Integer.class)).isEqualTo(1);
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(tabelas).isEqualTo(9);
    }

    @Test
    @Order(2)
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.AFTER_METHOD)
    void persisteFluxoCompletoComIdsSintesesEReferencias() {
        var usuario = usuarios.cadastrar(new CadastroUsuarioRequestDTO("Teste MySQL", EMAIL, SENHA));
        assertThat(usuario.id()).isNotNull();
        assertThat(autenticacao.login(new LoginRequestDTO(EMAIL, SENHA)).token()).isNotBlank();
        autenticar(EMAIL);

        Processo processo = processos.cadastrar(processo("Em andamento"));
        processoId = processo.getId();
        assertThat(processoId).isNotNull();

        Processo atualizado = processos.atualizar(processoId, processo("Concluido"));
        assertThat(atualizado.getStatus()).isEqualTo("Concluido");

        var estatistica = new DataJud();
        estatistica.setProcesso(atualizado);
        estatistica.setExecutadaEm(Instant.parse("2026-11-10T02:00:00Z"));
        estatistica.setTribunalConsulta("TJPR");
        estatistica.setClasseConsulta("7");
        estatistica.setGrauConsulta("G1");
        estatistica.setLimiteConsulta(400);
        estatistica.setDataInicio(LocalDate.of(2025, 1, 1));
        estatistica.setDataFim(LocalDate.of(2025, 12, 31));
        estatistica.setQuantidadeProcessos(12);
        estatistica.setProcessosPendentes(3);
        estatistica.setTempoAtePrimeiraBaixa(tempo(8, 35.5));
        estatistica.setTempoAtePrimeiroJulgamento(tempo(6, 20.0));
        estatistica.salvarSintese("Sintese DataJud validada sem chamada externa.", Instant.parse("2026-11-10T02:01:00Z"));
        estatisticaId = dataJudRepository.saveAndFlush(estatistica).getId();

        var comunicacao = new Comunicacao();
        comunicacao.setProcesso(atualizado);
        comunicacao.setCodigoClasse("7");
        comunicacao.setDataDisponibilizacao("2026-11-09");
        comunicacao.setIdOrgao(123);
        comunicacao.setSiglaTribunal("TJPR");
        assertThat(TEXTO_EXTENSO.getBytes(java.nio.charset.StandardCharsets.UTF_8).length).isGreaterThan(65_535);
        comunicacao.setTexto(TEXTO_EXTENSO);
        comunicacao.setTipoComunicacao("Intimacao");
        assertThat(comunicacaoRepository.saveAndFlush(comunicacao).getId()).isNotNull();

        var pesquisa = new PesquisaComunicacao();
        pesquisa.processo = atualizado;
        pesquisa.textoBusca = "contrato";
        pesquisa.tribunal = "TJPR";
        pesquisa.pesquisadaEm = Instant.parse("2026-11-10T02:02:00Z");
        pesquisa.quantidade = 1;
        pesquisa.comunicacaoIds = "[1]";
        pesquisa.publicacoes = "[{\"texto\":\"publicacao preservada\"}]";
        pesquisa.sintese = "Sintese Gemini persistida sem consumir a API externa.";
        pesquisa.evidencias = "[]";
        pesquisa.geradaEm = Instant.parse("2026-11-10T02:03:00Z");
        pesquisa.comunicacoesConsideradas = 1;
        pesquisa.duplicadasIgnoradas = 0;
        pesquisaId = pesquisaRepository.saveAndFlush(pesquisa).id;

        assertThat(estatisticaId).isNotNull();
        assertThat(pesquisaId).isNotNull();
        assertThat(tokenRepository.saveAndFlush(new TokenRevogado("a".repeat(64), Instant.now().plusSeconds(3600))))
                .isNotNull();

        new TransactionTemplate(gerenciadorTransacoes).executeWithoutResult(status -> {
            var em = emf.createEntityManager();
            try {
                var solicitacao = new SolicitacaoEstatistica();
                solicitacao.id = "00000000-0000-0000-0000-000000000001";
                solicitacao.processoId = processoId;
                solicitacao.usuario = EMAIL;
                solicitacao.chave = "b".repeat(64);
                solicitacao.parametros = "{}";
                solicitacao.status = "CONCLUIDA";
                solicitacao.criadaEm = System.currentTimeMillis();
                solicitacao.atualizadaEm = solicitacao.criadaEm;
                solicitacao.estatisticaId = estatisticaId;
                em.joinTransaction();
                em.persist(solicitacao);
            } finally {
                em.close();
            }
        });
    }

    @Test
    @Order(3)
    void dadosPermanecemAposReinicioDoContextoEValidacaoFlyway() {
        assertThat(comunicacaoRepository.findByProcessoIdOrderByIdAsc(processoId))
                .singleElement().extracting(Comunicacao::getTexto).isEqualTo(TEXTO_EXTENSO);
        assertThat(usuarioRepository.findByEmail(EMAIL)).isPresent();
        assertThat(processoRepository.findById(processoId)).isPresent();
        assertThat(dataJudRepository.findById(estatisticaId))
                .get().extracting(DataJud::getSintese)
                .isEqualTo("Sintese DataJud validada sem chamada externa.");
        assertThat(pesquisaRepository.findById(pesquisaId))
                .get().extracting(pesquisa -> pesquisa.sintese)
                .isEqualTo("Sintese Gemini persistida sem consumir a API externa.");
        assertThat(jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success = 1", Integer.class)).isEqualTo(1);
    }

    @Test
    @Order(4)
    void versionamentoOtimistaRejeitaAtualizacaoConcorrente() {
        var primeiro = emf.createEntityManager();
        var segundo = emf.createEntityManager();
        try {
            primeiro.getTransaction().begin();
            segundo.getTransaction().begin();
            DataJud a = primeiro.find(DataJud.class, estatisticaId);
            DataJud b = segundo.find(DataJud.class, estatisticaId);
            a.salvarSintese("Primeira atualizacao concorrente.", Instant.now());
            b.salvarSintese("Segunda atualizacao concorrente.", Instant.now());
            primeiro.getTransaction().commit();
            assertThrows(RuntimeException.class, () -> segundo.getTransaction().commit());
        } finally {
            if (primeiro.getTransaction().isActive()) primeiro.getTransaction().rollback();
            if (segundo.getTransaction().isActive()) segundo.getTransaction().rollback();
            primeiro.close();
            segundo.close();
        }
    }

    @Test
    @Order(5)
    void exclusaoRemoveDependenciasSemDeixarReferenciasOrfas() {
        autenticar(EMAIL);
        processos.excluir(processoId);

        assertThat(processoRepository.findById(processoId)).isEmpty();
        assertThat(dataJudRepository.findById(estatisticaId)).isEmpty();
        assertThat(comunicacaoRepository.findByProcessoIdOrderByIdAsc(processoId)).isEmpty();
        assertThat(pesquisaRepository.findById(pesquisaId)).isEmpty();
        assertThat(jdbc.queryForObject(
                "select count(*) from datajud_solicitacao where processo_id = ?", Integer.class, processoId))
                .isZero();
    }

    private void autenticar(String email) {
        var principal = detalhesUsuario.loadUserByUsername(email);
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
    }

    private ProcessoRequestDTO processo(String status) {
        var orgao = new OrgaoJulgador();
        orgao.setNome("1 Vara");
        orgao.setCodigo("123");
        var classe = new ClasseProcessual();
        classe.setNome("Procedimento Comum");
        classe.setCodigo("7");
        var assunto = new Assunto();
        assunto.setNome("Contratos");
        assunto.setCodigo("9580");
        return new ProcessoRequestDTO("0000001-00.2026.8.16.0001", "Cliente MySQL", "AUTOR",
                "TJPR", orgao, "G1", "Curitiba", "PR", classe, assunto, "Civel", status,
                new BigDecimal("1234.56"), "Magistrado", LocalDate.of(2026, 1, 2), null);
    }

    private TempoEstatistica tempo(int quantidade, double mediana) {
        var tempo = new TempoEstatistica();
        tempo.setQuantidade(quantidade);
        tempo.setMedianaDias(mediana);
        return tempo;
    }
}
