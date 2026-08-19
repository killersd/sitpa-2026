package br.com.sitpa.dto;

import jakarta.validation.constraints.*;

public record SintomaRequest(

        @NotBlank
        @Size(max = 80)
        @Pattern(regexp = "^[A-Z0-9_]+$", message = "codigo deve conter apenas letras maiusculas, numeros e underscore")
        String codigo,

        @NotBlank @Size(max = 160) String nome,

        @Size(max = 500) String descricao,

        @NotNull Long classificacaoId,

        Boolean ativo) {
}
