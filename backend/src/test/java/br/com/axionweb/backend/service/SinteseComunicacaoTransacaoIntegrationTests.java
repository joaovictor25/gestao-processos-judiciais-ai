package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.auth.CadastroUsuarioRequestDTO;
import br.com.axionweb.backend.dto.comunicacao.AnaliseComunicacaoDTO;
import br.com.axionweb.backend.dto.comunicacao.ComunicacaoSalvaDTO;
import br.com.axionweb.backend.dto.comunicacao.SinteseComunicacaoRequestDTO;
import br.com.axionweb.backend.dto.comunicacao.TipoAnaliseComunicacao;
import br.com.axionweb.backend.dto.processo.ProcessoRequestDTO;
import br.com.axionweb.backend.exception.AnaliseComunicacaoException;
import br.com.axionweb.backend.model.comunicacao.Comunicacao;
import br.com.axionweb.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:sinteseTransacao;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop"})
@ActiveProfiles("test")
@WithMockUser(username = "transacao@teste.com")
class SinteseComunicacaoTransacaoIntegrationTests {
    private static final String TEXTO = "O juízo deferiu a tutela de urgência requerida pela parte autora.";
    @Autowired SinteseComunicacaoService sinteses;
    @Autowired ProcessoService processos;
    @Autowired UsuarioService usuarios;
    @Autowired PesquisaComunicacaoRepository pesquisas;
    @Autowired ComunicacaoRepository comunicacoes;
    @Autowired DataJudRepository estatisticas;
    @Autowired ProcessoRepository processoRepository;
    @Autowired UsuarioRepository usuarioRepository;
    @MockitoBean LLMService llm;
    private Long processoId;
    private List<Long> ids;

    @BeforeEach
    void preparar() {
        pesquisas.deleteAllInBatch(); comunicacoes.deleteAllInBatch(); estatisticas.deleteAllInBatch();
        processoRepository.deleteAllInBatch(); usuarioRepository.deleteAllInBatch();
        usuarios.cadastrar(new CadastroUsuarioRequestDTO("Transação", "transacao@teste.com", "senha123"));
        processoId = processos.cadastrar(new ProcessoRequestDTO("0000001", null, null, "TJPR", null,
                null, null, null, null, null, null, null, null, null, null, null)).getId();
        ids = List.of(salvarComunicacao(TEXTO));
    }

    @Test
    void chamadaDaIaOcorreSemTransacaoEResultadoEGravadoDepois() {
        when(llm.sintetizarComunicacoes(any())).thenAnswer(chamada -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive())
                    .as("nenhuma transação ou conexão retida durante a chamada externa").isFalse();
            return categoriaValida();
        });
        var gerada = sinteses.gerar(processoId, new SinteseComunicacaoRequestDTO(null, ids));
        assertThat(gerada.sintese()).startsWith("A comunicação registra");
        assertThat(pesquisas.count()).isEqualTo(1);
        assertThat(processoRepository.findById(processoId).orElseThrow().getUltimaAtividadeTitulo())
                .isEqualTo("Síntese das comunicações concluída");
        // Repetir o pedido devolve a síntese salva sem nova chamada.
        when(llm.sintetizarComunicacoes(any())).thenThrow(new AssertionError("não deveria chamar a IA"));
        assertThat(sinteses.gerar(processoId, new SinteseComunicacaoRequestDTO(null, ids)).id()).isEqualTo(gerada.id());
    }

    @Test
    void falhaDaIaNaoRegistraPesquisaNemAtividade() {
        String atividadeAntes = processoRepository.findById(processoId).orElseThrow().getUltimaAtividadeTitulo();
        when(llm.sintetizarComunicacoes(any())).thenThrow(new AnaliseComunicacaoException(AnaliseComunicacaoException.Motivo.SERVICO_INDISPONIVEL));
        assertThatThrownBy(() -> sinteses.gerar(processoId, new SinteseComunicacaoRequestDTO(null, ids)))
                .isInstanceOf(AnaliseComunicacaoException.class);
        assertThat(pesquisas.count()).isZero();
        assertThat(processoRepository.findById(processoId).orElseThrow().getUltimaAtividadeTitulo()).isEqualTo(atividadeAntes);
    }

    @Test
    void novaPesquisaDuranteChamadaDaIaImpedeGravacaoDeSinteseObsoleta() {
        when(llm.sintetizarComunicacoes(any())).thenAnswer(chamada -> {
            comunicacoes.deleteAllInBatch(); // Outra aba substitui as comunicações do processo.
            salvarComunicacao("Nova publicação com outro conteúdo.");
            return categoriaValida();
        });
        assertThatThrownBy(() -> sinteses.gerar(processoId, new SinteseComunicacaoRequestDTO(null, ids)))
                .isInstanceOfSatisfying(AnaliseComunicacaoException.class, e -> assertThat(e.getCodigo()).isEqualTo("PESQUISA_DESATUALIZADA"));
        assertThat(pesquisas.count()).isZero();
    }

    @Test
    void respostaAtrasadaNaoSobrescreveSinteseGravadaPorOutraGeracao() {
        var processo = processoRepository.findById(processoId).orElseThrow();
        var salvas = comunicacoes.findByProcessoIdOrderByIdAsc(processoId).stream().map(ComunicacaoSalvaDTO::de).toList();
        Long pesquisaId = sinteses.registrar(processo, null, salvas).id;
        when(llm.sintetizarComunicacoes(any())).thenAnswer(chamada -> {
            var concorrente = pesquisas.findById(pesquisaId).orElseThrow();
            concorrente.sintese = "Síntese gravada por outra geração.";
            pesquisas.saveAndFlush(concorrente);
            return categoriaValida();
        });
        assertThatThrownBy(() -> sinteses.gerar(processoId, new SinteseComunicacaoRequestDTO(pesquisaId, ids)))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(pesquisas.findById(pesquisaId).orElseThrow().sintese).isEqualTo("Síntese gravada por outra geração.");
    }

    @Test
    void registroConcorrenteDaMesmaBuscaNaoDuplicaPesquisa() {
        when(llm.sintetizarComunicacoes(any())).thenAnswer(chamada -> {
            var processo = processoRepository.findById(processoId).orElseThrow();
            var salvas = comunicacoes.findByProcessoIdOrderByIdAsc(processoId).stream().map(ComunicacaoSalvaDTO::de).toList();
            sinteses.registrar(processo, null, salvas);
            return categoriaValida();
        });
        assertThatThrownBy(() -> sinteses.gerar(processoId, new SinteseComunicacaoRequestDTO(null, ids)))
                .isInstanceOf(OptimisticLockingFailureException.class);
        assertThat(pesquisas.count()).isEqualTo(1);
    }

    private Long salvarComunicacao(String texto) {
        var comunicacao = new Comunicacao();
        comunicacao.setProcesso(processoRepository.findById(processoId).orElseThrow());
        comunicacao.setTexto(texto);
        return comunicacoes.saveAndFlush(comunicacao).getId();
    }

    private static AnaliseComunicacaoDTO.Categoria categoriaValida() {
        return new AnaliseComunicacaoDTO.Categoria(TipoAnaliseComunicacao.SINTESE_GERAL, List.of(new AnaliseComunicacaoDTO.Item(
                "Síntese", "A comunicação registra o deferimento da tutela de urgência requerida pela parte autora.", null, null,
                List.of(new AnaliseComunicacaoDTO.Evidencia(1, "deferiu a tutela de urgência")))), null);
    }
}
