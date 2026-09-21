package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.comunicacao.ComunicacaoResponseDTO;
import br.com.axionweb.backend.dto.comunicacao.ParametrosComunicacaoDTO;
import br.com.axionweb.backend.model.Processo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

@Service
public class ComunicacaoService {
    @Autowired
    @Qualifier("restComunicacao")
    RestClient restComunicacao;

    //Instanciando
    ParametrosComunicacaoDTO parametro = new ParametrosComunicacaoDTO();

    //Melhorar os parametros para busca
    public ParametrosComunicacaoDTO montarParametroComunicacao(Processo processo){
        parametro.setTexto(processo.getAssunto().getNome());//Em breve, Texto será setado com uma descrição do processo
        parametro.setSiglaTribunal(processo.getTribunal());
        parametro.setDataDisponibilizacaoInicio(processo.getDataInicio());
        parametro.setDataDisponibilizacaoFim(processo.getDataFim());
        parametro.setMeio("D");
        parametro.setPagina(2);
        parametro.setItensPorPagina(100);
        return parametro;
    }

    public ComunicacaoResponseDTO consumirApiComunicacao(ParametrosComunicacaoDTO parametro){
        //Analisar para o retorno da API já abstrair apenas as informações que preciso
        return restComunicacao.get()
                .uri(
                        uriBuilder -> uriBuilder
                                .path("/api/v1/comunicacao")
                                .queryParam("texto",parametro.getTexto())
                                .queryParam("siglaTribunal", parametro.getSiglaTribunal())
                                //.queryParam("dataDisponibilizacaoInicio", atualParametro.getDataDisponibilizacaoInicio())
                                //.queryParam("dataDisponibilizacaoFim", atualParametro.getDataDisponibilizacaoFim())
                                .queryParam("meio", parametro.getMeio())
                                .queryParam("pagina", parametro.getPagina())
                                .queryParam("itensPorPagina", parametro.getItensPorPagina())
                                .build()
                )
                .retrieve()
                .body(ComunicacaoResponseDTO.class);
    }
}
