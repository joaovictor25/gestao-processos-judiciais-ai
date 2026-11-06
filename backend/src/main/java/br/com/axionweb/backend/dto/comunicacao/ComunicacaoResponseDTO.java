package br.com.axionweb.backend.dto.comunicacao;

import java.util.List;

public class ComunicacaoResponseDTO {

    private String status;
    private String message;
    private Integer count;
    private List<ComunicacaoDTO> items;

    public ComunicacaoResponseDTO() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public List<ComunicacaoDTO> getItems() {
        return items;
    }

    public void setItems(List<ComunicacaoDTO> items) {
        this.items = items;
    }
}
