package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.comunicacao.ComunicacaoSalvaDTO;
import br.com.axionweb.backend.dto.comunicacao.DadosComunicacaoDTO;
import br.com.axionweb.backend.dto.comunicacao.PesquisaPublicacoesRequestDTO;
import br.com.axionweb.backend.exception.ConsultaExternaException;
import br.com.axionweb.backend.model.comunicacao.Comunicacao;
import br.com.axionweb.backend.repository.ComunicacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import java.util.List;

@Service
public class PesquisaPublicacoesService {
    private final ProcessoService processos;
    private final ComunicacaoService djen;
    private final ComunicacaoRepository comunicacoes;
    private final SinteseComunicacaoService sinteses;

    public PesquisaPublicacoesService(ProcessoService processos, ComunicacaoService djen, ComunicacaoRepository comunicacoes, SinteseComunicacaoService sinteses) {
        this.processos = processos;
        this.djen = djen;
        this.comunicacoes = comunicacoes;
        this.sinteses = sinteses;
    }

    @Transactional
    public List<ComunicacaoSalvaDTO> pesquisar(Long processoId, PesquisaPublicacoesRequestDTO filtros) {
        var processo = processos.buscarPorId(processoId);
        var parametros = djen.montarParametroComunicacao(processo, filtros.texto().strip(),
                filtros.dataDisponibilizacaoInicio(), filtros.dataDisponibilizacaoFim());
        List<DadosComunicacaoDTO> encontradas;
        try {
            encontradas = djen.consumirApiComunicacao(parametros);
        } catch (RestClientException erro) {
            throw new ConsultaExternaException(ConsultaExternaException.Servico.DJEN, erro);
        }
        var novas = encontradas.stream().map(dto -> {
            var comunicacao = new Comunicacao();
            comunicacao.setProcesso(processo);
            comunicacao.setCodigoClasse(dto.getCodigoClasse());
            comunicacao.setDataDisponibilizacao(dto.getData_disponibilizacao());
            comunicacao.setDataDisponibilizacaoFormatada(dto.getDatadisponibilizacao());
            comunicacao.setIdOrgao(dto.getIdOrgao());
            comunicacao.setMeio(dto.getMeio());
            comunicacao.setMeioCompleto(dto.getMeiocompleto());
            comunicacao.setSiglaTribunal(dto.getSiglaTribunal());
            comunicacao.setStatus(dto.getStatus());
            comunicacao.setTexto(dto.getTexto());
            comunicacao.setTipoComunicacao(dto.getTipoComunicacao());
            comunicacao.setTipoDocumento(dto.getTipoDocumento());
            return comunicacao;
        }).toList();
        processos.bloquearParaAtualizarAnalise(processoId);
        comunicacoes.deleteByProcessoId(processoId);
        comunicacoes.flush();
        processo.registrarAtividade("comunicacao", "Pesquisa de publicações concluída");
        var salvas = comunicacoes.saveAllAndFlush(novas).stream().map(ComunicacaoSalvaDTO::de).toList();
        sinteses.registrar(processo, filtros, salvas);
        return salvas;
    }
}
