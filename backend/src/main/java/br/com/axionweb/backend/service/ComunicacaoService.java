package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.comunicacao.ComunicacaoResponseDTO;
import br.com.axionweb.backend.dto.comunicacao.DadosComunicacaoDTO;
import br.com.axionweb.backend.dto.comunicacao.ParametrosComunicacaoDTO;
import br.com.axionweb.backend.model.processo.Processo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class ComunicacaoService {

    @Autowired
    @Qualifier("restComunicacao")
    RestClient restComunicacao;


    // Montar os parâmetros para busca
    public ParametrosComunicacaoDTO montarParametroComunicacao(Processo processo) {

        ParametrosComunicacaoDTO parametro = new ParametrosComunicacaoDTO();

        parametro.setTexto(
                processo.getAssunto().getNome()
        );

        parametro.setSiglaTribunal(
                processo.getTribunal()
        );

        parametro.setDataDisponibilizacaoInicio(
                processo.getDataInicio()
        );

        parametro.setDataDisponibilizacaoFim(
                processo.getDataFim()
        );

        parametro.setMeio("D");
        parametro.setPagina(1);
        parametro.setItensPorPagina(200);

        return parametro;
    }


    // Consumir API do DJEN
    public List<DadosComunicacaoDTO> consumirApiComunicacao(
            ParametrosComunicacaoDTO parametro) {

        return filtrarDadosComunicacao(restComunicacao.get()
                .uri(
                        uriBuilder -> uriBuilder
                                .path("/api/v1/comunicacao")
                                .queryParam(
                                        "texto",
                                        parametro.getTexto()
                                )
                                .queryParam(
                                        "siglaTribunal",
                                        parametro.getSiglaTribunal()
                                )
                                //.queryParam(
                                //        "dataDisponibilizacaoInicio",
                                //        parametro.getDataDisponibilizacaoInicio()
                                //)
                                //.queryParam(
                                //        "dataDisponibilizacaoFim",
                                //        parametro.getDataDisponibilizacaoFim()
                                //)
                                .queryParam(
                                        "meio",
                                        parametro.getMeio()
                                )
                                .queryParam(
                                        "pagina",
                                        parametro.getPagina()
                                )
                                .queryParam(
                                        "itensPorPagina",
                                        parametro.getItensPorPagina()
                                )
                                .build()
                )
                .retrieve()
                .body(ComunicacaoResponseDTO.class));
    }


    // Filtrar os dados que serão utilizados pelo sistema
    public List<DadosComunicacaoDTO> filtrarDadosComunicacao(
            ComunicacaoResponseDTO resposta) {

        return resposta.getItems()
                .stream()
                .map(comunicacao -> {

                    DadosComunicacaoDTO dados =
                            new DadosComunicacaoDTO();

                    dados.setCodigoClasse(
                            comunicacao.getCodigoClasse()
                    );

                    dados.setData_disponibilizacao(
                            comunicacao.getDataDisponibilizacao()
                    );

                    dados.setDatadisponibilizacao(
                            comunicacao.getDatadisponibilizacao()
                    );

                    dados.setIdOrgao(
                            comunicacao.getIdOrgao()
                    );

                    dados.setMeio(
                            comunicacao.getMeio()
                    );

                    dados.setMeiocompleto(
                            comunicacao.getMeiocompleto()
                    );

                    dados.setSiglaTribunal(
                            comunicacao.getSiglaTribunal()
                    );

                    dados.setStatus(
                            comunicacao.getStatus()
                    );

                    dados.setTexto(
                            comunicacao.getTexto()
                    );

                    dados.setTipoComunicacao(
                            comunicacao.getTipoComunicacao()
                    );

                    dados.setTipoDocumento(
                            comunicacao.getTipoDocumento()
                    );

                    return dados;
                })
                .toList();
    }
}