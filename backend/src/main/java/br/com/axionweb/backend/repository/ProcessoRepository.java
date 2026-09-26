package br.com.axionweb.backend.repository;

import br.com.axionweb.backend.model.processo.Processo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface ProcessoRepository extends JpaRepository<Processo, Long> {

    List<Processo> findByUsuarioEmailOrderByIdAsc(String email);

    Optional<Processo> findByIdAndUsuarioEmail(Long id, String email);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Processo> findWithLockByIdAndUsuarioEmail(Long id, String email);

    Optional<Processo> findFirstByNumeroProcessoAndUsuarioEmailOrderByIdAsc(String numeroProcesso, String email);
}
