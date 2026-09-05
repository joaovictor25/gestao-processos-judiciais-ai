package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.comunicacao.ParametrosComunicacaoDTO;
import br.com.axionweb.backend.model.Processo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

@Service
public class ComunicacaoService {
    @Autowired
    @Qualifier("restComunicacao")
    RestClient restComunicacao;

    //Instanciando
    ParametrosComunicacaoDTO parametro = new ParametrosComunicacaoDTO();

    //Melhorar os parametros para busca
    public ParametrosComunicacaoDTO montarParametroComunicacao(Processo processo){
        parametro.setNumeroProcesso(processo.getNumeroProcesso());
        parametro.setSiglaTribunal(processo.getTribunal());
        parametro.setDataDisponibilizacaoInicio(processo.getDataDistribuicao());
        parametro.setDataDisponibilizacaoFim(LocalDate.now());
        parametro.setMeio("D");
        parametro.setPagina(2);
        parametro.setPagina(100);
        return parametro;
    }

    public String consumirApiComunicacao(ParametrosComunicacaoDTO atualParametro){
        //Analisar para o retorno da API já abstrair apenas as informações que preciso
        return restComunicacao.get()
                .uri(
                        uriBuilder -> uriBuilder
                                .path("/api/v1/comunicacao")
                                //.queryParam("numeroProcesso", atualParametro.getNumeroProcesso())
                                .queryParam("siglaTribunal", atualParametro.getSiglaTribunal())
                                .queryParam("dataDisponibilizacaoInicio", atualParametro.getDataDisponibilizacaoInicio())
                                .queryParam("dataDisponibilizacaoFim", atualParametro.getDataDisponibilizacaoFim())
                                .queryParam("meio", atualParametro.getMeio())
                                .queryParam("pagina", atualParametro.getPagina())
                                .queryParam("itensPorPagina", atualParametro.getItensPorPagina())
                                .build()
                )
                .retrieve()
                .body(String.class);
    }
}
