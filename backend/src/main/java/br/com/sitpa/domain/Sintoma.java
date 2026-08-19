package br.com.sitpa.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Sintoma / discriminador avaliado na triagem.
 *
 * <p>Cada sintoma carrega uma {@link Classificacao} padrao, que e a gravidade atribuida
 * quando ele e observado. Um mesmo sintoma pode participar de varios fluxogramas com
 * gravidades diferentes &mdash; nesse caso a associacao {@link FluxogramaSintoma} sobrescreve
 * o padrao.</p>
 */
@Entity
@Table(name = "sintoma", uniqueConstraints = @UniqueConstraint(name = "uk_sintoma_codigo", columnNames = "codigo"))
public class Sintoma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador estavel referenciado pelas regras DRL e pelo modelo DMN. */
    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, length = 80)
    private String codigo;

    @NotBlank
    @Size(max = 160)
    @Column(nullable = false, length = 160)
    private String nome;

    @Size(max = 500)
    @Column(length = 500)
    private String descricao;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "classificacao_id", nullable = false, foreignKey = @ForeignKey(name = "fk_sintoma_classificacao"))
    private Classificacao classificacao;

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

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Classificacao getClassificacao() {
        return classificacao;
    }

    public void setClassificacao(Classificacao classificacao) {
        this.classificacao = classificacao;
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
