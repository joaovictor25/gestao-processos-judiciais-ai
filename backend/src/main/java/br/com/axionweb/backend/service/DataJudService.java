package br.com.axionweb.backend.service;

import br.com.axionweb.backend.dto.datajud.ParametrosDataJudDTO;
import br.com.axionweb.backend.model.Processo;
import org.springframework.stereotype.Service;

@Service
public class DataJudService {

    ParametrosDataJudDTO parametro = new ParametrosDataJudDTO();

    public ParametrosDataJudDTO montarParametroDataJud(Processo processo){
        //Setar os parametros com os get de processo
        return null; //Retornar os paremetros
    }
}
