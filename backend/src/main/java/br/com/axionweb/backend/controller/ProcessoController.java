package br.com.axionweb.backend.controller;

import br.com.axionweb.backend.dto.processo.ProcessoRequestDTO;
import br.com.axionweb.backend.dto.processo.ProcessoResponseDTO;
import br.com.axionweb.backend.service.ProcessoService;
import br.com.axionweb.backend.service.ConsultaCnjService;
import br.com.axionweb.backend.dto.processo.ConsultaCnjResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequestMapping("/api/processo")
@RestController
public class ProcessoController {
    @Autowired
    ProcessoService processoService;
    @Autowired
    ConsultaCnjService consultaCnjService;

    @GetMapping("/consultar-datajud")
    public ConsultaCnjResponseDTO consultarDataJud(@RequestParam String numeroProcesso) {
        return consultaCnjService.consultar(numeroProcesso);
    }

    @PostMapping({"/cadastrar"})
    public ProcessoResponseDTO cadastrar(@RequestBody ProcessoRequestDTO dto) {
        return ProcessoResponseDTO.de(processoService.cadastrar(dto));
    }

    @PutMapping("/atualizar/{id}")
    public ProcessoResponseDTO atualizar(@PathVariable Long id, @RequestBody ProcessoRequestDTO dto) {
        return ProcessoResponseDTO.de(processoService.atualizar(id, dto));
    }

    @DeleteMapping("/deletar/{id}")
    public void deletar(@PathVariable Long id) {
        processoService.excluir(id);
    }

    @GetMapping("/listar-usuario")
    public List<ProcessoResponseDTO> listarPorUsuario() {
        return processoService.listarPorUsuario().stream().map(ProcessoResponseDTO::de).toList();
    }

    @GetMapping("/buscar-num-processo")
    public ProcessoResponseDTO buscarPorNumProcesso(@RequestParam String numeroProcesso) {
        return ProcessoResponseDTO.de(processoService.buscarPorNumProcesso(numeroProcesso));
    }

    @GetMapping("/{id}")
    public ProcessoResponseDTO buscarPorId(@PathVariable Long id) {
        return ProcessoResponseDTO.de(processoService.buscarPorId(id));
    }
}
