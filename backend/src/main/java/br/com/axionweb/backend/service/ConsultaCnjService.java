package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.datajud.DadosDataJudDTO;
import br.com.axionweb.backend.dto.datajud.RespostaDataJudDTO;
import br.com.axionweb.backend.dto.processo.ConsultaCnjResponseDTO;
import br.com.axionweb.backend.dto.processo.ConsultaCnjResponseDTO.Classificacao;
import br.com.axionweb.backend.dto.processo.ConsultaCnjResponseDTO.Registro;
import br.com.axionweb.backend.exception.ConsultaCnjException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import static br.com.axionweb.backend.exception.ConsultaCnjException.Motivo.*;

@Service
public class ConsultaCnjService {
    private static final Logger log = LoggerFactory.getLogger(ConsultaCnjService.class);
    private final DataJudService dataJudService;
    public ConsultaCnjService(DataJudService dataJudService) { this.dataJudService = dataJudService; }

    public ConsultaCnjResponseDTO consultar(String numeroProcesso) {
        NumeroCnj numero = new NumeroCnj(numeroProcesso);
        String tribunal = numero.tribunalOrigem();
        RespostaDataJudDTO resposta;
        try {
            resposta = dataJudService.buscarNumeroExato(numero);
        } catch (RestClientException erro) {
            Integer status = erro instanceof RestClientResponseException http ? http.getStatusCode().value() : null;
            log.warn("Consulta de CNJ no DataJud falhou: tribunal={}, tipo={}, httpExterno={}", tribunal, erro.getClass().getSimpleName(), status);
            throw new ConsultaCnjException(Integer.valueOf(429).equals(status) ? CNJ_CONSULTA_LIMITADA : CNJ_CONSULTA_INDISPONIVEL);
        }
        // Confere o número retornado, mesmo utilizando o filtro por número na API.
        List<Registro> registros = resposta.getHits().getHits().stream().filter(Objects::nonNull)
                .map(hit -> hit.getSource()).filter(Objects::nonNull)
                .filter(dado -> mesmoNumero(dado.getNumeroProcesso(), numero.digitos()))
                .map(dado -> converter(dado, tribunal)).distinct().toList();
        if (registros.isEmpty()) throw new ConsultaCnjException(CNJ_NAO_ENCONTRADO);
        var total = resposta.getHits().getTotal();
        boolean limite = total != null && total.getValue() != null && total.getValue() > resposta.getHits().getHits().size();
        return new ConsultaCnjResponseDTO(numero.formatado(), tribunal, registros, limite);
    }

    private boolean mesmoNumero(String retornado, String solicitado) {
        return retornado != null && retornado.replaceAll("[.\\-\\s]", "").equals(solicitado);
    }

    private Registro converter(DadosDataJudDTO dado, String tribunal) {
        var orgao = dado.getOrgaoJulgador();
        var classe = dado.getClasse();
        List<Classificacao> assuntos = dado.getAssuntos() == null ? List.of() : dado.getAssuntos().stream()
                .filter(Objects::nonNull).map(item -> classificacao(item.getNome(), item.getCodigo())).filter(Objects::nonNull).distinct().toList();
        return new Registro(texto(dado.getTribunal()) == null ? tribunal : dado.getTribunal().trim(), texto(dado.getGrau()),
                orgao == null ? null : classificacao(orgao.getNome(), orgao.getCodigo()),
                classe == null ? null : classificacao(classe.getNome(), classe.getCodigo()), assuntos, data(dado.getDataAjuizamento()));
    }

    private Classificacao classificacao(String nome, Object codigo) {
        String valorNome = texto(nome), valorCodigo = codigo == null ? null : texto(codigo.toString());
        return valorNome == null && valorCodigo == null ? null : new Classificacao(valorNome, valorCodigo);
    }
    private String texto(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }

    private LocalDate data(String valor) {
        if (texto(valor) == null) return null;
        try {
            if (valor.matches("\\d{14}")) return LocalDateTime.parse(valor, DateTimeFormatter.ofPattern("uuuuMMddHHmmss")
                    .withResolverStyle(java.time.format.ResolverStyle.STRICT)).toLocalDate();
            // Data de calendário da origem, sem deslocar o dia por fuso do navegador.
            return LocalDate.parse(valor.length() >= 10 ? valor.substring(0, 10) : valor);
        } catch (DateTimeParseException erro) { return null; }
    }
}
