package br.com.axionweb.backend.repository;

import br.com.axionweb.backend.model.processo.Processo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessoRepository extends JpaRepository<Processo, Long> {

    public Processo findByNumeroProcesso(String numeroProcesso);
}
