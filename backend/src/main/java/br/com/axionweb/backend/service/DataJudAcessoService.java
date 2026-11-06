package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.datajud.RespostaDataJudDTO;
import br.com.axionweb.backend.exception.DataJudAguardarException;
import br.com.axionweb.backend.model.datajud.CacheConsultaDataJud;
import br.com.axionweb.backend.model.datajud.ControleDataJud;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.*;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.*;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Supplier;

@Service
public class DataJudAcessoService {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(DataJudAcessoService.class);
    @PersistenceContext private EntityManager em;
    private final TransactionTemplate tx;
    private final ObjectMapper mapper;
    @Value("${app.datajud.intervalo-ms:1500}") private long intervalo;
    @Value("${app.datajud.pausa-429-ms:30000}") private long pausa429;
    @Value("${app.datajud.retry-base-ms:5000}") private long retryBase;
    @Value("${app.datajud.cache-ms:300000}") private long cacheMs;
    @Value("${app.datajud.cache-max-entradas:200}") private int cacheMax;

    public DataJudAcessoService(PlatformTransactionManager manager, ObjectMapper mapper) {
        this.mapper = mapper;
        tx = new TransactionTemplate(manager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @PostConstruct
    void inicializar() {
        if (intervalo < 0 || pausa429 < 0 || retryBase < 0 || cacheMs < 0 || cacheMax < 1)
            throw new IllegalArgumentException("Configuração DataJud inválida");
        try {
            tx.executeWithoutResult(s -> {
                if (em.find(ControleDataJud.class, 1) == null) {
                    var controle = new ControleDataJud(); controle.id = 1; em.persist(controle); em.flush();
                }
            });
        } catch (RuntimeException concorrente) {
            if (!Boolean.TRUE.equals(tx.execute(s -> em.find(ControleDataJud.class, 1) != null))) throw concorrente;
        }
    }

    /** Transação curta; nunca mantém a conexão/lock durante uma chamada HTTP. */
    public <T> T coordenar(Supplier<T> acao) {
        return tx.execute(s -> { em.find(ControleDataJud.class, 1, LockModeType.PESSIMISTIC_WRITE); return acao.get(); });
    }

    public RespostaDataJudDTO consultar(String endpoint, Map<String, Object> consulta, Supplier<RespostaDataJudDTO> chamada) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        String chave = digest((auth == null ? "sem-usuario" : auth.getName()) + "\n" + endpoint + "\n" + mapper.writeValueAsString(ordenar(consulta)));
        String reserva = UUID.randomUUID().toString();
        RespostaDataJudDTO cache = coordenar(() -> {
            long agora = System.currentTimeMillis();
            var salvo = em.find(CacheConsultaDataJud.class, chave);
            if (cacheMs > 0 && salvo != null && salvo.expiraEm > agora) return mapper.readValue(salvo.resposta, RespostaDataJudDTO.class);
            var controle = em.find(ControleDataJud.class, 1);
            long disponivel = Math.max(controle.proximaConsulta, controle.ocupadoAte > agora ? Math.min(controle.ocupadoAte, agora + 1000) : 0);
            if (disponivel > agora) throw new DataJudAguardarException(disponivel, false, null, null);
            controle.reserva = reserva;
            // Maior que o timeout HTTP de 55 s; recupera reservas de processos encerrados.
            controle.ocupadoAte = agora + 90000;
            controle.proximaConsulta = agora + intervalo;
            return null;
        });
        if (cache != null) return cache;
        long inicio = System.nanoTime();
        long[] pausa = {0};
        try {
            var resposta = chamada.get(); // inclui validação antes de entrar no cache
            if (cacheMs > 0) {
                String json = mapper.writeValueAsString(resposta);
                if (json.length() <= 4_000_000) coordenar(() -> {
                    em.createQuery("delete from CacheConsultaDataJud c where c.expiraEm <= :agora")
                            .setParameter("agora", System.currentTimeMillis()).executeUpdate();
                    if (em.find(CacheConsultaDataJud.class, chave) != null ||
                            em.createQuery("select count(c) from CacheConsultaDataJud c", Long.class).getSingleResult() < cacheMax) {
                        var item = new CacheConsultaDataJud(); item.chave = chave; item.resposta = json;
                        item.expiraEm = System.currentTimeMillis() + cacheMs; em.merge(item);
                    }
                    return null;
                });
            }
            log.info("Consulta DataJud recebida: endpoint={}, tamanho={}, duracaoMs={}", endpoint, consulta.get("size"), (System.nanoTime() - inicio) / 1_000_000);
            return resposta;
        } catch (RestClientException erro) {
            Integer status = erro instanceof RestClientResponseException http ? http.getStatusCode().value() : null;
            boolean transitorio = erro instanceof ResourceAccessException || Objects.equals(status, 429) ||
                    Objects.equals(status, 502) || Objects.equals(status, 503) || Objects.equals(status, 504);
            long espera = Objects.equals(status, 429) ? pausa429 : retryBase;
            if (erro instanceof RestClientResponseException http && http.getResponseHeaders() != null)
                espera = Math.max(espera, retryAfterMillis(http.getResponseHeaders().getFirst("Retry-After"), Instant.now()));
            pausa[0] = transitorio ? System.currentTimeMillis() + espera : 0;
            // Somente o tipo estruturado. Nunca registra reason, corpo, consulta, usuário ou credenciais.
            log.warn("Consulta DataJud recusada: endpoint={}, httpExterno={}, tipoExterno={}, aguardarMs={}, duracaoMs={}",
                    endpoint, status, tipoExterno(erro), transitorio ? espera : 0, (System.nanoTime() - inicio) / 1_000_000);
            if (transitorio) throw new DataJudAguardarException(pausa[0], true, status, erro);
            throw erro;
        } finally {
            coordenar(() -> {
                var controle = em.find(ControleDataJud.class, 1);
                controle.proximaConsulta = Math.max(controle.proximaConsulta, pausa[0]);
                if (reserva.equals(controle.reserva)) { controle.reserva = null; controle.ocupadoAte = 0; }
                return null;
            });
        }
    }

    private String tipoExterno(RestClientException erro) {
        if (erro instanceof RestClientResponseException http) {
            try {
                String tipo = mapper.readTree(http.getResponseBodyAsString()).path("error").path("type").asText("");
                if (tipo.matches("[a-zA-Z_]{1,100}")) return tipo;
            } catch (RuntimeException ignorado) { }
        }
        return erro.getClass().getSimpleName();
    }

    public static long retryAfterMillis(String valor, Instant agora) {
        if (valor == null) return 0;
        try {
            if (valor.trim().matches("\\d+")) return Math.min(Long.MAX_VALUE / 4, Math.multiplyExact(Long.parseLong(valor.trim()), 1000));
            return Math.max(0, Duration.between(agora, ZonedDateTime.parse(valor.trim(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).toMillis());
        } catch (ArithmeticException | NumberFormatException excedido) { return Long.MAX_VALUE / 4; }
        catch (java.time.DateTimeException invalido) { return 0; }
    }

    public static String digest(String valor) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossivel) { throw new IllegalStateException(impossivel); }
    }

    private Object ordenar(Object valor) {
        if (valor instanceof Map<?, ?> mapa) {
            Map<String, Object> ordenado = new TreeMap<>();
            mapa.forEach((k, v) -> ordenado.put(k.toString(), ordenar(v))); return ordenado;
        }
        if (valor instanceof List<?> lista) return lista.stream().map(this::ordenar).toList();
        return valor;
    }
}
