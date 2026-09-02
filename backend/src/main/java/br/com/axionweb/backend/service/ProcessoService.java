package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.comunicacao.ParametrosComunicacaoDTO;
import br.com.axionweb.backend.model.Processo;
import br.com.axionweb.backend.repository.ProcessoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

@Service
public class ProcessoService {

    @Autowired
    ProcessoRepository repository;

    @Autowired
    @Qualifier("restComunicacao")
    RestClient restComunicacao;

    ParametrosComunicacaoDTO parametro = new ParametrosComunicacaoDTO();

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
        processoNovo.setDataDistribuicao(processo.getDataDistribuicao());

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


    public ParametrosComunicacaoDTO montarParametro(Long id){
        Processo processo = repository.findById(id).orElseThrow(() -> new RuntimeException("Processo não encontrado"));
        parametro.setNumeroProcesso(processo.getNumeroProcesso());
        parametro.setSiglaTribunal(processo.getTribunal());
        parametro.setDataDisponibilizacaoInicio(processo.getDataDistribuicao());
        parametro.setDataDisponibilizacaoFim(LocalDate.now());
        parametro.setMeio("D");
        parametro.setPagina(2);
        parametro.setPagina(100);
        return parametro;
    }

    public String solicitarAnalise(Long id){

        ParametrosComunicacaoDTO atualParametro = montarParametro(id);

        return restComunicacao.get()
                .uri(
            uriBuilder -> uriBuilder
                    .path("/api/v1/comunicacao")
                    //.queryParam("numeroProcesso", atualParametro.getNumeroProcesso())
                    .queryParam("siglaTribunal", atualParametro.getSiglaTribunal())
                    .queryParam("dataDisponibilizacaoInicio", atualParametro.getDataDisponibilizacaoInicio())
                    .queryParam("dataDisponibilizacaoFim", atualParametro.getDataDisponibilizacaoFim())
                    .queryParam("meio", atualParametro.getMeio())
                    .queryParam("pagina", atualParametro.getPagina())
                    .queryParam("itensPorPagina", atualParametro.getItensPorPagina())
                    .build()
                )
                .retrieve()
                .body(String.class);
    }





}
