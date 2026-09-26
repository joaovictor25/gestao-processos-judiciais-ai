package br.com.axionweb.backend.repository;

import br.com.axionweb.backend.model.datajud.DataJud;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataJudRepository extends JpaRepository<DataJud, Long> {

    List<DataJud> findByProcessoIdOrderByIdAsc(Long processoId);

    void deleteByProcessoId(Long processoId);
}
