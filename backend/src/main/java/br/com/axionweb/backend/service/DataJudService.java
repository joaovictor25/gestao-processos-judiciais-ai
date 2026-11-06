package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.datajud.DadosDataJudDTO;
import br.com.axionweb.backend.dto.datajud.ParametrosDataJudDTO;
import br.com.axionweb.backend.dto.datajud.ParametroConsultaDataJudDTO;
import br.com.axionweb.backend.dto.datajud.RespostaDataJudDTO;
import br.com.axionweb.backend.model.processo.Processo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

@Service
public class DataJudService {
    private static final DateTimeFormatter DATA_COMPACTA = DateTimeFormatter.ofPattern("uuuuMMddHHmmss")
            .withResolverStyle(ResolverStyle.STRICT);
    private static final List<String> CAMPOS_ESTATISTICA = List.of(
            "numeroProcesso", "dataAjuizamento",
            "movimentos.nome", "movimentos.dataHora");

    @Autowired
    @Qualifier("restDataJud")
    private RestClient restDataJud;

    @Autowired
    private DataJudAcessoService acesso;

    public ParametrosDataJudDTO montarParametroDataJud(Processo processo) {
        var parametro = new ParametrosDataJudDTO();
        parametro.setTribunal(processo.getTribunal());
        parametro.setClasseProcessual(processo.getClasseProcessual());
        parametro.setGrau(processo.getGrau());
        // O período é definido exclusivamente pela solicitação de geração, não pelo cadastro.
        parametro.setQuantidadeResultado(400);
        return parametro;
    }

    public List<DadosDataJudDTO> buscarProcessos(ParametrosDataJudDTO parametro) {
        String endpoint = montarEndpoint(parametro.getTribunal());
        Integer limite = parametro.getQuantidadeResultados();
        if (limite == null || limite < 1 || limite > 400) throw new IllegalArgumentException("Limite DataJud deve estar entre 1 e 400");
        List<DadosDataJudDTO> processos = new ArrayList<>();
        var ids = new HashSet<String>();
        var resposta = consultar(endpoint, montarQuery(parametro, limite));
        for (var registro : resposta.getHits().getHits()) {
            if (registro.getId() != null && !ids.add(registro.getId()))
                throw new RestClientException("DataJud repetiu um registro na resposta");
            if (dentroDoPeriodo(registro.getSource(), parametro)) processos.add(registro.getSource());
        }
        // Não repõe registros descartados com novas chamadas; a amostra pode ser menor que o limite.
        return List.copyOf(processos);
    }

    public void validarParametros(ParametrosDataJudDTO parametros) { montarEndpoint(parametros.getTribunal()); }

    private RespostaDataJudDTO consultar(String endpoint, Map<String, Object> body) {
        return acesso.consultar(endpoint, body, () -> {
            var resposta = restDataJud.post().uri(endpoint).body(body).retrieve().body(RespostaDataJudDTO.class);
            validarResposta(resposta);
            if (resposta.getHits().getHits().size() > ((Number) body.get("size")).intValue())
                throw new RestClientException("DataJud excedeu o tamanho solicitado");
            var ids = new HashSet<String>();
            for (var hit : resposta.getHits().getHits()) {
                if (hit.getId() != null && !ids.add(hit.getId())) throw new RestClientException("DataJud repetiu um registro na resposta");
            }
            return resposta;
        });
    }

    public RespostaDataJudDTO buscarNumeroExato(NumeroCnj numero) {
        // Somente capa processual; não consulta processos semelhantes ou movimentações.
        Map<String, Object> body = Map.of(
                "size", 100, "track_total_hits", 100,
                "_source", List.of("numeroProcesso", "tribunal", "grau", "orgaoJulgador", "classe", "assuntos", "dataAjuizamento"),
                // Mantém o match documentado pelo CNJ, sem calcular relevância ou ordenar por data.
                "query", Map.of("bool", Map.of("filter", Map.of("match", Map.of("numeroProcesso", numero.digitos())))));
        return consultar(montarEndpoint(numero.tribunalOrigem()), body);
    }

