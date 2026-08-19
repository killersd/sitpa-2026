package br.com.sitpa.dto;

/** Distribuicao de atendimentos por nivel de risco. */
public record EstatisticaClassificacaoResponse(String codigo,
                                               String nome,
                                               String cor,
                                               Integer prioridade,
                                               long total,
                                               double percentual) {
}
