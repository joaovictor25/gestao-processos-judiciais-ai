package br.com.axionweb.backend.persistence;

import br.com.axionweb.backend.model.comunicacao.Comunicacao;
import br.com.axionweb.backend.model.comunicacao.PesquisaComunicacao;
import br.com.axionweb.backend.model.datajud.CacheConsultaDataJud;
import br.com.axionweb.backend.model.datajud.ControleDataJud;
import br.com.axionweb.backend.model.datajud.DataJud;
import br.com.axionweb.backend.model.datajud.SolicitacaoEstatistica;
import br.com.axionweb.backend.model.processo.Processo;
import br.com.axionweb.backend.model.usuario.TokenRevogado;
import br.com.axionweb.backend.model.usuario.Usuario;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.dialect.MySQLDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.tool.schema.extract.spi.ColumnInformation;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.test.util.ReflectionTestUtils;

import java.sql.Types;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Verifica o validador do Hibernate com metadados JDBC simulados; nao substitui MySQL real. */
class MySqlTextMappingTests {
    private static StandardServiceRegistry registry;
    private static Metadata metadata;

    @BeforeAll
    static void mapearEntidadesSemConexao() {
        registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", new MySQLDialect(DatabaseVersion.make(8, 4, 13)))
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
                .build();
        var sources = new MetadataSources(registry);
        Stream.of(Usuario.class, Processo.class, Comunicacao.class, PesquisaComunicacao.class,
                DataJud.class, ControleDataJud.class, CacheConsultaDataJud.class,
                SolicitacaoEstatistica.class, TokenRevogado.class).forEach(sources::addAnnotatedClass);
        metadata = sources.buildMetadata();
    }

    @AfterAll
    static void fecharRegistry() {
        StandardServiceRegistryBuilder.destroy(registry);
    }

    static Stream<Arguments> camposTexto() {
        return Stream.of(
                Arguments.of(Comunicacao.class, "texto", "longtext"),
                Arguments.of(CacheConsultaDataJud.class, "resposta", "longtext"),
                Arguments.of(DataJud.class, "sintese", "text"),
                Arguments.of(PesquisaComunicacao.class, "textoBusca", "longtext"),
                Arguments.of(PesquisaComunicacao.class, "comunicacaoIds", "longtext"),
                Arguments.of(PesquisaComunicacao.class, "publicacoes", "longtext"),
                Arguments.of(PesquisaComunicacao.class, "sintese", "longtext"),
                Arguments.of(PesquisaComunicacao.class, "evidencias", "longtext"));
    }

    @ParameterizedTest
    @MethodSource("camposTexto")
    void mapeamentoAceitaTipoTextualDoSchema(Class<?> entidade, String atributo, String tipoSql) throws Exception {
        var coluna = metadata.getEntityBinding(entidade.getName()).getProperty(atributo).getColumns().getFirst();
        var jdbc = mock(ColumnInformation.class);
        when(jdbc.getTypeCode()).thenReturn(Types.LONGVARCHAR);
        when(jdbc.getTypeName()).thenReturn(tipoSql);
        when(jdbc.getColumnSize()).thenReturn(tipoSql.equals("longtext") ? Integer.MAX_VALUE : 65_535);
        var dialect = registry.getService(JdbcEnvironment.class).getDialect();

        // Mesmo predicado usado pela validacao de schema da versao Hibernate do projeto.
        Boolean compativel = ReflectionTestUtils.invokeMethod(
                Class.forName("org.hibernate.tool.schema.internal.ColumnDefinitions"),
                "hasMatchingType", coluna, jdbc, metadata, dialect);
        assertThat(compativel).as("%s.%s: esperado %s, banco %s",
                entidade.getSimpleName(), atributo, coluna.getSqlType(metadata), tipoSql).isTrue();
        assertThat(coluna.getSqlType(metadata)).isEqualToIgnoringCase(tipoSql);
    }
}