    private void validarResposta(RespostaDataJudDTO resposta) {

        if (resposta == null ||
                resposta.getHits() == null ||
                resposta.getHits().getHits() == null) {

            throw new RestClientException("Resposta DataJud sem a estrutura de resultados esperada");
        }

        if (Boolean.TRUE.equals(resposta.getTimedOut())) {
            throw new RestClientException("DataJud retornou uma consulta incompleta por timeout");
        }

        if (resposta.getShards() != null && resposta.getShards().failed() != null && resposta.getShards().failed() > 0) {
            throw new RestClientException("DataJud retornou uma consulta incompleta por falha em shards");
        }

        if (resposta.getHits().getHits().stream().anyMatch(hit -> hit == null || hit.getSource() == null)) {
            throw new RestClientException("Resposta DataJud contém registros sem os dados do processo");
        }

    }


    private Map<String, Object> montarQuery(ParametrosDataJudDTO parametros, int tamanho) {
        Map<String, Object> query = new LinkedHashMap<>();
        query.put("size", tamanho);
        query.put("track_total_hits", false);
        query.put("_source", CAMPOS_ESTATISTICA);
        query.put("query", Map.of("bool", Map.of("filter", montarFiltros(parametros))));
        // Mantém a ordem da amostra sem percorrer páginas adicionais.
        query.put("sort", List.of(Map.of("id.keyword", "asc")));
        return query;
    }

    private List<Map<String, Object>> montarFiltros(ParametrosDataJudDTO parametros) {

        List<Map<String, Object>> filtros =
                new ArrayList<>();

        if (parametros.getClasseProcessual() != null && StringUtils.hasText(parametros.getClasseProcessual().getCodigo())) {
            filtros.add(Map.of("term", Map.of("classe.codigo", parametros.getClasseProcessual().getCodigo().trim())));
        }

        if (StringUtils.hasText(parametros.getGrau())) {
            filtros.add(
                    Map.of(
                            "term",
                            Map.of(
                                    "grau.keyword",
                                    parametros.getGrau().trim().toUpperCase(Locale.ROOT)
                            )
                    )
            );
        }


        Map<String, Object> periodoIso = new LinkedHashMap<>();
        Map<String, Object> periodoCompacto = new LinkedHashMap<>();
        if (parametros.getDataInicio() != null) {
            periodoIso.put("gte", parametros.getDataInicio() + "T00:00:00.000Z");
            periodoCompacto.put("gte", parametros.getDataInicio().format(DateTimeFormatter.BASIC_ISO_DATE) + "000000");
        }
        if (parametros.getDataFim() != null) {
            periodoIso.put("lte", parametros.getDataFim() + "T23:59:59.999Z");
            periodoCompacto.put("lte", parametros.getDataFim().format(DateTimeFormatter.BASIC_ISO_DATE) + "235959");
        }
        if (!periodoIso.isEmpty()) {
            periodoIso.put("format", "strict_date_optional_time");
            periodoIso.put("time_zone", "+00:00");
            // Há registros compactos indexados como números, além de datas ISO.
            // Não aplicar format à alternativa compacta: isso muda a interpretação do valor indexado.
            filtros.add(Map.of("bool", Map.of("minimum_should_match", 1, "should", List.of(
                    Map.of("range", Map.of("dataAjuizamento", periodoIso)),
                    Map.of("range", Map.of("dataAjuizamento", periodoCompacto))))));
        }
        return filtros;
    }

