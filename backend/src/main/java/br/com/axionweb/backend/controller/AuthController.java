package br.com.axionweb.backend.controller;

import br.com.axionweb.backend.dto.auth.*;
import br.com.axionweb.backend.security.ProtecaoAutenticacaoService;
import br.com.axionweb.backend.service.AuthService;
import br.com.axionweb.backend.service.UsuarioAutenticadoService;
import br.com.axionweb.backend.service.UsuarioService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UsuarioService usuarioService;
    private final AuthService authService;
    private final UsuarioAutenticadoService usuarioAutenticadoService;
    private final ProtecaoAutenticacaoService protecao;

    public AuthController(UsuarioService usuarioService, AuthService authService,
                          UsuarioAutenticadoService usuarioAutenticadoService,
                          ProtecaoAutenticacaoService protecao) {
        this.usuarioService = usuarioService;
        this.authService = authService;
        this.usuarioAutenticadoService = usuarioAutenticadoService;
        this.protecao = protecao;
    }

    @PostMapping("/cadastro")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponseDTO cadastrar(@Valid @RequestBody CadastroUsuarioRequestDTO dto, HttpServletRequest request) {
        protecao.verificarCadastro(request.getRemoteAddr());
        return usuarioService.cadastrar(dto);
    }

    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequestDTO dto, HttpServletRequest request) {
        // O IP vem do proxy reverso confiável quando server.forward-headers-strategy está ativo.
        protecao.verificarLogin(request.getRemoteAddr(), dto.email());
        try {
            var resposta = authService.login(dto);
            protecao.registrarSucessoLogin(dto.email());
            return resposta;
        } catch (AuthenticationException falha) {
            protecao.registrarFalhaLogin(dto.email());
            throw falha;
        }
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
