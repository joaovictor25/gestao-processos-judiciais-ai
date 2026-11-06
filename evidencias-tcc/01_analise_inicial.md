# Análise inicial — 05/11/2026

Registro anterior à implementação dos novos testes desta tarefa. O checkout já continha numerosas modificações e arquivos não rastreados; esses arquivos são a linha de base, não contribuições desta execução. O commit sozinho não reproduz esse estado. O inventário e hashes registram essa distinção.

## Software observado

Backend Java 25 declarado, Spring Boot 4.1.1, MVC, Spring Security/JWT, JPA/Hibernate, drivers MySQL e H2. Frontend React/TypeScript/Vite, rotas protegidas, cadastro/login, carteira CRUD, dashboard e abas de estatísticas/comunicações. As versões resolvidas e o runtime serão registrados em 02_ambiente_testes.md. Há documentação anterior desatualizada sobre dashboard e processamento síncrono; o código e os testes têm precedência.

DataJud consulta tribunal, código de classe e grau quando presentes, período opcional e limite de 400. Não usa assunto ou órgão como filtro estatístico, embeddings ou similaridade semântica. A implementação atual faz uma requisição limitada, sem paginação automática; há fila, cache e tratamento coordenado de 429. DJEN usa texto, tribunal, datas opcionais, meio D, página 1 e 80 itens. Paginação local da interface não equivale a percorrer páginas externas.

O cálculo atual fornece medianas e contagens, não médias. Identifica eventos por nomes reconhecidos, normaliza instantes e exclui durações inválidas. Ausência de baixa identificada não comprova pendência jurídica.

Gemini recebe textos normalizados/deduplicados ou somente quatro campos estatísticos. Há validação de formato, citações e indicadores. Isso não comprova fidelidade semântica. Sínteses DataJud são vinculadas à execução; regeneração substitui a síntese dessa execução. Comunicações mantêm pesquisa, fontes e histórico e reutilizam síntese existente. Falhas devem preservar dados anteriores.

## Testes encontrados e lacunas

Suíte JUnit existente: autenticação/isolamento, console H2 local, CRUD, consultas CNJ/DataJud, fila HTTP local, estatística, processamento de textos, Gemini simulado, persistência/histórico, migração e dashboard. Scripts Node/DevTools existentes verificam frontend com backend local e respostas controladas. A existência desses arquivos não é evidência de aprovação.

Reaproveitar as verificações de medianas pares/ímpares, amostras vazias, datas, subamostras, autorização e persistência. Complementar principalmente o contrato DJEN (parâmetros/página, resposta inválida e falhas) e a verificação de credenciais no frontend. Preparar corpus e protocolo de avaliação humana da IA, sem atribuir notas a respostas não geradas.

## Achados prévios e limites

Valor de credencial embutido no construtor GeminiService: registrar localização sem divulgar valor; propor remoção do fallback e avaliação de rotação pelo responsável, sem alterar produção nesta tarefa. A presença não estabelece que a credencial esteja ativa. Inspecionar também a configuração sem copiar segredos.

Postman CLI não inicia: erro de manifesto Private Cloud definido por política local. Reutilizar MockRestServiceServer e servidor HTTP local existentes, sem instalação adicional ou publicação externa. Nenhuma chamada real a Gemini/CNJ foi autorizada. Banco de validação deve ser H2 em memória; MySQL real permanece fora da execução.
