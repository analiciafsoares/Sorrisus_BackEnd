package com.ifpb.sorrisus.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private Map<String, Object> buildResponse(HttpStatus status, String message, String path) {
                Map<String, Object> body = new HashMap<>();
                body.put("timestamp", LocalDateTime.now());
                body.put("status", status.value());
                body.put("error", status.getReasonPhrase());
                body.put("message", message);
                body.put("path", path);
                return body;
        }

        @ExceptionHandler(ResourceNotFoundException.class)
        public ResponseEntity<?> handleNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI()));
        }

        @ExceptionHandler(BusinessException.class)
        public ResponseEntity<?> handleBusiness(BusinessException ex, HttpServletRequest request) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI()));
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
                Map<String, String> errors = new HashMap<>();

                ex.getBindingResult().getFieldErrors()
                                .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(buildResponse(HttpStatus.BAD_REQUEST, errors.toString(),
                                                request.getRequestURI()));
        }

        @ExceptionHandler(DataIntegrityViolationException.class)
        public ResponseEntity<?> handleIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(buildResponse(HttpStatus.CONFLICT,
                                                "Violação de integridade no banco de dados.", request.getRequestURI()));
        }

        @ExceptionHandler(HttpMessageNotReadableException.class)
        public ResponseEntity<?> handleBadJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(buildResponse(HttpStatus.BAD_REQUEST,
                                                "Formato JSON inválido ou campos incorretos.",
                                                request.getRequestURI()));
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<?> handleGeneric(Exception ex, HttpServletRequest request) {
                ex.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                                                "Erro interno no servidor.", request.getRequestURI()));
        }

        @ExceptionHandler(BadCredentialsException.class)
        public ResponseEntity<?> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                                .body(buildResponse(HttpStatus.UNAUTHORIZED, "Credenciais inválidas.",
                                                request.getRequestURI()));
        }

        // Sem este handler, o @ExceptionHandler(Exception.class) transformaria o acesso negado
        // por @PreAuthorize em erro 500.
        @ExceptionHandler(AccessDeniedException.class)
        public ResponseEntity<?> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(buildResponse(HttpStatus.FORBIDDEN,
                                                "Você não tem permissão para acessar este recurso.",
                                                request.getRequestURI()));
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<?> handleConstraintViolation(ConstraintViolationException ex,
                        HttpServletRequest request) {

                boolean isCPFError = ex.getConstraintViolations().stream()
                                .anyMatch(v -> v.getPropertyPath().toString().toLowerCase().contains("cpf"));

                if (isCPFError) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                        .body(buildResponse(
                                                        HttpStatus.BAD_REQUEST,
                                                        "CPF inválido. Verifique o número informado.",
                                                        request.getRequestURI()));
                }

                String mensagemErro = ex.getConstraintViolations().stream()
                                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                                .reduce("", (a, b) -> a + "\n" + b);

                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(buildResponse(
                                                HttpStatus.BAD_REQUEST,
                                                mensagemErro.trim(),
                                                request.getRequestURI()));
        }

}
