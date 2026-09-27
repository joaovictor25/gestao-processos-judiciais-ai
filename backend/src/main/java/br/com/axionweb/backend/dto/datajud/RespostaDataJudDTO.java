package br.com.axionweb.backend.dto.datajud;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RespostaDataJudDTO {

    private Integer took;
    @JsonProperty("timed_out")
    private Boolean timedOut;
    private HitsDataJudDTO hits;

    public Integer getTook() {
        return took;
    }

    public void setTook(Integer took) {
        this.took = took;
    }

    public Boolean getTimedOut() {
        return timedOut;
    }

    public void setTimedOut(Boolean timedOut) {
        this.timedOut = timedOut;
    }

    public HitsDataJudDTO getHits() {
        return hits;
    }

    public void setHits(HitsDataJudDTO hits) {
        this.hits = hits;
    }
}
