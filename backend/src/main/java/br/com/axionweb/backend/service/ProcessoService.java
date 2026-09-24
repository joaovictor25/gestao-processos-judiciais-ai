package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.comunicacao.DadosComunicacaoDTO;

import br.com.axionweb.backend.dto.datajud.DadosDataJudDTO;
import br.com.axionweb.backend.model.Processo;
import br.com.axionweb.backend.repository.ProcessoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProcessoService {

    @Autowired
    ProcessoRepository repository;



    @Autowired
    AnaliseService analise;

    //Cadastro dos processos
    public Processo cadastrar(Processo processo){
        return repository.save(processo);
    }

    //Alteração das informações do processo que estão salvas no banco
    public Processo atualizar(Long id, Processo processo){

        Processo processoNovo = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Processo não encontrado"));

        processoNovo.setNumeroProcesso(processo.getNumeroProcesso());
        processoNovo.setClienteVinculado(processo.getClienteVinculado());
        processoNovo.setPosicaoCliente(processo.getPosicaoCliente());
        processoNovo.setTribunal(processo.getTribunal());
        processoNovo.setOrgaoJulgador(processo.getOrgaoJulgador());
        processoNovo.setComarca(processo.getComarca());
        processoNovo.setUf(processo.getUf());
        processoNovo.setClasseProcessual(processo.getClasseProcessual());
        processoNovo.setAssunto(processo.getAssunto());
        processoNovo.setAreaAtuacao(processo.getAreaAtuacao());
        processoNovo.setStatus(processo.getStatus());
        processoNovo.setValorCausa(processo.getValorCausa());
        processoNovo.setNomeMagistrado(processo.getNomeMagistrado());
        processoNovo.setDataInicio(processo.getDataInicio());

        return repository.save(processoNovo);
    }

    //Excluir processo
    public void excluir(Long id){
        repository.deleteById(id);
    }

    //Listar todos os processos do usuario
    public List<Processo> listarPorUsuario(){
        return repository.findAll();
    }

    //Buscar processo pelo número CNJ
    public Processo buscarPorNumProcesso(String numeroProcesso){
        return repository.findByNumeroProcesso(numeroProcesso);
    }






    public List<DadosDataJudDTO> analisar(Long id){
        return analise.solicitarAnalise(id);
    }
}