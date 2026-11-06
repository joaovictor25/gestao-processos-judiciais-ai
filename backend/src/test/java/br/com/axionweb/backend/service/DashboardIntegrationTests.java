package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.auth.CadastroUsuarioRequestDTO;
import br.com.axionweb.backend.dto.processo.ProcessoRequestDTO;
import br.com.axionweb.backend.model.datajud.DataJud;
import br.com.axionweb.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.util.ArrayList;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:dashboard;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@WithMockUser(username = "painel@teste.com")
class DashboardIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired UsuarioService usuariosService;
    @Autowired ProcessoService processosService;
    @Autowired DashboardService dashboard;
    @Autowired ExecucaoAnaliseService execucoes;
    @Autowired ProcessoRepository processos;
    @Autowired UsuarioRepository usuarios;
    @Autowired DataJudRepository estatisticas;
    @Autowired ComunicacaoRepository comunicacoes;
    private final List<Long> ids = new ArrayList<>();

    @BeforeEach
    void preparar() {
        comunicacoes.deleteAllInBatch(); estatisticas.deleteAllInBatch(); processos.deleteAllInBatch(); usuarios.deleteAllInBatch();
        usuariosService.cadastrar(new CadastroUsuarioRequestDTO("Painel", "painel@teste.com", "senha123"));
        usuariosService.cadastrar(new CadastroUsuarioRequestDTO("Outra conta", "outra@teste.com", "senha123"));
        for (int i = 0; i < 6; i++) ids.add(processosService.cadastrar(new ProcessoRequestDTO("PROC-" + i,
                "Cliente", "AUTOR", "TJPR", null, null, null, null, null, null,
                i < 2 ? "Cível" : i < 4 ? "Trabalhista" : null, null, null, null, null, null)).getId());
        var outro = new br.com.axionweb.backend.model.processo.Processo();
        outro.setUsuario(usuarios.findByEmail("outra@teste.com").orElseThrow());
        outro.setNumeroProcesso("PRIVADO"); processos.saveAndFlush(outro);
    }

    @Test
    void contaSomenteCarteiraDaContaEPaginaQuatroProcessos() throws Exception {
        var primeira = dashboard.consultar(1); var segunda = dashboard.consultar(2);
        assertThat(primeira.areas()).extracting(DashboardService.Area::quantidade).containsExactly(2L, 2L, 2L);
        assertThat(segunda.atividades()).hasSize(2);
        assertThat(primeira.atividades()).extracting(DashboardService.Atividade::processoId)
                .doesNotContainAnyElementsOf(segunda.atividades().stream().map(DashboardService.Atividade::processoId).toList());
        assertThat(dashboard.consultar(999).pagina()).isEqualTo(2);
        assertThat(primeira.atividades()).allSatisfy(a -> assertThat(a.data()).isNotNull());
        // MockMvc limpa o contexto ao encerrar a requisição; chamadas diretas ficam antes dela.
        mvc.perform(get("/api/dashboard").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("painel@teste.com")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cadastrados").value(6))
                .andExpect(jsonPath("$.aguardando").value(6)).andExpect(jsonPath("$.emAnalise").value(0))
                .andExpect(jsonPath("$.atividades.length()").value(4)).andExpect(jsonPath("$.totalPaginas").value(2));
    }

    @Test
    void distingueAnalisesConcluidasExecucaoEEsperaELimpaFalhas() {
        processosService.registrarAnaliseIA(ids.getFirst());
        var estatistica = new DataJud(); estatistica.setProcesso(processos.findById(ids.get(1)).orElseThrow());
        estatisticas.saveAndFlush(estatistica);
        assertThat(dashboard.consultar(1).analisados()).isEqualTo(2);
        assertThat(dashboard.consultar(1).atividades().getFirst().titulo()).contains("Análise das comunicações");
        assertThatThrownBy(() -> execucoes.executar(ids.getFirst(), () -> {
            var durante = dashboard.consultar(1);
            assertThat(durante.emAnalise()).isEqualTo(1);
            assertThat(durante.analisados()).isEqualTo(1);
            assertThat(durante.aguardando()).isEqualTo(4);
            execucoes.executar(ids.getFirst(), () -> { assertThat(dashboard.consultar(1).emAnalise()).isEqualTo(1); return null; });
            assertThat(dashboard.consultar(1).emAnalise()).isEqualTo(1);
            throw new IllegalStateException("falha simulada");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(dashboard.consultar(1).emAnalise()).isZero();
        assertThat(dashboard.consultar(1).analisados()).isEqualTo(2);
    }

    @Test
    void contaSemProcessosTemZerosESemAtividades() throws Exception {
        usuariosService.cadastrar(new CadastroUsuarioRequestDTO("Vazia", "vazia@teste.com", "senha123"));
        mvc.perform(get("/api/dashboard").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("vazia@teste.com")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cadastrados").value(0))
                .andExpect(jsonPath("$.areas.length()").value(0)).andExpect(jsonPath("$.atividades.length()").value(0));
    }
}
