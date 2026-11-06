package br.com.axionweb.backend.controller;

import br.com.axionweb.backend.dto.comunicacao.*;
import br.com.axionweb.backend.service.ExecucaoAnaliseService;
import br.com.axionweb.backend.service.SinteseComunicacaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dados/comunicacoes/{processoId}")
public class SinteseComunicacaoController {
    private final SinteseComunicacaoService sinteses;
    private final ExecucaoAnaliseService execucoes;
    public SinteseComunicacaoController(SinteseComunicacaoService sinteses, ExecucaoAnaliseService execucoes) {
        this.sinteses = sinteses; this.execucoes = execucoes;
    }
    @GetMapping("/pesquisas/atual")
    public ResponseEntity<PesquisaComunicacaoDTO> atual(@PathVariable Long processoId) {
        var atual = sinteses.atual(processoId);
        return atual == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(atual);
    }
    @GetMapping("/pesquisas/{pesquisaId}")
    public PesquisaComunicacaoDTO.Detalhe consultar(@PathVariable Long processoId, @PathVariable Long pesquisaId) {
        return sinteses.consultar(processoId, pesquisaId);
    }
    @GetMapping("/sinteses")
    public PesquisaComunicacaoDTO.Historico historico(@PathVariable Long processoId, @RequestParam(defaultValue = "0") int pagina) {
        return sinteses.historico(processoId, pagina);
    }
    @PostMapping("/sintese-ia")
    public PesquisaComunicacaoDTO gerar(@PathVariable Long processoId, @Valid @RequestBody SinteseComunicacaoRequestDTO pedido) {
        return execucoes.executar(processoId, () -> sinteses.gerar(processoId, pedido));
    }
}
