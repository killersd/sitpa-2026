package br.com.sitpa.dto;

import br.com.sitpa.domain.OrigemDecisao;

import java.time.Instant;
import java.util.List;

/**
 * Registro do historico. Os campos vem do <i>snapshot</i> gravado no momento da triagem,
 * nao das entidades atuais: o historico mostra o protocolo como ele era naquele atendimento.
 */
public record ClassificacaoRealizadaResponse(Long id,
                                             Instant realizadaEm,
                                             String grupoNome,
                                             String fluxogramaNome,
                                             String classificacaoCodigo,
                                             String classificacaoNome,
                                             String classificacaoCor,
                                             Integer classificacaoPrioridade,
                                             Integer tempoMaximoEsperaMinutos,
                                             String localAtendimento,
                                             String tipoAtendimento,
                                             List<String> sintomas,
                                             OrigemDecisao origemDecisao,
                                             String regraAplicada,
                                             String justificativa,
                                             String identificadorPaciente,
                                             Integer idadeAnos,
                                             String observacoes,
                                             String usuarioLogin) {
}
