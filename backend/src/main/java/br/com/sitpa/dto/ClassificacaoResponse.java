package br.com.sitpa.dto;

public record ClassificacaoResponse(Long id,
                                    String codigo,
                                    String nome,
                                    String cor,
                                    Integer prioridade,
                                    Integer tempoMaximoEsperaMinutos,
                                    String localAtendimento,
                                    String tipoAtendimento,
                                    boolean ativo) {
}
