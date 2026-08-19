package br.com.sitpa.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Vincula um sintoma a um fluxograma.
 *
 * @param classificacaoEspecificaId gravidade deste sintoma <i>neste</i> fluxograma; quando nulo,
 *                                  vale a classificacao padrao cadastrada no sintoma
 */
public record AssociacaoSintomaRequest(@NotNull Long sintomaId,
                                       Long classificacaoEspecificaId,
                                       @Min(0) Integer ordem) {
}
