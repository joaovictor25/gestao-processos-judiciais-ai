package br.com.axionweb.backend.service;

import br.com.axionweb.backend.exception.LimiteExcedidoException;
import br.com.axionweb.backend.security.LimitadorTaxa;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

import static br.com.axionweb.backend.exception.LimiteExcedidoException.Motivo.*;

/**
 * Cotas de chamadas à IA por usuário e para toda a instância, além de um teto de chamadas simultâneas.
 * Cada tentativa autorizada consome a cota, mesmo que a IA falhe, pois a chamada externa já ocorreu.
 */
@Service
public class CotaIAService {
    private static final String GLOBAL = "global";
    private final LimitadorTaxa porUsuario;
    private final LimitadorTaxa global;
    private final Semaphore simultaneas;
    private final UsuarioAutenticadoService usuarios;

    public CotaIAService(
            @Value("${app.ia.limite.por-usuario:30}") int porUsuario,
            @Value("${app.ia.limite.janela-usuario:PT1H}") Duration janelaUsuario,
            @Value("${app.ia.limite.global:300}") int global,
            @Value("${app.ia.limite.janela-global:PT1H}") Duration janelaGlobal,
            @Value("${app.ia.limite.simultaneas:4}") int simultaneas,
            UsuarioAutenticadoService usuarios) {
        var relogio = Clock.systemUTC();
        this.porUsuario = new LimitadorTaxa(porUsuario, janelaUsuario, 100_000, relogio);
        this.global = new LimitadorTaxa(global, janelaGlobal, 1, relogio);
        if (simultaneas < 1) throw new IllegalArgumentException("app.ia.limite.simultaneas deve ser positivo");
        this.simultaneas = new Semaphore(simultaneas);
        this.usuarios = usuarios;
    }

    public <T> T executar(Supplier<T> chamada) {
        String usuario = usuarios.getEmailAutenticado();
        if (!simultaneas.tryAcquire()) throw new LimiteExcedidoException(IA_OCUPADA, Duration.ofSeconds(10));
        try {
            porUsuario.consumir(usuario).ifPresent(espera -> { throw new LimiteExcedidoException(LIMITE_IA_USUARIO, espera); });
            var esperaGlobal = global.consumir(GLOBAL);
            if (esperaGlobal.isPresent()) {
                porUsuario.devolver(usuario); // Recusa do sistema não penaliza a cota do usuário.
                throw new LimiteExcedidoException(LIMITE_IA_GLOBAL, esperaGlobal.get());
            }
            return chamada.get();
        } finally {
            simultaneas.release();
        }
    }
}
