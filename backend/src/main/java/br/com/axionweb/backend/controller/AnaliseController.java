package br.com.axionweb.backend.controller;

import br.com.axionweb.backend.dto.analise.AnaliseResponseDTO;
import br.com.axionweb.backend.dto.comunicacao.ComunicacaoSalvaDTO;
import br.com.axionweb.backend.dto.datajud.DataJudResponseDTO;
import br.com.axionweb.backend.service.AnaliseService;
import br.com.axionweb.backend.service.AnaliseComunicacaoService;
import br.com.axionweb.backend.dto.comunicacao.AnaliseComunicacaoDTO;
import br.com.axionweb.backend.dto.comunicacao.AnaliseComunicacaoRequestDTO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/api/dados")
@RestController
public class AnaliseController {

    @Autowired
    AnaliseService analiseService;
    @Autowired
    AnaliseComunicacaoService analiseComunicacaoService;

    @PostMapping("/comunicacoes/{processoId}/analise-ia")
    public AnaliseComunicacaoDTO analisarComunicacoes(@PathVariable Long processoId,
                                                     @Valid @RequestBody AnaliseComunicacaoRequestDTO requisicao) {
        return analiseComunicacaoService.analisar(processoId, requisicao);
    }

    //Rota antiga
    @GetMapping("/analisar/{id}")
    public String analisar(@PathVariable Long id){
        return analiseService.analisar(id);
    }

    @PostMapping("/analisar/{id}")
    public String solicitarAnalise(@PathVariable Long id) {
        return analiseService.analisar(id);
    }

    @GetMapping("/analise/{processoId}")
    public AnaliseResponseDTO consultar(@PathVariable Long processoId) {
        return analiseService.consultar(processoId);
    }

    @GetMapping("/estatisticas/{processoId}")
    public List<DataJudResponseDTO> consultarEstatisticas(@PathVariable Long processoId) {
        return analiseService.consultarEstatisticas(processoId);
    }

    @GetMapping("/comunicacoes/{processoId}")
    public List<ComunicacaoSalvaDTO> consultarComunicacoes(@PathVariable Long processoId) {
        return analiseService.consultarComunicacoes(processoId);
    }
}
