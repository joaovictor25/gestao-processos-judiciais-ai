package br.com.axionweb.backend.security;

import br.com.axionweb.backend.dto.auth.CadastroUsuarioRequestDTO;
import br.com.axionweb.backend.dto.datajud.DadosDataJudDTO;
import br.com.axionweb.backend.dto.datajud.GerarEstatisticasRequestDTO;
import br.com.axionweb.backend.dto.datajud.MovimentoDataJudDTO;
import br.com.axionweb.backend.dto.processo.ProcessoRequestDTO;
import br.com.axionweb.backend.service.AnaliseService;
import br.com.axionweb.backend.service.DataJudService;
import br.com.axionweb.backend.service.LLMService;
import br.com.axionweb.backend.service.ProcessoService;
import br.com.axionweb.backend.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Cada teste usa IPs e e-mails próprios, pois os contadores são compartilhados pelo contexto. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:limites;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop",
        "app.auth.limite.login-por-ip=5", "app.auth.limite.falhas-por-email=3", "app.auth.limite.cadastro-por-ip=2",
        "app.ia.limite.por-usuario=2", "app.ia.limite.global=1000"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class LimitesHttpIntegrationTests {
    private static final String SENHA = "senha-correta-123";
    @Autowired MockMvc mvc;
    @Autowired UsuarioService usuarios;
    @Autowired ProcessoService processos;
    @Autowired AnaliseService analises;
    @MockitoBean LLMService llm;
    @MockitoBean DataJudService datajud;

    @Test
    void cadastroExcedidoPorIpRetorna429ComRetryAfterSemAfetarOutroIp() throws Exception {
        cadastro("10.0.1.1", "c1@teste.com").andExpect(status().isCreated());
        cadastro("10.0.1.1", "c2@teste.com").andExpect(status().isCreated());
        cadastro("10.0.1.1", "c3@teste.com").andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.codigo").value("CADASTRO_LIMITADO"));
        cadastro("10.0.1.2", "c3@teste.com").andExpect(status().isCreated());
    }

    @Test
    void falhasDeLoginBloqueiamSomenteOEmailAtacadoInclusiveComSenhaCorreta(CapturedOutput saida) throws Exception {
        usuarios.cadastrar(new CadastroUsuarioRequestDTO("Alvo", "alvo@teste.com", SENHA));
        usuarios.cadastrar(new CadastroUsuarioRequestDTO("Vizinho", "vizinho@teste.com", SENHA));
        for (int i = 0; i < 3; i++) login("10.0.2." + i, "alvo@teste.com", "errada").andExpect(status().isUnauthorized());
        login("10.0.2.9", "ALVO@teste.com", SENHA).andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.codigo").value("LOGIN_LIMITADO"))
                .andExpect(jsonPath("$.token").doesNotExist());
        login("10.0.2.9", "vizinho@teste.com", SENHA).andExpect(status().isOk());
        org.assertj.core.api.Assertions.assertThat(saida.getAll()).contains("codigo=LOGIN_LIMITADO")
                .doesNotContain("alvo@teste.com", SENHA, "10.0.2.9");
    }

    @Test
    void loginBemSucedidoZeraFalhasDoEmail() throws Exception {
        usuarios.cadastrar(new CadastroUsuarioRequestDTO("Reset", "reset@teste.com", SENHA));
        for (int i = 0; i < 2; i++) login("10.0.3." + i, "reset@teste.com", "errada").andExpect(status().isUnauthorized());
        login("10.0.3.5", "reset@teste.com", SENHA).andExpect(status().isOk());
        for (int i = 0; i < 2; i++) login("10.0.4." + i, "reset@teste.com", "errada").andExpect(status().isUnauthorized());
        login("10.0.4.5", "reset@teste.com", SENHA).andExpect(status().isOk());
    }

    @Test
    void excessoDeLoginsDoMesmoIpRetorna429AntesDeAutenticar() throws Exception {
        for (int i = 0; i < 5; i++) login("10.0.5.1", "inexistente" + i + "@teste.com", "x").andExpect(status().isUnauthorized());
        login("10.0.5.1", "outro@teste.com", "x").andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("LOGIN_LIMITADO"));
        login("10.0.5.2", "outro@teste.com", "x").andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "cota@teste.com")
    void cotaDeIaPorUsuarioRetorna429SemChamarGeminiNemAlterarSintese() throws Exception {
        usuarios.cadastrar(new CadastroUsuarioRequestDTO("Cota", "cota@teste.com", SENHA));
        Long processoId = processos.cadastrar(new ProcessoRequestDTO("0000009", null, null, "TJPR", null,
                null, null, null, null, null, null, null, null, null, null, null)).getId();
        when(datajud.montarParametroDataJud(any())).thenCallRealMethod();
        when(datajud.buscarProcessos(any())).thenReturn(List.of(dado(11, 21), dado(12, 22)));
        String texto = "Na amostra de 2 processos, a mediana até o primeiro julgamento foi de 10,5 dias e até a primeira baixa de 20,5 dias. As medianas descrevem conjuntos de registros válidos que podem ser distintos. Em 0 processos, não foi identificada baixa definitiva. Os resultados não permitem prever prazos individuais.";
        when(llm.sintetizarEstatisticas(any())).thenReturn(texto);
        Long estatisticaId = analises.gerarEstatisticas(processoId, new GerarEstatisticasRequestDTO(null, null)).getFirst().id();
        String rota = "/api/dados/estatisticas/" + processoId + "/sintese-ia";
        String corpo = "{\"estatisticaId\":" + estatisticaId + "}";
        for (int i = 0; i < 2; i++)
            mvc.perform(post(rota).with(user("cota@teste.com")).contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isOk());
        mvc.perform(post(rota).with(user("cota@teste.com")).contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().exists("Retry-After"))
                .andExpect(jsonPath("$.codigo").value("LIMITE_IA_USUARIO"));
        verify(llm, times(2)).sintetizarEstatisticas(any());
    }

    private ResultActions cadastro(String ip, String email) throws Exception {
        return mvc.perform(post("/api/auth/cadastro").with(ip(ip)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Teste\",\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"));
    }

    private ResultActions login(String ip, String email, String senha) throws Exception {
        return mvc.perform(post("/api/auth/login").with(ip(ip)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"));
    }

    private static RequestPostProcessor ip(String ip) {
        return request -> { request.setRemoteAddr(ip); return request; };
    }

    private static DadosDataJudDTO dado(int julgamento, int baixa) {
        var dado = new DadosDataJudDTO(); dado.setDataAjuizamento("20250101000000");
        var j = new MovimentoDataJudDTO(); j.setNome("Provimento"); j.setDataHora("2025-01-" + julgamento + "T00:00:00Z");
        var b = new MovimentoDataJudDTO(); b.setNome("Baixa Definitiva"); b.setDataHora("2025-01-" + baixa + "T00:00:00Z");
        dado.setMovimentos(List.of(j, b)); return dado;
    }
}
