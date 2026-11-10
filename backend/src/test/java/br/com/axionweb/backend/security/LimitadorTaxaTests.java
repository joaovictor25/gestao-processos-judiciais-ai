package br.com.axionweb.backend.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LimitadorTaxaTests {
    /** Relógio controlado pelo teste. */
    static final class RelogioManual extends Clock {
        private volatile long ms = 1_000_000;
        void avancar(Duration d) { ms += d.toMillis(); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return this; }
        @Override public Instant instant() { return Instant.ofEpochMilli(ms); }
    }

    private final RelogioManual relogio = new RelogioManual();

    @Test
    void recusaAposLimiteInformaEsperaEReabreAoFimDaJanela() {
        var limitador = new LimitadorTaxa(3, Duration.ofMinutes(1), 100, relogio);
        for (int i = 0; i < 3; i++) assertThat(limitador.consumir("ip")).isEmpty();
        relogio.avancar(Duration.ofSeconds(20));
        assertThat(limitador.consumir("ip")).hasValue(Duration.ofSeconds(40));
        assertThat(limitador.consumir("outro-ip")).as("chaves são independentes").isEmpty();
        relogio.avancar(Duration.ofSeconds(40));
        assertThat(limitador.consumir("ip")).isEmpty();
    }

    @Test
    void verificarNaoConsomeEDevolverELimparLiberamCota() {
        var limitador = new LimitadorTaxa(2, Duration.ofMinutes(15), 100, relogio);
        assertThat(limitador.verificar("email")).isEmpty();
        limitador.consumir("email");
        assertThat(limitador.verificar("email")).isEmpty();
        limitador.consumir("email");
        assertThat(limitador.verificar("email")).isPresent();
        limitador.devolver("email");
        assertThat(limitador.verificar("email")).isEmpty();
        limitador.consumir("email");
        limitador.limpar("email");
        assertThat(limitador.consumir("email")).isEmpty();
    }

    @Test
    void capacidadeCheiaRecusaNovasChavesAteExpirarEntradas() {
        var limitador = new LimitadorTaxa(5, Duration.ofMinutes(1), 2, relogio);
        assertThat(limitador.consumir("a")).isEmpty();
        assertThat(limitador.consumir("b")).isEmpty();
        assertThat(limitador.consumir("c")).as("falha fechada com o mapa cheio").isPresent();
        assertThat(limitador.consumir("a")).as("chave existente continua contando").isEmpty();
        relogio.avancar(Duration.ofMinutes(1));
        assertThat(limitador.consumir("c")).as("limpeza remove janelas expiradas").isEmpty();
    }

    @Test
    void consumoConcorrenteNaoUltrapassaLimite() throws Exception {
        var limitador = new LimitadorTaxa(50, Duration.ofHours(1), 10, relogio);
        var permitidas = new AtomicInteger();
        try (var executor = Executors.newFixedThreadPool(8)) {
            for (int i = 0; i < 400; i++)
                executor.submit(() -> { if (limitador.consumir("global").isEmpty()) permitidas.incrementAndGet(); });
        }
        assertThat(permitidas).hasValue(50);
    }

    @Test
    void rejeitaConfiguracaoInvalida() {
        assertThatThrownBy(() -> new LimitadorTaxa(0, Duration.ofMinutes(1), 1, relogio)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LimitadorTaxa(1, Duration.ZERO, 1, relogio)).isInstanceOf(IllegalArgumentException.class);
    }
}
