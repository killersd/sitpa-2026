package br.com.sitpa.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Registro historico de uma triagem concluida.
 *
 * <p><b>Decisao de modelagem:</b> o historico guarda uma <i>copia</i> dos textos (nome do grupo,
 * do fluxograma, da classificacao, cor, tempo de espera, sintomas) em vez de apenas chaves
 * estrangeiras. O protocolo e editavel pelo administrador; se o historico apontasse para as
 * entidades vivas, renomear uma classificacao ou trocar seu tempo de espera reescreveria
 * retroativamente o registro clinico. As FKs sao mantidas apenas para filtro e estatistica,
 * e sao anulaveis para tolerar exclusao de cadastro.</p>
 */
@Entity
@Table(name = "classificacao_realizada", indexes = {
        @Index(name = "ix_cr_realizada_em", columnList = "realizada_em"),
        @Index(name = "ix_cr_classificacao", columnList = "classificacao_id"),
        @Index(name = "ix_cr_fluxograma", columnList = "fluxograma_id"),
        @Index(name = "ix_cr_grupo", columnList = "grupo_id")
})
public class ClassificacaoRealizada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "realizada_em", nullable = false)
    private Instant realizadaEm;

    // --- Referencias para filtro/estatistica (podem ficar orfas) ---

    @Column(name = "grupo_id")
    private Long grupoId;

    @Column(name = "fluxograma_id")
    private Long fluxogramaId;

    @Column(name = "classificacao_id")
    private Long classificacaoId;

    // --- Snapshot imutavel do que foi decidido ---

    @Column(name = "grupo_nome", nullable = false, length = 120)
    private String grupoNome;

    @Column(name = "fluxograma_nome", nullable = false, length = 120)
    private String fluxogramaNome;

    @Column(name = "classificacao_codigo", nullable = false, length = 60)
    private String classificacaoCodigo;

    @Column(name = "classificacao_nome", nullable = false, length = 120)
    private String classificacaoNome;

    @Column(name = "classificacao_cor", nullable = false, length = 7)
    private String classificacaoCor;

    @Column(name = "classificacao_prioridade", nullable = false)
    private Integer classificacaoPrioridade;

    @Column(name = "tempo_maximo_espera_minutos", nullable = false)
    private Integer tempoMaximoEsperaMinutos;

    @Column(name = "local_atendimento", nullable = false, length = 160)
    private String localAtendimento;

    @Column(name = "tipo_atendimento", nullable = false, length = 160)
    private String tipoAtendimento;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "classificacao_realizada_sintoma",
            joinColumns = @JoinColumn(name = "classificacao_realizada_id",
                    foreignKey = @ForeignKey(name = "fk_crs_classificacao_realizada")))
    @Column(name = "sintoma_nome", nullable = false, length = 160)
    private List<String> sintomas = new ArrayList<>();

    // --- Rastreabilidade da decisao ---

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_decisao", nullable = false, length = 20)
    private OrigemDecisao origemDecisao;

    @Column(name = "regra_aplicada", length = 200)
    private String regraAplicada;

    @Column(length = 1000)
    private String justificativa;

    // --- Contexto do atendimento ---

    @Column(name = "identificador_paciente", length = 80)
    private String identificadorPaciente;

    @Column(name = "idade_anos")
    private Integer idadeAnos;

    @Column(length = 1000)
    private String observacoes;

    @Column(name = "usuario_login", nullable = false, length = 80)
    private String usuarioLogin;

    @PrePersist
    void aoCriar() {
        if (this.realizadaEm == null) {
            this.realizadaEm = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getRealizadaEm() {
        return realizadaEm;
    }

    public void setRealizadaEm(Instant realizadaEm) {
        this.realizadaEm = realizadaEm;
    }

    public Long getGrupoId() {
        return grupoId;
    }

    public void setGrupoId(Long grupoId) {
        this.grupoId = grupoId;
    }

    public Long getFluxogramaId() {
        return fluxogramaId;
    }

    public void setFluxogramaId(Long fluxogramaId) {
        this.fluxogramaId = fluxogramaId;
    }

    public Long getClassificacaoId() {
        return classificacaoId;
    }

    public void setClassificacaoId(Long classificacaoId) {
        this.classificacaoId = classificacaoId;
    }

    public String getGrupoNome() {
        return grupoNome;
    }

    public void setGrupoNome(String grupoNome) {
        this.grupoNome = grupoNome;
    }

    public String getFluxogramaNome() {
        return fluxogramaNome;
    }

    public void setFluxogramaNome(String fluxogramaNome) {
        this.fluxogramaNome = fluxogramaNome;
    }

    public String getClassificacaoCodigo() {
        return classificacaoCodigo;
    }

    public void setClassificacaoCodigo(String classificacaoCodigo) {
        this.classificacaoCodigo = classificacaoCodigo;
    }

    public String getClassificacaoNome() {
        return classificacaoNome;
    }

    public void setClassificacaoNome(String classificacaoNome) {
        this.classificacaoNome = classificacaoNome;
    }

    public String getClassificacaoCor() {
        return classificacaoCor;
    }

    public void setClassificacaoCor(String classificacaoCor) {
        this.classificacaoCor = classificacaoCor;
    }

    public Integer getClassificacaoPrioridade() {
        return classificacaoPrioridade;
    }

    public void setClassificacaoPrioridade(Integer classificacaoPrioridade) {
        this.classificacaoPrioridade = classificacaoPrioridade;
    }

    public Integer getTempoMaximoEsperaMinutos() {
        return tempoMaximoEsperaMinutos;
    }

    public void setTempoMaximoEsperaMinutos(Integer tempoMaximoEsperaMinutos) {
        this.tempoMaximoEsperaMinutos = tempoMaximoEsperaMinutos;
    }

    public String getLocalAtendimento() {
        return localAtendimento;
    }

    public void setLocalAtendimento(String localAtendimento) {
        this.localAtendimento = localAtendimento;
    }

    public String getTipoAtendimento() {
        return tipoAtendimento;
    }

    public void setTipoAtendimento(String tipoAtendimento) {
        this.tipoAtendimento = tipoAtendimento;
    }

    public List<String> getSintomas() {
        return sintomas;
    }

    public void setSintomas(List<String> sintomas) {
        this.sintomas = sintomas;
    }

    public OrigemDecisao getOrigemDecisao() {
        return origemDecisao;
    }

    public void setOrigemDecisao(OrigemDecisao origemDecisao) {
        this.origemDecisao = origemDecisao;
    }

    public String getRegraAplicada() {
        return regraAplicada;
    }

    public void setRegraAplicada(String regraAplicada) {
        this.regraAplicada = regraAplicada;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    public String getIdentificadorPaciente() {
        return identificadorPaciente;
    }

    public void setIdentificadorPaciente(String identificadorPaciente) {
        this.identificadorPaciente = identificadorPaciente;
    }

    public Integer getIdadeAnos() {
        return idadeAnos;
    }

    public void setIdadeAnos(Integer idadeAnos) {
        this.idadeAnos = idadeAnos;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public String getUsuarioLogin() {
        return usuarioLogin;
    }

    public void setUsuarioLogin(String usuarioLogin) {
        this.usuarioLogin = usuarioLogin;
    }
}
