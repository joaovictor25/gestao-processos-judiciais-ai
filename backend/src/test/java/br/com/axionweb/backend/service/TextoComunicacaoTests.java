package br.com.axionweb.backend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class TextoComunicacaoTests {
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Intime-se: a parte. | “Intime-se a parte”",
            "Foi determinada a apresentação do contrato. | APRESENTAÇÃO DO CONTRATO!",
            "Custas: R$ 5.000,00; prazo: 15 dias. | Custas R$ 5.000,00, prazo 15 dias",
            "Audiência em 03/10/2026. | Audiência: em 03/10/2026",
            "O pedido (principal) foi deferido. | O pedido principal foi deferido"
    })
    void aceitaDiferencasDePontuacaoSemAlterarPalavrasENumeros(String texto, String trecho) {
        assertThat(TextoComunicacao.contemCitacao(texto, trecho)).isTrue();
    }

    @Test
    void aceitaEspacosUnicodeEQuebrasDeLinhaNaCitacao() {
        assertThat(TextoComunicacao.contemCitacao("Intimação: da parte.", "Intimac\u0327a\u0303o\u00a0da\nparte"))
                .isTrue();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "Custas de R$ 500,00. | Custas: de R$ 5.000,00",
            "Custas de R$ 5.000,00. | Custas: de R$ 5,000.00",
            "Valor 1500,00. | 500,00",
            "Valor 500,00. | 500",
            "Saldo -500,00. | Saldo: 500,00",
            "Audiência em 03/10/2026. | Audiência: em 10/03/2026",
            "Prazo de 15 dias. | Prazo: de 5 dias",
            "Pedido indeferido. | deferido",
            "Pagamento não autorizado. | Pagamento: autorizado",
            "Apresentar contrato e comprovante. | Apresentar comprovante e contrato",
            "Intime-se a parte. | Notifique-se: a parte",
            "Texto da comunicação. | …!!!"
    })
    void rejeitaMudancasDeConteudoECorrespondenciasParciais(String texto, String trecho) {
        assertThat(TextoComunicacao.contemCitacao(texto, trecho)).isFalse();
    }

    @Test
    void extraiTextoVisivelSemMisturarTagsInlineOuPerderSeparacaoDeParagrafos() {
        assertThat(TextoComunicacao.paraAnalise("<p>Inde<b>ferido</b> o pedido.</p><p>Valor: R$&nbsp;10,00.</p>"))
                .isEqualTo("Indeferido o pedido. Valor: R$ 10,00.");
    }

    @Test
    void preservaNegacaoValoresEConteudoEscapadoAoConferirEvidencias() {
        String texto = TextoComunicacao.paraAnalise("<p>NÃO foi pago R$&nbsp;500,00. Símbolo: &lt;valor&gt;.</p>");
        assertThat(TextoComunicacao.paraComparacao(texto)).isEqualTo("não foi pago r$ 500,00. símbolo: <valor>.");
        assertThat(TextoComunicacao.paraComparacao(texto)).doesNotContain("r$ 5.000,00");
    }

    @Test
    void comparaAcentosUnicodeEquivalentesEEspacosInseparaveis() {
        assertThat(TextoComunicacao.paraComparacao("Intimac\u0327a\u0303o\u00a0da\u202fparte"))
                .isEqualTo(TextoComunicacao.paraComparacao("Intimação da parte"));
    }

    @Test
    void ignoraConteudoNaoVisivel() {
        assertThat(TextoComunicacao.paraAnalise("<script>conteúdo indevido</script><style>body { color: red }</style><p>&nbsp;</p>"))
                .isEmpty();
    }
}
