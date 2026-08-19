package br.com.sitpa.dto;

import br.com.sitpa.domain.TipoRegra;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegraProtocoloRequest(

        @NotBlank @Size(max = 120) String nome,

        @Size(max = 500) String descricao,

        @NotNull TipoRegra tipo,

        @NotBlank String conteudo,

        Boolean ativo) {
}
