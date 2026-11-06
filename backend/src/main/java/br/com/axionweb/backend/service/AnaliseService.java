package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.analise.AnaliseResponseDTO;
import br.com.axionweb.backend.dto.comunicacao.ComunicacaoSalvaDTO;
import br.com.axionweb.backend.dto.datajud.DataJudResponseDTO;
import br.com.axionweb.backend.dto.comunicacao.DadosComunicacaoDTO;
import br.com.axionweb.backend.dto.comunicacao.ParametrosComunicacaoDTO;
import br.com.axionweb.backend.dto.datajud.DadosDataJudDTO;
import br.com.axionweb.backend.dto.datajud.EstatisticasProcessuaisDTO;
import br.com.axionweb.backend.dto.datajud.ParametrosDataJudDTO;
import br.com.axionweb.backend.dto.datajud.TempoEstatisticaDTO;
import br.com.axionweb.backend.model.comunicacao.Comunicacao;
import br.com.axionweb.backend.model.datajud.DataJud;
import br.com.axionweb.backend.model.datajud.TempoEstatistica;
import br.com.axionweb.backend.model.processo.Processo;
import br.com.axionweb.backend.repository.ComunicacaoRepository;
import br.com.axionweb.backend.repository.DataJudRepository;
import br.com.axionweb.backend.exception.ConsultaExternaException;
import br.com.axionweb.backend.exception.EstatisticasSemProcessosException;
import br.com.axionweb.backend.dto.datajud.GerarEstatisticasRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import java.util.List;

@Service
public class AnaliseService {
    private static final Logger log = LoggerFactory.getLogger(AnaliseService.class);

    @Autowired
    DataJudRepository dataJudRepository;

    @Autowired
    ComunicacaoRepository comunicacaoRepository;

    @Autowired
    ProcessoService processoService;

    @Autowired
    ComunicacaoService comunicacaoService;

    @Autowired
    DataJudService dataJudService;

    @Autowired
    EstatisticaProcessualService estatisticaProcessualService;

    @Autowired
    org.springframework.transaction.PlatformTransactionManager transacoes;

    public String analisar(Long id) {

        Processo processo = processoService.buscarPorId(id);


        // 1. Monta parâmetros DataJud
        ParametrosDataJudDTO parametrosDataJud =
                dataJudService.montarParametroDataJud(processo);


        // 2. Consulta DataJud
        List<DadosDataJudDTO> dadosDataJud;
        try {
            dadosDataJud = dataJudService.buscarProcessos(parametrosDataJud);
        } catch (RestClientException erro) {
            throw new ConsultaExternaException(ConsultaExternaException.Servico.DATAJUD, erro);
        }
        log.info("Consulta DataJud do processo {} retornou {} processos", id, dadosDataJud.size());


        // 3. Calcula estatísticas
        EstatisticasProcessuaisDTO estatisticas =
                estatisticaProcessualService.calcular(dadosDataJud);


        // 4. Converte DTO -> entidade
        DataJud entidadeDataJud = converterEstatisticas(processo, estatisticas, parametrosDataJud);
        // Comunicação CNJ
        ParametrosComunicacaoDTO parametrosComunicacao = comunicacaoService.montarParametroComunicacao(processo);
        List<DadosComunicacaoDTO> dadosComunicacaoDTO;
        try {
            dadosComunicacaoDTO = comunicacaoService.consumirApiComunicacao(parametrosComunicacao);
        } catch (RestClientException erro) {
            throw new ConsultaExternaException(ConsultaExternaException.Servico.DJEN, erro);
        }

        log.info("Consulta DJEN do processo {} retornou {} comunicações", id, dadosComunicacaoDTO.size());
        List<Comunicacao> comunicacoes = dadosComunicacaoDTO.stream()
                .map(dto -> {

                    Comunicacao comunicacao = new Comunicacao();
                    comunicacao.setProcesso(processo);

                    comunicacao.setCodigoClasse(dto.getCodigoClasse());
                    comunicacao.setDataDisponibilizacao(dto.getData_disponibilizacao());
                    comunicacao.setDataDisponibilizacaoFormatada(dto.getDatadisponibilizacao());
                    comunicacao.setIdOrgao(dto.getIdOrgao());
                    comunicacao.setMeio(dto.getMeio());
                    comunicacao.setMeioCompleto(dto.getMeiocompleto());
                    comunicacao.setSiglaTribunal(dto.getSiglaTribunal());
                    comunicacao.setStatus(dto.getStatus());
                    comunicacao.setTexto(dto.getTexto());
                    comunicacao.setTipoComunicacao(dto.getTipoComunicacao());
                    comunicacao.setTipoDocumento(dto.getTipoDocumento());

                    return comunicacao;
                })
                .toList();

        // Uma transação curta somente para a gravação; HTTP não ocupa conexões do banco.
        new org.springframework.transaction.support.TransactionTemplate(transacoes).executeWithoutResult(tx -> {
            var atual = processoService.bloquearParaAtualizarAnalise(processo.getId());
            comunicacaoRepository.deleteByProcessoId(processo.getId());
            entidadeDataJud.setProcesso(atual);
            comunicacoes.forEach(c -> c.setProcesso(atual));
            dataJudRepository.save(entidadeDataJud);
            comunicacaoRepository.saveAllAndFlush(comunicacoes);
            atual.registrarAtividade("analise", "Análise DataJud e consulta de comunicações concluídas");
        });

        // Contexto


        // LLM


        return "200";
    }

