package br.com.sitpa.dto;

public record FluxogramaResponse(Long id,
                                 String codigo,
                                 String nome,
                                 String descricao,
                                 ReferenciaResponse grupo,
                                 String modeloDmn,
                                 boolean ativo,
                                 long quantidadeSintomas) {
}
