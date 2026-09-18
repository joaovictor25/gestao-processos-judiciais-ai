package br.com.axionweb.backend.dto.datajud;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ResultadoDataJudDTO {

    @JsonProperty("_index")
    private String index;

    @JsonProperty("_id")
    private String id;

    @JsonProperty("_score")
    private Double score;

    @JsonProperty("_source")
    private DadosDataJudDTO source;

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public DadosDataJudDTO getSource() {
        return source;
    }

    public void setSource(DadosDataJudDTO source) {
        this.source = source;
    }
}