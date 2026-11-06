package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.datajud.*;
import br.com.axionweb.backend.exception.DataJudAguardarException;
import br.com.axionweb.backend.exception.EstatisticasSemProcessosException;
import br.com.axionweb.backend.model.datajud.SolicitacaoEstatistica;
import br.com.axionweb.backend.model.processo.ClasseProcessual;
import br.com.axionweb.backend.repository.DataJudRepository;
import br.com.axionweb.backend.security.CustomUserDetailsService;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

import java.time.*;
import java.util.*;

@Service
public class GeracaoEstatisticaService {
    private static final List<String> ATIVAS = List.of("AGUARDANDO", "CONSULTANDO", "REAGENDADA");
    @PersistenceContext private EntityManager em;
    private final DataJudAcessoService acesso;
    private final ProcessoService processos;
    private final DataJudService datajud;
    private final AnaliseService analises;
    private final DataJudRepository estatisticas;
    private final UsuarioAutenticadoService usuario;
    private final CustomUserDetailsService usuarios;
    private final ObjectMapper mapper;
    @Value("${app.datajud.max-tentativas:3}") private int maxTentativas;
    @Value("${app.datajud.prazo-geracao-ms:300000}") private long prazo;
    @Value("${app.datajud.retry-base-ms:5000}") private long retryBase;
    @Value("${app.datajud.cache-ms:300000}") private long cacheMs;
    @Value("${app.datajud.fila-max:100}") private int filaMax;
    @Value("${app.datajud.fila-por-usuario:5}") private int filaUsuario;

    public GeracaoEstatisticaService(DataJudAcessoService acesso, ProcessoService processos, DataJudService datajud,
            AnaliseService analises, DataJudRepository estatisticas, UsuarioAutenticadoService usuario,
            CustomUserDetailsService usuarios, ObjectMapper mapper) {
        this.acesso = acesso; this.processos = processos; this.datajud = datajud; this.analises = analises;
        this.estatisticas = estatisticas; this.usuario = usuario; this.usuarios = usuarios; this.mapper = mapper;
    }

    public record Filtros(String tribunal, String classe, String grau, LocalDate dataInicio, LocalDate dataFim) {
        ParametrosDataJudDTO parametro() {
            var p = new ParametrosDataJudDTO(); p.setTribunal(tribunal); p.setGrau(grau);
            if (classe != null) { var c = new ClasseProcessual(); c.setCodigo(classe); p.setClasseProcessual(c); }
            p.setDataInicio(dataInicio); p.setDataFim(dataFim); p.setQuantidadeResultado(400); return p;
        }
    }

    @jakarta.annotation.PostConstruct
    void validarConfiguracao() {
        if (maxTentativas < 1 || maxTentativas > 10 || prazo < 1000 || prazo > 3_600_000 || filaMax < 1 || filaUsuario < 1)
            throw new IllegalArgumentException("Configuração da fila DataJud inválida");
    }

