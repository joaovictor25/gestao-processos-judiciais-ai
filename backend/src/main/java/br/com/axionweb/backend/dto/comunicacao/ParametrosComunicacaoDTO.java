package br.com.axionweb.backend.dto.comunicacao;

import org.hibernate.loader.ast.spi.Loadable;

import java.time.LocalDate;

public class ParametrosComunicacaoDTO {
    private String texto;
    private String siglaTribunal;
    private LocalDate dataDisponibilizacaoInicio;
    private LocalDate dataDisponibilizacaoFim;
    private String meio;
    private Integer pagina;
    private Integer itensPorPagina;


    public void setTexto(String texto){
        this.texto = texto;
    }
    public String getTexto(){
        return texto;
    }

    public void setSiglaTribunal(String siglaTribunal){
        this.siglaTribunal = siglaTribunal;
    }
    public String getSiglaTribunal(){
        return siglaTribunal;
    }

    public void setDataDisponibilizacaoInicio(LocalDate dataDisponibilizacaoInicio){
        this.dataDisponibilizacaoInicio = dataDisponibilizacaoInicio;
    }
    public LocalDate getDataDisponibilizacaoInicio(){
        return dataDisponibilizacaoInicio;
    }

    public void setDataDisponibilizacaoFim(LocalDate dataDisponibilizacaoFim){
        this.dataDisponibilizacaoFim = dataDisponibilizacaoFim;
    }
    public LocalDate getDataDisponibilizacaoFim(){
        return dataDisponibilizacaoFim;
    }

    public void setMeio(String meio){
        this.meio = meio;
    }
    public String getMeio(){
        return meio;
    }

    public void setPagina(Integer pagina){
        this.pagina = pagina;
    }
    public Integer getPagina(){
        return pagina;
    }

    public void setItensPorPagina(Integer itensPorPagina){
        this.itensPorPagina = itensPorPagina;
    }
    public Integer getItensPorPagina(){
        return itensPorPagina;
    }
}