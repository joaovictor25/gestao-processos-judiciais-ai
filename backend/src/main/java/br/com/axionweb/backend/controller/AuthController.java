package br.com.axionweb.backend.controller;

import br.com.axionweb.backend.dto.auth.*;
import br.com.axionweb.backend.service.AuthService;
import br.com.axionweb.backend.service.UsuarioAutenticadoService;
import br.com.axionweb.backend.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UsuarioService usuarioService;
    private final AuthService authService;
    private final UsuarioAutenticadoService usuarioAutenticadoService;

    public AuthController(UsuarioService usuarioService, AuthService authService,
                          UsuarioAutenticadoService usuarioAutenticadoService) {
        this.usuarioService = usuarioService;
        this.authService = authService;
        this.usuarioAutenticadoService = usuarioAutenticadoService;
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponseDTO cadastrar(@Valid @RequestBody CadastroUsuarioRequestDTO dto) {
        return usuarioService.cadastrar(dto);
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO dto) {
        return authService.login(dto);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(authorization);
    }

    @GetMapping("/me")
    public UsuarioResponseDTO me() {
        return UsuarioResponseDTO.de(usuarioAutenticadoService.getUsuarioAutenticado());
    }
}
