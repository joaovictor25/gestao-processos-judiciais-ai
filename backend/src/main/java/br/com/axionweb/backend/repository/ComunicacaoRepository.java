package br.com.axionweb.backend.repository;

import br.com.axionweb.backend.model.comunicacao.Comunicacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComunicacaoRepository extends JpaRepository<Comunicacao, Long> {

    List<Comunicacao> findByProcessoIdOrderByIdAsc(Long processoId);

    void deleteByProcessoId(Long processoId);
}
