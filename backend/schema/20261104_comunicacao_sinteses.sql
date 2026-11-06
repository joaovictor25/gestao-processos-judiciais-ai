-- Migração aditiva para MySQL. Aplicar antes de iniciar com ddl-auto=validate.
CREATE TABLE comunicacao_pesquisa (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    versao BIGINT,
    processo_id BIGINT NOT NULL,
    texto_busca LONGTEXT,
    tribunal VARCHAR(255),
    data_inicio DATE,
    data_fim DATE,
    pesquisada_em DATETIME(6) NOT NULL,
    quantidade INTEGER NOT NULL,
    comunicacao_ids LONGTEXT NOT NULL,
    publicacoes LONGTEXT NOT NULL,
    sintese LONGTEXT,
    evidencias LONGTEXT,
    gerada_em DATETIME(6),
    comunicacoes_consideradas INTEGER,
    duplicadas_ignoradas INTEGER,
    CONSTRAINT fk_comunicacao_pesquisa_processo FOREIGN KEY (processo_id) REFERENCES processos(id) ON DELETE CASCADE,
    INDEX idx_comunicacao_pesquisa (processo_id, id)
);
