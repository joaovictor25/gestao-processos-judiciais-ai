package br.com.axionweb.backend.dto.auth;

import br.com.axionweb.backend.model.usuario.Role;

public record LoginResponseDTO(String token, String tipo, Long usuarioId, String nome, String email, Role role) {
    @Override
    public String toString() {
        return "LoginResponseDTO[token=PROTEGIDO, usuarioId=" + usuarioId + "]";
    }
}
