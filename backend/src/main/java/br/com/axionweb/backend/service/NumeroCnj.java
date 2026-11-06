package br.com.axionweb.backend.service;

import br.com.axionweb.backend.exception.ConsultaCnjException;
import java.math.BigInteger;
import java.util.List;
import static br.com.axionweb.backend.exception.ConsultaCnjException.Motivo.*;

/** Numeração e tribunais de origem conforme a Resolução CNJ 65/2008, art. 1º e Anexo VIII. */
public record NumeroCnj(String digitos) {
    private static final List<String> UFS_CNJ = List.of("AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO", "MA", "MT", "MS", "MG",
            "PA", "PB", "PR", "PE", "PI", "RJ", "RN", "RS", "RO", "RR", "SC", "SE", "SP", "TO");

    public NumeroCnj {
        if (digitos == null) throw new ConsultaCnjException(CNJ_INVALIDO);
        digitos = digitos.replaceAll("[.\\-\\s]", "");
        if (!digitos.matches("[0-9]{20}")) throw new ConsultaCnjException(CNJ_INVALIDO);
        String verificacao = digitos.substring(0, 7) + digitos.substring(9) + digitos.substring(7, 9);
        if (!new BigInteger(verificacao).mod(BigInteger.valueOf(97)).equals(BigInteger.ONE)) {
            throw new ConsultaCnjException(CNJ_INVALIDO);
        }
    }

    public String formatado() {
        return digitos.substring(0, 7) + "-" + digitos.substring(7, 9) + "." + digitos.substring(9, 13) + "."
                + digitos.charAt(13) + "." + digitos.substring(14, 16) + "." + digitos.substring(16);
    }

    public String tribunalOrigem() {
        int tr = Integer.parseInt(digitos.substring(14, 16));
        return switch (digitos.charAt(13)) {
            case '3' -> tr == 0 ? "STJ" : naoSuportado();
            case '4' -> tr >= 1 && tr <= 6 ? "TRF" + tr : naoSuportado();
            case '5' -> tr == 0 ? "TST" : tr >= 1 && tr <= 24 ? "TRT" + tr : naoSuportado();
            case '6' -> tr == 0 ? "TSE" : "TRE-" + uf(tr);
            case '7' -> tr <= 12 ? "STM" : naoSuportado();
            case '8' -> "TJ" + uf(tr);
            case '9' -> switch (tr) { case 13 -> "TJMMG"; case 21 -> "TJMRS"; case 26 -> "TJM-SP"; default -> naoSuportado(); };
            default -> naoSuportado();
        };
    }

    private static String uf(int codigo) {
        if (codigo < 1 || codigo > UFS_CNJ.size()) return naoSuportado();
        String uf = UFS_CNJ.get(codigo - 1);
        return uf.equals("DF") ? "DFT" : uf;
    }
    private static String naoSuportado() { throw new ConsultaCnjException(CNJ_TRIBUNAL_NAO_SUPORTADO); }
}
