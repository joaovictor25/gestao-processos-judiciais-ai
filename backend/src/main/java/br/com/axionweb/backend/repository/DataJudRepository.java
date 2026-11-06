package br.com.axionweb.backend.repository;

import br.com.axionweb.backend.model.datajud.DataJud;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DataJudRepository extends JpaRepository<DataJud, Long> {

    @org.springframework.data.jpa.repository.Query("select distinct d.processo.id from DataJud d where d.processo.usuario.email = :email")
    java.util.Set<Long> processosAnalisadosDoUsuario(String email);

    List<DataJud> findByProcessoIdOrderByIdAsc(Long processoId);

    java.util.Optional<DataJud> findByIdAndProcessoId(Long id, Long processoId);

    org.springframework.data.domain.Slice<DataJud> findByProcessoIdAndSinteseIsNotNullAndSinteseGeradaEmIsNotNullOrderBySinteseGeradaEmDescIdDesc(
            Long processoId, org.springframework.data.domain.Pageable pagina);

    void deleteByProcessoId(Long processoId);
}
