package br.com.axionweb.backend.repository;

import br.com.axionweb.backend.model.comunicacao.PesquisaComunicacao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PesquisaComunicacaoRepository extends JpaRepository<PesquisaComunicacao, Long> {
    Optional<PesquisaComunicacao> findByIdAndProcessoId(Long id, Long processoId);
    Optional<PesquisaComunicacao> findFirstByProcessoIdOrderByIdDesc(Long processoId);
    Slice<PesquisaComunicacao> findByProcessoIdAndSinteseIsNotNullOrderByGeradaEmDescIdDesc(Long processoId, Pageable pagina);
}
