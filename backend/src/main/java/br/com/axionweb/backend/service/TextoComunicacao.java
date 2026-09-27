package br.com.axionweb.backend.service;

import org.jsoup.Jsoup;

import java.text.Normalizer;
import java.util.Locale;

/** Texto usado tanto no envio à IA quanto na conferência das citações. */
final class TextoComunicacao {
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

    private static String normalizarEspacos(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFC).replaceAll("(?U)\\s+", " ").trim();
    }
}
