package br.com.axionweb.backend.dto.erro;

import java.util.Map;

public record ErroResponseDTO(int status, String mensagem, Map<String, String> campos) {
    public ErroResponseDTO(int status, String mensagem) {
        this(status, mensagem, Map.of());
    }
}
