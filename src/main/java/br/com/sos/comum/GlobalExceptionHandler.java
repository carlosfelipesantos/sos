package br.com.sos.comum;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Converte as exceções da aplicação no formato único de erro (RNF012). */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> regraNegocio(RegraNegocioException e) {
        return erro(HttpStatus.BAD_REQUEST, e.getCampo(), e.getMessage());
    }

    @ExceptionHandler(NaoEncontradoException.class)
    public ResponseEntity<ErroResponse> naoEncontrado(NaoEncontradoException e) {
        return erro(HttpStatus.NOT_FOUND, null, e.getMessage());
    }

    @ExceptionHandler(ConflitoException.class)
    public ResponseEntity<ErroResponse> conflito(ConflitoException e) {
        return erro(HttpStatus.CONFLICT, e.getCampo(), e.getMessage());
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResponse> acessoNegado(AcessoNegadoException e) {
        return erro(HttpStatus.FORBIDDEN, null, e.getMessage());
    }

    /** @PreAuthorize recusou: o papel do usuário não pode usar esta função (RNF002). */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> semPermissao(AccessDeniedException e) {
        return erro(HttpStatus.FORBIDDEN, null, "Você não tem permissão para usar esta função.");
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponse> autenticacao(AuthenticationException e) {
        return erro(HttpStatus.UNAUTHORIZED, null, "Login ou senha inválidos.");
    }

    /** Erros de @Valid: devolve o primeiro campo com problema. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> validacao(MethodArgumentNotValidException e) {
        FieldError campo = e.getBindingResult().getFieldError();
        if (campo == null) {
            return erro(HttpStatus.BAD_REQUEST, null, "Dados inválidos.");
        }
        return erro(HttpStatus.BAD_REQUEST, campo.getField(), campo.getDefaultMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> corpoInvalido(HttpMessageNotReadableException e) {
        return erro(HttpStatus.BAD_REQUEST, null, "O corpo da requisição está ausente ou em formato inválido.");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return erro(HttpStatus.BAD_REQUEST, e.getName(), "Valor inválido para o parâmetro " + e.getName() + ".");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> rotaInexistente(NoResourceFoundException e) {
        return erro(HttpStatus.NOT_FOUND, null, "Endereço não encontrado.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> inesperado(Exception e) {
        log.error("Erro inesperado", e);
        return erro(HttpStatus.INTERNAL_SERVER_ERROR, null, "Erro interno. Tente novamente.");
    }

    private ResponseEntity<ErroResponse> erro(HttpStatus status, String campo, String mensagem) {
        return ResponseEntity.status(status).body(new ErroResponse(campo, mensagem));
    }
}
