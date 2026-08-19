package br.com.sitpa.rules.model;

import br.com.sitpa.domain.OrigemDecisao;

import java.util.Objects;

/**
 * Sugestao de gravidade produzida por uma regra.
 *
 * <p>Repare que a proposta carrega apenas o <b>codigo</b> da classificacao, nunca a prioridade
 * numerica. Quem sabe qual codigo e mais grave e o cadastro, nao a regra &mdash; assim a unidade
 * de saude pode reordenar ou renomear niveis de risco sem tocar em nenhum DRL, e o motor nunca
 * fica com uma copia desatualizada da escala de gravidade.</p>
 */
public class PropostaClassificacao {

    private final String classificacaoCodigo;
    private final String regra;
    private final String justificativa;
    private final OrigemDecisao origem;

    public PropostaClassificacao(String classificacaoCodigo, String regra, String justificativa, OrigemDecisao origem) {
        this.classificacaoCodigo = classificacaoCodigo;
        this.regra = regra;
        this.justificativa = justificativa;
        this.origem = origem;
    }

    /** Atalho usado pelas regras DRL especializadas, que sempre tem origem {@link OrigemDecisao#DRL}. */
    public PropostaClassificacao(String classificacaoCodigo, String regra, String justificativa) {
        this(classificacaoCodigo, regra, justificativa, OrigemDecisao.DRL);
    }

    public String getClassificacaoCodigo() {
        return classificacaoCodigo;
    }

    public String getRegra() {
        return regra;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public OrigemDecisao getOrigem() {
        return origem;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof PropostaClassificacao outra
                && Objects.equals(classificacaoCodigo, outra.classificacaoCodigo)
                && Objects.equals(regra, outra.regra)
                && Objects.equals(justificativa, outra.justificativa)
                && origem == outra.origem;
    }

    @Override
    public int hashCode() {
        return Objects.hash(classificacaoCodigo, regra, justificativa, origem);
    }

    @Override
    public String toString() {
        return "PropostaClassificacao[" + classificacaoCodigo + " via " + origem + " / " + regra + "]";
    }
}
