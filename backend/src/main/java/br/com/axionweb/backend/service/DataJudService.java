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

        parametro.setGrau(processo.getGrau());

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

        filtros.add(
                Map.of(
                        "match",
                        Map.of(
                                "grau",
                                parametros.getGrau()
                        )
                )
        );

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

            // Tribunais Superiores
            case "TST" ->
                    "/api_publica_tst/_search";

            case "TSE" ->
                    "/api_publica_tse/_search";

            case "STJ" ->
                    "/api_publica_stj/_search";

            case "STM" ->
                    "/api_publica_stm/_search";


            // Tribunais Regionais Federais
            case "TRF1" ->
                    "/api_publica_trf1/_search";

            case "TRF2" ->
                    "/api_publica_trf2/_search";

            case "TRF3" ->
                    "/api_publica_trf3/_search";

            case "TRF4" ->
                    "/api_publica_trf4/_search";

            case "TRF5" ->
                    "/api_publica_trf5/_search";

            case "TRF6" ->
                    "/api_publica_trf6/_search";


            // Tribunais de Justiça Estaduais
            case "TJAC" ->
                    "/api_publica_tjac/_search";

            case "TJAL" ->
                    "/api_publica_tjal/_search";

            case "TJAM" ->
                    "/api_publica_tjam/_search";

            case "TJAP" ->
                    "/api_publica_tjap/_search";

            case "TJBA" ->
                    "/api_publica_tjba/_search";

            case "TJCE" ->
                    "/api_publica_tjce/_search";

            case "TJDFT" ->
                    "/api_publica_tjdft/_search";

            case "TJES" ->
                    "/api_publica_tjes/_search";

            case "TJGO" ->
                    "/api_publica_tjgo/_search";

            case "TJMA" ->
                    "/api_publica_tjma/_search";

            case "TJMG" ->
                    "/api_publica_tjmg/_search";

            case "TJMS" ->
                    "/api_publica_tjms/_search";

            case "TJMT" ->
                    "/api_publica_tjmt/_search";

            case "TJPA" ->
                    "/api_publica_tjpa/_search";

            case "TJPB" ->
                    "/api_publica_tjpb/_search";

            case "TJPE" ->
                    "/api_publica_tjpe/_search";

            case "TJPI" ->
                    "/api_publica_tjpi/_search";

            case "TJPR" ->
                    "/api_publica_tjpr/_search";

            case "TJRJ" ->
                    "/api_publica_tjrj/_search";

            case "TJRN" ->
                    "/api_publica_tjrn/_search";

            case "TJRO" ->
                    "/api_publica_tjro/_search";

            case "TJRR" ->
                    "/api_publica_tjrr/_search";

            case "TJRS" ->
                    "/api_publica_tjrs/_search";

            case "TJSC" ->
                    "/api_publica_tjsc/_search";

            case "TJSE" ->
                    "/api_publica_tjse/_search";

            case "TJSP" ->
                    "/api_publica_tjsp/_search";

            case "TJTO" ->
                    "/api_publica_tjto/_search";


            // Tribunais Regionais do Trabalho
            case "TRT1" ->
                    "/api_publica_trt1/_search";

            case "TRT2" ->
                    "/api_publica_trt2/_search";

            case "TRT3" ->
                    "/api_publica_trt3/_search";

            case "TRT4" ->
                    "/api_publica_trt4/_search";

            case "TRT5" ->
                    "/api_publica_trt5/_search";

            case "TRT6" ->
                    "/api_publica_trt6/_search";

            case "TRT7" ->
                    "/api_publica_trt7/_search";

            case "TRT8" ->
                    "/api_publica_trt8/_search";

            case "TRT9" ->
                    "/api_publica_trt9/_search";

            case "TRT10" ->
                    "/api_publica_trt10/_search";

            case "TRT11" ->
                    "/api_publica_trt11/_search";

            case "TRT12" ->
                    "/api_publica_trt12/_search";

            case "TRT13" ->
                    "/api_publica_trt13/_search";

            case "TRT14" ->
                    "/api_publica_trt14/_search";

            case "TRT15" ->
                    "/api_publica_trt15/_search";

            case "TRT16" ->
                    "/api_publica_trt16/_search";

            case "TRT17" ->
                    "/api_publica_trt17/_search";

            case "TRT18" ->
                    "/api_publica_trt18/_search";

            case "TRT19" ->
                    "/api_publica_trt19/_search";

            case "TRT20" ->
                    "/api_publica_trt20/_search";

            case "TRT21" ->
                    "/api_publica_trt21/_search";

            case "TRT22" ->
                    "/api_publica_trt22/_search";

            case "TRT23" ->
                    "/api_publica_trt23/_search";

            case "TRT24" ->
                    "/api_publica_trt24/_search";


            // Tribunais de Justiça Militar
            case "TJMMG" ->
                    "/api_publica_tjmmg/_search";

            case "TJMRS" ->
                    "/api_publica_tjmrs/_search";

            case "TJM-SP" ->
                    "/api_publica_tjmsp/_search";


            // Tribunais Regionais Eleitorais
            case "TRE-AC" ->
                    "/api_publica_tre-ac/_search";

            case "TRE-AL" ->
                    "/api_publica_tre-al/_search";

            case "TRE-AM" ->
                    "/api_publica_tre-am/_search";

            case "TRE-AP" ->
                    "/api_publica_tre-ap/_search";

            case "TRE-BA" ->
                    "/api_publica_tre-ba/_search";

            case "TRE-CE" ->
                    "/api_publica_tre-ce/_search";

            case "TRE-DFT" ->
                    "/api_publica_tre-dft/_search";

            case "TRE-ES" ->
                    "/api_publica_tre-es/_search";

            case "TRE-GO" ->
                    "/api_publica_tre-go/_search";

            case "TRE-MA" ->
                    "/api_publica_tre-ma/_search";

            case "TRE-MG" ->
                    "/api_publica_tre-mg/_search";

            case "TRE-MS" ->
                    "/api_publica_tre-ms/_search";

            case "TRE-MT" ->
                    "/api_publica_tre-mt/_search";

            case "TRE-PA" ->
                    "/api_publica_tre-pa/_search";

            case "TRE-PB" ->
                    "/api_publica_tre-pb/_search";

            case "TRE-PE" ->
                    "/api_publica_tre-pe/_search";

            case "TRE-PI" ->
                    "/api_publica_tre-pi/_search";

            case "TRE-PR" ->
                    "/api_publica_tre-pr/_search";

            case "TRE-RJ" ->
                    "/api_publica_tre-rj/_search";

            case "TRE-RN" ->
                    "/api_publica_tre-rn/_search";

            case "TRE-RO" ->
                    "/api_publica_tre-ro/_search";

            case "TRE-RR" ->
                    "/api_publica_tre-rr/_search";

            case "TRE-RS" ->
                    "/api_publica_tre-rs/_search";

            case "TRE-SC" ->
                    "/api_publica_tre-sc/_search";

            case "TRE-SE" ->
                    "/api_publica_tre-se/_search";

            case "TRE-SP" ->
                    "/api_publica_tre-sp/_search";

            case "TRE-TO" ->
                    "/api_publica_tre-to/_search";


            default ->
                    throw new IllegalArgumentException(
                            "Tribunal não suportado: " + tribunal
                    );
        };
    }
}