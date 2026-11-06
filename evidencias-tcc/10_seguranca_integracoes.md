# Segurança básica e integrações

Autenticação, hash de senha, token inválido/expirado/revogado, usuário sem sessão e operações de outra conta são verificados pela suíte de isolamento. O comportamento de ocultar processo alheio com 404 está nos testes, assim como 401 para ausência de autenticação e respostas de validação. H2 console no perfil dev tem testes próprios; a execução de navegador desabilitou o console e vinculou o servidor ao loopback.

Fontes e bundle frontend passaram na verificação de padrões definidos em seguranca-tcc.test.mjs. JWT de sessão no navegador é credencial de acesso esperada, diferente de chave privada do provedor. O teste não é auditoria exaustiva nem certificação LGPD.

Achado S01: GeminiService.java contém fallback literal com aparência de credencial no construtor. Valor omitido. Proposta: remover fallback, exigir variável de ambiente e avaliar revogação/rotação pelo responsável; não foi testada a validade do valor nem feita alteração em produção. A configuração RestComunicacaoConfig também contém Authorization DataJud literal; classificá-lo como credencial de integração embutida, sem presumir que seja segredo privado (há chaves públicas de acesso DataJud). Externalizar configuração separadamente.

Achado F01 reproduzido: ComunicacaoService.filtrarDadosComunicacao aceita lista contendo null até o mapeamento e lança NullPointerException. Teste novo esperava RestClientException. Proposta específica: validar elementos antes de mapear e lançar erro de resposta DJEN inválida; manter resposta anterior e mensagem segura no serviço chamador. Correção não aplicada.

Limitação S02 por inspeção: cliente DJEN em RestComunicacaoConfig não define explicitamente connect/read timeout, ao contrário de DataJud/Gemini. A exceção de transporte simulada não mede timeout real nem garante limite de espera. Proposta: configurar limites explícitos e adicionar teste HTTP com atraso controlado.

Minimização: síntese estatística envia indicadores; síntese de comunicações envia texto, que pode conter dados jurídicos/pessoais em uso real. Nesta execução todas as entradas são fictícias. Persistência inclui fontes e textos de síntese; regras de retenção, bases legais, consentimento, contratos com provedor, criptografia em repouso e direitos dos titulares não foram auditados. Não se declara conformidade legal.

DataJud: filtros estruturados, limite 400, fila/cache/429 e rollback testados. DJEN: página 1/80 e texto/tribunal/datas/meio testados; nenhuma garantia sobre completude de todos os resultados. Gemini: formato e citações testados com respostas controladas. Logs de teste incluem dados fictícios e foram sanitizados antes de empacotar; cabeçalhos e segredos não devem integrar evidência compartilhável.
