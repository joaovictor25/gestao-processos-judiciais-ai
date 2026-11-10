package br.com.axionweb.backend.model.comunicacao;

import br.com.axionweb.backend.model.processo.Processo;
import jakarta.persistence.*;
import org.hibernate.Length;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "comunicacao")
public class Comunicacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "processo_id", nullable = false)
    private Processo processo;

    @Column(name = "codigo_classe")
    private String codigoClasse;

    @Column(name = "data_disponibilizacao")
    private String dataDisponibilizacao;

    @Column(name = "data_disponibilizacao_formatada")
    private String dataDisponibilizacaoFormatada;

    @Column(name = "id_orgao")
    private Integer idOrgao;

    @Column(name = "meio")
    private String meio;

    @Column(name = "meio_completo")
    private String meioCompleto;

    @Column(name = "sigla_tribunal")
    private String siglaTribunal;

    @Column(name = "status")
    private String status;

    // Texto extenso via JDBC String; no MySQL corresponde ao LONGTEXT da migracao.
    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "texto", length = Length.LONG32)
    private String texto;

    @Column(name = "tipo_comunicacao")
    private String tipoComunicacao;

    @Column(name = "tipo_documento")
    private String tipoDocumento;


    public Long getId() {
        return id;
    }

    public Processo getProcesso() {
        return processo;
    }

    public void setProcesso(Processo processo) {
        this.processo = processo;
    }

    public String getCodigoClasse() {
        return codigoClasse;
    }

    public void setCodigoClasse(String codigoClasse) {
        this.codigoClasse = codigoClasse;
    }

    public String getDataDisponibilizacao() {
        return dataDisponibilizacao;
    }

    public void setDataDisponibilizacao(String dataDisponibilizacao) {
        this.dataDisponibilizacao = dataDisponibilizacao;
    }

    public String getDataDisponibilizacaoFormatada() {
        return dataDisponibilizacaoFormatada;
    }

    public void setDataDisponibilizacaoFormatada(
            String dataDisponibilizacaoFormatada) {
        this.dataDisponibilizacaoFormatada = dataDisponibilizacaoFormatada;
    }

    public Integer getIdOrgao() {
        return idOrgao;
    }

    public void setIdOrgao(Integer idOrgao) {
        this.idOrgao = idOrgao;
    }

    public String getMeio() {
        return meio;
    }

    public void setMeio(String meio) {
        this.meio = meio;
    }

    public String getMeioCompleto() {
        return meioCompleto;
    }

    public void setMeioCompleto(String meioCompleto) {
        this.meioCompleto = meioCompleto;
    }

    public String getSiglaTribunal() {
        return siglaTribunal;
    }

    public void setSiglaTribunal(String siglaTribunal) {
        this.siglaTribunal = siglaTribunal;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getTipoComunicacao() {
        return tipoComunicacao;
    }

    public void setTipoComunicacao(String tipoComunicacao) {
        this.tipoComunicacao = tipoComunicacao;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }
}
