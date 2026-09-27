package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.datajud.ParametrosDataJudDTO;
import br.com.axionweb.backend.model.processo.Assunto;
import br.com.axionweb.backend.model.processo.ClasseProcessual;
import br.com.axionweb.backend.model.processo.OrgaoJulgador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ConsultaDataJudTests {
    private DataJudService service;
    private MockRestServiceServer servidor;

    @BeforeEach
    void preparar() {
        var builder = RestClient.builder().baseUrl("https://datajud.teste");
        servidor = MockRestServiceServer.bindTo(builder).build();
        service = new DataJudService();
        ReflectionTestUtils.setField(service, "restDataJud", builder.build());
    }

    @Test
    void camposOpcionaisNulosNaoDevemImpedirConsulta() {
        var parametros = parametros();
        servidor.expect(requestTo("https://datajud.teste/api_publica_tjpr/_search"))
                .andExpect(content().json("""
                        {"size":400,"query":{"bool":{"must":[]}}}
                        """))
                .andRespond(withSuccess("{\"hits\":{\"hits\":[]}}", MediaType.APPLICATION_JSON));

        assertThat(service.buscarProcessos(parametros)).isEmpty();
        servidor.verify();
    }

    @Test
    void deveConsultarPorNomeQuandoCodigoOpcionalNaoFoiInformado() {
        var parametros = parametros();
        var classe = new ClasseProcessual(); classe.setNome(" Recurso Inominado Cível ");
        var assunto = new Assunto(); assunto.setNome("Dano material");
        var orgao = new OrgaoJulgador(); orgao.setCodigo("72197");
        parametros.setClasseProcessual(classe); parametros.setAssunto(assunto); parametros.setOrgaoJulgador(orgao);
        servidor.expect(requestTo("https://datajud.teste/api_publica_tjpr/_search"))
                .andExpect(content().json("""
                        {"size":400,"query":{"bool":{"must":[
                          {"match":{"classe.nome":"Recurso Inominado Cível"}},
                          {"match":{"assuntos.nome":"Dano material"}},
                          {"match":{"orgaoJulgador.codigo":"72197"}}
                        ]}}}
                        """))
                .andRespond(withSuccess("{\"hits\":{\"hits\":[]}}", MediaType.APPLICATION_JSON));
        assertThat(service.buscarProcessos(parametros)).isEmpty();
        servidor.verify();
    }

    @Test
    void respostaInvalidaNaoPodeSerConfundidaComAmostraVazia() {
        servidor.expect(anything()).andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.buscarProcessos(parametros())).isInstanceOf(RestClientException.class);
    }

    @Test
    void timeoutNaRespostaNaoPodeSubstituirResultadosPorAmostraParcial() {
        servidor.expect(anything()).andRespond(withSuccess("{\"timed_out\":true,\"hits\":{\"hits\":[]}}", MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> service.buscarProcessos(parametros())).isInstanceOf(RestClientException.class);
    }

    private ParametrosDataJudDTO parametros() {
        var parametros = new ParametrosDataJudDTO();
        parametros.setTribunal("TJPR");
        parametros.setQuantidadeResultado(400);
        return parametros;
    }
}
