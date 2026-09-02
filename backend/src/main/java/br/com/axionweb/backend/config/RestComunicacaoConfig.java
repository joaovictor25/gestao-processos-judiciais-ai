package br.com.axionweb.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestComunicacaoConfig {

    @Bean("restComunicacao")
    public RestClient restComunicacao(){
        return RestClient.builder()
                .baseUrl("https://comunicaapi.pje.jus.br")
                .build();
    }
}
