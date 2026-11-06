package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.comunicacao.AnaliseComunicacaoDTO;
import br.com.axionweb.backend.dto.comunicacao.TipoAnaliseComunicacao;
import java.util.List;

public interface LLMService {
    default AnaliseComunicacaoDTO.Categoria sintetizarComunicacoes(List<String> textos) {
        return analisarComunicacoes(textos, List.of(TipoAnaliseComunicacao.SINTESE_GERAL)).getFirst();
    }
    String sintetizarEstatisticas(br.com.axionweb.backend.dto.datajud.DataJudResponseDTO estatistica);
    List<AnaliseComunicacaoDTO.Categoria> analisarComunicacoes(List<String> textos, List<TipoAnaliseComunicacao> tiposAnalise);
}
