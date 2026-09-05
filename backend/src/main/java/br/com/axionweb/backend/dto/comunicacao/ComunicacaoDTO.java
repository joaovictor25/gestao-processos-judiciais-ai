package br.com.axionweb.backend.dto.comunicacao;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

//Refazer para praticar o DTO
@JsonIgnoreProperties(ignoreUnknown = true)
public class ComunicacaoDTO {

    @JsonProperty("numero_processo")
    private String numeroProcesso;

    @JsonProperty("data_disponibilizacao")
    private LocalDate dataDisponibilizacao;

    private String siglaTribunal;

    private String tipoComunicacao;

    private String nomeOrgao;

    private Long idOrgao;

    private String tipoDocumento;

    private String nomeClasse;

    private String codigoClasse;

    private String texto;

    private String status;
}
