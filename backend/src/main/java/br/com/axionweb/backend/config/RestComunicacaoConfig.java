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
    public RestClient restComunicacao(
            @Value("${app.djen.base-url:https://comunicaapi.pje.jus.br}") String baseUrl,
            @Value("${app.djen.connect-timeout-ms:10000}") long connectTimeoutMs,
            @Value("${app.djen.read-timeout-ms:55000}") long readTimeoutMs){
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs)).build());
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl)
                .build();
    }

    @Bean("restDataJud")
    public RestClient restDataJud(
            @Value("${app.datajud.base-url:https://api-publica.datajud.cnj.jus.br}") String baseUrl,
            @Value("${app.datajud.api-key:}") String apiKey){
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(55));
        var builder = RestClient.builder()
                .requestFactory(factory)
                .baseUrl(baseUrl);
        if (apiKey != null && !apiKey.isBlank()) {
            builder.defaultHeader("Authorization", "APIKey " + apiKey.strip());
        }
        return builder.build();
    }
}
