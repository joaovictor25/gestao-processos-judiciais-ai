package br.com.axionweb.backend.service;

import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/** Execuções síncronas ativas nesta instância; não confundir com o status jurídico do processo. */
@Service
public class ExecucaoAnaliseService {
    private final ConcurrentHashMap<Long, Integer> ativas = new ConcurrentHashMap<>();
    private final ProcessoService processos;
    @jakarta.persistence.PersistenceContext private jakarta.persistence.EntityManager em;

    public ExecucaoAnaliseService(ProcessoService processos) { this.processos = processos; }

    public <T> T executar(Long id, Supplier<T> acao) {
        processos.buscarPorId(id); // Autoriza antes de registrar a execução.
        ativas.merge(id, 1, Integer::sum);
        try { return acao.get(); }
        finally { ativas.computeIfPresent(id, (chave, quantidade) -> quantidade == 1 ? null : quantidade - 1); }
    }

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public Set<Long> processosEmAnalise() {
        var resultado = new java.util.HashSet<>(ativas.keySet());
        resultado.addAll(em.createQuery("select distinct s.processoId from SolicitacaoEstatistica s where s.status in ('AGUARDANDO', 'CONSULTANDO', 'REAGENDADA')", Long.class).getResultList());
        return Set.copyOf(resultado);
    }
}
