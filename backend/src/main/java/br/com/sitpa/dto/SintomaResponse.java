package br.com.sitpa.dto;

public record SintomaResponse(Long id,
                              String codigo,
                              String nome,
                              String descricao,
                              ClassificacaoResponse classificacao,
                              boolean ativo) {
}
