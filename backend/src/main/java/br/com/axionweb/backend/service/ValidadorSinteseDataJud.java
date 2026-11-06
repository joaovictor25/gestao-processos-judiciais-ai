package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.datajud.DataJudResponseDTO;
import br.com.axionweb.backend.dto.datajud.TempoEstatisticaDTO;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Verificações básicas, sem pretender avaliar toda a semântica de linguagem natural. */
public final class ValidadorSinteseDataJud {
    private static final String NUMERO = "(?:\\d{1,3}(?:\\.\\d{3})+(?:,\\d+)?|\\d+(?:[.,]\\d+)?)";
    private static final Pattern NUMEROS = Pattern.compile("(?<![\\p{L}\\d])[-+]?" + NUMERO);
    private static final Pattern DIAS = Pattern.compile("(" + NUMERO + ")\\s*dias?\\b");
    private static final Pattern CONTAGENS = Pattern.compile("(" + NUMERO + ")\\s+(processos?|duracoes? validas?|registros? validos?)\\b");
    private static final Pattern PROIBIDO = Pattern.compile(
            "[%<>#{}\\[\\]`]|https?://|\\b(media|percentil|desvio padrao|variancia|taxa|porcentagem|percentual)\\b"
            + "|probabilidade de (exito|sucesso)|chance de (exito|sucesso)|recomenda\\w*|sugere-se"
            + "|estrategia\\w* juridica\\w*|(?:deve|deveria) (?:recorrer|ajuizar|interpor)"
            + "|(?:sera|serao|devera|deverao)\\s+(?:\\w+\\s+){0,3}(?:julgad|baixad|encerrad)\\w*"
            + "|(?:permanecem|estao|sao|continuam)\\s+(?:necessariamente\\s+)?pendentes"
            + "|(?:todos|juridicamente)\\s+(?:os processos sao\\s+)?semelhantes"
            + "|(?:causad[oa]|decorre|decorrem|em razao|devido)\\b");

    private ValidadorSinteseDataJud() {}

    public static String validar(String resposta, DataJudResponseDTO dados) {
        if (resposta == null) throw invalida();
        String texto = resposta.strip();
        int palavras = texto.split("\\s+").length;
        String normal = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        if (palavras < 25 || palavras > 110 || texto.length() > 1200 || texto.matches("(?s).*\\R.*")
                || PROIBIDO.matcher(normal).find() || normal.matches("^[-*•].*")
                || !normal.matches(".*\\b(nao|sem|limitacoes|limitacao|indisponivel|insuficientes)\\b.*")) throw invalida();
        Set<BigDecimal> permitidos = new HashSet<>();
        adicionar(permitidos, dados.quantidadeProcessos());
        adicionar(permitidos, dados.processosPendentes());
        Set<BigDecimal> medianas = new HashSet<>();
        for (TempoEstatisticaDTO tempo : new TempoEstatisticaDTO[]{dados.tempoAtePrimeiroJulgamento(), dados.tempoAtePrimeiraBaixa()}) {
            if (tempo != null) {
                adicionar(permitidos, tempo.getQuantidade());
                if (tempo.getQuantidade() != null && tempo.getQuantidade() > 0) adicionar(medianas, tempo.getMedianaDias());
            }
        }
        permitidos.addAll(medianas);
        var numeros = NUMEROS.matcher(texto);
        while (numeros.find()) if (!permitidos.contains(numero(numeros.group()))) throw invalida();
        if (Pattern.compile(NUMERO + "\\s*(meses|anos|semanas|horas)\\b").matcher(normal).find()) throw invalida();
        var contagens = CONTAGENS.matcher(normal);
        while (contagens.find()) {
            String antes = normal.substring(0, contagens.start());
            String depois = normal.substring(contagens.end());
            Number esperado = null;
            if (contagens.group(2).startsWith("processo")) {
                if (depois.matches("(?s)^\\s*(?:,\\s*)?(?:sem baixa|nao (?:foi|foram) identificad[ao]s? baixa).*")
                        || depois.matches("(?s)^\\s*(?:,\\s*)?sem baixa.*")) esperado = dados.processosPendentes();
                else if (antes.matches("(?s).*amostra\\s+(?:de|com|contem|reune)\\s*$")) esperado = dados.quantidadeProcessos();
            } else {
                int julgamento = antes.lastIndexOf("julgamento");
                int baixa = antes.lastIndexOf("baixa");
                int marco = Math.max(julgamento, baixa);
                if (marco >= 0 && contagens.start() - marco < 100) {
                    var tempo = julgamento > baixa ? dados.tempoAtePrimeiroJulgamento() : dados.tempoAtePrimeiraBaixa();
                    esperado = tempo == null ? null : tempo.getQuantidade();
                }
            }
            if (esperado != null && numero(contagens.group(1)).compareTo(new BigDecimal(esperado.toString())) != 0) throw invalida();
        }
        var dias = DIAS.matcher(normal);
        while (dias.find()) {
            BigDecimal valor = numero(dias.group(1));
            if (!medianas.contains(valor)) throw invalida();
            // Confere a associação explícita ao marco mais próximo antes do número.
            String antes = normal.substring(0, dias.start());
            int julgamento = antes.lastIndexOf("julgamento");
            int baixa = antes.lastIndexOf("baixa");
            int marco = Math.max(julgamento, baixa);
            if (marco >= 0 && dias.start() - marco < 80 && !antes.substring(marco).contains(" dias")) {
                var tempo = julgamento > baixa ? dados.tempoAtePrimeiroJulgamento() : dados.tempoAtePrimeiraBaixa();
                if (tempo == null || tempo.getQuantidade() == null || tempo.getQuantidade() <= 0
                        || tempo.getMedianaDias() == null || valor.compareTo(BigDecimal.valueOf(tempo.getMedianaDias())) != 0)
                    throw invalida();
            }
        }
        return texto;
    }

    private static BigDecimal numero(String texto) {
        String valor = texto.contains(",") ? texto.replace(".", "").replace(',', '.')
                : texto.matches("[-+]?\\d{1,3}(?:\\.\\d{3})+") ? texto.replace(".", "") : texto;
        return new BigDecimal(valor).stripTrailingZeros();
    }

    private static void adicionar(Set<BigDecimal> valores, Number numero) {
        if (numero != null) valores.add(new BigDecimal(numero.toString()).stripTrailingZeros());
    }

    private static ResponseStatusException invalida() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "A IA retornou uma síntese fora do formato ou inconsistente com os indicadores. Os dados salvos foram preservados. Tente novamente.");
    }
}
