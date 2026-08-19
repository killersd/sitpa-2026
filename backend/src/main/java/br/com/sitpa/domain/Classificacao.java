package br.com.sitpa.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.time.Instant;

/**
 * Nivel de risco do protocolo (ex.: Vermelho/Emergente).
 *
 * <p>A gravidade e expressa por {@link #prioridade}: <b>quanto menor o numero, mais grave</b>.
 * O motor de regras usa exclusivamente esse campo para eleger "a classificacao mais grave"
 * entre as propostas, o que permite que a unidade de saude defina quantos niveis quiser.</p>
 */
@Entity
@Table(name = "classificacao", uniqueConstraints = {
        @UniqueConstraint(name = "uk_classificacao_codigo", columnNames = "codigo"),
        @UniqueConstraint(name = "uk_classificacao_prioridade", columnNames = "prioridade")
})
public class Classificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 60)
    @Column(nullable = false, length = 60)
    private String codigo;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nome;

    /** Cor de apresentacao em hexadecimal (#RRGGBB). */
    @NotBlank
    @Pattern(regexp = "^#([0-9a-fA-F]{6})$", message = "cor deve estar no formato #RRGGBB")
    @Column(nullable = false, length = 7)
    private String cor;

    @NotNull
    @Min(1)
    @Column(nullable = false)
    private Integer prioridade;

    /** Tempo maximo de espera recomendado, em minutos. 0 = atendimento imediato. */
    @NotNull
    @Min(0)
    @Column(name = "tempo_maximo_espera_minutos", nullable = false)
    private Integer tempoMaximoEsperaMinutos;

    @NotBlank
    @Size(max = 160)
    @Column(name = "local_atendimento", nullable = false, length = 160)
    private String localAtendimento;

    @NotBlank
    @Size(max = 160)
    @Column(name = "tipo_atendimento", nullable = false, length = 160)
    private String tipoAtendimento;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    void aoCriar() {
        this.criadoEm = Instant.now();
        this.atualizadoEm = this.criadoEm;
    }

    @PreUpdate
    void aoAtualizar() {
        this.atualizadoEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCor() {
        return cor;
    }

    public void setCor(String cor) {
        this.cor = cor;
    }

    public Integer getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(Integer prioridade) {
        this.prioridade = prioridade;
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

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
