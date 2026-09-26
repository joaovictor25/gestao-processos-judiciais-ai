package br.com.axionweb.backend.repository;

import br.com.axionweb.backend.model.usuario.TokenRevogado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface TokenRevogadoRepository extends JpaRepository<TokenRevogado, Long> {
    boolean existsByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from TokenRevogado token where token.expiraEm < :agora")
    void removerExpirados(@Param("agora") Instant agora);
}
