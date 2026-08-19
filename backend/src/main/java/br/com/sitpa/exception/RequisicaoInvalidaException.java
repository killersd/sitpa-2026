package br.com.sitpa.exception;

/** Entrada coerente com o schema mas invalida no dominio; vira HTTP 400. */
public class RequisicaoInvalidaException extends RuntimeException {

    public RequisicaoInvalidaException(String mensagem) {
        super(mensagem);
    }
}
