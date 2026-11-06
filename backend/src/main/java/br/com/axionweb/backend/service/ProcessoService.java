package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.processo.ProcessoRequestDTO;
import br.com.axionweb.backend.exception.ProcessoNaoEncontradoException;
import br.com.axionweb.backend.model.processo.Processo;
import br.com.axionweb.backend.repository.ComunicacaoRepository;
import br.com.axionweb.backend.repository.DataJudRepository;
import br.com.axionweb.backend.repository.ProcessoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProcessoService {
    @Autowired
    ProcessoRepository repository;

    @Autowired
    DataJudRepository dataJudRepository;

    @Autowired
    ComunicacaoRepository comunicacaoRepository;

    @Autowired
    UsuarioAutenticadoService usuarioAutenticadoService;

    @Transactional
    public Processo cadastrar(ProcessoRequestDTO dto) {
        Processo processo = new Processo();
        dto.aplicarEm(processo);
        processo.setUsuario(usuarioAutenticadoService.getUsuarioAutenticado());
        return repository.save(processo);
    }

    @Transactional(readOnly = true)
    public Processo buscarPorId(Long id) {
        return repository.findByIdAndUsuarioEmail(id, usuarioAutenticadoService.getEmailAutenticado())
                .orElseThrow(ProcessoNaoEncontradoException::new);
    }

    @Transactional
    public Processo bloquearParaAtualizarAnalise(Long id) {
        return repository.findWithLockByIdAndUsuarioEmail(id, usuarioAutenticadoService.getEmailAutenticado())
                .orElseThrow(ProcessoNaoEncontradoException::new);
    }

    @Transactional
    public Processo atualizar(Long id, ProcessoRequestDTO dto) {
        Processo processo = buscarPorId(id);
        dto.aplicarEm(processo);
        processo.registrarAtividade("cadastro", "Cadastro atualizado");
        return repository.save(processo);
    }

    @Transactional
    public void registrarAnaliseIA(Long id) {
        bloquearParaAtualizarAnalise(id).registrarAtividade("analise", "Análise das comunicações concluída");
    }

    @Transactional
    public void excluir(Long id) {
        Processo processo = buscarPorId(id);
        comunicacaoRepository.deleteByProcessoId(processo.getId());
        dataJudRepository.deleteByProcessoId(processo.getId());
        repository.delete(processo);
    }

    @Transactional(readOnly = true)
    public List<Processo> listarPorUsuario() {
        return repository.findByUsuarioEmailOrderByIdAsc(usuarioAutenticadoService.getEmailAutenticado());
    }

    @Transactional(readOnly = true)
    public Processo buscarPorNumProcesso(String numeroProcesso) {
        return repository.findFirstByNumeroProcessoAndUsuarioEmailOrderByIdAsc(
                        numeroProcesso, usuarioAutenticadoService.getEmailAutenticado())
                .orElseThrow(ProcessoNaoEncontradoException::new);
    }
}
