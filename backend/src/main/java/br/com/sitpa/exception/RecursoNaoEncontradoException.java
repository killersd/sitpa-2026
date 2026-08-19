package br.com.sitpa.exception;

/** Recurso inexistente; vira HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Object identificador) {
        super(recurso + " nao encontrado(a): " + identificador);
    }

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
