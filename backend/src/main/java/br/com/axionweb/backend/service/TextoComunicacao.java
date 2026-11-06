package br.com.axionweb.backend.service;

import org.jsoup.Jsoup;

import java.text.Normalizer;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/** Texto usado tanto no envio à IA quanto na conferência das citações. */
final class TextoComunicacao {
    // Valores e datas ficam em um único token, preservando seus separadores e sinais.
    private static final Pattern TOKEN = Pattern.compile(
            "[+-]?\\p{N}+(?:[.,/:\\-]\\p{N}+)*|[\\p{L}\\p{M}\\p{N}]+|[^\\s]",
            Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern PONTUACAO_APRESENTACAO = Pattern.compile("[.,;:!?\"'“”‘’«»()\\[\\]{}…]");

    private TextoComunicacao() {}

    static String paraAnalise(String texto) {
        if (texto == null || texto.isBlank()) return "";
        // Apenas interpreta o HTML localmente: não carrega links, imagens ou scripts.
        var documento = Jsoup.parseBodyFragment(texto);
        documento.select("script, style, template").remove();
        return normalizarEspacos(documento.body().text());
    }

    static String paraComparacao(String texto) {
        // O texto já foi extraído do HTML. Não decodificar novamente entidades ou tags.
        return normalizarEspacos(texto).toLowerCase(Locale.ROOT);
    }

    static boolean contemCitacao(String texto, String trecho) {
        if (texto == null || trecho == null || trecho.codePoints().noneMatch(Character::isLetterOrDigit)) return false;
        List<String> citacao = tokensDaCitacao(trecho);
        // A correspondência deve conter palavras/números inteiros na mesma ordem,
        // sem pular palavras do documento nem aceitar um trecho só de pontuação.
        return !citacao.isEmpty() && Collections.indexOfSubList(tokensDaCitacao(texto), citacao) >= 0;
    }

    private static List<String> tokensDaCitacao(String texto) {
        return TOKEN.matcher(paraComparacao(texto)).results()
                .map(MatchResult::group)
                .filter(token -> !PONTUACAO_APRESENTACAO.matcher(token).matches())
                .toList();
    }

    private static String normalizarEspacos(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFC).replaceAll("(?U)\\s+", " ").trim();
    }
}
