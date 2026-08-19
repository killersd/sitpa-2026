package br.com.sitpa.dto;

import jakarta.validation.constraints.*;

public record FluxogramaRequest(

        @NotBlank
        @Size(max = 60)
        @Pattern(regexp = "^[A-Z0-9_]+$", message = "codigo deve conter apenas letras maiusculas, numeros e underscore")
        String codigo,

        @NotBlank @Size(max = 120) String nome,

        @Size(max = 500) String descricao,

        @NotNull Long grupoId,

        /** Nome do modelo DMN a executar para esta queixa. Opcional. */
        @Size(max = 120) String modeloDmn,

        Boolean ativo) {
}
