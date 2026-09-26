package br.com.axionweb.backend.security;

import br.com.axionweb.backend.dto.comunicacao.DadosComunicacaoDTO;
import br.com.axionweb.backend.model.comunicacao.Comunicacao;
import br.com.axionweb.backend.model.datajud.DataJud;
import br.com.axionweb.backend.model.datajud.TempoEstatistica;
import br.com.axionweb.backend.model.processo.Processo;
import br.com.axionweb.backend.model.usuario.Role;
import br.com.axionweb.backend.model.usuario.TokenRevogado;
import br.com.axionweb.backend.model.usuario.Usuario;
import br.com.axionweb.backend.repository.*;
import br.com.axionweb.backend.service.ComunicacaoService;
import br.com.axionweb.backend.service.DataJudService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:auth-isolamento;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AutenticacaoIsolamentoIntegrationTests {
    private static final String SENHA = "senha123";
    private static final String CHAVE_TESTE = "c29tZS1yYW5kb20tdGVzdC1vbmx5LWtleS0zMi1ieXRlcyE=";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper mapper;
    @Autowired private UsuarioRepository usuarios;
    @Autowired private ProcessoRepository processos;
    @Autowired private DataJudRepository estatisticas;
    @Autowired private ComunicacaoRepository comunicacoes;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private JwtService jwtService;
    @Autowired private TokenRevogadoRepository tokensRevogados;
    @MockitoBean private DataJudService dataJudService;
    @MockitoBean private ComunicacaoService comunicacaoService;

    @BeforeEach
    void prepararBanco() {
        tokensRevogados.deleteAllInBatch();
        comunicacoes.deleteAllInBatch();
        estatisticas.deleteAllInBatch();
        processos.deleteAllInBatch();
        usuarios.deleteAllInBatch();
        when(dataJudService.montarParametroDataJud(any())).thenCallRealMethod();
        when(dataJudService.buscarProcessos(any())).thenReturn(List.of());
        when(comunicacaoService.montarParametroComunicacao(any())).thenCallRealMethod();
        DadosComunicacaoDTO comunicacao = new DadosComunicacaoDTO();
        comunicacao.setTexto("Publicação usada na análise");
        when(comunicacaoService.consumirApiComunicacao(any())).thenReturn(List.of(comunicacao));
    }

    @Test
    void cadastroDeveNormalizarEmailProtegerSenhaEIgnorarRoleEnviada() throws Exception {
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(json(Map.of(
                        "nome", "  João Victor  ", "email", "  JOAO@EMAIL.COM  ", "senha", SENHA,
                        "role", "ADMIN", "id", 999))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome").value("João Victor"))
                .andExpect(jsonPath("$.email").value("joao@email.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.senha").doesNotExist());
        Usuario usuario = usuarios.findByEmail("joao@email.com").orElseThrow();
        assertThat(usuario.getSenha()).startsWith("{bcrypt}").isNotEqualTo(SENHA);
        assertThat(passwordEncoder.matches(SENHA, usuario.getSenha())).isTrue();
        assertThat(usuario.getRole()).isEqualTo(Role.USER);
        assertThat(mapper.readTree(mapper.writeValueAsString(usuario)).has("senha")).isFalse();
    }

    @Test
    void cadastroDuplicadoDeveRetornar409InclusiveVariacaoDeMaiusculas() throws Exception {
        criarConta("joao@email.com");
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nome", "Outro", "email", " JOAO@EMAIL.COM ", "senha", SENHA))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.trace").doesNotExist());
        assertThat(usuarios.count()).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "{\"nome\":\" \"}",
            "{\"nome\":\"Joao\",\"email\":\"invalido\",\"senha\":\"senha123\"}",
            "{\"nome\":\"Joao\",\"email\":\"joao@email.com\",\"senha\":\"123\"}",
            "{\"nome\":\"Joao\",\"email\":\"joao@email.com\",\"senha\":\"      \"}"
    })
    void cadastroInvalidoDeveRetornar400(String corpo) throws Exception {
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos").isNotEmpty())
                .andExpect(jsonPath("$.trace").doesNotExist());
        assertThat(usuarios.count()).isZero();
    }

    @Test
    void senhaUnicodeAcimaDoLimiteBCryptDeveRetornar400() throws Exception {
        mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nome", "Joao", "email", "joao@email.com", "senha", "á".repeat(40)))))
                .andExpect(status().isBadRequest());
        assertThat(usuarios.count()).isZero();
    }

    @Test
    void loginEMeDevemRetornarSomenteDadosPublicosSemCriarSessao() throws Exception {
        Conta conta = criarConta("joao@email.com");
        var jwt = jwtService.validarToken(conta.token());
        assertThat(jwt.getSubject()).isEqualTo(conta.email());
        assertThat(jwt.getExpiresAt()).isAfter(Instant.now());
        assertThat(jwt.getClaims()).doesNotContainKeys("senha", "password");
        MvcResult resposta = mvc.perform(get("/api/auth/me").header("Authorization", conta.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(conta.id()))
                .andExpect(jsonPath("$.email").value(conta.email()))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(header().doesNotExist("Set-Cookie")).andReturn();
        assertThat(resposta.getRequest().getSession(false)).isNull();
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutDeveRevogarTokenEBloquearLeituraEAlteracaoSemApagarDados() throws Exception {
        Conta conta = criarConta("joao@email.com");
        long processoId = criarProcesso(conta, "PROCESSO-LOGOUT");
        salvarResultados(processoId, 5, "Resultado salvo");
        Instant expiracao = jwtService.validarToken(conta.token()).getExpiresAt();

        MvcResult resposta = mvc.perform(post("/api/auth/logout").header("Authorization", conta.bearer()))
                .andExpect(status().isNoContent()).andExpect(content().string(""))
                .andExpect(header().doesNotExist("Set-Cookie")).andReturn();
        assertThat(resposta.getRequest().getSession(false)).isNull();
        assertThat(tokensRevogados.findAll()).singleElement().satisfies(token -> {
            assertThat(token.getTokenHash()).matches("[0-9a-f]{64}").isNotEqualTo(conta.token());
            assertThat(token.getExpiraEm()).isEqualTo(expiracao);
        });

        for (String endpoint : List.of("/api/auth/me", "/api/processo/listar-usuario",
                "/api/dados/estatisticas/" + processoId, "/api/dados/comunicacoes/" + processoId)) {
            mvc.perform(get(endpoint).header("Authorization", conta.bearer()))
                    .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401));
        }
        mvc.perform(post("/api/dados/analisar/{id}", processoId).header("Authorization", conta.bearer()))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/processo/deletar/{id}", processoId).header("Authorization", conta.bearer()))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout").header("Authorization", conta.bearer()))
                .andExpect(status().isUnauthorized());
        assertThat(usuarios.count()).isEqualTo(1);
        assertThat(processos.count()).isEqualTo(1);
        assertThat(estatisticas.count()).isEqualTo(1);
        assertThat(comunicacoes.count()).isEqualTo(1);
        verifyNoInteractions(dataJudService, comunicacaoService);
    }

    @Test
    void logoutDevePreservarOutrosLoginsEPermitirEntrarNovamente() throws Exception {
        Conta joao = criarConta("joao@email.com");
        Conta maria = criarConta("maria@email.com");
        String outroLogin = body(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", joao.email(), "senha", SENHA))))
                .andExpect(status().isOk()).andReturn()).get("token").asText();
        assertThat(outroLogin).isNotEqualTo(joao.token());
        assertThat(jwtService.validarToken(outroLogin).getId()).isNotBlank()
                .isNotEqualTo(jwtService.validarToken(joao.token()).getId());

        mvc.perform(post("/api/auth/logout").header("Authorization", joao.bearer()))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + outroLogin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(joao.id()));
        mvc.perform(get("/api/auth/me").header("Authorization", maria.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(maria.id()));

        String novoToken = body(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", joao.email(), "senha", SENHA))))
                .andExpect(status().isOk()).andReturn()).get("token").asText();
        assertThat(novoToken).isNotEqualTo(joao.token()).isNotEqualTo(outroLogin);
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + novoToken))
                .andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization", joao.bearer()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutSemTokenDeveRetornar401() throws Exception {
        mvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
        assertThat(tokensRevogados.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer invalido", "Bearer ", "Basic dXN1YXJpbzpzZW5oYQ=="})
    void logoutComCredencialInvalidaDeveRetornar401(String authorization) throws Exception {
        mvc.perform(post("/api/auth/logout").header("Authorization", authorization))
                .andExpect(status().isUnauthorized());
        assertThat(tokensRevogados.count()).isZero();
    }

    @Test
    void logoutPorGetNaoDeveRevogarToken() throws Exception {
        Conta conta = criarConta("joao@email.com");
        mvc.perform(get("/api/auth/logout").header("Authorization", conta.bearer()))
                .andExpect(status().isMethodNotAllowed());
        mvc.perform(get("/api/auth/me").header("Authorization", conta.bearer()))
                .andExpect(status().isOk());
        assertThat(tokensRevogados.count()).isZero();
    }

    @Test
    void logoutDeveRemoverSomenteRevogacoesExpiradas() throws Exception {
        Conta conta = criarConta("joao@email.com");
        tokensRevogados.saveAndFlush(new TokenRevogado("a".repeat(64), Instant.now().minusSeconds(60)));
        tokensRevogados.saveAndFlush(new TokenRevogado("b".repeat(64), Instant.now().plusSeconds(600)));

        mvc.perform(post("/api/auth/logout").header("Authorization", conta.bearer()))
                .andExpect(status().isNoContent());
        assertThat(tokensRevogados.existsByTokenHash("a".repeat(64))).isFalse();
        assertThat(tokensRevogados.existsByTokenHash("b".repeat(64))).isTrue();
        assertThat(tokensRevogados.count()).isEqualTo(2);
    }

    @Test
    void logoutDeveRevogarTokenAnteriorSemJtiInclusiveAssinaturaComPadding() throws Exception {
        Conta conta = criarConta("joao@email.com");
        var encoder = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(
                Base64.getDecoder().decode(CHAVE_TESTE), "HmacSHA256")).build();
        var claims = JwtClaimsSet.builder().issuer("gestao-processos").subject(conta.email())
                .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(600)).build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token + "="))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/logout").header("Authorization", "bearer " + token + " "))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token + "="))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginInvalidoDeveRetornar401SemDistinguirUsuarioInexistente() throws Exception {
        criarConta("joao@email.com");
        JsonNode senhaErrada = body(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "joao@email.com", "senha", "incorreta"))))
                .andExpect(status().isUnauthorized()).andReturn());
        JsonNode inexistente = body(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "ninguem@email.com", "senha", SENHA))))
                .andExpect(status().isUnauthorized()).andReturn());
        assertThat(senhaErrada).isEqualTo(inexistente);
        assertThat(senhaErrada.has("trace")).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"/api/auth/me", "/api/processo/listar-usuario", "/api/processo/1",
            "/api/dados/analisar/1", "/api/dados/analise/1", "/api/dados/estatisticas/1",
            "/api/dados/comunicacoes/1", "/estatisticas/calcular", "/h2/"})
    void endpointsPrivadosDevemExigirAutenticacao(String endpoint) throws Exception {
        mvc.perform(get(endpoint)).andExpect(status().isUnauthorized());
        verifyNoInteractions(dataJudService, comunicacaoService);
    }

    @Test
    void cadastroDeProcessoSemTokenDeveRetornar401() throws Exception {
        mvc.perform(post("/api/processo/cadastrar").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        assertThat(processos.count()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer invalido", "Bearer ", "Basic dXN1YXJpbzpzZW5oYQ=="})
    void credencialInvalidaDeveRetornar401(String authorization) throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", authorization))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"expirado", "assinatura", "emissor", "sem-expiracao", "sem-subject", "futuro"})
    void jwtInvalidoDeveSerRejeitado(String tipo) throws Exception {
        Conta conta = criarConta("joao@email.com");
        var claims = JwtClaimsSet.builder().issuer("gestao-processos").subject(conta.email())
                .issuedAt(Instant.now().minusSeconds(120));
        if (!tipo.equals("sem-expiracao")) {
            claims.expiresAt(tipo.equals("expirado") ? Instant.now().minusSeconds(1) : Instant.now().plusSeconds(600));
        }
        if (tipo.equals("emissor")) claims.issuer("outro-sistema");
        if (tipo.equals("sem-subject")) claims.subject("");
        if (tipo.equals("futuro")) claims.notBefore(Instant.now().plusSeconds(600));
        byte[] chave = tipo.equals("assinatura") ? new byte[32] : Base64.getDecoder().decode(CHAVE_TESTE);
        var encoder = NimbusJwtEncoder.withSecretKey(new SecretKeySpec(chave, "HmacSHA256")).build();
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
        assertThat(tokensRevogados.count()).isZero();
        verifyNoInteractions(dataJudService, comunicacaoService);
    }

    @Test
    void tokenDeUsuarioRemovidoDeveSerRejeitado() throws Exception {
        Conta conta = criarConta("joao@email.com");
        usuarios.deleteById(conta.id());
        mvc.perform(get("/api/auth/me").header("Authorization", conta.bearer()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cadastroDeProcessoDeveIgnorarIdsEProprietarioDoJson() throws Exception {
        Conta joao = criarConta("joao@email.com");
        Conta maria = criarConta("maria@email.com");
        long processoMaria = criarProcesso(maria, "MARIA");
        Map<String, Object> corpo = new LinkedHashMap<>(dadosProcesso("JOAO"));
        corpo.put("id", processoMaria);
        corpo.put("usuarioId", maria.id());
        corpo.put("usuario", Map.of("id", maria.id(), "email", maria.email(), "role", "ADMIN"));

        JsonNode criado = body(mvc.perform(post("/api/processo/cadastrar").header("Authorization", joao.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(corpo)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.usuario").doesNotExist())
                .andExpect(jsonPath("$.senha").doesNotExist()).andReturn());
        long id = criado.get("id").asLong();
        assertThat(id).isNotEqualTo(processoMaria);
        assertThat(processos.findById(id).orElseThrow().getUsuario().getId()).isEqualTo(joao.id());
        assertThat(processos.findById(processoMaria).orElseThrow().getNumeroProcesso()).isEqualTo("MARIA");
    }

    @Test
    void listagemEBuscaPorNumeroDevemSerIsoladasMesmoComNumeroIgual() throws Exception {
        Conta joao = criarConta("joao@email.com");
        Conta maria = criarConta("maria@email.com");
        long idJoao = criarProcesso(joao, "MESMO-NUMERO");
        long idMaria = criarProcesso(maria, "MESMO-NUMERO");
        criarProcesso(maria, "EXCLUSIVO-MARIA");

        mvc.perform(get("/api/processo/listar-usuario").header("Authorization", joao.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(idJoao));
        mvc.perform(get("/api/processo/listar-usuario").header("Authorization", maria.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/processo/buscar-num-processo").param("numeroProcesso", "MESMO-NUMERO")
                        .header("Authorization", joao.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(idJoao));
        mvc.perform(get("/api/processo/buscar-num-processo").param("numeroProcesso", "MESMO-NUMERO")
                        .header("Authorization", maria.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(idMaria));
        mvc.perform(get("/api/processo/buscar-num-processo").param("numeroProcesso", "EXCLUSIVO-MARIA")
                        .header("Authorization", joao.bearer())).andExpect(status().isNotFound());
    }

    @Test
    void outroUsuarioNaoPodeLerEditarExcluirNemSolicitarAnalise() throws Exception {
        Conta joao = criarConta("joao@email.com");
        Conta maria = criarConta("maria@email.com");
        long idMaria = criarProcesso(maria, "MARIA");
        JsonNode desconhecido = body(mvc.perform(get("/api/processo/-1").header("Authorization", joao.bearer()))
                .andExpect(status().isNotFound()).andReturn());
        JsonNode proibido = body(mvc.perform(get("/api/processo/{id}", idMaria).header("Authorization", joao.bearer()))
                .andExpect(status().isNotFound()).andReturn());
        assertThat(proibido).isEqualTo(desconhecido);
        mvc.perform(put("/api/processo/atualizar/{id}", idMaria).header("Authorization", joao.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(dadosProcesso("ALTERADO"))))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/processo/deletar/{id}", idMaria).header("Authorization", joao.bearer()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/dados/analisar/{id}", idMaria).header("Authorization", joao.bearer()))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/dados/analisar/{id}", idMaria).header("Authorization", joao.bearer()))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/dados/analise/{id}", idMaria).header("Authorization", joao.bearer()))
                .andExpect(status().isNotFound());
        verifyNoInteractions(dataJudService, comunicacaoService);
        assertThat(processos.findById(idMaria).orElseThrow().getNumeroProcesso()).isEqualTo("MARIA");
        assertThat(estatisticas.count()).isZero();
        assertThat(comunicacoes.count()).isZero();
    }

    @Test
    void proprietarioPodeEditarSemTransferirPropriedade() throws Exception {
        Conta joao = criarConta("joao@email.com");
        Conta maria = criarConta("maria@email.com");
        long id = criarProcesso(joao, "ORIGINAL");
        Map<String, Object> corpo = new LinkedHashMap<>(dadosProcesso("ATUALIZADO"));
        corpo.put("usuarioId", maria.id());
        corpo.put("usuario", Map.of("id", maria.id()));
        corpo.put("grau", "G2");
        corpo.put("uf", "RJ");
        corpo.put("dataFim", "2026-09-25");
        mvc.perform(put("/api/processo/atualizar/{id}", id).header("Authorization", joao.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(corpo)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.numeroProcesso").value("ATUALIZADO"))
                .andExpect(jsonPath("$.grau").value("G2")).andExpect(jsonPath("$.uf").value("RJ"))
                .andExpect(jsonPath("$.dataFim").value("2026-09-25"));
        assertThat(processos.findById(id).orElseThrow().getUsuario().getId()).isEqualTo(joao.id());
        mvc.perform(get("/api/processo/{id}", id).header("Authorization", joao.bearer())).andExpect(status().isOk());
    }

    @Test
    void resultadosDevemContinuarVinculadosAoProcessoEIsoladosPorUsuario() throws Exception {
        Conta joao = criarConta("joao@email.com");
        Conta maria = criarConta("maria@email.com");
        long idJoao = criarProcesso(joao, "JOAO");
        long idMaria = criarProcesso(maria, "MARIA");
        mvc.perform(post("/api/dados/analisar/{id}", idJoao).header("Authorization", joao.bearer()))
                .andExpect(status().isOk());
        mvc.perform(get("/api/dados/analisar/{id}", idMaria).header("Authorization", maria.bearer()))
                .andExpect(status().isOk());
        clearInvocations(dataJudService, comunicacaoService);

        mvc.perform(get("/api/dados/analise/{id}", idJoao).header("Authorization", joao.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.processoId").value(idJoao))
                .andExpect(jsonPath("$.estatisticas", hasSize(1)))
                .andExpect(jsonPath("$.estatisticas[0].processoId").value(idJoao))
                .andExpect(jsonPath("$.comunicacoes[0].processoId").value(idJoao))
                .andExpect(jsonPath("$.comunicacoes[0].texto").value("Publicação usada na análise"))
                .andExpect(jsonPath("$..senha").isEmpty()).andExpect(jsonPath("$..usuario").isEmpty());
        mvc.perform(get("/api/dados/analise/{id}", idMaria).header("Authorization", joao.bearer()))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/processo/deletar/{id}", idMaria).header("Authorization", joao.bearer()))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/processo/deletar/{id}", idJoao).header("Authorization", joao.bearer()))
                .andExpect(status().isOk());
        verifyNoInteractions(dataJudService, comunicacaoService);
        assertThat(estatisticas.findByProcessoIdOrderByIdAsc(idJoao)).isEmpty();
        assertThat(comunicacoes.findByProcessoIdOrderByIdAsc(idJoao)).isEmpty();
        assertThat(estatisticas.findByProcessoIdOrderByIdAsc(idMaria)).hasSize(1);
        assertThat(comunicacoes.findByProcessoIdOrderByIdAsc(idMaria)).hasSize(1);
    }

    @Test
    void roleAdminTambemDeveRespeitarPropriedadeDosProcessos() throws Exception {
        Conta admin = criarConta("admin@email.com");
        Usuario usuario = usuarios.findById(admin.id()).orElseThrow();
        usuario.setRole(Role.ADMIN);
        usuarios.saveAndFlush(usuario);
        Conta maria = criarConta("maria@email.com");
        long idMaria = criarProcesso(maria, "MARIA");
        mvc.perform(get("/api/auth/me").header("Authorization", admin.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.role").value("ADMIN"));
        mvc.perform(get("/api/processo/{id}", idMaria).header("Authorization", admin.bearer()))
                .andExpect(status().isNotFound());
    }

    @Test
    void bancoDeveExigirProprietarioExistenteEEmailUnico() throws Exception {
        assertThatThrownBy(() -> processos.saveAndFlush(new Processo()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update("insert into processos (id, usuario_id) values (-1, -1)"))
                .isInstanceOf(DataIntegrityViolationException.class);
        criarConta("joao@email.com");
        assertThatThrownBy(() -> jdbcTemplate.update(
                "insert into usuarios (id, nome, email, senha, role) values (-1, 'Outro', 'joao@email.com', 'hash', 'USER')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"estatisticas", "comunicacoes"})
    void consultasSeparadasDevemRetornarSomenteDadosSalvosDoProcesso(String recurso) throws Exception {
        Conta joao = criarConta("joao@email.com");
        Conta maria = criarConta("maria@email.com");
        long idJoao = criarProcesso(joao, "JOAO");
        long outroIdJoao = criarProcesso(joao, "OUTRO-JOAO");
        long idMaria = criarProcesso(maria, "MARIA");
        salvarResultados(idJoao, 5, "Primeira comunicação");
        salvarResultados(idJoao, 9, "Segunda comunicação");
        salvarResultados(outroIdJoao, 12, "Outro processo do João");
        salvarResultados(idMaria, 20, "Processo da Maria");

        JsonNode resultado = body(mvc.perform(get("/api/dados/" + recurso + "/{id}", idJoao)
                        .header("Authorization", joao.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].processoId").value(idJoao))
                .andExpect(jsonPath("$[1].processoId").value(idJoao))
                .andExpect(jsonPath("$..senha").isEmpty())
                .andExpect(jsonPath("$..usuario").isEmpty())
                .andExpect(jsonPath("$..processo").isEmpty()).andReturn());
        assertThat(resultado.get(0).get("id").asLong()).isLessThan(resultado.get(1).get("id").asLong());
        if (recurso.equals("estatisticas")) {
            assertThat(resultado.get(0).get("quantidadeProcessos").asInt()).isEqualTo(5);
            assertThat(resultado.get(1).get("quantidadeProcessos").asInt()).isEqualTo(9);
            assertThat(resultado.get(0).get("tempoPendente").get("mediaDias").asDouble()).isEqualTo(15.0);
            assertThat(resultado.get(0).has("texto")).isFalse();
        } else {
            assertThat(resultado.get(0).get("texto").asText()).isEqualTo("Primeira comunicação");
            assertThat(resultado.get(1).get("texto").asText()).isEqualTo("Segunda comunicação");
            assertThat(resultado.get(0).has("quantidadeProcessos")).isFalse();
        }
        assertThat(estatisticas.count()).isEqualTo(4);
        assertThat(comunicacoes.count()).isEqualTo(4);
        verifyNoInteractions(dataJudService, comunicacaoService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"estatisticas", "comunicacoes"})
    void consultasSeparadasDevemRetornarListaVaziaParaProcessoSemResultados(String recurso) throws Exception {
        Conta joao = criarConta("joao@email.com");
        long id = criarProcesso(joao, "SEM-ANALISE");
        mvc.perform(get("/api/dados/" + recurso + "/{id}", id).header("Authorization", joao.bearer()))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        assertThat(estatisticas.count()).isZero();
        assertThat(comunicacoes.count()).isZero();
        verifyNoInteractions(dataJudService, comunicacaoService);
    }

    @ParameterizedTest
    @ValueSource(strings = {"estatisticas", "comunicacoes"})
    void consultasSeparadasDevemOcultarDadosDeOutroUsuario(String recurso) throws Exception {
        Conta joao = criarConta("joao@email.com");
        Conta maria = criarConta("maria@email.com");
        long idMaria = criarProcesso(maria, "MARIA");
        salvarResultados(idMaria, 10, "Comunicação da Maria");

        JsonNode proibido = body(mvc.perform(get("/api/dados/" + recurso + "/{id}", idMaria)
                        .header("Authorization", joao.bearer()))
                .andExpect(status().isNotFound()).andReturn());
        JsonNode inexistente = body(mvc.perform(get("/api/dados/" + recurso + "/-1")
                        .header("Authorization", joao.bearer()))
                .andExpect(status().isNotFound()).andReturn());
        assertThat(proibido).isEqualTo(inexistente);
        assertThat(estatisticas.count()).isEqualTo(1);
        assertThat(comunicacoes.count()).isEqualTo(1);
        verifyNoInteractions(dataJudService, comunicacaoService);
    }

    @Test
    void reanaliseDeveSubstituirHistoricoNasConsultasSemAfetarOutroUsuario() throws Exception {
        Conta joao = criarConta("joao@email.com");
        Conta maria = criarConta("maria@email.com");
        long idJoao = criarProcesso(joao, "JOAO");
        long idMaria = criarProcesso(maria, "MARIA");
        salvarResultados(idJoao, 5, "Resultado antigo A");
        salvarResultados(idJoao, 9, "Resultado antigo B");
        salvarResultados(idMaria, 20, "Resultado da Maria");

        mvc.perform(post("/api/dados/analisar/{id}", idJoao).header("Authorization", joao.bearer()))
                .andExpect(status().isOk());
        clearInvocations(dataJudService, comunicacaoService);

        mvc.perform(get("/api/dados/estatisticas/{id}", idJoao).header("Authorization", joao.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].processoId").value(idJoao))
                .andExpect(jsonPath("$[0].quantidadeProcessos").value(0));
        mvc.perform(get("/api/dados/comunicacoes/{id}", idJoao).header("Authorization", joao.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].texto").value("Publicação usada na análise"));
        mvc.perform(get("/api/dados/analise/{id}", idJoao).header("Authorization", joao.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estatisticas", hasSize(1)))
                .andExpect(jsonPath("$.comunicacoes", hasSize(1)));
        mvc.perform(get("/api/dados/analise/{id}", idMaria).header("Authorization", maria.bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estatisticas[0].quantidadeProcessos").value(20))
                .andExpect(jsonPath("$.comunicacoes[0].texto").value("Resultado da Maria"));
        assertThat(estatisticas.count()).isEqualTo(2);
        assertThat(comunicacoes.count()).isEqualTo(2);
        verifyNoInteractions(dataJudService, comunicacaoService);
    }

    private void salvarResultados(long processoId, int quantidade, String texto) {
        Processo processo = processos.findById(processoId).orElseThrow();
        DataJud estatistica = new DataJud();
        estatistica.setProcesso(processo);
        estatistica.setQuantidadeProcessos(quantidade);
        TempoEstatistica tempo = new TempoEstatistica();
        tempo.setQuantidade(quantidade);
        tempo.setMediaDias(15.0);
        estatistica.setTempoPendente(tempo);
        estatisticas.saveAndFlush(estatistica);
        Comunicacao comunicacao = new Comunicacao();
        comunicacao.setProcesso(processo);
        comunicacao.setTexto(texto);
        comunicacoes.saveAndFlush(comunicacao);
    }

    private Conta criarConta(String email) throws Exception {
        JsonNode cadastro = body(mvc.perform(post("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("nome", "Teste", "email", email, "senha", SENHA))))
                .andExpect(status().isCreated()).andReturn());
        MvcResult login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email.toUpperCase(), "senha", SENHA))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.senha").doesNotExist()).andExpect(header().doesNotExist("Set-Cookie"))
                .andReturn();
        assertThat(login.getRequest().getSession(false)).isNull();
        return new Conta(cadastro.get("id").asLong(), email, body(login).get("token").asText());
    }

    private long criarProcesso(Conta conta, String numero) throws Exception {
        return body(mvc.perform(post("/api/processo/cadastrar").header("Authorization", conta.bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(dadosProcesso(numero))))
                .andExpect(status().isOk()).andReturn()).get("id").asLong();
    }

    private Map<String, Object> dadosProcesso(String numero) {
        return Map.of("numeroProcesso", numero, "tribunal", "TJSP", "grau", "G1", "uf", "SP",
                "assunto", Map.of("nome", "Mesmo assunto", "codigo", "1"));
    }

    private String json(Object valor) {
        return mapper.writeValueAsString(valor);
    }

    private JsonNode body(MvcResult resultado) throws Exception {
        return mapper.readTree(resultado.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private record Conta(Long id, String email, String token) {
        String bearer() { return "Bearer " + token; }
    }
}
