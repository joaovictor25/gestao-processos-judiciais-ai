package br.com.axionweb.backend.service;

import br.com.axionweb.backend.exception.LimiteExcedidoException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CotaIAServiceTests {
    private final UsuarioAutenticadoService usuarios = mock(UsuarioAutenticadoService.class);

    private CotaIAService cotas(int porUsuario, int global, int simultaneas) {
        return new CotaIAService(porUsuario, Duration.ofHours(1), global, Duration.ofHours(1), simultaneas, usuarios);
    }

    @Test
    void limitePorUsuarioNaoAfetaOutroUsuario() {
        var cotas = cotas(2, 100, 4);
        when(usuarios.getEmailAutenticado()).thenReturn("a@teste.com");
        assertThat(cotas.executar(() -> "ok")).isEqualTo("ok");
        assertThat(cotas.executar(() -> "ok")).isEqualTo("ok");
        assertThatThrownBy(() -> cotas.executar(() -> "nao chama"))
                .isInstanceOfSatisfying(LimiteExcedidoException.class, e -> {
                    assertThat(e.getCodigo()).isEqualTo("LIMITE_IA_USUARIO");
                    assertThat(e.getRetryAfterSegundos()).isBetween(3500L, 3600L);
                });
        when(usuarios.getEmailAutenticado()).thenReturn("b@teste.com");
        assertThat(cotas.executar(() -> "ok")).isEqualTo("ok");
    }

    @Test
    void limiteGlobalNaoConsomeCotaDoUsuarioRecusado() {
        var cotas = cotas(2, 1, 4);
        when(usuarios.getEmailAutenticado()).thenReturn("a@teste.com");
        cotas.executar(() -> "ok");
        when(usuarios.getEmailAutenticado()).thenReturn("b@teste.com");
        for (int i = 0; i < 3; i++)
            assertThatThrownBy(() -> cotas.executar(() -> "nao chama"))
                    .isInstanceOfSatisfying(LimiteExcedidoException.class, e -> assertThat(e.getCodigo()).isEqualTo("LIMITE_IA_GLOBAL"));
    }

    @Test
    void falhaDaIaConsomeCotaELiberaVagaSimultanea() {
        var cotas = cotas(2, 100, 1);
        when(usuarios.getEmailAutenticado()).thenReturn("a@teste.com");
        assertThatThrownBy(() -> cotas.executar(() -> { throw new IllegalStateException("falha externa"); }))
                .isInstanceOf(IllegalStateException.class);
        assertThat(cotas.executar(() -> "vaga liberada")).isEqualTo("vaga liberada");
        assertThatThrownBy(() -> cotas.executar(() -> "nao chama")).isInstanceOf(LimiteExcedidoException.class);
    }

    @Test
    void limiteDeChamadasSimultaneasRecusaSemEsperar() throws Exception {
        var cotas = cotas(100, 100, 1);
        when(usuarios.getEmailAutenticado()).thenReturn("a@teste.com");
        var emAndamento = new CountDownLatch(1);
        var liberar = new CountDownLatch(1);
        try (var executor = Executors.newSingleThreadExecutor()) {
            var primeira = executor.submit(() -> cotas.executar(() -> {
                emAndamento.countDown();
                try { return liberar.await(10, TimeUnit.SECONDS); }
                catch (InterruptedException e) { throw new IllegalStateException(e); }
            }));
            assertThat(emAndamento.await(10, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> cotas.executar(() -> "nao chama"))
                    .isInstanceOfSatisfying(LimiteExcedidoException.class, e -> assertThat(e.getCodigo()).isEqualTo("IA_OCUPADA"));
            liberar.countDown();
            assertThat(primeira.get(10, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(cotas.executar(() -> "ok")).isEqualTo("ok");
    }
}
