package br.com.sitpa.dto;

import br.com.sitpa.domain.OrigemDecisao;

import java.util.List;

/**
 * Resultado da inferencia, alimentando a tela de revisao antes da confirmacao.
 *
 * @param classificacao   nivel de risco vencedor (o mais grave entre as propostas)
 * @param origemDecisao   camada do motor que produziu a vencedora
 * @param regraAplicada   nome da regra vencedora, para auditoria
 * @param propostas       todas as sugestoes avaliadas, inclusive as descartadas
 * @param versaoBaseRegras versao da base de conhecimento usada nesta inferencia
 */
public record ResultadoTriagemResponse(ReferenciaResponse grupo,
                                       ReferenciaResponse fluxograma,
                                       List<ReferenciaResponse> sintomasSelecionados,
                                       ClassificacaoResponse classificacao,
                                       OrigemDecisao origemDecisao,
                                       String regraAplicada,
                                       String justificativa,
                                       List<PropostaResponse> propostas,
                                       long versaoBaseRegras) {
}
