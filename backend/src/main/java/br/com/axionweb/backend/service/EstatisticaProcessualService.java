package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.datajud.DadosDataJudDTO;
import br.com.axionweb.backend.dto.datajud.EstatisticasProcessuaisDTO;
import br.com.axionweb.backend.dto.datajud.MovimentoDataJudDTO;
import br.com.axionweb.backend.dto.datajud.TempoEstatisticaDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class EstatisticaProcessualService {

    private static final DateTimeFormatter FORMATTER_DATA_AJUIZAMENTO =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");


    /*
     * ============================================================
     * MOVIMENTOS DE JULGAMENTO
     * ============================================================
     *
     * Movimentos utilizados para identificar um resultado
     * de julgamento.
     *
     * Esta é uma regra de negócio do sistema.
     */

    private static final Set<String> MOVIMENTOS_JULGAMENTO = Set.of(
            "Provimento",
            "Não-Provimento",
            "Provimento em Parte",
            "Homologação de Transação"
    );


    /*
     * ============================================================
     * MÉTODO PRINCIPAL
     * ============================================================
     *
     * Calcula as estatísticas dos processos retornados pelo DataJud.
     *
     * São calculadas três métricas principais:
     *
     * 1. Tempo até a primeira baixa;
     * 2. Tempo dos processos pendentes;
     * 3. Tempo até o primeiro julgamento.
     *
     * O tempo pendente líquido não faz parte desta implementação.
     */

    public EstatisticasProcessuaisDTO calcular(
            List<DadosDataJudDTO> processos) {



        EstatisticasProcessuaisDTO resultado =
                new EstatisticasProcessuaisDTO();


        /*
         * Caso não existam processos.
         */

        if (processos == null || processos.isEmpty()) {

            resultado.setQuantidadeProcessos(0);
            resultado.setProcessosBaixados(0);
            resultado.setProcessosPendentes(0);
            resultado.setProcessosComJulgamento(0);

            resultado.setTempoAtePrimeiraBaixa(
                    criarEstatisticaVazia()
            );

            resultado.setTempoPendente(
                    criarEstatisticaVazia()
            );

            resultado.setTempoAtePrimeiroJulgamento(
                    criarEstatisticaVazia()
            );

            return resultado;
        }


        /*
         * Quantidade total de processos analisados.
         */

        resultado.setQuantidadeProcessos(processos.size());


        /*
         * Listas utilizadas para armazenar os tempos
         * que serão utilizados nos cálculos estatísticos.
         */

        List<Long> temposAteBaixa =
                new ArrayList<>();

        List<Long> temposPendentes =
                new ArrayList<>();

        List<Long> temposAteJulgamento =
                new ArrayList<>();


        /*
         * Contadores.
         */

        int processosBaixados = 0;

        int processosPendentes = 0;

        int processosComJulgamento = 0;


        /*
         * ============================================================
         * ANÁLISE DOS PROCESSOS
         * ============================================================
         */

        for (DadosDataJudDTO processo : processos) {


            /*
             * ========================================================
             * PRIMEIRA BAIXA
             * ========================================================
             */

            Optional<LocalDateTime> primeiraBaixa =
                    encontrarPrimeiraBaixa(processo);


            if (primeiraBaixa.isPresent()) {

                processosBaixados++;

                Optional<Long> tempo =
                        calcularDiasEntreInicioEEvento(
                                processo,
                                primeiraBaixa.get()
                        );

                tempo.ifPresent(
                        temposAteBaixa::add
                );

            } else {


                /*
                 * ====================================================
                 * PROCESSO PENDENTE
                 * ====================================================
                 *
                 * Para processos sem Baixa Definitiva,
                 * calcula-se o tempo entre a data de ajuizamento
                 * e a data da última atualização registrada no DataJud.
                 */

                processosPendentes++;

                Optional<Long> tempoPendente =
                        calcularTempoPendente(processo);

                tempoPendente.ifPresent(
                        temposPendentes::add
                );
            }


            /*
             * ========================================================
             * PRIMEIRO JULGAMENTO
             * ========================================================
             */

            Optional<LocalDateTime> primeiroJulgamento =
                    encontrarPrimeiroJulgamento(processo);


            if (primeiroJulgamento.isPresent()) {

                processosComJulgamento++;

                Optional<Long> tempo =
                        calcularDiasEntreInicioEEvento(
                                processo,
                                primeiroJulgamento.get()
                        );

                tempo.ifPresent(
                        temposAteJulgamento::add
                );
            }
        }


        /*
         * ============================================================
         * CONTADORES
         * ============================================================
         */

        resultado.setProcessosBaixados(
                processosBaixados
        );

        resultado.setProcessosPendentes(
                processosPendentes
        );

        resultado.setProcessosComJulgamento(
                processosComJulgamento
        );


        /*
         * ============================================================
         * ESTATÍSTICAS
         * ============================================================
         */

        resultado.setTempoAtePrimeiraBaixa(
                calcularEstatisticas(
                        temposAteBaixa
                )
        );

        resultado.setTempoPendente(
                calcularEstatisticas(
                        temposPendentes
                )
        );

        resultado.setTempoAtePrimeiroJulgamento(
                calcularEstatisticas(
                        temposAteJulgamento
                )
        );


        return resultado;
    }


    /*
     * ============================================================
     * PRIMEIRA BAIXA
     * ============================================================
     *
     * Localiza a primeira ocorrência de "Baixa Definitiva"
     * entre os movimentos do processo.
     */

    private Optional<LocalDateTime> encontrarPrimeiraBaixa(
            DadosDataJudDTO processo) {

        if (processo == null ||
                processo.getMovimentos() == null) {

            return Optional.empty();
        }

        return processo.getMovimentos()
                .stream()

                .filter(this::movimentoValido)

                .filter(movimento ->
                        "Baixa Definitiva".equalsIgnoreCase(
                                movimento.getNome()
                        )
                )

                .map(this::converterDataMovimentoSeguro)

                .filter(Optional::isPresent)

                .map(Optional::get)

                .min(LocalDateTime::compareTo);
    }


    /*
     * ============================================================
     * PRIMEIRO JULGAMENTO
     * ============================================================
     *
     * Localiza o primeiro movimento que representa
     * um resultado de julgamento.
     */

    private Optional<LocalDateTime> encontrarPrimeiroJulgamento(
            DadosDataJudDTO processo) {

        if (processo == null ||
                processo.getMovimentos() == null) {

            return Optional.empty();
        }

        return processo.getMovimentos()
                .stream()

                .filter(this::movimentoValido)

                .filter(movimento ->
                        MOVIMENTOS_JULGAMENTO
                                .stream()
                                .anyMatch(nome ->
                                        nome.equalsIgnoreCase(
                                                movimento.getNome()
                                        )
                                )
                )

                .map(this::converterDataMovimentoSeguro)

                .filter(Optional::isPresent)

                .map(Optional::get)

                .min(LocalDateTime::compareTo);
    }


    /*
     * ============================================================
     * TEMPO ENTRE INÍCIO E EVENTO
     * ============================================================
     *
     * Calcula a quantidade de dias entre:
     *
     * data de ajuizamento
     * +
     * data do evento
     *
     * É utilizado para:
     *
     * - tempo até a primeira baixa;
     * - tempo até o primeiro julgamento.
     */

    private Optional<Long> calcularDiasEntreInicioEEvento(
            DadosDataJudDTO processo,
            LocalDateTime evento) {

        Optional<LocalDateTime> inicio =
                converterDataAjuizamentoSeguro(processo);


        if (inicio.isEmpty() || evento == null) {

            return Optional.empty();
        }


        long dias =
                ChronoUnit.DAYS.between(
                        inicio.get(),
                        evento
                );


        /*
         * Evita valores inconsistentes quando a data
         * do evento for anterior à data de ajuizamento.
         */

        if (dias < 0) {

            return Optional.empty();
        }


        return Optional.of(dias);
    }


    /*
     * ============================================================
     * TEMPO PENDENTE
     * ============================================================
     *
     * Para um processo que ainda não possui "Baixa Definitiva",
     * calcula:
     *
     * data da última atualização
     * -
     * data de ajuizamento
     *
     * Este cálculo representa o tempo decorrido registrado
     * para os processos considerados pendentes.
     */

    private Optional<Long> calcularTempoPendente(
            DadosDataJudDTO processo) {

        Optional<LocalDateTime> inicio =
                converterDataAjuizamentoSeguro(processo);

        Optional<LocalDateTime> referencia =
                converterDataUltimaAtualizacaoSeguro(processo);


        if (inicio.isEmpty() ||
                referencia.isEmpty()) {

            return Optional.empty();
        }


        long dias =
                ChronoUnit.DAYS.between(
                        inicio.get(),
                        referencia.get()
                );


        /*
         * Evita valores inconsistentes.
         */

        if (dias < 0) {

            return Optional.empty();
        }


        return Optional.of(dias);
    }


    /*
     * ============================================================
     * ESTATÍSTICAS
     * ============================================================
     *
     * Calcula:
     *
     * - quantidade;
     * - média;
     * - mediana;
     * - mínimo;
     * - máximo;
     * - amplitude;
     * - desvio padrão;
     * - percentil 25;
     * - percentil 50;
     * - percentil 75.
     */

    private TempoEstatisticaDTO calcularEstatisticas(
            List<Long> valores) {

        TempoEstatisticaDTO resultado =
                new TempoEstatisticaDTO();


        /*
         * Não existem dados para calcular.
         */

        if (valores == null ||
                valores.isEmpty()) {

            resultado.setQuantidade(0);
            resultado.setMediaDias(null);
            resultado.setMedianaDias(null);
            resultado.setMinimoDias(null);
            resultado.setMaximoDias(null);
            resultado.setAmplitudeDias(null);
            resultado.setDesvioPadraoDias(null);
            resultado.setPercentil25Dias(null);
            resultado.setPercentil50Dias(null);
            resultado.setPercentil75Dias(null);

            return resultado;
        }


        /*
         * Ordena os valores para cálculo de:
         *
         * - mediana;
         * - mínimo;
         * - máximo;
         * - percentis.
         */

        List<Long> ordenados =
                valores.stream()
                        .sorted()
                        .toList();


        int quantidade =
                ordenados.size();


        /*
         * ============================================================
         * MÉDIA
         * ============================================================
         */

        double media =
                ordenados.stream()
                        .mapToLong(Long::longValue)
                        .average()
                        .orElse(0);


        /*
         * ============================================================
         * MEDIANA
         * ============================================================
         *
         * A mediana corresponde ao percentil 50.
         */

        double mediana =
                calcularPercentil(
                        ordenados,
                        50
                );


        /*
         * ============================================================
         * MÍNIMO
         * ============================================================
         */

        long minimo =
                ordenados.get(0);


        /*
         * ============================================================
         * MÁXIMO
         * ============================================================
         */

        long maximo =
                ordenados.get(
                        ordenados.size() - 1
                );


        /*
         * ============================================================
         * AMPLITUDE
         * ============================================================
         */

        long amplitude =
                maximo - minimo;


        /*
         * ============================================================
         * DESVIO PADRÃO
         * ============================================================
         *
         * É utilizado o desvio padrão populacional sobre
         * os valores efetivamente retornados pela consulta.
         */

        double desvioPadrao =
                calcularDesvioPadrao(
                        ordenados,
                        media
                );


        /*
         * ============================================================
         * PERCENTIS
         * ============================================================
         */

        double p25 =
                calcularPercentil(
                        ordenados,
                        25
                );

        double p50 =
                calcularPercentil(
                        ordenados,
                        50
                );

        double p75 =
                calcularPercentil(
                        ordenados,
                        75
                );


        /*
         * ============================================================
         * PREENCHIMENTO DO DTO
         * ============================================================
         */

        resultado.setQuantidade(
                quantidade
        );

        resultado.setMediaDias(
                arredondar(media)
        );

        resultado.setMedianaDias(
                arredondar(mediana)
        );

        resultado.setMinimoDias(
                minimo
        );

        resultado.setMaximoDias(
                maximo
        );

        resultado.setAmplitudeDias(
                amplitude
        );

        resultado.setDesvioPadraoDias(
                arredondar(desvioPadrao)
        );

        resultado.setPercentil25Dias(
                arredondar(p25)
        );

        resultado.setPercentil50Dias(
                arredondar(p50)
        );

        resultado.setPercentil75Dias(
                arredondar(p75)
        );


        return resultado;
    }


    /*
     * ============================================================
     * PERCENTIL
     * ============================================================
     *
     * Utiliza interpolação linear entre os valores.
     */

    private double calcularPercentil(
            List<Long> valores,
            double percentil) {

        if (valores == null ||
                valores.isEmpty()) {

            return 0;
        }


        if (valores.size() == 1) {

            return valores.get(0);
        }


        double posicao =
                (percentil / 100.0)
                        * (valores.size() - 1);


        int inferior =
                (int) Math.floor(posicao);


        int superior =
                (int) Math.ceil(posicao);


        if (inferior == superior) {

            return valores.get(inferior);
        }


        double peso =
                posicao - inferior;


        return valores.get(inferior)
                + peso *
                (
                        valores.get(superior)
                                - valores.get(inferior)
                );
    }


    /*
     * ============================================================
     * DESVIO PADRÃO
     * ============================================================
     */

    private double calcularDesvioPadrao(
            List<Long> valores,
            double media) {

        if (valores == null ||
                valores.isEmpty()) {

            return 0;
        }


        double soma =
                valores.stream()
                        .mapToDouble(
                                valor ->
                                        Math.pow(
                                                valor - media,
                                                2
                                        )
                        )
                        .sum();


        return Math.sqrt(
                soma / valores.size()
        );
    }


    /*
     * ============================================================
     * CONVERSÃO DA DATA DE AJUIZAMENTO
     * ============================================================
     *
     * Exemplo:
     *
     * 20250315143000
     */

    private LocalDateTime converterDataAjuizamento(
            String data) {

        return LocalDateTime.parse(
                data,
                FORMATTER_DATA_AJUIZAMENTO
        );
    }


    /*
     * ============================================================
     * CONVERSÃO DA DATA DO MOVIMENTO
     * ============================================================
     *
     * Exemplo:
     *
     * 2025-03-15T14:30:00.000Z
     */

    private LocalDateTime converterDataMovimento(
            String data) {

        return OffsetDateTime
                .parse(data)
                .toLocalDateTime();
    }


    /*
     * ============================================================
     * CONVERSÃO SEGURA - DATA DE AJUIZAMENTO
     * ============================================================
     */

    private Optional<LocalDateTime>
    converterDataAjuizamentoSeguro(
            DadosDataJudDTO processo) {

        if (processo == null ||
                processo.getDataAjuizamento() == null ||
                processo.getDataAjuizamento().isBlank()) {

            return Optional.empty();
        }


        try {

            return Optional.of(
                    converterDataAjuizamento(
                            processo.getDataAjuizamento()
                    )
            );

        } catch (Exception e) {

            return Optional.empty();
        }
    }


    /*
     * ============================================================
     * CONVERSÃO SEGURA - DATA DO MOVIMENTO
     * ============================================================
     */

    private Optional<LocalDateTime>
    converterDataMovimentoSeguro(
            MovimentoDataJudDTO movimento) {

        if (movimento == null ||
                movimento.getDataHora() == null ||
                movimento.getDataHora().isBlank()) {

            return Optional.empty();
        }


        try {

            return Optional.of(
                    converterDataMovimento(
                            movimento.getDataHora()
                    )
            );

        } catch (Exception e) {

            return Optional.empty();
        }
    }


    /*
     * ============================================================
     * CONVERSÃO SEGURA - ÚLTIMA ATUALIZAÇÃO
     * ============================================================
     */

    private Optional<LocalDateTime>
    converterDataUltimaAtualizacaoSeguro(
            DadosDataJudDTO processo) {

        if (processo == null ||
                processo.getDataHoraUltimaAtualizacao() == null ||
                processo.getDataHoraUltimaAtualizacao().isBlank()) {

            return Optional.empty();
        }


        try {

            return Optional.of(
                    converterDataMovimento(
                            processo.getDataHoraUltimaAtualizacao()
                    )
            );

        } catch (Exception e) {

            return Optional.empty();
        }
    }


    /*
     * ============================================================
     * VALIDAÇÃO DE MOVIMENTO
     * ============================================================
     */

    private boolean movimentoValido(
            MovimentoDataJudDTO movimento) {

        return movimento != null &&
                movimento.getNome() != null &&
                movimento.getDataHora() != null &&
                !movimento.getDataHora().isBlank();
    }


    /*
     * ============================================================
     * ESTATÍSTICA VAZIA
     * ============================================================
     */

    private TempoEstatisticaDTO criarEstatisticaVazia() {

        TempoEstatisticaDTO dto =
                new TempoEstatisticaDTO();

        dto.setQuantidade(0);

        return dto;
    }


    /*
     * ============================================================
     * ARREDONDAMENTO
     * ============================================================
     *
     * Mantém duas casas decimais.
     */

    private Double arredondar(double valor) {

        return Math.round(valor * 100.0) / 100.0;
    }
}