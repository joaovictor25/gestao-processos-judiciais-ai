package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.datajud.DadosDataJudDTO;
import br.com.axionweb.backend.dto.datajud.EstatisticasProcessuaisDTO;
import br.com.axionweb.backend.dto.datajud.MovimentoDataJudDTO;
import br.com.axionweb.backend.dto.datajud.TempoEstatisticaDTO;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class EstatisticaProcessualService {
    private static final DateTimeFormatter FORMATTER_DATA_AJUIZAMENTO =
            DateTimeFormatter.ofPattern("uuuuMMddHHmmss").withResolverStyle(ResolverStyle.STRICT);
    private static final Set<String> MOVIMENTOS_JULGAMENTO = Set.of(
            "Provimento", "Não-Provimento", "Provimento em Parte", "Homologação de Transação");
    private static final Set<String> MOVIMENTOS_BAIXA = Set.of("Baixa Definitiva");

    public EstatisticasProcessuaisDTO calcular(List<DadosDataJudDTO> processos) {
        var amostra = processos == null ? List.<DadosDataJudDTO>of()
                : processos.stream().filter(Objects::nonNull).toList();
        List<Long> temposAteBaixa = new ArrayList<>();
        List<Long> temposAteJulgamento = new ArrayList<>();
        int pendentes = 0;

        for (var processo : amostra) {
            var inicio = converterDataAjuizamentoSeguro(processo);
            // Identificar a baixa não depende de conseguir calcular sua duração.
            // Como antes, o movimento precisa de nome reconhecido e data válida.
            var baixaIdentificada = encontrarPrimeiroEvento(processo, MOVIMENTOS_BAIXA, Optional.empty());
            if (baixaIdentificada.isEmpty()) pendentes++;

            if (inicio.isPresent()) {
                // Filtrar antes de escolher o evento preserva eventos posteriores válidos
                // quando o histórico contém movimentos anteriores ao ajuizamento.
                encontrarPrimeiroEvento(processo, MOVIMENTOS_BAIXA, inicio)
                        .map(evento -> ChronoUnit.DAYS.between(inicio.get(), evento))
                        .ifPresent(temposAteBaixa::add);
                encontrarPrimeiroEvento(processo, MOVIMENTOS_JULGAMENTO, inicio)
                        .map(evento -> ChronoUnit.DAYS.between(inicio.get(), evento))
                        .ifPresent(temposAteJulgamento::add);
            }
        }

        var resultado = new EstatisticasProcessuaisDTO();
        resultado.setQuantidadeProcessos(amostra.size());
        resultado.setProcessosPendentes(pendentes);
        resultado.setTempoAtePrimeiraBaixa(calcularMediana(temposAteBaixa));
        resultado.setTempoAtePrimeiroJulgamento(calcularMediana(temposAteJulgamento));
        return resultado;
    }

    private Optional<LocalDateTime> encontrarPrimeiroEvento(DadosDataJudDTO processo,
            Set<String> nomes, Optional<LocalDateTime> inicio) {
        if (processo.getMovimentos() == null) return Optional.empty();
        return processo.getMovimentos().stream()
                .filter(Objects::nonNull)
                .filter(movimento -> movimento.getNome() != null && nomes.stream()
                        .anyMatch(nome -> nome.equalsIgnoreCase(movimento.getNome().trim())))
                .map(this::converterDataMovimentoSeguro)
                .flatMap(Optional::stream)
                .filter(data -> inicio.isEmpty() || !data.isBefore(inicio.get()))
                .min(LocalDateTime::compareTo);
    }

    private TempoEstatisticaDTO calcularMediana(List<Long> valores) {
        var resultado = new TempoEstatisticaDTO();
        resultado.setQuantidade(valores.size());
        if (valores.isEmpty()) return resultado;

        var ordenados = valores.stream().sorted().toList();
        int meio = ordenados.size() / 2;
        double mediana = ordenados.size() % 2 == 0
                ? ordenados.get(meio - 1) / 2.0 + ordenados.get(meio) / 2.0
                : ordenados.get(meio).doubleValue();
        resultado.setMedianaDias(mediana);
        return resultado;
    }

    private Optional<LocalDateTime> converterDataAjuizamentoSeguro(DadosDataJudDTO processo) {
        String data = processo.getDataAjuizamento();
        if (data == null || data.isBlank()) return Optional.empty();
        try {
            // Convenção existente: datas compactas sem fuso são interpretadas em UTC.
            return Optional.of(data.trim().matches("[0-9]{14}")
                    ? LocalDateTime.parse(data.trim(), FORMATTER_DATA_AJUIZAMENTO)
                    : converterDataMovimento(data));
        } catch (DateTimeParseException erro) {
            return Optional.empty();
        }
    }

    private Optional<LocalDateTime> converterDataMovimentoSeguro(MovimentoDataJudDTO movimento) {
        String data = movimento.getDataHora();
        if (data == null || data.isBlank()) return Optional.empty();
        try {
            return Optional.of(converterDataMovimento(data));
        } catch (DateTimeParseException erro) {
            return Optional.empty();
        }
    }

    private LocalDateTime converterDataMovimento(String data) {
        return OffsetDateTime.parse(data.trim()).withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime();
    }
}
