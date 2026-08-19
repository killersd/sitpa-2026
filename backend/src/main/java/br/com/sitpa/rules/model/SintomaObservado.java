package br.com.sitpa.rules.model;

import java.util.Objects;

/**
 * Fato inserido na memoria de trabalho para cada sintoma marcado pelo profissional.
 *
 * <p>Ja chega com a classificacao <b>efetiva</b> resolvida a partir do cadastro (considerando
 * a sobrescrita por fluxograma). E isso que permite que a regra base do DRL seja generica:
 * ela nao conhece nenhum sintoma especifico, apenas propaga o que a unidade de saude
 * cadastrou.</p>
 */
public class SintomaObservado {

    private final Long id;
    private final String codigo;
    private final String nome;
    private final String classificacaoCodigo;

    public SintomaObservado(Long id, String codigo, String nome, String classificacaoCodigo) {
        this.id = id;
        this.codigo = codigo;
        this.nome = nome;
        this.classificacaoCodigo = classificacaoCodigo;
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    public String getClassificacaoCodigo() {
        return classificacaoCodigo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof SintomaObservado outro && Objects.equals(codigo, outro.codigo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(codigo);
    }

    @Override
    public String toString() {
        return "SintomaObservado[" + codigo + " -> " + classificacaoCodigo + "]";
    }
}
