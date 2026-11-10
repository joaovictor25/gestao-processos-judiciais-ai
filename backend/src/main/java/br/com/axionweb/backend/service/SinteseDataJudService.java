package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.datajud.*;
import br.com.axionweb.backend.model.datajud.DataJud;
import br.com.axionweb.backend.repository.DataJudRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

@Service
public class SinteseDataJudService {
    private final DataJudRepository estatisticas;
    private final LLMService llm;
    private final ProcessoService processos;
    private final CotaIAService cotas;
    private final TransactionTemplate leitura;
    private final TransactionTemplate escrita;

    public SinteseDataJudService(DataJudRepository estatisticas, LLMService llm, ProcessoService processos,
                                 CotaIAService cotas, PlatformTransactionManager transacoes) {
        this.estatisticas = estatisticas;
        this.llm = llm;
        this.processos = processos;
        this.cotas = cotas;
        this.leitura = new TransactionTemplate(transacoes);
        this.leitura.setReadOnly(true);
        this.escrita = new TransactionTemplate(transacoes);
    }

    private record Leitura(DataJudResponseDTO dados, Long versao) {}

    /**
     * Três etapas: leitura curta, chamada à IA sem transação nem conexão retida e gravação curta.
     * A versão lida é conferida sob o bloqueio do processo, rejeitando respostas atrasadas.
     */
    public SinteseDataJudDTO gerar(Long processoId, Long estatisticaId) {
        var lida = leitura.execute(status -> {
            processos.buscarPorId(processoId);
            var entidade = buscar(processoId, estatisticaId);
            var dados = DataJudResponseDTO.de(entidade);
            if (dados.quantidadeProcessos() == null || dados.quantidadeProcessos() <= 0)
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Não há processos na amostra para gerar uma síntese.");
            return new Leitura(dados, entidade.getVersao());
        });
        // Não altera o registro antes de receber e validar a resposta completa.
        String sintese = ValidadorSinteseDataJud.validar(cotas.executar(() -> llm.sintetizarEstatisticas(lida.dados())), lida.dados());
        return escrita.execute(status -> {
            var processo = processos.bloquearParaAtualizarAnalise(processoId);
            var entidade = buscar(processoId, estatisticaId);
            if (!Objects.equals(entidade.getVersao(), lida.versao()))
                throw new ObjectOptimisticLockingFailureException(DataJud.class, estatisticaId);
            entidade.salvarSintese(sintese, Instant.now().truncatedTo(ChronoUnit.MICROS));
            // @Version também protege a gravação caso outra instância altere o registro.
            estatisticas.saveAndFlush(entidade);
            processo.registrarAtividade("analise", "Síntese das estatísticas DataJud concluída");
            return SinteseDataJudDTO.de(entidade);
        });
    }

    @Transactional(readOnly = true)
    public DataJudResponseDTO consultar(Long processoId, Long estatisticaId) {
        processos.buscarPorId(processoId);
        return DataJudResponseDTO.de(buscar(processoId, estatisticaId));
    }

    @Transactional(readOnly = true)
    public HistoricoSintesesDTO historico(Long processoId, int pagina) {
        processos.buscarPorId(processoId);
        if (pagina < 0 || pagina > 100000)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Página inválida.");
        var resultados = estatisticas.findByProcessoIdAndSinteseIsNotNullAndSinteseGeradaEmIsNotNullOrderBySinteseGeradaEmDescIdDesc(
                processoId, PageRequest.of(pagina, 5));
        return new HistoricoSintesesDTO(resultados.stream().map(HistoricoSintesesDTO.Item::de).toList(), pagina, resultados.hasNext());
    }

    private DataJud buscar(Long processoId, Long estatisticaId) {
        return estatisticas.findByIdAndProcessoId(estatisticaId, processoId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Execução estatística não encontrada para este processo."));
    }
}