    public List<DataJudResponseDTO> gerarEstatisticas(Long id,
            GerarEstatisticasRequestDTO filtros) {
        Processo processo = processoService.buscarPorId(id);
        var parametros = dataJudService.montarParametroDataJud(processo);
        parametros.setDataInicio(filtros.dataInicio());
        parametros.setDataFim(filtros.dataFim());
        log.info("Geração DataJud iniciada: processoId={}, tribunal={}, inicio={}, fim={}, limite={}",
                id, parametros.getTribunal(), filtros.dataInicio(), filtros.dataFim(), parametros.getQuantidadeResultados());
        List<DadosDataJudDTO> dados;
        try {
            dados = dataJudService.buscarProcessos(parametros);
        } catch (RestClientException erro) {
            throw new ConsultaExternaException(ConsultaExternaException.Servico.DATAJUD, erro);
        }
        log.info("Consulta DataJud concluída: processoId={}, registros={}", id, dados.size());
        return new org.springframework.transaction.support.TransactionTemplate(transacoes)
                .execute(tx -> salvarEstatisticas(id, dados, parametros));
    }

    @Transactional
    public List<DataJudResponseDTO> salvarEstatisticas(Long id, List<DadosDataJudDTO> dados, ParametrosDataJudDTO parametros) {
        Processo processo = processoService.bloquearParaAtualizarAnalise(id);
        if (dados.isEmpty()) throw new EstatisticasSemProcessosException();
        var entidade = converterEstatisticas(processo, estatisticaProcessualService.calcular(dados), parametros);
        entidade = dataJudRepository.saveAndFlush(entidade);
        processo.registrarAtividade("analise", "Estatísticas DataJud geradas");
        log.info("Estatísticas DataJud gravadas na transação: processoId={}, estatisticaId={}, registros={}",
                id, entidade.getId(), entidade.getQuantidadeProcessos());
        return List.of(DataJudResponseDTO.de(entidade));
    }

    private DataJud converterEstatisticas(Processo processo, EstatisticasProcessuaisDTO estatisticas,
                                          ParametrosDataJudDTO parametros) {
        DataJud entidadeDataJud = new DataJud();
        entidadeDataJud.setProcesso(processo);
        entidadeDataJud.setExecutadaEm(java.time.Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS));
        entidadeDataJud.setTribunalConsulta(parametros.getTribunal());
        entidadeDataJud.setClasseConsulta(parametros.getClasseProcessual() == null ? null : parametros.getClasseProcessual().getCodigo());
        entidadeDataJud.setGrauConsulta(parametros.getGrau());
        entidadeDataJud.setLimiteConsulta(parametros.getQuantidadeResultados());
        entidadeDataJud.setDataInicio(parametros.getDataInicio());
        entidadeDataJud.setDataFim(parametros.getDataFim());

        entidadeDataJud.setQuantidadeProcessos(
                estatisticas.getQuantidadeProcessos()
        );

        entidadeDataJud.setProcessosPendentes(
                estatisticas.getProcessosPendentes()
        );

        entidadeDataJud.setTempoAtePrimeiraBaixa(
                converterTempoEstatistica(
                        estatisticas.getTempoAtePrimeiraBaixa()
                )
        );

        entidadeDataJud.setTempoAtePrimeiroJulgamento(
                converterTempoEstatistica(
                        estatisticas.getTempoAtePrimeiroJulgamento()
                )
        );
        return entidadeDataJud;
    }

    @Transactional(readOnly = true)
    public AnaliseResponseDTO consultar(Long processoId) {
        Processo processo = processoService.buscarPorId(processoId);
        return new AnaliseResponseDTO(processo.getId(),
                carregarEstatisticas(processo.getId()), carregarComunicacoes(processo.getId()));
    }

    @Transactional(readOnly = true)
    public List<DataJudResponseDTO> consultarEstatisticas(Long processoId) {
        Processo processo = processoService.buscarPorId(processoId);
        return carregarEstatisticas(processo.getId());
    }

    @Transactional(readOnly = true)
    public List<ComunicacaoSalvaDTO> consultarComunicacoes(Long processoId) {
        Processo processo = processoService.buscarPorId(processoId);
        return carregarComunicacoes(processo.getId());
    }

    private List<DataJudResponseDTO> carregarEstatisticas(Long processoId) {
        return dataJudRepository.findByProcessoIdOrderByIdAsc(processoId).stream()
                .map(DataJudResponseDTO::de).toList();
    }

    private List<ComunicacaoSalvaDTO> carregarComunicacoes(Long processoId) {
        return comunicacaoRepository.findByProcessoIdOrderByIdAsc(processoId).stream()
                .map(ComunicacaoSalvaDTO::de).toList();
    }

    private TempoEstatistica converterTempoEstatistica(
            TempoEstatisticaDTO dto) {

        if (dto == null) {
            return null;
        }

        TempoEstatistica entidade = new TempoEstatistica();

        entidade.setQuantidade(dto.getQuantidade());
        entidade.setMedianaDias(dto.getMedianaDias());

        return entidade;
    }
}
