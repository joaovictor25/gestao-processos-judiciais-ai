package br.com.axionweb.backend.model.processo;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Table(name="processos")
@Entity
public class Processo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    @Column(name="numero_processo")
    private String numeroProcesso;
    @Column(name="cliente_vinculado")
    private String clienteVinculado;
    @Column(name="posicao_cliente")
    private String posicaoCliente;
    @Column(name="tribunal")
    private String tribunal;

    @Embedded
    private OrgaoJulgador orgaoJulgador;
    //Nome Orgao julgador

    @Column(name="grau")
    private String grau;
    @Column(name="comarca")
    private String comarca;
    @Column(name="uf")
    private String uf;

    @Embedded
    private ClasseProcessual classeProcessual;

    @Embedded
    private Assunto assunto;

    @Column(name="area_atuacao")
    private String areaAtuacao;
    @Column(name="status")
    private String status;
    @Column(name="valor_causa")
    private BigDecimal valorCausa;
    @Column(name="nome_magistrado")
    private String nomeMagistrado;
    //Analisar o campo de data para definir o recorte de inicio e fim
    @Column(name="data_distribuicao")
    private LocalDate dataInicio;
    @Column(name="data_fim")
    private LocalDate dataFim;

    public Long getId(){
        return id;
    }

    public void setNumeroProcesso(String numeroProcesso){
        this.numeroProcesso = numeroProcesso;
    }
    public String getNumeroProcesso(){
        return numeroProcesso;
    }

    public void setClienteVinculado(String clienteVinculado){
        this.clienteVinculado = clienteVinculado;
    }
    public String getClienteVinculado(){
        return clienteVinculado;
    }

    public void setPosicaoCliente(String posicaoCliente){
        this.posicaoCliente = posicaoCliente;
    }
    public String getPosicaoCliente(){
        return posicaoCliente;
    }

    public void setTribunal(String tribunal){
        this.tribunal = tribunal;
    }
    public String getTribunal(){
        return tribunal;
    }

    public void setOrgaoJulgador(OrgaoJulgador orgaoJulgador){
        this.orgaoJulgador = orgaoJulgador;
    }
    public OrgaoJulgador getOrgaoJulgador(){
        return orgaoJulgador;
    }

    public void setGrau(String grau){
        this.grau = grau;
    }
    public String getGrau(){
        return grau;
    }


    public void setComarca(String comarca){
        this.comarca = comarca;
    }
    public String getComarca(){
        return comarca;
    }

    public void setUf(String uf){
        this.uf = uf;
    }
    public String getUf(){
        return uf;
    }

    public void setClasseProcessual(ClasseProcessual classeProcessual) {
        this.classeProcessual = classeProcessual;
    }
    public ClasseProcessual getClasseProcessual() {
        return classeProcessual;
    }

    public void setAssunto(Assunto assunto){
        this.assunto = assunto;
    }
    public Assunto getAssunto(){
        return assunto;
    }

    public void setAreaAtuacao(String areaAtuacao){
        this.areaAtuacao = areaAtuacao;
    }
    public String getAreaAtuacao(){
        return areaAtuacao;
    }

    public void setStatus(String status){
        this.status = status;
    }
    public String getStatus(){
        return status;
    }

    public void setValorCausa(BigDecimal valorCausa){
        this.valorCausa = valorCausa;
    }
    public BigDecimal getValorCausa(){
        return valorCausa;
    }

    public void setNomeMagistrado(String nomeMagistrado){
        this.nomeMagistrado = nomeMagistrado;
    }
    public String getNomeMagistrado(){
        return nomeMagistrado;
    }

    public void setDataInicio(LocalDate dataInicio){
        this.dataInicio = dataInicio;
    }
    public LocalDate getDataInicio(){
        return dataInicio;
    }

    public void setDataFim(LocalDate dataFim){
        this.dataFim = dataFim;
    }
    public LocalDate getDataFim(){
        return dataFim;
    }
}
