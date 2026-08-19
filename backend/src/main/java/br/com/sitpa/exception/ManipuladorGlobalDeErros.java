package br.com.sitpa.exception;

import br.com.sitpa.dto.ErroResponse;
import br.com.sitpa.rules.ErroCompilacaoRegrasException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/** Traduz excecoes do dominio em respostas HTTP consistentes. */
@RestControllerAdvice
public class ManipuladorGlobalDeErros {

    private static final Logger log = LoggerFactory.getLogger(ManipuladorGlobalDeErros.class);

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(RecursoNaoEncontradoException e, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErroResponse.de(404, "Nao encontrado", e.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResponse> conflito(ConflitoException e, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErroResponse.de(409, "Conflito", e.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(RequisicaoInvalidaException.class)
    public ResponseEntity<ErroResponse> requisicaoInvalida(RequisicaoInvalidaException e, HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(ErroResponse.de(400, "Requisicao invalida", e.getMessage(), req.getRequestURI()));
    }

    /**
     * Corpo ilegivel ou com tipo errado (por exemplo {@code "grupoId": [1]} em vez de {@code 1}).
     * E erro de quem chamou, nao falha do servidor: sem este tratamento a excecao cairia no
     * handler generico e viraria um 500 enganoso.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoIlegivel(HttpMessageNotReadableException e, HttpServletRequest req) {
        log.warn("Corpo invalido em {} {}: {}", req.getMethod(), req.getRequestURI(), e.getMessage());
        return ResponseEntity.badRequest().body(ErroResponse.de(400, "Corpo invalido",
                "Nao foi possivel interpretar o corpo da requisicao. Verifique os tipos dos campos enviados.",
                req.getRequestURI()));
    }

    /** Parametro de rota ou de query com tipo incompativel (ex.: /grupos/abc). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> parametroInvalido(MethodArgumentTypeMismatchException e,
                                                          HttpServletRequest req) {
        return ResponseEntity.badRequest().body(ErroResponse.de(400, "Parametro invalido",
                "O parametro \"" + e.getName() + "\" recebeu um valor incompativel.", req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException e, HttpServletRequest req) {
        Map<String, String> campos = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(erro -> campos.putIfAbsent(erro.getField(), erro.getDefaultMessage()));
        e.getBindingResult().getGlobalErrors()
                .forEach(erro -> campos.putIfAbsent(erro.getObjectName(), erro.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ErroResponse(Instant.now(), 400, "Validacao",
                "Ha campos invalidos na requisicao.", req.getRequestURI(), campos, null));
    }

    /**
     * Uma regra que nao compila e erro de quem a escreveu, nao falha do servidor: devolve 422 com
     * a lista de problemas do compilador para que o administrador corrija o DRL/DMN.
     */
    @ExceptionHandler(ErroCompilacaoRegrasException.class)
    public ResponseEntity<ErroResponse> regraInvalida(ErroCompilacaoRegrasException e, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(new ErroResponse(Instant.now(), 422,
                "Regra invalida",
                "As regras nao compilam; a base de conhecimento anterior continua ativa.",
                req.getRequestURI(), null, e.getProblemas()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> acessoNegado(AccessDeniedException e, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErroResponse.de(403, "Acesso negado",
                        "Seu perfil nao permite esta operacao.", req.getRequestURI()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponse> naoAutenticado(AuthenticationException e, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErroResponse.de(401, "Nao autenticado", e.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> inesperado(Exception e, HttpServletRequest req) {
        log.error("Erro nao tratado em {} {}", req.getMethod(), req.getRequestURI(), e);
        return ResponseEntity.internalServerError()
                .body(ErroResponse.de(500, "Erro interno",
                        "Ocorreu um erro inesperado. Consulte os logs do servidor.", req.getRequestURI()));
    }
}
