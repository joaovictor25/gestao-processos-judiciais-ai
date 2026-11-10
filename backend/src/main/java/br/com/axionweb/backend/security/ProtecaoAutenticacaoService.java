package br.com.axionweb.backend.security;

import br.com.axionweb.backend.exception.LimiteExcedidoException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.Locale;

import static br.com.axionweb.backend.exception.LimiteExcedidoException.Motivo.*;

/** Limita cadastro e login por IP e falhas de login por e-mail, antes do custo do hash de senha. */
@Service
public class ProtecaoAutenticacaoService {
    private final LimitadorTaxa loginPorIp;
    private final LimitadorTaxa falhasPorEmail;
    private final LimitadorTaxa cadastroPorIp;

    public ProtecaoAutenticacaoService(
            @Value("${app.auth.limite.login-por-ip:20}") int loginPorIp,
            @Value("${app.auth.limite.login-janela:PT1M}") Duration loginJanela,
            @Value("${app.auth.limite.falhas-por-email:10}") int falhasPorEmail,
            @Value("${app.auth.limite.falhas-janela:PT15M}") Duration falhasJanela,
            @Value("${app.auth.limite.cadastro-por-ip:5}") int cadastroPorIp,
            @Value("${app.auth.limite.cadastro-janela:PT1H}") Duration cadastroJanela,
            @Value("${app.auth.limite.max-chaves:100000}") int maxChaves) {
        var relogio = Clock.systemUTC();
        this.loginPorIp = new LimitadorTaxa(loginPorIp, loginJanela, maxChaves, relogio);
        this.falhasPorEmail = new LimitadorTaxa(falhasPorEmail, falhasJanela, maxChaves, relogio);
        this.cadastroPorIp = new LimitadorTaxa(cadastroPorIp, cadastroJanela, maxChaves, relogio);
    }

    public void verificarCadastro(String ip) {
        cadastroPorIp.consumir(chave(ip)).ifPresent(espera -> { throw new LimiteExcedidoException(CADASTRO_LIMITADO, espera); });
    }

    public void verificarLogin(String ip, String email) {
        // A conta bloqueada não consome a cota do IP; a mesma resposta não revela se o e-mail existe.
        falhasPorEmail.verificar(chave(email)).ifPresent(espera -> { throw new LimiteExcedidoException(LOGIN_LIMITADO, espera); });
        loginPorIp.consumir(chave(ip)).ifPresent(espera -> { throw new LimiteExcedidoException(LOGIN_LIMITADO, espera); });
    }

    public void registrarFalhaLogin(String email) { falhasPorEmail.consumir(chave(email)); }

    public void registrarSucessoLogin(String email) { falhasPorEmail.limpar(chave(email)); }

    private static String chave(String valor) {
        return valor == null ? "" : valor.strip().toLowerCase(Locale.ROOT);
    }
}
