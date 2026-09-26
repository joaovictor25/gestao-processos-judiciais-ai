package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.auth.LoginRequestDTO;
import br.com.axionweb.backend.dto.auth.LoginResponseDTO;
import br.com.axionweb.backend.model.usuario.Usuario;
import br.com.axionweb.backend.repository.UsuarioRepository;
import br.com.axionweb.backend.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository repository;
    private final JwtService jwtService;

    public AuthService(AuthenticationManager authenticationManager, UsuarioRepository repository, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.repository = repository;
        this.jwtService = jwtService;
    }

    public LoginResponseDTO login(LoginRequestDTO dto) {
        if (dto.senha().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new BadCredentialsException("Credenciais inválidas");
        }
        var authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(dto.email(), dto.senha()));
        Usuario usuario = repository.findByEmail(authentication.getName())
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));
        return new LoginResponseDTO(jwtService.gerarToken(usuario.getEmail()), "Bearer",
                usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getRole());
    }
}
