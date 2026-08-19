package br.com.sitpa.dto;

/**
 * Sintoma no contexto de um fluxograma.
 *
 * @param classificacaoEfetiva a que realmente vale aqui: a especifica quando houver, senao a padrao
 */
public record SintomaAssociadoResponse(Long associacaoId,
                                       Long sintomaId,
                                       String codigo,
                                       String nome,
                                       String descricao,
                                       ClassificacaoResponse classificacaoPadrao,
                                       ClassificacaoResponse classificacaoEspecifica,
                                       ClassificacaoResponse classificacaoEfetiva,
                                       Integer ordem,
                                       boolean ativo) {
}
