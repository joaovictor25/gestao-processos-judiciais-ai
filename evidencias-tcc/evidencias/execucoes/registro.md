# Registro das execuções — 05/11/2026 (America/Sao_Paulo)

| Execução | Resultado | Evidência |
| --- | --- | --- |
| Maven baseline, finalizada 00:59:12 | 312 testes, 0 falhas, 0 erros; 56,973 s | logs/backend-baseline.log e resultados/baseline |
| Maven ampliada, finalizada 01:00:46 | 322 testes, 1 falha, 0 erros; 55,835 s | logs/backend-final.log e resultados/final |
| npm build, tentativa 1 | bloqueio spawn EPERM, sem conclusão do bundler | logs/frontend-build.log |
| npm build, tentativa 2 | aprovado com permissão para subprocessos | logs/frontend-build-retry.log |
| Node segurança, tentativa 1 | executor não iniciou subprocesso (EPERM), não é falha do produto | logs/frontend-seguranca.log |
| Node segurança, tentativa 2 | 2 testes aprovados | logs/frontend-seguranca-retry.log |
| Navegador comunicações | 8 checkpoints, nenhuma exceção JS | logs/browser-comunicacoes.log e resultados/browser-comunicacoes |
| Navegador fila, tentativa 1 | corrida após reload: teste acessou elemento inexistente | logs/browser-datajud-fila.log |
| Navegador fila, tentativa 2 | espera do teste menor que pausa padrão 429 do servidor | logs/browser-datajud-fila-retry.log |
| Reinício do servidor, tentativa intermediária | porta 18081 ainda ocupada; aplicação não iniciou (mesmo com BUILD SUCCESS do wrapper) | logs/backend-browser-retry.log |
| Navegador fila, tentativa 3 | aprovado com sincronização corrigida e parâmetros explícitos | logs/browser-datajud-fila-configurado.log e resultados/browser-datajud-fila |
| Navegador detalhes | aprovado; includes CNJ interceptado, CRUD real e detalhes com fixtures | logs/browser-detalhes.log e resultados/browser-detalhes |
| Navegador exclusão, tentativa 1 | corrida após reload, campo ainda indisponível | logs/browser-exclusao.log |
| Navegador exclusão, tentativa 2 | marcador de recarregamento aplicado à tabela inexistente no estado vazio | logs/browser-exclusao-retry.log |
| Navegador exclusão, tentativa 3 | aprovado com marcador no documento e espera pela nova tabela | logs/browser-exclusao-final.log e resultados/browser-exclusao |
| Postman versão/status | falha de inicialização: manifesto Private Cloud local não legível | logs/postman-status.log |

Quantidade de execuções principais: duas suítes Maven, duas tentativas de build, duas de Node segurança e oito fluxos/tentativas de navegador (1 comunicações + 3 fila + 1 detalhes + 3 exclusão). Não somar repetições ao consolidado de cenários únicos. Nenhuma API real chamada. Nenhuma correção de produção aplicada.

As duas alterações nos scripts de navegador corrigem apenas sincronização do teste com documentos recarregados. A falha Java do DJEN permanece. Logs e XML não foram substituídos por resultados esperados. Inspeções estáticas de configuração não constituem testes manuais realizados por pessoa.
