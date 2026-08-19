package br.com.sitpa.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

/**
 * Associacao N:N entre {@link Fluxograma} e {@link Sintoma}, modelada como entidade
 * porque a relacao carrega informacao propria.
 *
 * <p>{@link #classificacaoEspecifica} permite que o mesmo discriminador tenha gravidades
 * distintas conforme a queixa &mdash; "dor moderada" e Amarelo em "Cefaleia" mas pode ser
 * Laranja em "Dor toracica". Quando nulo, vale a classificacao padrao do sintoma.</p>
 */
@Entity
@Table(name = "fluxograma_sintoma", uniqueConstraints = @UniqueConstraint(
        name = "uk_fluxograma_sintoma", columnNames = {"fluxograma_id", "sintoma_id"}))
public class FluxogramaSintoma {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fluxograma_id", nullable = false, foreignKey = @ForeignKey(name = "fk_fs_fluxograma"))
    private Fluxograma fluxograma;

    @NotNull
    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "sintoma_id", nullable = false, foreignKey = @ForeignKey(name = "fk_fs_sintoma"))
    private Sintoma sintoma;

    /** Sobrescreve a classificacao padrao do sintoma neste fluxograma. Opcional. */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "classificacao_especifica_id", foreignKey = @ForeignKey(name = "fk_fs_classificacao"))
    private Classificacao classificacaoEspecifica;

    /** Ordem de exibicao do sintoma na tela de triagem. */
    @Column(nullable = false)
    private Integer ordem = 0;

    public FluxogramaSintoma() {
    }

    public FluxogramaSintoma(Fluxograma fluxograma, Sintoma sintoma, Classificacao classificacaoEspecifica, Integer ordem) {
        this.fluxograma = fluxograma;
        this.sintoma = sintoma;
        this.classificacaoEspecifica = classificacaoEspecifica;
        this.ordem = ordem == null ? 0 : ordem;
    }

    /** Classificacao efetiva deste sintoma no contexto deste fluxograma. */
    public Classificacao classificacaoEfetiva() {
        return classificacaoEspecifica != null ? classificacaoEspecifica : sintoma.getClassificacao();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Fluxograma getFluxograma() {
        return fluxograma;
    }

    public void setFluxograma(Fluxograma fluxograma) {
        this.fluxograma = fluxograma;
    }

    public Sintoma getSintoma() {
        return sintoma;
    }

    public void setSintoma(Sintoma sintoma) {
        this.sintoma = sintoma;
    }

    public Classificacao getClassificacaoEspecifica() {
        return classificacaoEspecifica;
    }

    public void setClassificacaoEspecifica(Classificacao classificacaoEspecifica) {
        this.classificacaoEspecifica = classificacaoEspecifica;
    }

    public Integer getOrdem() {
        return ordem;
    }

    public void setOrdem(Integer ordem) {
        this.ordem = ordem;
    }
}
