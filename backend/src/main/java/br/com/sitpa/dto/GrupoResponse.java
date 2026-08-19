package br.com.sitpa.dto;

public record GrupoResponse(Long id,
                            String codigo,
                            String nome,
                            String descricao,
                            boolean ativo,
                            long quantidadeFluxogramas) {
}
