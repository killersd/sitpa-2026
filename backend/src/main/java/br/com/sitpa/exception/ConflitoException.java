package br.com.sitpa.exception;

/** Violacao de regra de cadastro (codigo duplicado, exclusao de registro em uso); vira HTTP 409. */
public class ConflitoException extends RuntimeException {

    public ConflitoException(String mensagem) {
        super(mensagem);
    }
}
