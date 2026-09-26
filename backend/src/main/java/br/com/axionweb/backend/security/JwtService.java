package br.com.axionweb.backend.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final String issuer;
    private final Duration validade;

    public JwtService(@Value("${app.jwt.secret:}") String segredo,
                      @Value("${app.jwt.issuer:gestao-processos}") String issuer,
                      @Value("${app.jwt.validade:PT1H}") Duration validade,
                      Environment environment) {
        if (validade.isZero() || validade.isNegative() || issuer.isBlank()) {
            throw new IllegalArgumentException("Configuração de validade/emissor JWT inválida");
        }
        byte[] chave;
        if (segredo.isBlank()) {
            if (!environment.acceptsProfiles(Profiles.of("dev"))) {
                throw new IllegalStateException("Configure JWT_SECRET em Base64 com pelo menos 32 bytes");
            }
            chave = new byte[32];
            new SecureRandom().nextBytes(chave);
            log.warn("JWT_SECRET ausente no perfil dev: chave temporária gerada; tokens expiram ao reiniciar");
        } else {
            try {
                chave = Base64.getDecoder().decode(segredo);
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("JWT_SECRET deve ser uma chave Base64 válida");
            }
            if (chave.length < 32) {
                throw new IllegalArgumentException("JWT_SECRET deve conter pelo menos 32 bytes após decodificar Base64");
            }
        }
        SecretKey secretKey = new SecretKeySpec(chave, "HmacSHA256");
        this.encoder = NimbusJwtEncoder.withSecretKey(secretKey).algorithm(MacAlgorithm.HS256).build();
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(secretKey).macAlgorithm(MacAlgorithm.HS256).build();
        jwtDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(Duration.ZERO),
                new JwtIssuerValidator(issuer),
                new JwtClaimValidator<String>("sub", subject -> subject != null && !subject.isBlank()),
                new JwtClaimValidator<Instant>("exp", expiration -> expiration != null)));
        this.decoder = jwtDecoder;
        this.issuer = issuer;
        this.validade = validade;
    }

    public String gerarToken(String email) {
        Instant agora = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer).subject(email)
                .issuedAt(agora).expiresAt(agora.plus(validade))
                .build();
        return encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    public Jwt validarToken(String token) {
        return decoder.decode(token);
    }
}
