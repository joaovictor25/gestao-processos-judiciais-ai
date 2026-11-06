package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.comunicacao.*;
import br.com.axionweb.backend.exception.AnaliseComunicacaoException;
import br.com.axionweb.backend.model.comunicacao.PesquisaComunicacao;
import br.com.axionweb.backend.model.processo.Processo;
import br.com.axionweb.backend.repository.ComunicacaoRepository;
import br.com.axionweb.backend.repository.PesquisaComunicacaoRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.*;
import static br.com.axionweb.backend.exception.AnaliseComunicacaoException.Motivo.*;

@Service
public class SinteseComunicacaoService {
    private final PesquisaComunicacaoRepository pesquisas;
    private final ComunicacaoRepository comunicacoes;
    private final ProcessoService processos;
    private final LLMService llm;
    private final ObjectMapper mapper;

    public SinteseComunicacaoService(PesquisaComunicacaoRepository pesquisas, ComunicacaoRepository comunicacoes,
            ProcessoService processos, LLMService llm, ObjectMapper mapper) {
        this.pesquisas = pesquisas; this.comunicacoes = comunicacoes;
        this.processos = processos; this.llm = llm; this.mapper = mapper;
    }

    @Transactional
    public PesquisaComunicacao registrar(Processo processo, PesquisaPublicacoesRequestDTO filtros, List<ComunicacaoSalvaDTO> publicacoes) {
        var pesquisa = new PesquisaComunicacao();
        pesquisa.processo = processo;
        pesquisa.textoBusca = filtros == null ? null : filtros.texto().strip();
        pesquisa.tribunal = processo.getTribunal();
        pesquisa.dataInicio = filtros == null ? null : filtros.dataDisponibilizacaoInicio();
        pesquisa.dataFim = filtros == null ? null : filtros.dataDisponibilizacaoFim();
        pesquisa.pesquisadaEm = Instant.now();
        pesquisa.quantidade = publicacoes.size();
        pesquisa.comunicacaoIds = mapper.writeValueAsString(publicacoes.stream().map(ComunicacaoSalvaDTO::id).toList());
        pesquisa.publicacoes = mapper.writeValueAsString(publicacoes);
        return pesquisas.saveAndFlush(pesquisa);
    }

    @Transactional(readOnly = true)
    public PesquisaComunicacaoDTO atual(Long processoId) {
        processos.buscarPorId(processoId);
        var pesquisa = pesquisas.findFirstByProcessoIdOrderByIdDesc(processoId).orElse(null);
        if (pesquisa == null) return null;
        var ids = comunicacoes.findByProcessoIdOrderByIdAsc(processoId).stream().map(c -> c.getId()).toList();
        return ids.equals(ids(pesquisa)) ? dto(pesquisa) : null;
    }

    @Transactional
    public PesquisaComunicacaoDTO gerar(Long processoId, SinteseComunicacaoRequestDTO pedido) {
        var processo = processos.buscarPorId(processoId);
        PesquisaComunicacao pesquisa;
        if (pedido.pesquisaId() != null) {
            pesquisa = buscar(processoId, pedido.pesquisaId());
        } else {
            var salvas = comunicacoes.findByProcessoIdOrderByIdAsc(processoId).stream().map(ComunicacaoSalvaDTO::de).toList();
            if (!salvas.stream().map(ComunicacaoSalvaDTO::id).toList().equals(pedido.comunicacaoIds()))
                throw new AnaliseComunicacaoException(PESQUISA_DESATUALIZADA);
            // Compatibilidade com comunicações salvas antes do histórico, sem inventar filtros.
            pesquisa = pesquisas.findFirstByProcessoIdOrderByIdDesc(processoId)
                    .filter(p -> ids(p).equals(pedido.comunicacaoIds())).orElseGet(() -> registrar(processo, null, salvas));
        }
        if (!ids(pesquisa).equals(pedido.comunicacaoIds())) throw new AnaliseComunicacaoException(PESQUISA_DESATUALIZADA);
        if (pesquisa.sintese != null) return dto(pesquisa); // Repetir o clique não consome outra chamada de IA.
        var textos = new ArrayList<String>();
        var vistos = new LinkedHashSet<String>();
        int duplicadas = 0, caracteres = 0;
        for (var publicacao : publicacoes(pesquisa)) {
            String texto = TextoComunicacao.paraAnalise(publicacao.texto());
            if (texto.isBlank()) continue;
            if (!vistos.add(texto)) { duplicadas++; continue; }
            caracteres += texto.length();
            if (caracteres > 1_000_000) throw new AnaliseComunicacaoException(EXCESSO_DE_TEXTO);
            textos.add(texto);
        }
        if (textos.isEmpty()) throw new AnaliseComunicacaoException(SEM_TEXTOS);
        var categoria = llm.sintetizarComunicacoes(textos);
        String sintese = validar(categoria, textos);
        var bloqueado = processos.bloquearParaAtualizarAnalise(processoId);
        pesquisa.sintese = sintese;
        pesquisa.geradaEm = Instant.now();
        pesquisa.comunicacoesConsideradas = textos.size();
        pesquisa.duplicadasIgnoradas = duplicadas;
        pesquisa.evidencias = mapper.writeValueAsString(categoria.itens().isEmpty() ? List.of() : categoria.itens().getFirst().evidencias());
        pesquisas.saveAndFlush(pesquisa); // @Version rejeita gravações concorrentes da mesma busca.
        bloqueado.registrarAtividade("analise", "Síntese das comunicações concluída");
        return dto(pesquisa);
    }

