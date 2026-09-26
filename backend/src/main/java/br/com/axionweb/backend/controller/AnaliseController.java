package br.com.axionweb.backend.controller;

import br.com.axionweb.backend.model.datajud.DataJud;
import br.com.axionweb.backend.service.AnaliseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/dados")
@RestController
public class AnaliseController {

    @Autowired
    AnaliseService analiseService;

    @GetMapping("/analisar/{id}")
    public String analisar(@PathVariable Long id){
        return analiseService.analisar(id);
    }
}
