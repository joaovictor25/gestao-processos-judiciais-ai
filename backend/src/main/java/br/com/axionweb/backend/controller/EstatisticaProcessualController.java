package br.com.axionweb.backend.controller;

import br.com.axionweb.backend.dto.datajud.DadosDataJudDTO;
import br.com.axionweb.backend.dto.datajud.EstatisticasProcessuaisDTO;
import br.com.axionweb.backend.service.EstatisticaProcessualService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/estatisticas")
public class EstatisticaProcessualController {

    private final EstatisticaProcessualService service;

    public EstatisticaProcessualController(
            br.com.axionweb.backend.service.EstatisticaProcessualService service) {

        this.service = service;
    }


    @PostMapping("/calcular")
    public EstatisticasProcessuaisDTO calcular(
            @RequestBody List<DadosDataJudDTO> processos) {

        return service.calcular(processos);
    }
}
