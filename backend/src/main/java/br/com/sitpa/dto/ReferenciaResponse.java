package br.com.sitpa.dto;

/** Referencia enxuta a uma entidade de cadastro, usada dentro de outras respostas. */
public record ReferenciaResponse(Long id, String codigo, String nome) {
}
