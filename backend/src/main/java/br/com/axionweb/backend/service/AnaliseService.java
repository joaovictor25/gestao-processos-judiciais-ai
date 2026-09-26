package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.datajud.DadosDataJudDTO;
import br.com.axionweb.backend.dto.datajud.EstatisticasProcessuaisDTO;
import br.com.axionweb.backend.dto.datajud.ParametrosDataJudDTO;
import br.com.axionweb.backend.dto.datajud.TempoEstatisticaDTO;
import br.com.axionweb.backend.model.datajud.DataJud;
import br.com.axionweb.backend.model.datajud.TempoEstatistica;
import br.com.axionweb.backend.model.processo.Processo;
import br.com.axionweb.backend.repository.DataJudRepository;
import br.com.axionweb.backend.repository.ProcessoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AnaliseService {

    @Autowired
    DataJudRepository dataJudRepository;

    private final ProcessoRepository processoRepository;
    private final ComunicacaoService comunicacaoService;
    private final DataJudService dataJudService;
    private final EstatisticaProcessualService estatisticaProcessualService;

    public AnaliseService(
            ProcessoRepository processoRepository,
            ComunicacaoService comunicacaoService,
            DataJudService dataJudService,
            EstatisticaProcessualService estatisticaProcessualService) {

        this.processoRepository = processoRepository;
        this.comunicacaoService = comunicacaoService;
        this.dataJudService = dataJudService;
        this.estatisticaProcessualService = estatisticaProcessualService;
    }


    public String analisar(Long id) {

        Processo processo = processoRepository
                .findById(id)
                .orElseThrow(
                        () -> new RuntimeException("Processo não encontrado")
                );


        // 1. Monta parâmetros DataJud
        ParametrosDataJudDTO parametrosDataJud =
                dataJudService.montarParametroDataJud(processo);


        // 2. Consulta DataJud
        List<DadosDataJudDTO> dadosDataJud =
                dataJudService.buscarProcessos(parametrosDataJud);


        // 3. Calcula estatísticas
        EstatisticasProcessuaisDTO estatisticas =
                estatisticaProcessualService.calcular(dadosDataJud);


        // 4. Converte DTO -> entidade
        DataJud entidadeDataJud = new DataJud();

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
        dataJudRepository.saveAndFlush(entidadeDataJud);

        // Comunicação CNJ
//        ParametrosComunicacaoDTO parametrosComunicacao = comunicacaoService.montarParametroComunicacao(processo);
//        List<DadosComunicacaoDTO> dadosComunicacao = comunicacaoService.consumirApiComunicacao(parametrosComunicacao);


        // Contexto


        // LLM


        return "200";
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
