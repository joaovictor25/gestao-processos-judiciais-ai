package br.com.axionweb.backend.dto.datajud;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HitsDataJudDTO {

    private TotalDataJudDTO total;
    private Double maxScore;
    private List<ResultadoDataJudDTO> hits;

    public TotalDataJudDTO getTotal() {
        return total;
    }

    public void setTotal(TotalDataJudDTO total) {
        this.total = total;
    }

    public Double getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(Double maxScore) {
        this.maxScore = maxScore;
    }

    public List<ResultadoDataJudDTO> getHits() {
        return hits;
    }

    public void setHits(List<ResultadoDataJudDTO> hits) {
        this.hits = hits;
    }
}