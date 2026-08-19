package br.com.sitpa.rules.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Fato principal da triagem: contexto completo da avaliacao de um paciente.
 *
 * <p>Os metodos {@code temSintoma}, {@code temAlgum} e {@code temTodos} existem para que as
 * regras DRL fiquem legiveis e proximas da linguagem clinica, em vez de manipular colecoes
 * dentro das condicoes.</p>
 */
public class FatoTriagem {

    private final String grupoCodigo;
    private final String grupoNome;
    private final String fluxogramaCodigo;
    private final String fluxogramaNome;
    private final Set<String> sintomas;
    private final Map<String, String> nomePorCodigo;
    private final Integer idadeAnos;
    private final boolean gestante;

    private FatoTriagem(Construtor c) {
        this.grupoCodigo = c.grupoCodigo;
        this.grupoNome = c.grupoNome;
        this.fluxogramaCodigo = c.fluxogramaCodigo;
        this.fluxogramaNome = c.fluxogramaNome;
        this.sintomas = Collections.unmodifiableSet(new LinkedHashSet<>(c.sintomas));
        this.nomePorCodigo = Collections.unmodifiableMap(new LinkedHashMap<>(c.nomePorCodigo));
        this.idadeAnos = c.idadeAnos;
        this.gestante = c.gestante;
    }

    public static Construtor novo() {
        return new Construtor();
    }

    // --- Predicados usados pelas regras DRL ---

    public boolean temSintoma(String codigo) {
        return sintomas.contains(codigo);
    }

    public boolean temAlgum(String... codigos) {
        for (String codigo : codigos) {
            if (sintomas.contains(codigo)) {
                return true;
            }
        }
        return false;
    }

    public boolean temTodos(String... codigos) {
        for (String codigo : codigos) {
            if (!sintomas.contains(codigo)) {
                return false;
            }
        }
        return codigos.length > 0;
    }

    public int quantidadeSintomas() {
        return sintomas.size();
    }

    /** Idade conhecida e abaixo do limite informado. Falso quando a idade nao foi coletada. */
    public boolean idadeMenorQue(int anos) {
        return idadeAnos != null && idadeAnos < anos;
    }

    /** Idade conhecida e igual ou acima do limite informado. */
    public boolean idadeMaiorOuIgualA(int anos) {
        return idadeAnos != null && idadeAnos >= anos;
    }

    /** Nome legivel de um sintoma, para compor a justificativa da regra. */
    public String nomeDoSintoma(String codigo) {
        return nomePorCodigo.getOrDefault(codigo, codigo);
    }

    // --- Acessores ---

    public String getGrupoCodigo() {
        return grupoCodigo;
    }

    public String getGrupoNome() {
        return grupoNome;
    }

    public String getFluxogramaCodigo() {
        return fluxogramaCodigo;
    }

    public String getFluxogramaNome() {
        return fluxogramaNome;
    }

    public Set<String> getSintomas() {
        return sintomas;
    }

    public Integer getIdadeAnos() {
        return idadeAnos;
    }

    public boolean isGestante() {
        return gestante;
    }

    @Override
    public String toString() {
        return "FatoTriagem[grupo=" + grupoCodigo + ", fluxograma=" + fluxogramaCodigo
                + ", sintomas=" + sintomas + ", idade=" + idadeAnos + ", gestante=" + gestante + "]";
    }

    /** Construtor fluente do fato. */
    public static final class Construtor {

        private String grupoCodigo;
        private String grupoNome;
        private String fluxogramaCodigo;
        private String fluxogramaNome;
        private final Set<String> sintomas = new LinkedHashSet<>();
        private final Map<String, String> nomePorCodigo = new LinkedHashMap<>();
        private Integer idadeAnos;
        private boolean gestante;

        public Construtor grupo(String codigo, String nome) {
            this.grupoCodigo = codigo;
            this.grupoNome = nome;
            return this;
        }

        public Construtor fluxograma(String codigo, String nome) {
            this.fluxogramaCodigo = codigo;
            this.fluxogramaNome = nome;
            return this;
        }

        public Construtor sintoma(String codigo, String nome) {
            this.sintomas.add(codigo);
            this.nomePorCodigo.put(codigo, nome);
            return this;
        }

        public Construtor idadeAnos(Integer idadeAnos) {
            this.idadeAnos = idadeAnos;
            return this;
        }

        public Construtor gestante(boolean gestante) {
            this.gestante = gestante;
            return this;
        }

        public FatoTriagem construir() {
            return new FatoTriagem(this);
        }
    }
}
