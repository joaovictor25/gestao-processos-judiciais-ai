package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.datajud.DataJudResponseDTO;
import br.com.axionweb.backend.model.datajud.DataJud;
import br.com.axionweb.backend.model.datajud.TempoEstatistica;
import br.com.axionweb.backend.model.processo.Processo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

class ValidadorSinteseDataJudTests {
    private static final String TEXTO = "Na amostra de 400 processos, o tempo mediano até o primeiro julgamento foi de 107 dias, enquanto a primeira baixa apresentou mediana de 162,5 dias. A baixa teve uma mediana superior, considerando conjuntos distintos de registros válidos. Em 60 processos, não foi identificada baixa definitiva. Esses resultados oferecem uma referência sobre a amostra, sem permitir prever a duração do processo analisado.";

    @Test void aceitaExemploDinamicoEFormatosBrasileiros() {
        assertThat(ValidadorSinteseDataJud.validar(TEXTO, dados(400, 162.5))).isEqualTo(TEXTO);
        var texto = TEXTO.replace("400", "1.400").replace("162,5", "1.162,5");
        assertThat(ValidadorSinteseDataJud.validar(texto, dados(1400, 1162.5))).isEqualTo(texto);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Texto curto", "\n", "999", "162,6", "-107", "107%", "média", "<p>", "taxa de sucesso", "recomenda-se recorrer", "será julgado", "permanecem pendentes", "juridicamente semelhantes", "devido à complexidade"})
    void rejeitaValoresFormatoOuAfirmacoesProibidas(String alteracao) {
        String texto = switch (alteracao) {
            case "", "Texto curto" -> alteracao;
            case "999", "-107", "107%" -> TEXTO.replace("107", alteracao);
            case "162,6" -> TEXTO.replace("162,5", alteracao);
            case "\n" -> TEXTO.replace(" A baixa", "\nA baixa");
            default -> TEXTO + " " + alteracao;
        };
        assertThatThrownBy(() -> ValidadorSinteseDataJud.validar(texto, dados(400, 162.5))).isInstanceOf(ResponseStatusException.class);
    }

    @Test void rejeitaMedianasTrocadasEDuracaoIndisponivel() {
        assertThatThrownBy(() -> ValidadorSinteseDataJud.validar(TEXTO.replace("Em 60 processos", "Em 400 processos"), dados(400, 162.5)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> ValidadorSinteseDataJud.validar(TEXTO.replace("107 dias", "162,5 dias"), dados(400, 162.5)))
                .isInstanceOf(ResponseStatusException.class);
        var dados = dados(400, 162.5);
        dados.tempoAtePrimeiraBaixa().setQuantidade(0);
        assertThatThrownBy(() -> ValidadorSinteseDataJud.validar(TEXTO, dados)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> ValidadorSinteseDataJud.validar(null, dados)).isInstanceOf(ResponseStatusException.class);
    }

    @Test void aceitaLimitacaoQuandoNaoHaDuracoesValidas() {
        var dados = dados(400, 162.5);
        dados.tempoAtePrimeiraBaixa().setQuantidade(0); dados.tempoAtePrimeiroJulgamento().setQuantidade(0);
        String texto = "A amostra reúne 400 processos, com 60 sem baixa definitiva identificada. Não há durações válidas suficientes para apresentar medianas de julgamento ou de baixa. Essa ausência limita a leitura dos tempos observados, sem permitir prever a duração de um processo individual.";
        assertThat(ValidadorSinteseDataJud.validar(texto, dados)).isEqualTo(texto);
    }

    private DataJudResponseDTO dados(int total, double medianaBaixa) {
        var entidade = new DataJud(); entidade.setProcesso(new Processo()); entidade.setQuantidadeProcessos(total); entidade.setProcessosPendentes(60);
        var j = new TempoEstatistica(); j.setQuantidade(304); j.setMedianaDias(107.0); entidade.setTempoAtePrimeiroJulgamento(j);
        var b = new TempoEstatistica(); b.setQuantidade(340); b.setMedianaDias(medianaBaixa); entidade.setTempoAtePrimeiraBaixa(b);
        return DataJudResponseDTO.de(entidade);
    }
}
