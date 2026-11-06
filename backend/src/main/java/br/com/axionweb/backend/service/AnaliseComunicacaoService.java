package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.comunicacao.AnaliseComunicacaoDTO;
import br.com.axionweb.backend.dto.comunicacao.AnaliseComunicacaoRequestDTO;
import br.com.axionweb.backend.dto.comunicacao.TipoAnaliseComunicacao;
import br.com.axionweb.backend.dto.comunicacao.ComunicacaoConsultadaDTO;
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
    private static final int MAX_CARACTERES = 1_000_000;
    private final ProcessoService processoService;
    private final ComunicacaoRepository comunicacoes;
    private final LLMService llm;

    public AnaliseComunicacaoService(ProcessoService processoService, ComunicacaoRepository comunicacoes, LLMService llm) {
        this.processoService = processoService;
        this.comunicacoes = comunicacoes;
        this.llm = llm;
    }

    public AnaliseComunicacaoDTO analisar(Long processoId, AnaliseComunicacaoRequestDTO requisicao) {
        processoService.buscarPorId(processoId); // Verifica o proprietário antes de ler os documentos.
        List<TipoAnaliseComunicacao> tipos = requisicao.tiposAnalise().stream().distinct().toList();
        if (tipos.isEmpty()) throw new AnaliseComunicacaoException(SEM_TEXTOS);
        var encontradas = comunicacoes.findByProcessoIdOrderByIdAsc(processoId);
        // Uma pesquisa em outra aba pode substituir os documentos. Nunca analisa outro conjunto silenciosamente.
        if (!encontradas.stream().map(Comunicacao::getId).toList().equals(requisicao.comunicacaoIds()))
            throw new AnaliseComunicacaoException(PESQUISA_DESATUALIZADA);
        List<String> textos = new ArrayList<>();
        List<ComunicacaoConsultadaDTO> fontes = new ArrayList<>();
        Set<String> vistos = new LinkedHashSet<>();
        int duplicadas = 0, caracteres = 0;
        for (Comunicacao comunicacao : encontradas) {
            String limpo = TextoComunicacao.paraAnalise(comunicacao.getTexto());
            if (limpo.isBlank()) continue;
            if (!vistos.add(limpo)) { duplicadas++; continue; }
            caracteres += limpo.length();
            if (caracteres > MAX_CARACTERES) throw new AnaliseComunicacaoException(EXCESSO_DE_TEXTO);
            textos.add(limpo);
            fontes.add(ComunicacaoConsultadaDTO.de(textos.size(), comunicacao, limpo));
        }
        if (textos.isEmpty()) throw new AnaliseComunicacaoException(SEM_TEXTOS);
        var categorias = llm.analisarComunicacoes(textos, tipos);
        processoService.registrarAnaliseIA(processoId);
        return new AnaliseComunicacaoDTO(textos.size(), duplicadas, categorias, List.copyOf(fontes));
    }
}
