package br.com.axionweb.backend.model.usuario;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "token_revogado", indexes = {
        @Index(name = "idx_token_revogado_hash", columnList = "token_hash"),
        @Index(name = "idx_token_revogado_expiracao", columnList = "expira_em")
})
public class TokenRevogado {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    // Logouts simultâneos podem registrar o mesmo hash sem conflito entre transações.
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    protected TokenRevogado() {
    }

    public TokenRevogado(String tokenHash, Instant expiraEm) {
        this.tokenHash = tokenHash;
        this.expiraEm = expiraEm;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiraEm() {
        return expiraEm;
    }
}
