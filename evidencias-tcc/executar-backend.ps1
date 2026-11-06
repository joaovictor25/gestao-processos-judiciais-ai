$ErrorActionPreference = 'Stop'
$raizTcc = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$nomeExecucaoTcc = 'reexecucao-' + (Get-Date -Format 'yyyyMMdd-HHmmss')
$destinoTcc = Join-Path $PSScriptRoot ('evidencias/execucoes/' + $nomeExecucaoTcc)
New-Item -ItemType Directory -Path $destinoTcc | Out-Null
$inicioTcc = Get-Date
Push-Location (Join-Path $raizTcc 'backend')
try {
    $ErrorActionPreference = 'Continue' # stderr nativo não deve impedir a coleta de uma execução reprovada.
    & .\mvnw.cmd -B -ntp '-Dspring.profiles.active=test' '-Dspring.datasource.url=jdbc:h2:mem:tcc-reexecucao' '-Dspring.jpa.hibernate.ddl-auto=create-drop' '-Dapp.datajud.base-url=http://127.0.0.1:1' '-Dapp.djen.base-url=http://127.0.0.1:1' '-Dapp.gemini.base-url=http://127.0.0.1:1' '-Dapp.gemini.api-key=teste-local' test 2>&1 | Out-File (Join-Path $destinoTcc 'maven.log') -Encoding utf8
    $codigoTcc = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    $logTcc = Get-Content (Join-Path $destinoTcc 'maven.log') -Raw
    Get-ChildItem target/surefire-reports/TEST-*.xml | ForEach-Object {
        [xml]$relatorioTcc = Get-Content $_.FullName -Raw
        $classeTcc = [regex]::Escape($relatorioTcc.testsuite.name)
        if ($logTcc -match "\[INFO\] Running\s+$classeTcc\b" -and $_.LastWriteTime -ge $inicioTcc) {
            @($relatorioTcc.testsuite.SelectNodes('properties|system-out|system-err')) | ForEach-Object { [void]$_.ParentNode.RemoveChild($_) }
            $relatorioTcc.Save((Join-Path $destinoTcc $_.Name))
        }
    }
    @{ inicio=$inicioTcc.ToString('o'); fim=(Get-Date).ToString('o'); exitCode=$codigoTcc; banco='H2 em memória'; provedores='simulados/loopback' } | ConvertTo-Json | Set-Content (Join-Path $destinoTcc 'execucao.json') -Encoding utf8
    Write-Output "ExitCode=$codigoTcc; evidencias=$destinoTcc"
} finally { Pop-Location }
