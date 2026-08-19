package br.com.sitpa.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Corpo padrao de erro da API. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErroResponse(Instant momento,
                           int status,
                           String erro,
                           String mensagem,
                           String caminho,
                           Map<String, String> camposInvalidos,
                           List<String> detalhes) {

    public static ErroResponse de(int status, String erro, String mensagem, String caminho) {
        return new ErroResponse(Instant.now(), status, erro, mensagem, caminho, null, null);
    }
}
