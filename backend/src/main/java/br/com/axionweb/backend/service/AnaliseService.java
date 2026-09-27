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

    @Transactional
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
        DataJud entidadeDataJud = new DataJud();
        entidadeDataJud.setProcesso(processo);

        entidadeDataJud.setQuantidadeProcessos(
                estatisticas.getQuantidadeProcessos()
        );

        entidadeDataJud.setProcessosBaixados(
                estatisticas.getProcessosBaixados()
        );

        entidadeDataJud.setProcessosPendentes(
                estatisticas.getProcessosPendentes()
        );

        entidadeDataJud.setProcessosComJulgamento(
                estatisticas.getProcessosComJulgamento()
        );

        entidadeDataJud.setTempoAtePrimeiraBaixa(
                converterTempoEstatistica(
                        estatisticas.getTempoAtePrimeiraBaixa()
                )
        );

        entidadeDataJud.setTempoPendente(
                converterTempoEstatistica(
                        estatisticas.getTempoPendente()
                )
        );

        entidadeDataJud.setTempoAtePrimeiroJulgamento(
                converterTempoEstatistica(
                        estatisticas.getTempoAtePrimeiroJulgamento()
                )
        );
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

        // Serializa a substituição por processo somente depois de consultar as APIs.
        processoService.bloquearParaAtualizarAnalise(processo.getId());
        comunicacaoRepository.deleteByProcessoId(processo.getId());
        dataJudRepository.deleteByProcessoId(processo.getId());
        dataJudRepository.flush();

        // Exclusão e gravação pertencem à mesma transação; falhas preservam o resultado anterior.
        dataJudRepository.save(entidadeDataJud);
        comunicacaoRepository.saveAllAndFlush(comunicacoes);

        // Contexto


        // LLM


        return "200";
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
        entidade.setMediaDias(dto.getMediaDias());
        entidade.setMedianaDias(dto.getMedianaDias());
        entidade.setMinimoDias(dto.getMinimoDias());
        entidade.setMaximoDias(dto.getMaximoDias());
        entidade.setAmplitudeDias(dto.getAmplitudeDias());
        entidade.setDesvioPadraoDias(dto.getDesvioPadraoDias());
        entidade.setPercentil25Dias(dto.getPercentil25Dias());
        entidade.setPercentil50Dias(dto.getPercentil50Dias());
        entidade.setPercentil75Dias(dto.getPercentil75Dias());

        return entidade;
    }
}
