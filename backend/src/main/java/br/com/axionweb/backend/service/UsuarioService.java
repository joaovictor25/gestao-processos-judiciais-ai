package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.auth.CadastroUsuarioRequestDTO;
import br.com.axionweb.backend.dto.auth.UsuarioResponseDTO;
import br.com.axionweb.backend.exception.EmailJaCadastradoException;
import br.com.axionweb.backend.model.usuario.Role;
import br.com.axionweb.backend.model.usuario.Usuario;
import br.com.axionweb.backend.repository.UsuarioRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

@Service
public class UsuarioService {
    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UsuarioResponseDTO cadastrar(CadastroUsuarioRequestDTO dto) {
        if (dto.senha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter no máximo 72 bytes em UTF-8");
        }
        if (repository.existsByEmail(dto.email())) {
            throw new EmailJaCadastradoException();
        }
        Usuario usuario = new Usuario();
        usuario.setNome(dto.nome());
        usuario.setEmail(dto.email());
        usuario.setSenha(passwordEncoder.encode(dto.senha()));
        usuario.setRole(Role.USER);
        try {
            return UsuarioResponseDTO.de(repository.saveAndFlush(usuario));
        } catch (DataIntegrityViolationException exception) {
            // A restrição UNIQUE também protege cadastros simultâneos do mesmo e-mail.
            throw new EmailJaCadastradoException();
        }
    }
}
