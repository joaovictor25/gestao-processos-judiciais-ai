package br.com.axionweb.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final SecurityErrorHandler errorHandler;

    public JwtAuthenticationFilter(JwtService jwtService, UserDetailsService userDetailsService,
                                   SecurityErrorHandler errorHandler) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.errorHandler = errorHandler;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)) {
            chain.doFilter(request, response);
            return;
        }
        try {
            String email = jwtService.validarToken(authorization.substring(7).strip()).getSubject();
            UserDetails usuario = userDetailsService.loadUserByUsername(email);
            var authentication = UsernamePasswordAuthenticationToken.authenticated(usuario, null, usuario.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            var context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
        } catch (JwtException | AuthenticationException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
            errorHandler.commence(request, response, new BadCredentialsException("Token inválido"));
            return;
        }
        chain.doFilter(request, response);
    }
}
