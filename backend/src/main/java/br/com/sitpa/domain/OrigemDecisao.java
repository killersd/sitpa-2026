package br.com.sitpa.domain;

/** Camada do motor que produziu a classificacao vencedora. */
public enum OrigemDecisao {

    /** Regra DRL especializada (combinacoes, dependencia de idade, gestacao etc.). */
    DRL,

    /** Tabela de decisao DMN associada ao fluxograma. */
    DMN,

    /** Camada base orientada a dados: classificacao cadastrada para o proprio sintoma. */
    BASE,

    /** Nenhum sintoma informado ou nenhuma regra disparou. */
    NENHUMA
}
