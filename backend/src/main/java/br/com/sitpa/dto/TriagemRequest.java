package br.com.sitpa.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Set;

/**
 * Entrada do fluxo de classificacao de paciente.
 *
 * <p>Usada tanto na simulacao (<code>POST /triagens/avaliar</code>, que so infere e devolve o
 * resultado para a tela de revisao) quanto na confirmacao (<code>POST /triagens</code>, que grava
 * o historico). O mesmo payload nos dois passos garante que o que foi revisado e exatamente o que
 * sera registrado.</p>
 *
 * @param identificadorPaciente identificacao local do atendimento (senha, prontuario); opcional e
 *                              deliberadamente livre, para nao obrigar a guardar dado pessoal
 */
public record TriagemRequest(

        @NotNull Long grupoId,

        @NotNull Long fluxogramaId,

        @NotNull @Size(min = 0, message = "informe a lista de sintomas, ainda que vazia")
        Set<Long> sintomasIds,

        @Min(0) @Max(130) Integer idadeAnos,

        Boolean gestante,

        @Size(max = 80) String identificadorPaciente,

        @Size(max = 1000) String observacoes) {

    public boolean isGestante() {
        return Boolean.TRUE.equals(gestante);
    }
}
