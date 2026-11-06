-- Aplicar antes de iniciar a versão com fila no perfil MySQL (ddl-auto=validate).
-- As três tabelas são novas; dados e estatísticas existentes não são modificados.
CREATE TABLE IF NOT EXISTS datajud_controle (
    id INTEGER PRIMARY KEY, proxima_consulta BIGINT NOT NULL, ocupado_ate BIGINT NOT NULL, reserva VARCHAR(36)
);
CREATE TABLE IF NOT EXISTS datajud_cache (
    chave VARCHAR(64) PRIMARY KEY, expira_em BIGINT NOT NULL, resposta LONGTEXT,
    INDEX idx_datajud_cache_expira (expira_em)
);
CREATE TABLE IF NOT EXISTS datajud_solicitacao (
    id VARCHAR(36) PRIMARY KEY, processo_id BIGINT NOT NULL, usuario VARCHAR(254) NOT NULL,
    chave VARCHAR(64) NOT NULL, parametros VARCHAR(4000) NOT NULL, status VARCHAR(20) NOT NULL,
    criada_em BIGINT NOT NULL, atualizada_em BIGINT NOT NULL, proxima_tentativa BIGINT NOT NULL,
    prazo_final BIGINT NOT NULL, ocupada_ate BIGINT NOT NULL, reserva VARCHAR(36), tentativas INTEGER NOT NULL,
    estatistica_id BIGINT, codigo_erro VARCHAR(64),
    INDEX idx_datajud_fila (status, proxima_tentativa),
    INDEX idx_datajud_solicitacao_processo (processo_id, criada_em)
);
