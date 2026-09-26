package br.com.axionweb.backend.service;

import br.com.axionweb.backend.model.usuario.Usuario;
import br.com.axionweb.backend.repository.UsuarioRepository;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioAutenticadoService {
    private final UsuarioRepository repository;

    public UsuarioAutenticadoService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public String getEmailAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AuthenticationCredentialsNotFoundException("Autenticação necessária");
        }
        return authentication.getName();
    }

    @Transactional(readOnly = true)
    public Usuario getUsuarioAutenticado() {
        return repository.findByEmail(getEmailAutenticado())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Autenticação necessária"));
    }
}
