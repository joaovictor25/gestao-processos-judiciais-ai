# Requisitos identificados

São 12 requisitos derivados do código atual, não uma especificação retrospectiva aprovada pelo orientador. A matriz vincula cada execução à exigência principal; um teste pode verificar aspectos de outros requisitos.

| ID | Descrição |
| --- | --- |
| RF01 | Cadastrar e autenticar usuários, revogar sessão e proteger acesso |
| RF02 | Gerenciar processos próprios com validação e persistência CRUD |
| RF03 | Consultar DataJud com filtros, limites, fila e recuperação de falhas |
| RF04 | Calcular medianas e contagens das subamostras válidas |
| RF05 | Consultar DJEN por texto/tribunal/período e preparar comunicações |
| RF06 | Sintetizar comunicações com evidências e limitações |
| RF07 | Explicar somente indicadores estatísticos fornecidos |
| RF08 | Persistir análises, histórico e fontes, preservando dados após falha |
| RF09 | Apresentar dashboard da carteira autenticada |
| RNF01 | Isolar contas e restringir acesso a dados e análises |
| RNF02 | Proteger credenciais e minimizar exposição de dados nas integrações |
| RNF03 | Manter integridade de persistência e inicialização/migração do ambiente |

A consulta estatística é por filtros estruturados; pesquisa DJEN é textual. Não foi identificada recuperação por embeddings, ranking semântico ou predição judicial. A persistência das sínteses está implementada. Não há obrigação implementada de percorrer todas as páginas externas.
