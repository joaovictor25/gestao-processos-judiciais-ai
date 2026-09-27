package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.comunicacao.AnaliseComunicacaoDTO;
import br.com.axionweb.backend.dto.comunicacao.AnaliseComunicacaoRequestDTO;
import br.com.axionweb.backend.dto.comunicacao.TipoAnaliseComunicacao;
import br.com.axionweb.backend.exception.AnaliseComunicacaoException;
import br.com.axionweb.backend.model.comunicacao.Comunicacao;
import br.com.axionweb.backend.repository.ComunicacaoRepository;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import static br.com.axionweb.backend.exception.AnaliseComunicacaoException.Motivo.*;

@Service
public class AnaliseComunicacaoService {
    private static final int MAX_CARACTERES = 500_000;
    private final ProcessoService processoService;
    private final ComunicacaoRepository comunicacoes;
    private final LLMService llm;

    public AnaliseComunicacaoService(ProcessoService processoService, ComunicacaoRepository comunicacoes, LLMService llm) {
        this.processoService = processoService;
        this.comunicacoes = comunicacoes;
        this.llm = llm;
    }

    public AnaliseComunicacaoDTO analisar(Long processoId, AnaliseComunicacaoRequestDTO requisicao) {
        processoService.buscarPorId(processoId); // Verifica o proprietário antes de ler qualquer comunicação.
        List<TipoAnaliseComunicacao> tipos = requisicao.tiposAnalise().stream().distinct().toList();
        if (tipos.isEmpty()) throw new AnaliseComunicacaoException(SEM_TEXTOS);
        List<String> textos = new ArrayList<>();
        Set<String> vistos = new LinkedHashSet<>();
        int duplicadas = 0, caracteres = 0;
        for (Comunicacao comunicacao : comunicacoes.findByProcessoIdOrderByIdAsc(processoId)) {
            String limpo = TextoComunicacao.paraAnalise(comunicacao.getTexto());
            if (limpo.isBlank()) continue;
            if (!vistos.add(limpo)) { duplicadas++; continue; }
            caracteres += limpo.length();
            if (caracteres > MAX_CARACTERES) throw new AnaliseComunicacaoException(EXCESSO_DE_TEXTO);
            textos.add(limpo);
        }
        if (textos.isEmpty()) throw new AnaliseComunicacaoException(SEM_TEXTOS);
        return new AnaliseComunicacaoDTO(textos.size(), duplicadas, llm.analisarComunicacoes(textos, tipos));
    }
}
