package br.com.sitpa.domain;

/** Formato do artefato de regra armazenado em {@link RegraProtocolo}. */
public enum TipoRegra {

    /** Drools Rule Language, compilado em runtime pelo KieBuilder. */
    DRL,

    /** Decision Model and Notation, carregado pelo DMNRuntime. */
    DMN
}
