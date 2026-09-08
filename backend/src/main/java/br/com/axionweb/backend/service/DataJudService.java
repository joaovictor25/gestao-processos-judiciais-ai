package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.datajud.ParametrosDataJudDTO;
import br.com.axionweb.backend.model.Processo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;

import java.time.LocalDate;

@Service
public class DataJudService {

    @Autowired
    @Qualifier("restDataJud")
    RestClient restDataJud;

    ParametrosDataJudDTO parametro = new ParametrosDataJudDTO();

    public ParametrosDataJudDTO montarParametroDataJud(Processo processo){
        parametro.setTribunal(processo.getTribunal());
        parametro.setClasseProcessual(processo.getClasseProcessual());
        parametro.setAssunto(processo.getAssunto());
        parametro.setOrgaoJulgador(processo.getOrgaoJulgador());
        parametro.setComarca(processo.getComarca());
        parametro.setUf(processo.getUf());
        parametro.setDataInicio(processo.getDataDistribuicao());
        parametro.setDataFim(LocalDate.now());
        parametro.setQuantidadeResultado(100);
        return parametro;
    }

    public String consumirApiDataJud(ParametrosDataJudDTO parametro){

        return "";
    }

}
