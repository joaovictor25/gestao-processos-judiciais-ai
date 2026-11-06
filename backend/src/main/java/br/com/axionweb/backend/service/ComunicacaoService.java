package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.comunicacao.ComunicacaoResponseDTO;
import br.com.axionweb.backend.dto.comunicacao.DadosComunicacaoDTO;
import br.com.axionweb.backend.dto.comunicacao.ParametrosComunicacaoDTO;
import br.com.axionweb.backend.model.processo.Processo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ComunicacaoService {

    @Autowired
    @Qualifier("restComunicacao")
    RestClient restComunicacao;


    // Montar os parâmetros para busca
    public ParametrosComunicacaoDTO montarParametroComunicacao(Processo processo) {
        return montarParametroComunicacao(processo,
                processo.getAssunto() == null ? null : processo.getAssunto().getNome(),
                processo.getDataInicio(), processo.getDataFim());
    }

    public ParametrosComunicacaoDTO montarParametroComunicacao(Processo processo, String texto,
                                                               LocalDate inicio, LocalDate fim) {

        ParametrosComunicacaoDTO parametro = new ParametrosComunicacaoDTO();

        parametro.setTexto(
                texto
        );

        parametro.setSiglaTribunal(
                processo.getTribunal()
        );

        parametro.setDataDisponibilizacaoInicio(
                inicio
        );

        parametro.setDataDisponibilizacaoFim(
                fim
        );

        parametro.setMeio("D");
        parametro.setPagina(1);
        parametro.setItensPorPagina(80);

        return parametro;
    }


    // Consumir API do DJEN
    public List<DadosComunicacaoDTO> consumirApiComunicacao(
            ParametrosComunicacaoDTO parametro) {

        return filtrarDadosComunicacao(restComunicacao.get()
                .uri(
                        uriBuilder -> uriBuilder
                                .path("/api/v1/comunicacao")
                                //Esse parametro de texto é usado para busca textual
                                .queryParamIfPresent(
                                        "texto",
                                        Optional.ofNullable(parametro.getTexto()).map(texto -> "{texto}")
                                )
                                .queryParam(
                                        "siglaTribunal",
                                        parametro.getSiglaTribunal()
                                )
                                //Esse parametro serve como recorte de tempo das comunicações. Precisa funcionar também
                                .queryParamIfPresent(
                                        "dataDisponibilizacaoInicio",
                                        Optional.ofNullable(parametro.getDataDisponibilizacaoInicio())
                                )
                                .queryParamIfPresent(
                                        "dataDisponibilizacaoFim",
                                        Optional.ofNullable(parametro.getDataDisponibilizacaoFim())
                                )
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
                                .build(Map.of("texto", parametro.getTexto() == null ? "" : parametro.getTexto()))
                )
                .retrieve()
                .body(ComunicacaoResponseDTO.class));
    }


    // Filtrar os dados que serão utilizados pelo sistema
    public List<DadosComunicacaoDTO> filtrarDadosComunicacao(
            ComunicacaoResponseDTO resposta) {

        if (resposta == null || resposta.getItems() == null) {
            throw new RestClientException("Resposta DJEN sem a lista de comunicações esperada");
        }
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
