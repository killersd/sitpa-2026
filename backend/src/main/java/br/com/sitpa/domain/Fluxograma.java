package br.com.sitpa.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Queixa apresentada pelo paciente (ex.: "Convulsoes", "Agressao", "Gravidez").
 * Pertence a um {@link Grupo} e reune os sintomas/discriminadores avaliaveis.
 */
@Entity
@Table(name = "fluxograma", uniqueConstraints = @UniqueConstraint(name = "uk_fluxograma_codigo", columnNames = "codigo"))
public class Fluxograma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Identificador estavel referenciado pelas regras DRL e pelo modelo DMN. */
    @NotBlank
    @Size(max = 60)
    @Column(nullable = false, length = 60)
    private String codigo;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nome;

    @Size(max = 500)
    @Column(length = 500)
    private String descricao;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grupo_id", nullable = false, foreignKey = @ForeignKey(name = "fk_fluxograma_grupo"))
    private Grupo grupo;

    /**
     * Nome do modelo DMN a ser usado por este fluxograma. Quando preenchido, o motor executa
     * a tabela de decisao DMN correspondente; quando nulo, usa apenas DRL + camada base.
     */
    @Size(max = 120)
    @Column(name = "modelo_dmn", length = 120)
    private String modeloDmn;

    @OneToMany(mappedBy = "fluxograma", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("ordem ASC, id ASC")
    private List<FluxogramaSintoma> sintomas = new ArrayList<>();

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

    public Grupo getGrupo() {
        return grupo;
    }

    public void setGrupo(Grupo grupo) {
        this.grupo = grupo;
    }

    public String getModeloDmn() {
        return modeloDmn;
    }

    public void setModeloDmn(String modeloDmn) {
        this.modeloDmn = modeloDmn;
    }

    public List<FluxogramaSintoma> getSintomas() {
        return sintomas;
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
