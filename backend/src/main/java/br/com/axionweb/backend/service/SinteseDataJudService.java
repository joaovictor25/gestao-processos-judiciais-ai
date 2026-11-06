package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.datajud.*;
import br.com.axionweb.backend.model.datajud.DataJud;
import br.com.axionweb.backend.repository.DataJudRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SinteseDataJudService {
    private final DataJudRepository estatisticas;
    private final LLMService llm;
    private final ProcessoService processos;

    public SinteseDataJudService(DataJudRepository estatisticas, LLMService llm, ProcessoService processos) {
        this.estatisticas = estatisticas;
        this.llm = llm;
        this.processos = processos;
    }

    @Transactional
    public SinteseDataJudDTO gerar(Long processoId, Long estatisticaId) {
        processos.buscarPorId(processoId);
        var entidade = buscar(processoId, estatisticaId);
        var dados = DataJudResponseDTO.de(entidade);
        if (dados.quantidadeProcessos() == null || dados.quantidadeProcessos() <= 0)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Não há processos na amostra para gerar uma síntese.");
        // Não altera o registro antes de receber e validar a resposta completa.
        String sintese = ValidadorSinteseDataJud.validar(llm.sintetizarEstatisticas(dados), dados);
        var processo = processos.bloquearParaAtualizarAnalise(processoId);
        entidade.salvarSintese(sintese, java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        // @Version rejeita respostas atrasadas se outra geração já salvou esta execução.
        estatisticas.saveAndFlush(entidade);
        processo.registrarAtividade("analise", "Síntese das estatísticas DataJud concluída");
        return SinteseDataJudDTO.de(entidade);
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
