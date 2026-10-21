package com.ecogestao.api.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ---------- Erros de negócio ----------

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> tratarRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        ErroResposta erro = new ErroResposta(HttpStatus.NOT_FOUND.value(), "Recurso não encontrado", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    @ExceptionHandler(ValorInvalidoException.class)
    public ResponseEntity<ErroResposta> tratarValorInvalido(ValorInvalidoException ex) {
        ErroResposta erro = new ErroResposta(HttpStatus.BAD_REQUEST.value(),
                "Valor inválido", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> tratarValidacao(MethodArgumentNotValidException ex) {
        String mensagens = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        ErroResposta erro = new ErroResposta(HttpStatus.BAD_REQUEST.value(), "Erro de validação", mensagens);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    // ---------- Erros de requisição (Dia 27) ----------

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResposta> tratarParametroInvalido(MethodArgumentTypeMismatchException ex) {
        String mensagem = "Parâmetro '" + ex.getName() + "' com valor inválido: '" + ex.getValue()
                + "'. Datas devem seguir o formato yyyy-MM-dd e ids devem ser números inteiros";
        ErroResposta erro = new ErroResposta(HttpStatus.BAD_REQUEST.value(),
                "Parâmetro inválido", mensagem);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResposta> tratarCorpoIlegivel(HttpMessageNotReadableException ex) {
        ErroResposta erro = new ErroResposta(HttpStatus.BAD_REQUEST.value(),
                "Requisição inválida",
                "Corpo da requisição ausente, malformado ou com valor em formato inválido (datas devem usar yyyy-MM-dd)");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erro);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResposta> tratarMetodoNaoSuportado(HttpRequestMethodNotSupportedException ex) {
        String aceitos = ex.getSupportedHttpMethods() == null ? "" :
                ex.getSupportedHttpMethods().stream()
                        .map(HttpMethod::name)
                        .collect(Collectors.joining(", "));
        String mensagem = "Método " + ex.getMethod() + " não é suportado para este endereço"
                + (aceitos.isEmpty() ? "" : ". Métodos aceitos: " + aceitos);
        ErroResposta erro = new ErroResposta(HttpStatus.METHOD_NOT_ALLOWED.value(),
                "Método não permitido", mensagem);
        ResponseEntity.BodyBuilder resposta = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        if (!aceitos.isEmpty()) {
            resposta.header(HttpHeaders.ALLOW, aceitos);
        }
        return resposta.body(erro);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResposta> tratarMediaType(HttpMediaTypeNotSupportedException ex) {
        ErroResposta erro = new ErroResposta(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                "Tipo de conteúdo não suportado", "Envie o corpo da requisição como application/json");
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(erro);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResposta> tratarRotaInexistente(NoResourceFoundException ex) {
        ErroResposta erro = new ErroResposta(HttpStatus.NOT_FOUND.value(),
                "Endereço não encontrado", "Nenhum endpoint corresponde a /" + ex.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }

    // ---------- Erro inesperado (500) ----------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> tratarErroGenerico(Exception ex) {
        LOG.error("Erro inesperado", ex);
        ErroResposta erro = new ErroResposta(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Erro interno", "Ocorreu um erro inesperado. Tente novamente mais tarde");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }
}