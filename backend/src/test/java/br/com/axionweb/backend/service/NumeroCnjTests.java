package br.com.axionweb.backend.service;

import br.com.axionweb.backend.exception.ConsultaCnjException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import static org.assertj.core.api.Assertions.*;

class NumeroCnjTests {
    @Test
    void aceitaExemploOficialComOuSemMascaraEPreservaZeros() {
        // Exemplo 1 da documentação oficial da API pública do DataJud.
        var numero = new NumeroCnj(" 0000832-35.2018.4.01.3202 ");
        assertThat(numero.digitos()).isEqualTo("00008323520184013202");
        assertThat(numero.formatado()).isEqualTo("0000832-35.2018.4.01.3202");
        assertThat(numero.tribunalOrigem()).isEqualTo("TRF1");
        assertThat(new NumeroCnj(numero.digitos())).isEqualTo(numero);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"0000832", "0000832-36.2018.4.01.3202", "00000000000000000000", "00008323520184013202abc", "000083235201840132020"})
    void rejeitaNumeroIncompletoDigitoIncorretoOuTexto(String valor) {
        assertThatThrownBy(() -> new NumeroCnj(valor)).isInstanceOfSatisfying(ConsultaCnjException.class,
                erro -> assertThat(erro.getCodigo()).isEqualTo("CNJ_INVALIDO"));
    }

    @ParameterizedTest
    @CsvSource({"8,3,TJAP", "8,4,TJAM", "8,7,TJDFT", "8,11,TJMT", "8,12,TJMS", "8,13,TJMG", "8,16,TJPR", "8,26,TJSP",
            "4,6,TRF6", "5,0,TST", "5,24,TRT24", "6,0,TSE", "6,7,TRE-DFT", "6,27,TRE-TO", "3,0,STJ",
            "7,0,STM", "7,12,STM", "9,13,TJMMG", "9,21,TJMRS", "9,26,TJM-SP"})
    void identificaTribunalDeOrigemPelaNumeracaoOficial(int segmento, int tribunal, String sigla) {
        assertThat(new NumeroCnj(numeroPara(segmento, tribunal)).tribunalOrigem()).isEqualTo(sigla);
    }

    @ParameterizedTest
    @CsvSource({"1,0", "2,0", "4,90", "5,90", "8,0", "8,28", "9,16"})
    void recusaTribunaisSemEndpointSuportado(int segmento, int tribunal) {
        assertThatThrownBy(() -> new NumeroCnj(numeroPara(segmento, tribunal)).tribunalOrigem())
                .isInstanceOfSatisfying(ConsultaCnjException.class,
                        erro -> assertThat(erro.getCodigo()).isEqualTo("CNJ_TRIBUNAL_NAO_SUPORTADO"));
    }

    // Gera somente as fixtures de roteamento; a validação de DV usa o exemplo oficial acima.
    private String numeroPara(int segmento, int tribunal) {
        String base = "0000001" + "2025" + segmento + "%02d".formatted(tribunal) + "0001";
        int digito = 98 - new BigInteger(base + "00").mod(BigInteger.valueOf(97)).intValue();
        return base.substring(0, 7) + "%02d".formatted(digito) + base.substring(7);
    }
}
