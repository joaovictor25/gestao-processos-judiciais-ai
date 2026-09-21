package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.comunicacao.ComunicacaoResponseDTO;
import br.com.axionweb.backend.dto.comunicacao.ParametrosComunicacaoDTO;
import br.com.axionweb.backend.dto.datajud.DadosDataJudDTO;
import br.com.axionweb.backend.dto.datajud.ParametrosDataJudDTO;
import br.com.axionweb.backend.model.Processo;
import br.com.axionweb.backend.repository.ProcessoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class AnaliseService {
    @Autowired
    ProcessoRepository repository;

    @Autowired
    ComunicacaoService comunicacaoService;

    @Autowired
    DataJudService dataJudService;

    public ComunicacaoResponseDTO solicitarAnalise(Long id){
        Processo processo = repository.findById(id).orElseThrow(() -> new RuntimeException("Processo não encontrado"));
        //Comunicação CNJ
/*        ParametrosComunicacaoDTO parametrosComunicacao = comunicacaoService.montarParametroComunicacao(processo);
        String resultado = comunicacaoService.consumirApiComunicacao(parametrosComunicacao);*/

        /*//DataJud
        ParametrosDataJudDTO parametrosDataJud = dataJudService.montarParametroDataJud(processo);0
        DadosDataJudDTO  = dataJudService.consumirApiDataJud(parametrosDataJud);*/ //Converter Objeto em String

        //DataJud
        ParametrosDataJudDTO parametrosDataJud = dataJudService.montarParametroDataJud(processo);
        List<DadosDataJudDTO>  dadosDataJudDTO = dataJudService.buscarProcessos(parametrosDataJud);


        //Comunicação CNJ
        ParametrosComunicacaoDTO parametrosComunicacao = comunicacaoService.montarParametroComunicacao(processo);
        ComunicacaoResponseDTO comunicacaoResponseDTO = comunicacaoService.consumirApiComunicacao(parametrosComunicacao);


//        List<String> numerosProcessos = dadosDataJudDTO.stream().map(DadosDataJudDTO::getNumeroProcesso).toList();
//        System.out.println(numerosProcessos);
//        String resultado = comunicacaoService.consumirApiComunicacao(numerosProcessos);



        //Contexto


        //LLM


        //String resultado = consumirApiComunicacao(id);


        return comunicacaoResponseDTO;
    }
}
