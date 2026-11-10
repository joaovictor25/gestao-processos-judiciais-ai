package br.com.axionweb.backend.security;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Contador em memória por chave, com janela fixa. Vale para uma única instância da aplicação.
 * Com o mapa cheio, novas chaves são recusadas (falha fechada) para limitar memória e CPU sob ataque.
 */
public final class LimitadorTaxa {
    private record Janela(long inicioMs, int contagem) {}

    private final ConcurrentHashMap<String, Janela> janelas = new ConcurrentHashMap<>();
    private final AtomicLong ultimaLimpezaMs = new AtomicLong();
    private final int limite;
    private final long duracaoMs;
    private final int maxChaves;
    private final Clock relogio;

    public LimitadorTaxa(int limite, Duration duracao, int maxChaves, Clock relogio) {
        if (limite < 1 || duracao.isNegative() || duracao.isZero() || maxChaves < 1)
            throw new IllegalArgumentException("Limite, janela e capacidade devem ser positivos");
        this.limite = limite;
        this.duracaoMs = duracao.toMillis();
        this.maxChaves = maxChaves;
        this.relogio = relogio;
    }

    /** Consome uma unidade. Retorna vazio quando permitido ou o tempo até a janela reabrir. */
    public Optional<Duration> consumir(String chave) {
        long agora = relogio.millis();
        if (!janelas.containsKey(chave) && janelas.size() >= maxChaves && !liberarEspaco(agora))
            return Optional.of(Duration.ofMillis(duracaoMs));
        var resultado = new boolean[1];
        var janela = janelas.compute(chave, (k, atual) -> {
            if (atual == null || agora - atual.inicioMs() >= duracaoMs) { resultado[0] = true; return new Janela(agora, 1); }
            if (atual.contagem() >= limite) return atual;
            resultado[0] = true;
            return new Janela(atual.inicioMs(), atual.contagem() + 1);
        });
        return resultado[0] ? Optional.empty() : Optional.of(restante(janela, agora));
    }

    /** Verifica sem consumir. */
    public Optional<Duration> verificar(String chave) {
        long agora = relogio.millis();
        var janela = janelas.get(chave);
        if (janela == null || agora - janela.inicioMs() >= duracaoMs || janela.contagem() < limite) return Optional.empty();
        return Optional.of(restante(janela, agora));
    }

    /** Devolve uma unidade consumida quando a operação foi recusada por outro limite. */
    public void devolver(String chave) {
        janelas.computeIfPresent(chave, (k, atual) -> atual.contagem() <= 1 ? null : new Janela(atual.inicioMs(), atual.contagem() - 1));
    }

    public void limpar(String chave) { janelas.remove(chave); }

    private boolean liberarEspaco(long agora) {
        long anterior = ultimaLimpezaMs.get();
        if (agora - anterior >= 1000 && ultimaLimpezaMs.compareAndSet(anterior, agora))
            janelas.values().removeIf(j -> agora - j.inicioMs() >= duracaoMs);
        return janelas.size() < maxChaves;
    }

    private Duration restante(Janela janela, long agora) {
        return Duration.ofMillis(Math.max(1, janela.inicioMs() + duracaoMs - agora));
    }
}
