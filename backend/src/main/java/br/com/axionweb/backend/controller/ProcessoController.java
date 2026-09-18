package br.com.axionweb.backend.controller;

import br.com.axionweb.backend.dto.datajud.DadosDataJudDTO;
import br.com.axionweb.backend.model.Processo;
import br.com.axionweb.backend.service.ProcessoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/processo")
@RestController
public class ProcessoController {

    @Autowired
    ProcessoService processoService;

    @PostMapping("/cadastar")
    public Processo cadastrar(@RequestBody Processo processo){
        return processoService.cadastrar(processo);
    }

    @PutMapping("/atualizar/{id}")
    public Processo atualizar(@PathVariable Long id, @RequestBody Processo processo){
        return processoService.atualizar(id, processo);
    }

    @DeleteMapping("/deletar/{id}")
    public void deletar(@PathVariable Long id){
        processoService.excluir(id);
    }

    @GetMapping("/listar-usuario")
    public List<Processo> listarPorUsuario(){
        return processoService.listarPorUsuario();
    }

    @GetMapping("/buscar-num-processo")
    public Processo buscarPorNumProcesso(@RequestParam String numeroProcesso){
        return processoService.buscarPorNumProcesso(numeroProcesso);
    }

    @GetMapping("/solicitar-analise/{id}")
    public List<DadosDataJudDTO> solicitarAnalise(@PathVariable Long id){
        return processoService.analisar(id);
    }
}
