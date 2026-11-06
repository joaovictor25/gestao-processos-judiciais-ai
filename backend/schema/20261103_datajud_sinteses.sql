-- Aplicar UMA VEZ ao esquema existente antes de iniciar a nova versão.
-- DDL aditivo compatível com MySQL; não recria tabelas nem remove registros.
-- Datas antigas permanecem desconhecidas. Não inferir filtros a partir do cadastro atual.
ALTER TABLE datajud_estatistica ADD COLUMN versao BIGINT NOT NULL DEFAULT 0;
ALTER TABLE datajud_estatistica ADD COLUMN executada_em TIMESTAMP(6) NULL;
ALTER TABLE datajud_estatistica ADD COLUMN tribunal_consulta VARCHAR(255) NULL;
ALTER TABLE datajud_estatistica ADD COLUMN classe_consulta VARCHAR(255) NULL;
ALTER TABLE datajud_estatistica ADD COLUMN grau_consulta VARCHAR(255) NULL;
ALTER TABLE datajud_estatistica ADD COLUMN limite_consulta INTEGER NULL;
ALTER TABLE datajud_estatistica ADD COLUMN data_inicio DATE NULL;
ALTER TABLE datajud_estatistica ADD COLUMN data_fim DATE NULL;
ALTER TABLE datajud_estatistica ADD COLUMN sintese TEXT NULL;
ALTER TABLE datajud_estatistica ADD COLUMN sintese_gerada_em TIMESTAMP(6) NULL;
ALTER TABLE datajud_estatistica ADD COLUMN status_sintese VARCHAR(255) DEFAULT 'SEM_SINTESE';
CREATE INDEX idx_datajud_sinteses ON datajud_estatistica (processo_id, sintese_gerada_em, id);
