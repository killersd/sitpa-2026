package br.com.sitpa.dto;

import jakarta.validation.constraints.*;

public record ClassificacaoRequest(

        @NotBlank
        @Size(max = 60)
        @Pattern(regexp = "^[A-Z0-9_]+$", message = "codigo deve conter apenas letras maiusculas, numeros e underscore")
        String codigo,

        @NotBlank @Size(max = 120) String nome,

        @NotBlank
        @Pattern(regexp = "^#([0-9a-fA-F]{6})$", message = "cor deve estar no formato #RRGGBB")
        String cor,

        @NotNull
        @Min(value = 1, message = "prioridade 1 e a mais grave")
        Integer prioridade,

        @NotNull @Min(0) Integer tempoMaximoEsperaMinutos,

        @NotBlank @Size(max = 160) String localAtendimento,

        @NotBlank @Size(max = 160) String tipoAtendimento,

        Boolean ativo) {
}
