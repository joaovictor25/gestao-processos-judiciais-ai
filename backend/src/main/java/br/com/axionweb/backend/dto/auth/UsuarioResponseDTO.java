package br.com.axionweb.backend.dto.auth;

import br.com.axionweb.backend.model.usuario.Role;
import br.com.axionweb.backend.model.usuario.Usuario;

public record UsuarioResponseDTO(Long id, String nome, String email, Role role) {
    public static UsuarioResponseDTO de(Usuario usuario) {
        return new UsuarioResponseDTO(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getRole());
    }
}
