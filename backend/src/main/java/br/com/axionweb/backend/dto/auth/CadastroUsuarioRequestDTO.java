package br.com.axionweb.backend.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record CadastroUsuarioRequestDTO(
        @NotBlank @Size(max = 255) String nome,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 6, max = 72) String senha) {

    public CadastroUsuarioRequestDTO {
        nome = nome == null ? null : nome.strip();
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }

    @Override
    public String toString() {
        return "CadastroUsuarioRequestDTO[senha=PROTEGIDA]";
    }
}
