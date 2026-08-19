package br.com.sitpa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GrupoRequest(

        @NotBlank
        @Size(max = 60)
        @Pattern(regexp = "^[A-Z0-9_]+$", message = "codigo deve conter apenas letras maiusculas, numeros e underscore")
        String codigo,

        @NotBlank @Size(max = 120) String nome,

        @Size(max = 500) String descricao,

        Boolean ativo) {
}
