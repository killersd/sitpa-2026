package br.com.sitpa.dto;

import br.com.sitpa.domain.OrigemDecisao;

/**
 * Uma das sugestoes produzidas pelo motor durante a inferencia.
 *
 * <p>A API devolve <b>todas</b> as propostas, nao so a vencedora. Em triagem, quem assina a
 * classificacao e o profissional, e ele precisa enxergar por que o sistema chegou aquele nivel
 * de risco &mdash; um resultado sem rastro seria uma caixa-preta em cima de uma decisao clinica.</p>
 */
public record PropostaResponse(String classificacaoCodigo,
                               String classificacaoNome,
                               String cor,
                               Integer prioridade,
                               OrigemDecisao origem,
                               String regra,
                               String justificativa,
                               boolean vencedora) {
}