    public SolicitacaoEstatisticaDTO solicitar(Long processoId, GerarEstatisticasRequestDTO pedido) {
        return acesso.coordenar(() -> {
            var p = processos.bloquearParaAtualizarAnalise(processoId);
            var filtros = new Filtros(normalizar(p.getTribunal()), p.getClasseProcessual() == null ? null :
                    normalizar(p.getClasseProcessual().getCodigo()), normalizar(p.getGrau()), pedido.dataInicio(), pedido.dataFim());
            // Valida o tribunal antes de aceitar trabalho que nunca poderá ser executado.
            datajud.validarParametros(filtros.parametro());
            String chave = DataJudAcessoService.digest(mapper.writeValueAsString(filtros));
            long agora = System.currentTimeMillis();
            var anteriores = em.createQuery("select s from SolicitacaoEstatistica s where s.processoId = :id and (s.status in :ativas or (s.status = 'CONCLUIDA' and s.atualizadaEm > :limite)) order by s.criadaEm desc", SolicitacaoEstatistica.class)
                    .setParameter("id", processoId).setParameter("ativas", ATIVAS).setParameter("limite", agora - cacheMs).getResultList();
            for (var s : anteriores) {
                if (ATIVAS.contains(s.status)) {
                    if (!s.chave.equals(chave)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma geração em andamento para este processo");
                    return dto(s);
                }
                if (cacheMs > 0 && s.status.equals("CONCLUIDA") && s.chave.equals(chave) && s.atualizadaEm + cacheMs > agora &&
                        estatisticas.findByIdAndProcessoId(s.estatisticaId, processoId).isPresent()) return dto(s);
            }
            String email = usuario.getEmailAutenticado();
            long total = em.createQuery("select count(s) from SolicitacaoEstatistica s where s.status in :ativas", Long.class).setParameter("ativas", ATIVAS).getSingleResult();
            long doUsuario = em.createQuery("select count(s) from SolicitacaoEstatistica s where s.status in :ativas and s.usuario = :usuario", Long.class)
                    .setParameter("ativas", ATIVAS).setParameter("usuario", email).getSingleResult();
            if (total >= filaMax || doUsuario >= filaUsuario) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Aguarde a conclusão das solicitações em andamento");
            var s = new SolicitacaoEstatistica(); s.id = UUID.randomUUID().toString(); s.processoId = processoId;
            s.usuario = email; s.chave = chave; s.parametros = mapper.writeValueAsString(filtros);
            s.status = "AGUARDANDO"; s.criadaEm = agora; s.atualizadaEm = agora; s.proximaTentativa = agora; s.prazoFinal = agora + prazo;
            em.persist(s); return dto(s);
        });
    }

    public SolicitacaoEstatisticaDTO consultar(Long processoId, String id) {
        processos.buscarPorId(processoId);
        return acesso.coordenar(() -> {
            var s = em.find(SolicitacaoEstatistica.class, id);
            if (s == null || !s.processoId.equals(processoId) || !s.usuario.equals(usuario.getEmailAutenticado()))
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            return dto(s);
        });
    }

    public SolicitacaoEstatisticaDTO atual(Long processoId) {
        processos.buscarPorId(processoId);
        return acesso.coordenar(() -> em.createQuery("select s from SolicitacaoEstatistica s where s.processoId = :id and s.usuario = :usuario and s.status in :ativas order by s.criadaEm desc", SolicitacaoEstatistica.class)
                .setParameter("id", processoId).setParameter("usuario", usuario.getEmailAutenticado()).setParameter("ativas", ATIVAS)
                .setMaxResults(1).getResultStream().findFirst().map(this::dto).orElse(null));
    }

    /** Chamado pelo agendador e pelos testes de integração; uma execução por vez em cada instância. */
    public void processarProxima() {
        var s = acesso.coordenar(() -> {
            long agora = System.currentTimeMillis();
            em.createQuery("delete from CacheConsultaDataJud c where c.expiraEm <= :agora").setParameter("agora", agora).executeUpdate();
            em.createQuery("delete from SolicitacaoEstatistica s where s.status not in :ativas and s.atualizadaEm < :limite")
                    .setParameter("ativas", ATIVAS).setParameter("limite", agora - Duration.ofDays(7).toMillis()).executeUpdate();
            em.createQuery("update SolicitacaoEstatistica s set s.status = 'FALHOU', s.codigoErro = 'DATAJUD_PRAZO_EXCEDIDO', s.atualizadaEm = :agora where s.status in :ativas and s.prazoFinal <= :agora and s.ocupadaAte <= :agora")
                    .setParameter("ativas", ATIVAS).setParameter("agora", agora).executeUpdate();
            var proxima = em.createQuery("select s from SolicitacaoEstatistica s where s.status in :ativas and s.proximaTentativa <= :agora and s.ocupadaAte <= :agora order by s.proximaTentativa, s.criadaEm", SolicitacaoEstatistica.class)
                    .setParameter("ativas", ATIVAS).setParameter("agora", agora).setMaxResults(1).getResultStream().findFirst().orElse(null);
            if (proxima != null) {
                if (proxima.tentativas >= maxTentativas) { falhar(proxima, "CONSULTA_DATAJUD_FALHOU"); return null; }
                proxima.status = "CONSULTANDO"; proxima.reserva = UUID.randomUUID().toString();
                proxima.ocupadaAte = agora + 90000; proxima.atualizadaEm = agora; proxima.tentativas++;
            }
            return proxima;
        });
        if (s == null) return;
        var anterior = SecurityContextHolder.getContext();
        try {
            var principal = usuarios.loadUserByUsername(s.usuario);
            var contexto = SecurityContextHolder.createEmptyContext();
            contexto.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal, null, principal.getAuthorities()));
            SecurityContextHolder.setContext(contexto);
            processos.buscarPorId(s.processoId);
            var filtros = mapper.readValue(s.parametros, Filtros.class);
            var dados = datajud.buscarProcessos(filtros.parametro());
            acesso.coordenar(() -> {
                var atual = em.find(SolicitacaoEstatistica.class, s.id);
                if (!s.reserva.equals(atual.reserva)) return null;
                if (System.currentTimeMillis() > atual.prazoFinal) { falhar(atual, "DATAJUD_PRAZO_EXCEDIDO"); return null; }
                var resultado = analises.salvarEstatisticas(s.processoId, dados, filtros.parametro());
                atual.estatisticaId = resultado.getFirst().id(); atual.status = "CONCLUIDA";
                atual.ocupadaAte = 0; atual.reserva = null; atual.codigoErro = null; atual.atualizadaEm = System.currentTimeMillis();
                return null;
            });
        } catch (RuntimeException erro) {
            acesso.coordenar(() -> {
                var atual = em.find(SolicitacaoEstatistica.class, s.id);
                if (!s.reserva.equals(atual.reserva)) return null;
                if (erro instanceof DataJudAguardarException aguardar) {
                    if (!aguardar.isConsultou()) atual.tentativas--;
                    long exponencial = retryBase * (1L << Math.min(10, Math.max(0, atual.tentativas - 1)));
                    long variacao = retryBase == 0 ? 0 : java.util.concurrent.ThreadLocalRandom.current().nextLong(Math.max(1, retryBase / 4));
                    atual.proximaTentativa = Math.max(aguardar.getTentarEm(), System.currentTimeMillis() + exponencial + variacao);
                    atual.codigoErro = Objects.equals(aguardar.getStatusExterno(), 429) ? "CONSULTA_DATAJUD_LIMITADA" : "CONSULTA_DATAJUD_FALHOU";
                    if (atual.tentativas < maxTentativas && atual.proximaTentativa < atual.prazoFinal) {
                        atual.status = "REAGENDADA"; atual.ocupadaAte = 0; atual.reserva = null; atual.atualizadaEm = System.currentTimeMillis(); return null;
                    }
                    falhar(atual, atual.codigoErro);
                } else falhar(atual, erro instanceof EstatisticasSemProcessosException ? "DATAJUD_SEM_PROCESSOS" : "CONSULTA_DATAJUD_FALHOU");
                return null;
            });
        } finally { SecurityContextHolder.setContext(anterior); }
    }

    private void falhar(SolicitacaoEstatistica s, String codigo) {
        s.status = "FALHOU"; s.codigoErro = codigo; s.reserva = null; s.ocupadaAte = 0; s.atualizadaEm = System.currentTimeMillis();
    }
    private SolicitacaoEstatisticaDTO dto(SolicitacaoEstatistica s) {
        var filtros = mapper.readValue(s.parametros, Filtros.class);
        var resultado = s.estatisticaId == null ? List.<DataJudResponseDTO>of() : estatisticas.findByIdAndProcessoId(s.estatisticaId, s.processoId).map(d -> List.of(DataJudResponseDTO.de(d))).orElse(List.of());
        return new SolicitacaoEstatisticaDTO(s.id, s.processoId, s.status, s.tentativas, Instant.ofEpochMilli(s.proximaTentativa), s.codigoErro, filtros.dataInicio(), filtros.dataFim(), resultado);
    }
    private static String normalizar(String valor) { return valor == null || valor.isBlank() ? null : valor.trim().toUpperCase(Locale.ROOT); }
}
