package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.datajud.DadosDataJudDTO;
import br.com.axionweb.backend.dto.datajud.MovimentoDataJudDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class EstatisticaProcessualServiceTests {
    private final EstatisticaProcessualService service = new EstatisticaProcessualService();

    @Test
    void calculaMedianasParesSemContarMovimentosRepetidos() {
        var processos = List.of(0, 10, 20, 30).stream().map(dias -> {
            var data = java.time.LocalDate.of(2025, 1, 1).plusDays(dias) + "T00:00:00Z";
            return processo("20250101000000", data,
                    movimento("Provimento", data), movimento("Provimento", data),
                    movimento("Baixa Definitiva", data));
        }).toList();
        var resultado = service.calcular(processos);
        var tempo = resultado.getTempoAtePrimeiroJulgamento();
        assertThat(resultado.getQuantidadeProcessos()).isEqualTo(4);
        assertThat(resultado.getProcessosPendentes()).isZero();
        assertThat(tempo.getQuantidade()).isEqualTo(4);
        assertThat(tempo.getMedianaDias()).isEqualTo(15.0);
        assertThat(resultado.getTempoAtePrimeiraBaixa()).usingRecursiveComparison().isEqualTo(tempo);
    }

    @ParameterizedTest
    @ValueSource(strings = {"20250230000000", "20251301000000", "invalida", ""})
    void excluiAjuizamentoInvalidoSemInventarDuracao(String inicio) {
        var resultado = service.calcular(List.of(processo(inicio, "2025-03-10T00:00:00Z",
                movimento("Baixa Definitiva", "2025-03-10T00:00:00Z"))));
        assertThat(resultado.getProcessosPendentes()).isZero();
        assertThat(resultado.getTempoAtePrimeiraBaixa().getQuantidade()).isZero();
        assertThat(resultado.getTempoAtePrimeiraBaixa().getMedianaDias()).isNull();
    }

    @Test
    void rejeitaDuracaoNegativaSemReclassificarBaixaIdentificadaComoPendente() {
        var resultado = service.calcular(List.of(
                processo("20250102120000", "2025-01-02T00:00:00Z"),
                processo("20250102120000", null,
                        movimento("Baixa Definitiva", "2025-01-02T00:00:00Z"),
                        movimento("Provimento", "2025-01-02T00:00:00Z"))));
        assertThat(resultado.getProcessosPendentes()).isEqualTo(1);
        assertThat(resultado.getTempoAtePrimeiraBaixa().getQuantidade()).isZero();
        assertThat(resultado.getTempoAtePrimeiroJulgamento().getQuantidade()).isZero();
    }

    @Test
    void buscaPrimeiroEventoConsistenteMesmoComHistoricoForaDeOrdem() {
        var resultado = service.calcular(List.of(processo("20250102000000", null,
                movimento("Provimento", "2025-01-12T00:00:00Z"),
                movimento("Provimento", "2025-01-01T00:00:00Z"),
                movimento(" Provimento ", "2025-01-04T00:00:00Z"),
                movimento("Baixa Definitiva", "2025-01-01T00:00:00Z"),
                movimento("Baixa Definitiva", "2025-01-05T00:00:00Z"))));
        assertThat(resultado.getTempoAtePrimeiroJulgamento().getMedianaDias()).isEqualTo(2.0);
        assertThat(resultado.getTempoAtePrimeiraBaixa().getMedianaDias()).isEqualTo(3.0);
    }

    @Test
    void normalizaOffsetsEAceitaAjuizamentoIso() {
        var resultado = service.calcular(List.of(
                processo("2025-01-01T21:00:00-03:00", null, movimento("Provimento", "2025-01-02T21:00:00-03:00")),
                processo("20250102000000", null, movimento("Baixa Definitiva", "2025-01-03T00:00:00Z"))));
        assertThat(resultado.getTempoAtePrimeiroJulgamento().getMedianaDias()).isEqualTo(1.0);
        assertThat(resultado.getTempoAtePrimeiraBaixa().getMedianaDias()).isEqualTo(1.0);
        assertThat(resultado.getProcessosPendentes()).isEqualTo(1);
    }

    @Test
    void ordenaMovimentosPeloInstanteESuportaAmostraUnitaria() {
        var resultado = service.calcular(List.of(processo("20250102000000", null,
                movimento("Provimento", "2025-01-02T23:00:00-03:00"),
                movimento("Provimento", "2025-01-03T00:00:00Z"))));
        var tempo = resultado.getTempoAtePrimeiroJulgamento();
        assertThat(tempo.getQuantidade()).isEqualTo(1);
        assertThat(tempo.getMedianaDias()).isEqualTo(1.0);
    }

    @Test
    void ignoraRegistrosNulosEDatasDeMovimentoInvalidas() {
        var resultado = service.calcular(Arrays.asList(null,
                processo("20250101000000", "invalida", null,
                        movimento("Provimento", "2025-02-30T00:00:00Z"))));
        assertThat(resultado.getQuantidadeProcessos()).isEqualTo(1);
        assertThat(service.calcular(null).getQuantidadeProcessos()).isZero();
        assertThat(service.calcular(List.of()).getTempoAtePrimeiraBaixa().getQuantidade()).isZero();
    }

    @Test
    void calculaMedianaImparEFracionariaSemConfundirAusenciaComZero() {
        var processos = List.of(1, 20, 2).stream().map(dias -> processo("20250101000000", null,
                movimento("Provimento", java.time.LocalDate.of(2025, 1, 1).plusDays(dias) + "T00:00:00Z"),
                movimento("Baixa Definitiva", java.time.LocalDate.of(2025, 1, 1).plusDays(dias) + "T00:00:00Z"))).toList();
        var impar = service.calcular(processos);
        assertThat(impar.getTempoAtePrimeiroJulgamento().getMedianaDias()).isEqualTo(2.0);
        assertThat(impar.getTempoAtePrimeiraBaixa().getMedianaDias()).isEqualTo(2.0);
        var par = service.calcular(List.of(processos.get(0), processos.get(2)));
        assertThat(par.getTempoAtePrimeiroJulgamento().getMedianaDias()).isEqualTo(1.5);
        assertThat(par.getTempoAtePrimeiraBaixa().getMedianaDias()).isEqualTo(1.5);
        var zero = service.calcular(List.of(processo("20250101000000", null,
                movimento("Provimento", "2025-01-01T12:00:00Z"))));
        assertThat(zero.getTempoAtePrimeiroJulgamento().getMedianaDias()).isZero();
        assertThat(zero.getTempoAtePrimeiraBaixa().getMedianaDias()).isNull();
    }

    @Test
    void contaAmostraESubamostrasSeparadasDeEventosIdentificados() {
        var semMovimentos = processo(null, null);
        semMovimentos.setMovimentos(null);
        var resultado = service.calcular(List.of(
                processo("20250101000000", null, movimento("Provimento", "2025-01-11T00:00:00Z"),
                        movimento("Baixa Definitiva", "2025-01-21T00:00:00Z")),
                processo(null, null, movimento("Baixa Definitiva", "2025-01-21T00:00:00Z")),
                processo("invalida", "invalida", movimento("Provimento", "2025-01-11T00:00:00Z")),
                processo("20250101000000", null, movimento("Baixa Definitiva", "invalida")),
                semMovimentos));
        assertThat(resultado.getQuantidadeProcessos()).isEqualTo(5);
        assertThat(resultado.getProcessosPendentes()).isEqualTo(3);
        assertThat(resultado.getTempoAtePrimeiraBaixa().getQuantidade()).isEqualTo(1);
        assertThat(resultado.getTempoAtePrimeiraBaixa().getMedianaDias()).isEqualTo(20.0);
        assertThat(resultado.getTempoAtePrimeiroJulgamento().getQuantidade()).isEqualTo(1);
        assertThat(resultado.getTempoAtePrimeiroJulgamento().getMedianaDias()).isEqualTo(10.0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Provimento", "Não-Provimento", "Provimento em Parte", "Homologação de Transação"})
    void preservaMovimentosDeJulgamentoIgnorandoCaixaEEspacos(String nome) {
        var resultado = service.calcular(List.of(processo("20250101000000", null,
                movimento(" " + nome.toLowerCase(java.util.Locale.ROOT) + " ", "2025-01-11T00:00:00Z"))));
        assertThat(resultado.getTempoAtePrimeiroJulgamento().getQuantidade()).isEqualTo(1);
        assertThat(resultado.getTempoAtePrimeiroJulgamento().getMedianaDias()).isEqualTo(10.0);
        assertThat(resultado.getProcessosPendentes()).isEqualTo(1);
    }

    @Test
    void amostraVaziaTemContagensZeroEMedianasIndisponiveis() {
        var resultado = service.calcular(List.of());
        assertThat(resultado.getQuantidadeProcessos()).isZero();
        assertThat(resultado.getProcessosPendentes()).isZero();
        assertThat(resultado.getTempoAtePrimeiroJulgamento().getQuantidade()).isZero();
        assertThat(resultado.getTempoAtePrimeiroJulgamento().getMedianaDias()).isNull();
        assertThat(resultado.getTempoAtePrimeiraBaixa().getQuantidade()).isZero();
        assertThat(resultado.getTempoAtePrimeiraBaixa().getMedianaDias()).isNull();
    }

    private DadosDataJudDTO processo(String inicio, String atualizacao, MovimentoDataJudDTO... movimentos) {
        var processo = new DadosDataJudDTO();
        processo.setDataAjuizamento(inicio);
        processo.setDataHoraUltimaAtualizacao(atualizacao);
        processo.setMovimentos(Arrays.asList(movimentos));
        return processo;
    }

    private MovimentoDataJudDTO movimento(String nome, String data) {
        var movimento = new MovimentoDataJudDTO();
        movimento.setNome(nome);
        movimento.setDataHora(data);
        return movimento;
    }
}