    private boolean dentroDoPeriodo(DadosDataJudDTO processo, ParametrosDataJudDTO parametros) {
        LocalDate inicio = parametros.getDataInicio(), fim = parametros.getDataFim();
        if (inicio == null && fim == null) return true;
        String texto = processo.getDataAjuizamento();
        if (!StringUtils.hasText(texto)) return false;
        LocalDate data;
        try {
            texto = texto.trim();
            if (texto.matches("\\d{14}")) data = LocalDateTime.parse(texto, DATA_COMPACTA).toLocalDate();
            else if (texto.length() == 10) data = LocalDate.parse(texto);
            else {
                try { data = OffsetDateTime.parse(texto).withOffsetSameInstant(ZoneOffset.UTC).toLocalDate(); }
                catch (DateTimeParseException semFuso) { data = LocalDateTime.parse(texto).toLocalDate(); }
            }
        } catch (DateTimeParseException invalida) { return false; }
        // Intervalos com apenas um limite podem se sobrepor entre as duas representações.
        // Confere a data da origem e continua paginando até completar a amostra válida.
        return (inicio == null || !data.isBefore(inicio)) && (fim == null || !data.isAfter(fim));
    }

    public List<ParametroConsultaDataJudDTO> descreverConsulta(Processo processo) {
        var parametros = montarParametroDataJud(processo);
        var filtros = montarFiltros(parametros);
        var resultado = new ArrayList<ParametroConsultaDataJudDTO>();
        try {
            montarEndpoint(parametros.getTribunal());
            resultado.add(new ParametroConsultaDataJudDTO("Tribunal", true,
                    parametros.getTribunal().trim().toUpperCase(Locale.ROOT), "Define o tribunal em que a busca é realizada."));
        } catch (ResponseStatusException erro) {
            resultado.add(new ParametroConsultaDataJudDTO("Tribunal", false, parametros.getTribunal(),
                    "Informe um tribunal suportado para realizar a consulta."));
        }
        resultado.add(descreverFiltro(filtros, "Classe processual", "classe"));
        resultado.add(new ParametroConsultaDataJudDTO("Assunto", false, null,
                "Não restringe a consulta. A amostra pode incluir assuntos diferentes."));
        resultado.add(new ParametroConsultaDataJudDTO("Órgão julgador", false, null,
                "Não restringe a consulta. A amostra pode incluir órgãos diferentes do mesmo tribunal."));
        resultado.add(descreverFiltro(filtros, "Grau", "grau"));
        resultado.add(new ParametroConsultaDataJudDTO("Limite de resultados", true,
                parametros.getQuantidadeResultados().toString(), "Quantidade máxima de processos retornados nesta consulta."));
        for (String nome : List.of("Comarca", "UF", "Data inicial", "Data final")) {
            resultado.add(new ParametroConsultaDataJudDTO(nome, false, null,
                    "Não aplicado pela consulta atual, mesmo quando preenchido no cadastro."));
        }
        return List.copyOf(resultado);
    }

    private ParametroConsultaDataJudDTO descreverFiltro(List<Map<String, Object>> filtros, String nome, String campo) {
        for (var filtro : filtros) {
            Object criterio = filtro.get("term");
            if (criterio instanceof Map<?, ?> campos) {
                for (var entrada : campos.entrySet()) {
                    String chave = entrada.getKey().toString();
                    if (chave.equals(campo) || chave.startsWith(campo + ".")) {
                        String descricao = chave.endsWith(".codigo") ? "Busca pelo código cadastrado."
                                : "Busca pelo grau cadastrado.";
                        return new ParametroConsultaDataJudDTO(nome, true, entrada.getValue().toString(), descricao);
                    }
                }
            }
        }
        return new ParametroConsultaDataJudDTO(nome, false, null, campo.equals("classe")
                ? "Código da classe não informado. A busca não filtra por classe nem usa o nome como alternativa."
                : "Grau não informado. A amostra pode incluir diferentes graus.");
    }

    private String montarEndpoint(String tribunal) {

        if (!StringUtils.hasText(tribunal)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o tribunal do processo antes de solicitar a análise");
        }
        return switch (tribunal.trim().toUpperCase(Locale.ROOT)) {

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
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Tribunal não suportado para consulta DataJud"
                    );
        };
    }
}
