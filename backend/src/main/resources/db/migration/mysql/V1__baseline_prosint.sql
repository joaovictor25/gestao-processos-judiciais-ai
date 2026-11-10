-- Baseline completo do ProSint para MySQL 8.4 LTS.
-- Instantes sao armazenados em DATETIME(6) e interpretados em UTC pela aplicacao.

CREATE TABLE usuarios (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(254) NOT NULL,
    senha VARCHAR(255) NOT NULL,
    role ENUM('ADMIN', 'USER') NOT NULL,
    CONSTRAINT pk_usuarios PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE processos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    numero_processo VARCHAR(255),
    cliente_vinculado VARCHAR(255),
    posicao_cliente VARCHAR(255),
    tribunal VARCHAR(255),
    orgao_julgador_nome VARCHAR(255),
    orgao_julgador_codigo VARCHAR(255),
    grau VARCHAR(255),
    comarca VARCHAR(255),
    uf VARCHAR(255),
    classe_processual_nome VARCHAR(255),
    classe_processual_codigo VARCHAR(255),
    assunto_nome VARCHAR(255),
    assunto_codigo VARCHAR(255),
    area_atuacao VARCHAR(255),
    status VARCHAR(255),
    valor_causa DECIMAL(38,2),
    nome_magistrado VARCHAR(255),
    data_distribuicao DATE,
    data_fim DATE,
    ultima_atividade_em DATETIME(6),
    ultima_atividade_tipo VARCHAR(255),
    ultima_atividade_titulo VARCHAR(255),
    ultima_analise_em DATETIME(6),
    CONSTRAINT pk_processos PRIMARY KEY (id),
    CONSTRAINT fk_processos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    INDEX idx_processos_usuario_numero (usuario_id, numero_processo, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE comunicacao (
    id BIGINT NOT NULL AUTO_INCREMENT,
    processo_id BIGINT NOT NULL,
    codigo_classe VARCHAR(255),
    data_disponibilizacao VARCHAR(255),
    data_disponibilizacao_formatada VARCHAR(255),
    id_orgao INTEGER,
    meio VARCHAR(255),
    meio_completo VARCHAR(255),
    sigla_tribunal VARCHAR(255),
    status VARCHAR(255),
    texto LONGTEXT,
    tipo_comunicacao VARCHAR(255),
    tipo_documento VARCHAR(255),
    CONSTRAINT pk_comunicacao PRIMARY KEY (id),
    CONSTRAINT fk_comunicacao_processo FOREIGN KEY (processo_id) REFERENCES processos (id),
    INDEX idx_comunicacao_processo (processo_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE datajud_estatistica (
    id BIGINT NOT NULL AUTO_INCREMENT,
    processo_id BIGINT NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    executada_em DATETIME(6),
    tribunal_consulta VARCHAR(255),
    classe_consulta VARCHAR(255),
    grau_consulta VARCHAR(255),
    limite_consulta INTEGER,
    data_inicio DATE,
    data_fim DATE,
    sintese TEXT,
    sintese_gerada_em DATETIME(6),
    status_sintese VARCHAR(255) DEFAULT 'SEM_SINTESE',
    quantidade_processos INTEGER,
    processos_pendentes INTEGER,
    primeira_baixa_quantidade INTEGER,
    primeira_baixa_mediana_dias DOUBLE,
    julgamento_quantidade INTEGER,
    julgamento_mediana_dias DOUBLE,
    CONSTRAINT pk_datajud_estatistica PRIMARY KEY (id),
    CONSTRAINT fk_datajud_estatistica_processo FOREIGN KEY (processo_id) REFERENCES processos (id),
    INDEX idx_datajud_sinteses (processo_id, sintese_gerada_em, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE comunicacao_pesquisa (
    id BIGINT NOT NULL AUTO_INCREMENT,
    versao BIGINT NOT NULL DEFAULT 0,
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
    CONSTRAINT pk_comunicacao_pesquisa PRIMARY KEY (id),
    CONSTRAINT fk_comunicacao_pesquisa_processo FOREIGN KEY (processo_id)
        REFERENCES processos (id) ON DELETE CASCADE,
    INDEX idx_comunicacao_pesquisa (processo_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE datajud_controle (
    id INTEGER NOT NULL,
    proxima_consulta BIGINT NOT NULL,
    ocupado_ate BIGINT NOT NULL,
    reserva VARCHAR(36),
    CONSTRAINT pk_datajud_controle PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE datajud_cache (
    chave VARCHAR(64) NOT NULL,
    expira_em BIGINT NOT NULL,
    resposta LONGTEXT,
    CONSTRAINT pk_datajud_cache PRIMARY KEY (chave),
    INDEX idx_datajud_cache_expira (expira_em)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE datajud_solicitacao (
    id VARCHAR(36) NOT NULL,
    processo_id BIGINT NOT NULL,
    usuario VARCHAR(254) NOT NULL,
    chave VARCHAR(64) NOT NULL,
    parametros VARCHAR(4000) NOT NULL,
    status VARCHAR(20) NOT NULL,
    criada_em BIGINT NOT NULL,
    atualizada_em BIGINT NOT NULL,
    proxima_tentativa BIGINT NOT NULL,
    prazo_final BIGINT NOT NULL,
    ocupada_ate BIGINT NOT NULL,
    reserva VARCHAR(36),
    tentativas INTEGER NOT NULL,
    estatistica_id BIGINT,
    codigo_erro VARCHAR(64),
    CONSTRAINT pk_datajud_solicitacao PRIMARY KEY (id),
    CONSTRAINT fk_datajud_solicitacao_processo FOREIGN KEY (processo_id)
        REFERENCES processos (id) ON DELETE CASCADE,
    CONSTRAINT fk_datajud_solicitacao_estatistica FOREIGN KEY (estatistica_id)
        REFERENCES datajud_estatistica (id) ON DELETE SET NULL,
    INDEX idx_datajud_fila (status, proxima_tentativa),
    INDEX idx_datajud_solicitacao_processo (processo_id, criada_em),
    INDEX idx_datajud_solicitacao_usuario (usuario, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE token_revogado (
    id BIGINT NOT NULL AUTO_INCREMENT,
    token_hash VARCHAR(64) NOT NULL,
    expira_em DATETIME(6) NOT NULL,
    CONSTRAINT pk_token_revogado PRIMARY KEY (id),
    INDEX idx_token_revogado_hash (token_hash),
    INDEX idx_token_revogado_expiracao (expira_em)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
