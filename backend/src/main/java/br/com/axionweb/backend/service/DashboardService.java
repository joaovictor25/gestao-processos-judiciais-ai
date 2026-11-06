package br.com.axionweb.backend.service;

import br.com.axionweb.backend.model.processo.Processo;
import br.com.axionweb.backend.repository.DataJudRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service
public class DashboardService {
    private final ProcessoService processos;
    private final UsuarioAutenticadoService usuario;
    private final DataJudRepository estatisticas;
    private final ExecucaoAnaliseService execucoes;
    private static final List<String> CORES = List.of("#2563eb", "#d4a545", "#10b981", "#8b5cf6", "#ec4899", "#0891b2");

    public DashboardService(ProcessoService processos, UsuarioAutenticadoService usuario,
                            DataJudRepository estatisticas, ExecucaoAnaliseService execucoes) {
        this.processos = processos; this.usuario = usuario; this.estatisticas = estatisticas; this.execucoes = execucoes;
    }

    public record Area(String nome, long quantidade, String cor) {}
    public record Atividade(String id, Long processoId, String numeroProcesso, String classe,
                            String tipo, String titulo, String descricao, Instant data) {}
    public record Resumo(long cadastrados, long emAnalise, long analisados, long aguardando,
                         List<Area> areas, List<Atividade> atividades, int pagina, int totalPaginas, int totalAtividades) {}

    @Transactional(readOnly = true)
    public Resumo consultar(int paginaSolicitada) {
        var carteira = processos.listarPorUsuario();
        var concluidos = estatisticas.processosAnalisadosDoUsuario(usuario.getEmailAutenticado());
        var ativas = execucoes.processosEmAnalise();
        long emAnalise = 0, analisados = 0;
        Map<String, Long> porArea = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (var processo : carteira) {
            if (ativas.contains(processo.getId())) emAnalise++;
            else if (processo.getUltimaAnaliseEm() != null || concluidos.contains(processo.getId())) analisados++;
            String area = processo.getAreaAtuacao();
            porArea.merge(area == null || area.isBlank() ? "Não informada" : area.strip(), 1L, Long::sum);
        }
        List<Area> areas = new ArrayList<>();
        porArea.forEach((nome, quantidade) -> areas.add(new Area(nome, quantidade, CORES.get(areas.size() % CORES.size()))));
        var ordenados = carteira.stream().sorted(Comparator
                .comparing(Processo::getUltimaAtividadeEm, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Processo::getId, Comparator.reverseOrder())).toList();
        int totalPaginas = Math.max(1, (carteira.size() + 3) / 4);
        int pagina = Math.max(1, Math.min(paginaSolicitada, totalPaginas));
        var atividades = ordenados.stream().skip((pagina - 1L) * 4).limit(4).map(p -> new Atividade(
                p.getId().toString(), p.getId(), p.getNumeroProcesso(),
                p.getClasseProcessual() == null || p.getClasseProcessual().getNome() == null ? "Classe não informada" : p.getClasseProcessual().getNome(),
                p.getUltimaAtividadeTipo() == null ? "cadastro" : p.getUltimaAtividadeTipo(),
                p.getUltimaAtividadeTitulo() == null ? "Processo cadastrado" : p.getUltimaAtividadeTitulo(),
                "Última atividade do processo", p.getUltimaAtividadeEm())).toList();
        return new Resumo(carteira.size(), emAnalise, analisados, carteira.size() - emAnalise - analisados,
                List.copyOf(areas), atividades, pagina, totalPaginas, carteira.size());
    }
}
