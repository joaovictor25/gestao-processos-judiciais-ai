package br.com.axionweb.backend.config;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.Arrays;

@Configuration
public class EnvironmentProfileGuard {

    @Bean
    @Profile("!dev & !test & !prod & !mysql")
    ApplicationRunner exigirPerfilExplicito() {
        return argumentos -> {
            throw new IllegalStateException(
                    "Ative explicitamente um perfil Spring: dev, test, mysql ou prod");
        };
    }

    @Bean
    @Profile("prod")
    ApplicationRunner validarPerfilProducao(Environment ambiente) {
        return argumentos -> {
            var ativos = Arrays.asList(ambiente.getActiveProfiles());
            if (ativos.contains("dev") || ativos.contains("test")) {
                throw new IllegalStateException("O perfil prod nao pode ser combinado com dev ou test");
            }
            String url = ambiente.getRequiredProperty("spring.datasource.url");
            if (!url.startsWith("jdbc:mysql:")) {
                throw new IllegalStateException("O perfil prod exige um datasource jdbc:mysql");
            }
        };
    }
}
