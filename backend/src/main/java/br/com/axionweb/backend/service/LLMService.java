package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.comunicacao.AnaliseComunicacaoDTO;
import br.com.axionweb.backend.dto.comunicacao.TipoAnaliseComunicacao;
import java.util.List;

public interface LLMService {
    List<AnaliseComunicacaoDTO.Categoria> analisarComunicacoes(List<String> textos, List<TipoAnaliseComunicacao> tiposAnalise);
}
