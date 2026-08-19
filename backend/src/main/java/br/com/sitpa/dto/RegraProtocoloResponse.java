package br.com.sitpa.dto;

import br.com.sitpa.domain.TipoRegra;

import java.time.Instant;

public record RegraProtocoloResponse(Long id,
                                     String nome,
                                     String descricao,
                                     TipoRegra tipo,
                                     String conteudo,
                                     boolean ativo,
                                     Long versao,
                                     Instant atualizadoEm) {
}
