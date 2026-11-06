$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '../backend')
try {
    # Apenas H2 em memória e provedores de loopback; nunca herda URLs externas.
    $argumentosTcc = @(
        '--server.port=18081', '--server.address=127.0.0.1', '--spring.profiles.active=test',
        '--spring.datasource.url=jdbc:h2:mem:tcc-browser-isolado', '--spring.jpa.hibernate.ddl-auto=create-drop',
        '--spring.h2.console.enabled=false', '--app.jwt.secret=dGNjLXRlc3RlLXNvbWVudGUtMzItYnl0ZXMtc2VtLXNlZ3JlZG8=',
        '--app.datajud.base-url=http://127.0.0.1:8092', '--app.djen.base-url=http://127.0.0.1:8093',
        '--app.gemini.base-url=http://127.0.0.1:8093', '--app.gemini.api-key=teste-local',
        '--app.datajud.worker-enabled=true', '--app.datajud.worker-intervalo-ms=100',
        '--app.datajud.intervalo-ms=0', '--app.datajud.retry-base-ms=0', '--app.datajud.pausa-429-ms=0',
        '--app.datajud.cache-ms=300000', '--logging.level.org.hibernate.SQL=OFF', '--logging.level.org.springframework=INFO'
    ) -join ' '
    & .\mvnw.cmd -B -ntp spring-boot:run "-Dspring-boot.run.arguments=$argumentosTcc"
} finally { Pop-Location }
