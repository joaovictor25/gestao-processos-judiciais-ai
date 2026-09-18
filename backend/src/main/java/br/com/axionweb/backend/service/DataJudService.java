//calcular tempo médio de tramitação
//Estou estudando usar parametros apenas para calcular tempo medio de tramitação

package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.datajud.DadosDataJudDTO;
import br.com.axionweb.backend.dto.datajud.ParametrosDataJudDTO;
import br.com.axionweb.backend.dto.datajud.RespostaDataJudDTO;
import br.com.axionweb.backend.model.Processo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class DataJudService {

    @Autowired
    @Qualifier("restDataJud")
    private RestClient restDataJud;

//Analisar quais parametros usar
    public ParametrosDataJudDTO montarParametroDataJud(
            Processo processo) {

        ParametrosDataJudDTO parametro =
                new ParametrosDataJudDTO();

        parametro.setTribunal(
                processo.getTribunal()
        );

        parametro.setClasseProcessual(
                processo.getClasseProcessual()
        );

        parametro.setAssunto(
                processo.getAssunto()
        );

        parametro.setOrgaoJulgador(
                processo.getOrgaoJulgador()
        );

        parametro.setComarca(
                processo.getComarca()
        );

        parametro.setUf(
                processo.getUf()
        );

        parametro.setDataInicio(
                processo.getDataDistribuicao()
        );

        parametro.setDataFim(
                LocalDate.now()
        );

        parametro.setQuantidadeResultado(100);

        return parametro;
    }


    public RespostaDataJudDTO consumirApiDataJud(
            ParametrosDataJudDTO parametro) {

        String endpoint =
                montarEndpoint(
                        parametro.getTribunal()
                );

        Map<String, Object> body =
                montarQuery(parametro);

        return restDataJud
                .post()
                .uri(endpoint)
                .body(body)
                .retrieve()
                .body(RespostaDataJudDTO.class);
    }


    public List<DadosDataJudDTO> buscarProcessos(
            ParametrosDataJudDTO parametro) {

        RespostaDataJudDTO resposta =
                consumirApiDataJud(parametro);

        if (resposta == null ||
                resposta.getHits() == null ||
                resposta.getHits().getHits() == null) {

            return List.of();
        }

        return resposta
                .getHits()
                .getHits()
                .stream()
                .map(resultado -> resultado.getSource())
                .toList();
    }


    private Map<String, Object> montarQuery(
            ParametrosDataJudDTO parametros) {

        List<Map<String, Object>> filtros =
                new ArrayList<>();

        filtros.add(
                Map.of(
                        "match",
                        Map.of(
                                "classe.nome",
                                parametros.getClasseProcessual()
                        )
                )
        );

        filtros.add(
                Map.of(
                        "match",
                        Map.of(
                                "assuntos.nome",
                                parametros.getAssunto()
                        )
                )
        );

        if (parametros.getOrgaoJulgador() != null) {

            filtros.add(
                    Map.of(
                            "match",
                            Map.of(
                                    "orgaoJulgador.nome",
                                    parametros.getOrgaoJulgador()
                            )
                    )
            );
        }

        return Map.of(
                "size",
                parametros.getQuantidadeResultados(),

                "query",
                Map.of(
                        "bool",
                        Map.of(
                                "must",
                                filtros
                        )
                )
        );
    }


    private String montarEndpoint(String tribunal) {

        return switch (tribunal.toUpperCase()) {

            case "TJRJ" ->
                    "/api_publica_tjrj/_search";

            case "TJSP" ->
                    "/api_publica_tjsp/_search";

            case "TJMG" ->
                    "/api_publica_tjmg/_search";

            default ->
                    throw new IllegalArgumentException(
                            "Tribunal não suportado: "
                                    + tribunal
                    );
        };
    }
}