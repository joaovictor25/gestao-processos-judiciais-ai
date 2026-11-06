package br.com.axionweb.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.FileSystemResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.assertThat;

class MigracaoSintesesTests {
    @Test void migracaoComunicacoesPreservaProcessosERemoveHistoricoNaExclusao() throws Exception {
        try (var conexao = DriverManager.getConnection("jdbc:h2:mem:migracao-comunicacoes;MODE=MySQL", "sa", "")) {
            var sql = conexao.createStatement();
            sql.execute("CREATE TABLE processos (id BIGINT PRIMARY KEY)");
            sql.execute("INSERT INTO processos VALUES (3)");
            ScriptUtils.executeSqlScript(conexao, new FileSystemResource("schema/20261104_comunicacao_sinteses.sql"));
            sql.execute("INSERT INTO comunicacao_pesquisa (processo_id, pesquisada_em, quantidade, comunicacao_ids, publicacoes) VALUES (3, CURRENT_TIMESTAMP, 0, '[]', '[]')");
            var processo = sql.executeQuery("SELECT COUNT(*) FROM processos");
            assertThat(processo.next()).isTrue(); assertThat(processo.getInt(1)).isEqualTo(1);
            sql.execute("DELETE FROM processos WHERE id=3");
            var historico = sql.executeQuery("SELECT COUNT(*) FROM comunicacao_pesquisa");
            assertThat(historico.next()).isTrue(); assertThat(historico.getInt(1)).isZero();
        }
    }

    @Test void migracaoAditivaPreservaEstatisticaLegadaSemInventarParametros() throws Exception {
        try (var conexao = DriverManager.getConnection("jdbc:h2:mem:migracao-sinteses;MODE=MySQL", "sa", "")) {
            var sql = conexao.createStatement();
            sql.execute("CREATE TABLE datajud_estatistica (id BIGINT PRIMARY KEY, processo_id BIGINT, quantidade_processos INTEGER)");
            sql.execute("INSERT INTO datajud_estatistica VALUES (7, 3, 400)");
            ScriptUtils.executeSqlScript(conexao, new FileSystemResource("schema/20261103_datajud_sinteses.sql"));
            var dados = sql.executeQuery("SELECT * FROM datajud_estatistica WHERE id=7");
            assertThat(dados.next()).isTrue();
            assertThat(dados.getInt("quantidade_processos")).isEqualTo(400);
            assertThat(dados.getLong("versao")).isZero();
            assertThat(dados.getString("status_sintese")).isEqualTo("SEM_SINTESE");
            assertThat(dados.getString("sintese")).isNull();
            assertThat(dados.getString("executada_em")).isNull();
            assertThat(dados.getString("data_inicio")).isNull();
        }
    }
}
