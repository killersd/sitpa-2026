package br.com.sitpa.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

/**
 * Artefato de regra armazenado no banco (DRL ou DMN).
 *
 * <p>E o que viabiliza o requisito de <b>alterar regras sem recompilar a aplicacao</b>:
 * o conteudo e texto, versionado, e o motor reconstroi o KieContainer / DMNRuntime a partir
 * dele sob demanda. Regras entregues junto com o build ficam no classpath
 * (<code>resources/rules</code>) e sao carregadas na mesma base de conhecimento.</p>
 */
@Entity
@Table(name = "regra_protocolo", uniqueConstraints = @UniqueConstraint(name = "uk_regra_nome", columnNames = "nome"))
public class RegraProtocolo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 120)
    @Column(nullable = false, length = 120)
    private String nome;

    @Size(max = 500)
    @Column(length = 500)
    private String descricao;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoRegra tipo;

    /** Texto integral do DRL ou do XML DMN. {@code text} evita o mapeamento para large object no PostgreSQL. */
    @NotBlank
    @Column(nullable = false, columnDefinition = "text")
    private String conteudo;

    @Column(nullable = false)
    private boolean ativo = true;

    /** Incrementado a cada alteracao; usado para detectar necessidade de recarga. */
    @Version
    @Column(nullable = false)
    private Long versao;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    @PreUpdate
    void aoSalvar() {
        this.atualizadoEm = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public TipoRegra getTipo() {
        return tipo;
    }

    public void setTipo(TipoRegra tipo) {
        this.tipo = tipo;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public Long getVersao() {
        return versao;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }
}
