package br.com.axionweb.backend.config;

import br.com.axionweb.backend.service.GeracaoEstatisticaService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.datajud.worker-enabled", havingValue = "true", matchIfMissing = true)
public class DataJudAgendamentoConfig {
    private final GeracaoEstatisticaService geracoes;
    public DataJudAgendamentoConfig(GeracaoEstatisticaService geracoes) { this.geracoes = geracoes; }
    @Scheduled(fixedDelayString = "${app.datajud.worker-intervalo-ms:1000}")
    public void executar() { geracoes.processarProxima(); }
}
