package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.auth.CadastroUsuarioRequestDTO;
import br.com.axionweb.backend.dto.processo.ProcessoRequestDTO;
import br.com.axionweb.backend.dto.comunicacao.TipoAnaliseComunicacao;
import br.com.axionweb.backend.model.comunicacao.Comunicacao;
import br.com.axionweb.backend.model.processo.Processo;
import br.com.axionweb.backend.model.processo.Assunto;
import br.com.axionweb.backend.model.datajud.DataJud;
import br.com.axionweb.backend.repository.*;
import br.com.axionweb.backend.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:analise-comunicacoes;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop", "app.gemini.api-key=chave-de-teste",
        "app.gemini.base-url=https://gemini.teste", "app.gemini.model=gemini-flash-latest"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AnaliseComunicacaoIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired GeminiService gemini;
    @Autowired ComunicacaoService comunicacaoService;
    @Autowired ProcessoService processoService;
    @Autowired UsuarioService usuarioService;
    @Autowired UsuarioRepository usuarios;
    @Autowired ProcessoRepository processos;
    @Autowired ComunicacaoRepository comunicacoes;
    @Autowired DataJudRepository estatisticas;
    @Autowired JwtService jwtService;
    @Autowired ObjectMapper mapper;
    private MockRestServiceServer servidor;
    private MockRestServiceServer djen;
    private Long processoId;
    private String tokenDono, tokenOutro;

    @BeforeEach
    void preparar() {
        comunicacoes.deleteAllInBatch(); estatisticas.deleteAllInBatch(); processos.deleteAllInBatch(); usuarios.deleteAllInBatch();
        usuarioService.cadastrar(new CadastroUsuarioRequestDTO("Dono", "dono-ia@teste.com", "senha123"));
        usuarioService.cadastrar(new CadastroUsuarioRequestDTO("Outro", "outro-ia@teste.com", "senha123"));
        tokenDono = "Bearer " + jwtService.gerarToken("dono-ia@teste.com");
        tokenOutro = "Bearer " + jwtService.gerarToken("outro-ia@teste.com");
        Processo processo;
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("dono-ia@teste.com", null, java.util.List.of()));
        try {
            processo = processoService.cadastrar(new ProcessoRequestDTO("PROC-IA", "Cliente que não deve ser enviado", "AUTOR", "TJPR",
                    null, null, null, null, null, null, null, null, null, null, null, null));
        } finally {
            SecurityContextHolder.clearContext();
        }
        processoId = processo.getId();
        var builder = RestClient.builder().baseUrl("https://gemini.teste");
        servidor = MockRestServiceServer.bindTo(builder).build();
        ReflectionTestUtils.setField(gemini, "restGemini", builder.build());
        var builderDjen = RestClient.builder().baseUrl("https://djen.teste");
        djen = MockRestServiceServer.bindTo(builderDjen).build();
        ReflectionTestUtils.setField(comunicacaoService, "restComunicacao", builderDjen.build());
    }

    @AfterEach
    void verificarChamadas() { servidor.verify(); djen.verify(); }

    private String requisicao(String json) {
        var body = (ObjectNode) mapper.readTree(json);
        var ids = body.putArray("comunicacaoIds");
        comunicacoes.findByProcessoIdOrderByIdAsc(processoId).forEach(item -> ids.add(item.getId()));
        return mapper.writeValueAsString(body);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"dataDisponibilizacaoInicio\":\"2026-01-01\"}",
            "{\"dataDisponibilizacaoFim\":\"2026-09-01\"}",
            "{\"dataDisponibilizacaoInicio\":\"2026-01-01\",\"dataDisponibilizacaoFim\":\"2026-09-01\"}"})
    void pesquisaUsaFiltrosDaTelaSalvaPublicacoesSemAcionarIAOuAlterarDataJud(String filtros) throws Exception {
        var processo = processos.findById(processoId).orElseThrow();
        processo.setDataInicio(LocalDate.of(2020, 1, 1)); processo.setDataFim(LocalDate.of(2021, 1, 1));
        var assunto = new Assunto(); assunto.setNome("Assunto do cadastro"); processo.setAssunto(assunto);
        processos.save(processo);
        var estatistica = new DataJud(); estatistica.setProcesso(processo); estatistica.setQuantidadeProcessos(42);
        estatisticas.save(estatistica);
        salvar("Publicação anterior.");
        var body = (ObjectNode) mapper.readTree(filtros);
        body.put("texto", "  contrato & indenização + {teste}  ");
        djen.expect(request -> {
            assertThat(request.getURI().getRawQuery()).contains("texto=contrato%20%26%20indeniza%C3%A7%C3%A3o%20%2B%20%7Bteste%7D", "siglaTribunal=TJPR");
            for (var campo : List.of("dataDisponibilizacaoInicio", "dataDisponibilizacaoFim")) {
                if (body.has(campo)) assertThat(request.getURI().getQuery()).contains(campo + "=" + body.get(campo).asText());
                else assertThat(request.getURI().getQuery()).doesNotContain(campo);
            }
            assertThat(request.getURI().getQuery()).doesNotContain("2020-01-01", "2021-01-01", "Assunto do cadastro");
        }).andRespond(withSuccess("{\"items\":[{\"texto\":\"Publicação pesquisada.\",\"data_disponibilizacao\":\"2026-02-01\",\"tipoComunicacao\":\"Intimação\"}]}", MediaType.APPLICATION_JSON));
        mvc.perform(post("/api/dados/comunicacoes/{id}/pesquisar", processoId).header("Authorization", tokenDono)
                        .contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").isNumber()).andExpect(jsonPath("$[0].processoId").value(processoId))
                .andExpect(jsonPath("$[0].texto").value("Publicação pesquisada."))
                .andExpect(jsonPath("$[0].dataDisponibilizacao").value("2026-02-01"));
        assertThat(comunicacoes.findByProcessoIdOrderByIdAsc(processoId)).singleElement()
                .extracting(Comunicacao::getTexto).isEqualTo("Publicação pesquisada.");
        assertThat(estatisticas.findByProcessoIdOrderByIdAsc(processoId)).singleElement()
                .extracting(DataJud::getQuantidadeProcessos).isEqualTo(42);
    }

    @Test
    void analisaExatamenteAPesquisaSemConsultarDjenNovamente() throws Exception {
        djen.expect(anything()).andRespond(withSuccess("{\"items\":[{\"texto\":\"Foi determinada apresentação do contrato.\"}]}", MediaType.APPLICATION_JSON));
        mvc.perform(post("/api/dados/comunicacoes/{id}/pesquisar", processoId).header("Authorization", tokenDono)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"contrato\"}"))
                .andExpect(status().isOk());
        servidor.expect(anything()).andRespond(withSuccess(resposta("{\"categorias\":[{\"tipo\":\"SINTESE_GERAL\",\"itens\":[],\"limitacao\":\"Texto insuficiente.\"}]}"), MediaType.APPLICATION_JSON));
        mvc.perform(post("/api/dados/comunicacoes/{id}/analise-ia", processoId).header("Authorization", tokenDono)
                        .contentType(MediaType.APPLICATION_JSON).content(requisicao("{\"tiposAnalise\":[\"SINTESE_GERAL\"]}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.comunicacoesConsideradas").value(1))
                .andExpect(jsonPath("$.comunicacoes[0].texto").value("Foi determinada apresentação do contrato."));
    }

    @Test
    void buscaVaziaLimpaPublicacoesAnterioresERejeitaAnaliseDaPesquisaAntiga() throws Exception {
        salvar("Publicação da pesquisa anterior.");
        var pedidoAntigo = requisicao("{\"tiposAnalise\":[\"SINTESE_GERAL\"]}");
        djen.expect(anything()).andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));
        mvc.perform(post("/api/dados/comunicacoes/{id}/pesquisar", processoId).header("Authorization", tokenDono)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"contrato\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        assertThat(comunicacoes.findByProcessoIdOrderByIdAsc(processoId)).isEmpty();
        mvc.perform(post("/api/dados/comunicacoes/{id}/analise-ia", processoId).header("Authorization", tokenDono)
                        .contentType(MediaType.APPLICATION_JSON).content(pedidoAntigo))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("PESQUISA_DESATUALIZADA"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"texto\":\"\"}", "{\"texto\":\"   \"}",
            "{\"texto\":\"contrato\",\"dataDisponibilizacaoInicio\":\"2026-09-02\",\"dataDisponibilizacaoFim\":\"2026-09-01\"}",
            "{\"texto\":\"contrato\",\"dataDisponibilizacaoInicio\":\"invalida\"}"})
    void rejeitaFiltrosInvalidosSemConsultarServicos(String body) throws Exception {
        mvc.perform(post("/api/dados/comunicacoes/{id}/pesquisar", processoId).header("Authorization", tokenDono)
                        .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }

    @Test
    void pesquisaBloqueiaVisitanteEOutroUsuarioAntesDeConsultarDjen() throws Exception {
        mvc.perform(post("/api/dados/comunicacoes/{id}/pesquisar", processoId)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"contrato\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/dados/comunicacoes/{id}/pesquisar", processoId).header("Authorization", tokenOutro)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"contrato\"}"))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @EnumSource(value = HttpStatus.class, names = {"TOO_MANY_REQUESTS", "BAD_GATEWAY"})
    void falhaDjenPreservaPublicacoesAnterioresSemAcionarIA(HttpStatus statusExterno) throws Exception {
        salvar("Publicação anterior.");
        djen.expect(anything()).andRespond(withStatus(statusExterno).body("detalhe externo reservado"));
        mvc.perform(post("/api/dados/comunicacoes/{id}/pesquisar", processoId).header("Authorization", tokenDono)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"texto\":\"contrato\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.codigo").value(statusExterno == HttpStatus.TOO_MANY_REQUESTS ? "CONSULTA_DJEN_LIMITADA" : "CONSULTA_DJEN_FALHOU"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(not(containsString("detalhe externo reservado"))));
        assertThat(comunicacoes.findByProcessoIdOrderByIdAsc(processoId)).singleElement()
                .extracting(Comunicacao::getTexto).isEqualTo("Publicação anterior.");
    }

    @Test
    void enviaSomenteTextosUnicosCategoriasSelecionadasERetornaEvidencias() throws Exception {
        salvar("Foi determinada apresentação do contrato. Custas de R$ 5.000,00.");
        salvar("  Foi determinada apresentação do contrato.  Custas de R$ 5.000,00.  ");
        salvar("   ");
        salvar("A parte apresentou contrato escrito.");
        String modelo = """
                {"categorias":[
                  {"tipo":"ELEMENTOS_PROBATORIOS","itens":[{"titulo":"Contrato","descricao":"Foi solicitada apresentação do contrato.","contexto":null,"decisaoRelacionada":null,"evidencias":[{"comunicacao":1,"trecho":"apresentação do contrato"}]}],"limitacao":null},
                  {"tipo":"VALORES","itens":[{"titulo":"R$ 5.000,00","descricao":"Valor de custas mencionado no texto.","contexto":"Custas","decisaoRelacionada":null,"evidencias":[{"comunicacao":1,"trecho":"Custas de R$ 5.000,00"}]}],"limitacao":null}
                ]}
                """;
        servidor.expect(requestTo("https://gemini.teste/v1beta/models/gemini-flash-latest:generateContent"))
                .andExpect(header("X-goog-api-key", "chave-de-teste"))
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content().string(
                        org.hamcrest.Matchers.allOf(containsString("COMUNICAÇÃO 1:"), containsString("COMUNICAÇÃO 2:"),
                                containsString("ELEMENTOS_PROBATORIOS"), containsString("VALORES"),
                                not(containsString("Cliente que não deve ser enviado")), not(containsString("PADROES_DECISAO")),
                                not(containsString("COMUNICAÇÃO 3:")), containsString("\"responseJsonSchema\""),
                                containsString("\"maxOutputTokens\":16384"))))
                .andRespond(withSuccess(resposta(modelo), MediaType.APPLICATION_JSON));
        mvc.perform(post("/api/dados/comunicacoes/{id}/analise-ia", processoId).header("Authorization", tokenDono)
                        .contentType(MediaType.APPLICATION_JSON).content(requisicao("{\"tiposAnalise\":[\"ELEMENTOS_PROBATORIOS\",\"VALORES\"]}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comunicacoesConsideradas").value(2))
                .andExpect(jsonPath("$.duplicadasIgnoradas").value(1))
                .andExpect(jsonPath("$.categorias.length()").value(2))
                .andExpect(jsonPath("$.categorias[1].itens[0].evidencias[0].trecho").value("Custas de R$ 5.000,00"));
        assertThat(comunicacoes.findByProcessoIdOrderByIdAsc(processoId)).hasSize(4);
        servidor.verify();
    }

    @ParameterizedTest
    @EnumSource(TipoAnaliseComunicacao.class)
    void aceitaCitacaoLiteralDoTextoVisivelMesmoComHtmlEEntidades(TipoAnaliseComunicacao tipo) throws Exception {
        String original = "<p>Foi determinada a apresenta&ccedil;&atilde;o do <strong>contrato</strong> em 15&nbsp;dias.</p>";
        salvar(original);
        String modelo = """
                {"categorias":[{"tipo":"%s","itens":[{"titulo":"Contrato","descricao":"Apresentação do contrato em 15 dias.","evidencias":[{"comunicacao":1,"trecho":"apresentação do contrato em 15 dias"}]}]}]}
                """.formatted(tipo.name());
        servidor.expect(anything())
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content().string(
                        org.hamcrest.Matchers.allOf(containsString("apresentação do contrato em 15 dias"),
                                not(containsString("<strong>")), not(containsString("&nbsp;")))))
                .andRespond(withSuccess(resposta(modelo), MediaType.APPLICATION_JSON));
        mvc.perform(post("/api/dados/comunicacoes/{id}/analise-ia", processoId).header("Authorization", tokenDono)
                        .contentType(MediaType.APPLICATION_JSON).content(requisicao("{\"tiposAnalise\":[\"%s\"]}".formatted(tipo.name()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categorias[0].itens[0].evidencias[0].trecho").value("apresentação do contrato em 15 dias"));
        assertThat(comunicacoes.findByProcessoIdOrderByIdAsc(processoId).getFirst().getTexto()).isEqualTo(original);
        servidor.verify();
    }

    @Test
    void ignoraHtmlSemTextoEDeduplicaVariacoesDeFormatacao() throws Exception {
        salvar("<p>&nbsp;</p><script>texto de script</script><style>p { color: red }</style>");
        salvar("<p>Custas de <b>R$&#160;5.000,00</b>.</p>");
        salvar("Custas de R$\u00a05.000,00.");
        String modelo = """
                {"categorias":[{"tipo":"VALORES","itens":[{"titulo":"Custas","descricao":"Custas de R$ 5.000,00.","evidencias":[{"comunicacao":1,"trecho":"Custas de R$ 5.000,00."}]}]}]}
                """;
        servidor.expect(anything())
                .andExpect(org.springframework.test.web.client.match.MockRestRequestMatchers.content().string(
                        org.hamcrest.Matchers.allOf(not(containsString("texto de script")), not(containsString("COMUNICAÇÃO 2:")))))
                .andRespond(withSuccess(resposta(modelo), MediaType.APPLICATION_JSON));
        mvc.perform(post("/api/dados/comunicacoes/{id}/analise-ia", processoId).header("Authorization", tokenDono)
                        .contentType(MediaType.APPLICATION_JSON).content(requisicao("{\"tiposAnalise\":[\"VALORES\"]}")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comunicacoesConsideradas").value(1))
                .andExpect(jsonPath("$.duplicadasIgnoradas").value(1));
        assertThat(comunicacoes.findByProcessoIdOrderByIdAsc(processoId)).hasSize(3);
        servidor.verify();
    }

    @Test
    void semTextosEValidacaoNaoAcionamIA() throws Exception {
        salvar("   ");
        mvc.perform(post("/api/dados/comunicacoes/{id}/analise-ia", processoId).header("Authorization", tokenDono)
                        .contentType(MediaType.APPLICATION_JSON).content(requisicao("{\"tiposAnalise\":[\"SINTESE_GERAL\"]}")))
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.codigo").value("SEM_TEXTOS"));
        mvc.perform(post("/api/dados/comunicacoes/{id}/analise-ia", processoId).header("Authorization", tokenDono)
                        .contentType(MediaType.APPLICATION_JSON).content(requisicao("{\"tiposAnalise\":[]}")))
                .andExpect(status().isBadRequest());
        servidor.verify();
    }

    @Test
    void chaveAusenteRetornaErroConfiguracaoSemEnviarDocumentos() {
        var semChave = new GeminiService("", "https://gemini.teste", "gemini-flash-latest", mapper);
        assertThatThrownBy(() -> semChave.analisarComunicacoes(
                java.util.List.of("Texto de teste"), java.util.List.of(br.com.axionweb.backend.dto.comunicacao.TipoAnaliseComunicacao.SINTESE_GERAL)))
                .isInstanceOfSatisfying(br.com.axionweb.backend.exception.AnaliseComunicacaoException.class,
                        erro -> assertThat(erro.getCodigo()).isEqualTo("CONFIGURACAO_AUSENTE"));
    }

    @Test
    void bloqueiaOutroUsuarioAntesDeLerOuEnviarTextos() throws Exception {
        salvar("Texto confidencial da conta titular.");
        mvc.perform(post("/api/dados/comunicacoes/{id}/analise-ia", processoId)
                        .contentType(MediaType.APPLICATION_JSON).content(requisicao("{\"tiposAnalise\":[\"SINTESE_GERAL\"]}")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/dados/comunicacoes/{id}/analise-ia", processoId).header("Authorization", tokenOutro)
                        .contentType(MediaType.APPLICATION_JSON).content(requisicao("{\"tiposAnalise\":[\"SINTESE_GERAL\"]}")))
                .andExpect(status().isNotFound());
        servidor.verify();
    }

    @Test
    void rejeitaRespostaSemTrechoPresenteNosDocumentos() throws Exception {
        salvar("Foi determinada apresentação do contrato.");
        String modelo = """
                {"categorias":[{"tipo":"SINTESE_GERAL","itens":[{"titulo":"Decisão","descricao":"Condenação por dano moral.","contexto":null,"decisaoRelacionada":null,"evidencias":[{"comunicacao":1,"trecho":"condenação por dano moral"}]}],"limitacao":null}]}
                """;
        servidor.expect(anything()).andRespond(withSuccess(resposta(modelo), MediaType.APPLICATION_JSON));
        mvc.perform(post("/api/dados/comunicacoes/{id}/analise-ia", processoId).header("Authorization", tokenDono)
                        .contentType(MediaType.APPLICATION_JSON).content(requisicao("{\"tiposAnalise\":[\"SINTESE_GERAL\"]}")))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.codigo").value("RESPOSTA_INVALIDA"));
        servidor.verify();
    }

    @Test
    void respostaTruncadaRetornaErroSemExporTextoDosDocumentos() throws Exception {
        salvar("Texto reservado do processo.");
        servidor.expect(anything()).andRespond(withSuccess(
                "{\"candidates\":[{\"finishReason\":\"MAX_TOKENS\",\"content\":{\"parts\":[{\"text\":\"Texto reservado do processo.\"}]}}]}",
                MediaType.APPLICATION_JSON));
        mvc.perform(post("/api/dados/comunicacoes/{id}/analise-ia", processoId).header("Authorization", tokenDono)
                        .contentType(MediaType.APPLICATION_JSON).content(requisicao("{\"tiposAnalise\":[\"SINTESE_GERAL\"]}")))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.codigo").value("RESPOSTA_INCOMPLETA"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(
                        not(containsString("Texto reservado do processo."))));
        servidor.verify();
    }

    @Test
    void limiteDoGeminiNaoExibeCorpoExternoNemAlteraComunicacoes() throws Exception {
        salvar("Foi determinada apresentação do contrato.");
        servidor.expect(anything()).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).body("detalhe externo que não deve aparecer"));
        mvc.perform(post("/api/dados/comunicacoes/{id}/analise-ia", processoId).header("Authorization", tokenDono)
                        .contentType(MediaType.APPLICATION_JSON).content(requisicao("{\"tiposAnalise\":[\"SINTESE_GERAL\"]}")))
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.codigo").value("LIMITE_EXTERNO"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(
                        not(containsString("detalhe externo que não deve aparecer"))));
        assertThat(comunicacoes.findByProcessoIdOrderByIdAsc(processoId)).hasSize(1);
        servidor.verify();
    }

    private void salvar(String texto) {
        Comunicacao comunicacao = new Comunicacao();
        comunicacao.setProcesso(processos.findById(processoId).orElseThrow());
        comunicacao.setTexto(texto);
        comunicacoes.save(comunicacao);
    }

    private String resposta(String jsonModelo) {
        return "{\"candidates\":[{\"finishReason\":\"STOP\",\"content\":{\"parts\":[{\"text\":" +
                mapper.writeValueAsString(jsonModelo) + "}]}}]}";
    }
}
