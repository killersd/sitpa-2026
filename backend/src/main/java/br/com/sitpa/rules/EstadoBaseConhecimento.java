package br.com.sitpa.rules;

import java.time.Instant;
import java.util.List;

/**
 * Retrato da base de conhecimento carregada, exposto pelo endpoint de administracao
 * para que se saiba exatamente qual conhecimento esta no ar.
 *
 * @param versao          numero incrementado a cada recarga bem-sucedida
 * @param carregadaEm     instante da ultima recarga
 * @param regrasDrl       nomes das regras DRL compiladas
 * @param modelosDmn      nomes dos modelos DMN compilados
 * @param artefatos       descricao "nome (origem)" de cada artefato compilado
 */
public record EstadoBaseConhecimento(long versao,
                                     Instant carregadaEm,
                                     List<String> regrasDrl,
                                     List<String> modelosDmn,
                                     List<String> artefatos) {
}
