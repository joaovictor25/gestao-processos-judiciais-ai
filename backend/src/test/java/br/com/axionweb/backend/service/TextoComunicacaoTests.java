package br.com.axionweb.backend.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextoComunicacaoTests {
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
