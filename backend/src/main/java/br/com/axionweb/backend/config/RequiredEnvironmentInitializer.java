package br.com.axionweb.backend.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

import java.util.Arrays;
import java.util.Set;

/** Valida o ambiente antes da criacao do DataSource e de qualquer conexao de banco. */
public class RequiredEnvironmentInitializer
        implements ApplicationContextInitializer<ConfigurableApplicationContext>, Ordered {
    private static final Set<String> PERFIS_VALIDOS = Set.of("dev", "test", "mysql", "prod");

    @Override
    public void initialize(ConfigurableApplicationContext contexto) {
        ConfigurableEnvironment ambiente = contexto.getEnvironment();
        var perfis = Set.copyOf(Arrays.asList(ambiente.getActiveProfiles()));
        if (perfis.stream().noneMatch(PERFIS_VALIDOS::contains)) {
            throw new IllegalStateException(
                    "Ative explicitamente um perfil Spring: dev, test, mysql ou prod");
        }
        if (!perfis.contains("prod")) return;
        if (perfis.contains("dev") || perfis.contains("test")) {
            throw new IllegalStateException("O perfil prod nao pode ser combinado com dev ou test");
        }

        String mysqlUrl = obrigatoria(ambiente, "MYSQL_URL");
        obrigatoria(ambiente, "MYSQL_USER");
        obrigatoria(ambiente, "MYSQL_PASSWORD");
        obrigatoria(ambiente, "JWT_SECRET");
        obrigatoria(ambiente, "DATAJUD_API_KEY");
        if (!mysqlUrl.startsWith("jdbc:mysql:")) {
            throw new IllegalStateException("MYSQL_URL deve usar jdbc:mysql no perfil prod");
        }
    }

    private String obrigatoria(ConfigurableEnvironment ambiente, String nome) {
        String valor = ambiente.getProperty(nome);
        if (valor == null || valor.isBlank()) {
            throw new IllegalStateException(nome + " e obrigatoria no perfil prod");
        }
        return valor;
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
