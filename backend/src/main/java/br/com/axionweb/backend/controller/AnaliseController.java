package br.com.axionweb.backend.controller;

import br.com.axionweb.backend.dto.analise.AnaliseResponseDTO;
import br.com.axionweb.backend.dto.comunicacao.ComunicacaoSalvaDTO;
import br.com.axionweb.backend.dto.datajud.DataJudResponseDTO;
import br.com.axionweb.backend.service.AnaliseService;
import br.com.axionweb.backend.service.AnaliseComunicacaoService;
import br.com.axionweb.backend.dto.comunicacao.AnaliseComunicacaoDTO;
import br.com.axionweb.backend.dto.comunicacao.AnaliseComunicacaoRequestDTO;
import br.com.axionweb.backend.dto.comunicacao.PesquisaPublicacoesRequestDTO;
import br.com.axionweb.backend.service.PesquisaPublicacoesService;
import br.com.axionweb.backend.service.DataJudService;
import br.com.axionweb.backend.service.ProcessoService;
import br.com.axionweb.backend.dto.datajud.ParametroConsultaDataJudDTO;
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
    @Autowired
    PesquisaPublicacoesService pesquisaPublicacoesService;
    @Autowired
    DataJudService dataJudService;
    @Autowired
    ProcessoService processoService;

    @Autowired
    br.com.axionweb.backend.service.SinteseDataJudService sinteseDataJudService;

    @PostMapping("/estatisticas/{processoId}/sintese-ia")
    public br.com.axionweb.backend.dto.datajud.SinteseDataJudDTO sintetizarEstatisticas(@PathVariable Long processoId,
            @Valid @RequestBody br.com.axionweb.backend.dto.datajud.SinteseDataJudRequestDTO requisicao) {
        return execucoes.executar(processoId, () -> sinteseDataJudService.gerar(processoId, requisicao.estatisticaId()));
    }

    @Autowired
    br.com.axionweb.backend.service.ExecucaoAnaliseService execucoes;

    @Autowired
    br.com.axionweb.backend.service.GeracaoEstatisticaService geracoes;

    @PostMapping("/estatisticas/{processoId}/solicitacoes")
    public org.springframework.http.ResponseEntity<br.com.axionweb.backend.dto.datajud.SolicitacaoEstatisticaDTO> solicitarEstatisticas(
            @PathVariable Long processoId, @Valid @RequestBody br.com.axionweb.backend.dto.datajud.GerarEstatisticasRequestDTO filtros) {
        var solicitacao = geracoes.solicitar(processoId, filtros);
        return org.springframework.http.ResponseEntity.status(solicitacao.status().equals("CONCLUIDA") ? 200 : 202)
                .header("Location", "/api/dados/estatisticas/" + processoId + "/solicitacoes/" + solicitacao.id())
                .body(solicitacao);
    }

    @GetMapping("/estatisticas/{processoId}/solicitacoes/atual")
    public org.springframework.http.ResponseEntity<br.com.axionweb.backend.dto.datajud.SolicitacaoEstatisticaDTO> geracaoAtual(@PathVariable Long processoId) {
        var atual = geracoes.atual(processoId);
        return atual == null ? org.springframework.http.ResponseEntity.noContent().build() : org.springframework.http.ResponseEntity.ok(atual);
    }

    @GetMapping("/estatisticas/{processoId}/solicitacoes/{solicitacaoId}")
    public br.com.axionweb.backend.dto.datajud.SolicitacaoEstatisticaDTO consultarGeracao(@PathVariable Long processoId, @PathVariable String solicitacaoId) {
        return geracoes.consultar(processoId, solicitacaoId);
    }

    @GetMapping("/estatisticas/{processoId}/sinteses")
    public br.com.axionweb.backend.dto.datajud.HistoricoSintesesDTO historicoSinteses(@PathVariable Long processoId,
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int pagina) {
        return sinteseDataJudService.historico(processoId, pagina);
    }

    @GetMapping("/estatisticas/{processoId}/{estatisticaId}")
    public DataJudResponseDTO consultarExecucao(@PathVariable Long processoId, @PathVariable Long estatisticaId) {
        return sinteseDataJudService.consultar(processoId, estatisticaId);
    }

    @GetMapping("/estatisticas/{processoId}/parametros")
    public List<ParametroConsultaDataJudDTO> consultarParametros(@PathVariable Long processoId) {
        return dataJudService.descreverConsulta(processoService.buscarPorId(processoId));
    }

    @PostMapping("/comunicacoes/{processoId}/pesquisar")
    public List<ComunicacaoSalvaDTO> pesquisarPublicacoes(@PathVariable Long processoId,
                                                        @Valid @RequestBody PesquisaPublicacoesRequestDTO filtros) {
        return pesquisaPublicacoesService.pesquisar(processoId, filtros);
    }

    @PostMapping("/comunicacoes/{processoId}/analise-ia")
    public AnaliseComunicacaoDTO analisarComunicacoes(@PathVariable Long processoId,
                                                     @Valid @RequestBody AnaliseComunicacaoRequestDTO requisicao) {
        return execucoes.executar(processoId, () -> analiseComunicacaoService.analisar(processoId, requisicao));
    }

    //Rota antiga
    @GetMapping("/analisar/{id}")
    public String analisar(@PathVariable Long id){
        return execucoes.executar(id, () -> analiseService.analisar(id));
    }

    @PostMapping("/analisar/{id}")
    public String solicitarAnalise(@PathVariable Long id) {
        return execucoes.executar(id, () -> analiseService.analisar(id));
    }

    @GetMapping("/analise/{processoId}")
    public AnaliseResponseDTO consultar(@PathVariable Long processoId) {
        return analiseService.consultar(processoId);
    }

    @GetMapping("/estatisticas/{processoId}")
    public List<DataJudResponseDTO> consultarEstatisticas(@PathVariable Long processoId) {
        return analiseService.consultarEstatisticas(processoId);
    }

    @PostMapping("/estatisticas/{processoId}/gerar")
    public List<DataJudResponseDTO> gerarEstatisticas(@PathVariable Long processoId,
            @Valid @RequestBody br.com.axionweb.backend.dto.datajud.GerarEstatisticasRequestDTO filtros) {
        return execucoes.executar(processoId, () -> analiseService.gerarEstatisticas(processoId, filtros));
    }

    @GetMapping("/comunicacoes/{processoId}")
    public List<ComunicacaoSalvaDTO> consultarComunicacoes(@PathVariable Long processoId) {
        return analiseService.consultarComunicacoes(processoId);
    }
}
