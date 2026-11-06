package br.com.axionweb.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestComunicacaoConfig {

    @Bean("restComunicacao")
    public RestClient restComunicacao(@Value("${app.djen.base-url:https://comunicaapi.pje.jus.br}") String baseUrl){
        return RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Bean("restDataJud")
    public RestClient restDataJud(@Value("${app.datajud.base-url:https://api-publica.datajud.cnj.jus.br}") String baseUrl){
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(55));
        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .defaultHeader(
                        "Authorization",
                        "APIKey cDZHYzlZa0JadVREZDJCendQbXY6SkJlTzNjLV9TRENyQk1RdnFKZGRQdw=="
                )
                .build();
    }
}