    private String validar(AnaliseComunicacaoDTO.Categoria categoria, List<String> textos) {
        if (categoria == null || categoria.tipo() != TipoAnaliseComunicacao.SINTESE_GERAL || categoria.itens() == null || categoria.itens().size() > 1)
            throw new AnaliseComunicacaoException(RESPOSTA_INVALIDA);
        String texto = categoria.itens().isEmpty() ? categoria.limitacao() : categoria.itens().getFirst().descricao();
        if (texto == null || texto.isBlank()) throw new AnaliseComunicacaoException(RESPOSTA_INVALIDA);
        texto = texto.strip().replaceAll("\\s+", " ");
        if (texto.length() > 1200 || texto.split("\\s+").length > 120 || texto.matches("(?s).*<[^>]+>.*") || texto.contains("**"))
            throw new AnaliseComunicacaoException(RESPOSTA_INVALIDA);
        if (!categoria.itens().isEmpty()) {
            var evidencias = categoria.itens().getFirst().evidencias();
            if (evidencias == null || evidencias.isEmpty() || evidencias.size() > 2) throw new AnaliseComunicacaoException(RESPOSTA_INVALIDA);
            for (var e : evidencias) if (e == null || e.comunicacao() < 1 || e.comunicacao() > textos.size() || e.trecho() == null || e.trecho().isBlank()
                    || e.trecho().length() > 500 || !TextoComunicacao.contemCitacao(textos.get(e.comunicacao() - 1), e.trecho()))
                throw new AnaliseComunicacaoException(RESPOSTA_INVALIDA);
        }
        return texto;
    }

    @Transactional(readOnly = true)
    public PesquisaComunicacaoDTO.Historico historico(Long processoId, int pagina) {
        processos.buscarPorId(processoId);
        if (pagina < 0 || pagina > 100000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        var resultado = pesquisas.findByProcessoIdAndSinteseIsNotNullOrderByGeradaEmDescIdDesc(processoId, PageRequest.of(pagina, 5));
        return new PesquisaComunicacaoDTO.Historico(resultado.stream().map(this::dto).toList(), pagina, resultado.hasNext());
    }

    @Transactional(readOnly = true)
    public PesquisaComunicacaoDTO.Detalhe consultar(Long processoId, Long pesquisaId) {
        processos.buscarPorId(processoId);
        var pesquisa = buscar(processoId, pesquisaId);
        return new PesquisaComunicacaoDTO.Detalhe(dto(pesquisa), publicacoes(pesquisa));
    }

    private PesquisaComunicacao buscar(Long processoId, Long pesquisaId) {
        return pesquisas.findByIdAndProcessoId(pesquisaId, processoId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
    private List<Long> ids(PesquisaComunicacao p) { return Arrays.asList(mapper.readValue(p.comunicacaoIds, Long[].class)); }
    private List<ComunicacaoSalvaDTO> publicacoes(PesquisaComunicacao p) { return Arrays.asList(mapper.readValue(p.publicacoes, ComunicacaoSalvaDTO[].class)); }
    private PesquisaComunicacaoDTO dto(PesquisaComunicacao p) {
        return new PesquisaComunicacaoDTO(p.id, p.textoBusca, p.tribunal, p.dataInicio, p.dataFim, p.pesquisadaEm,
                p.quantidade, ids(p), p.sintese, p.geradaEm, p.comunicacoesConsideradas, p.duplicadasIgnoradas,
                p.evidencias == null ? List.of() : Arrays.asList(mapper.readValue(p.evidencias, AnaliseComunicacaoDTO.Evidencia[].class)));
    }
}
