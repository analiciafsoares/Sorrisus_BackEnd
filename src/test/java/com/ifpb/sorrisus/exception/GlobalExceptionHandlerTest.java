package com.ifpb.sorrisus.exception;

import com.ifpb.sorrisus.model.Paciente;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.TransactionSystemException;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Teste Unitário - GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    @DisplayName("Deve retornar 400 com mensagem segura para CPF inválido antecipadamente")
    void deveTratarCpfInvalidoAntesDaPersistencia() {
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/pacientes/3");

        ResponseEntity<?> resposta = handler.handleBusiness(
                new InvalidCPFException(), request);

        Map<?, ?> corpo = (Map<?, ?>) resposta.getBody();
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(corpo.get("message")).isEqualTo("CPF inválido. Verifique o número informado.");
    }

    @Test
    @DisplayName("Deve retornar 400 quando a transação encapsular CPF inválido")
    void deveTratarCpfInvalidoEncapsuladoNaTransacao() {
        Paciente paciente = new Paciente();
        paciente.setCpf("12345678900");
        Set<ConstraintViolation<Paciente>> violacoes = validator.validateProperty(paciente, "cpf");
        ConstraintViolationException validacao = new ConstraintViolationException(violacoes);
        TransactionSystemException transacao = new TransactionSystemException(
                "Não foi possível confirmar a transação", validacao);
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/pacientes/3");

        ResponseEntity<?> resposta = handler.handleTransactionSystemException(transacao, request);

        assertThat(violacoes).isNotEmpty();
        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resposta.getBody()).isInstanceOf(Map.class);
        Map<?, ?> corpo = (Map<?, ?>) resposta.getBody();
        assertThat(corpo.get("message")).isEqualTo("CPF inválido. Verifique o número informado.");
        assertThat(corpo.get("path")).isEqualTo("/api/pacientes/3");
    }

    @Test
    @DisplayName("Deve manter 500 para falha transacional sem violação de validação")
    void deveManterErroInternoParaOutraFalhaTransacional() {
        TransactionSystemException transacao = new TransactionSystemException("Falha transacional");
        MockHttpServletRequest request = new MockHttpServletRequest("PUT", "/api/pacientes/3");

        ResponseEntity<?> resposta = handler.handleTransactionSystemException(transacao, request);

        assertThat(resposta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        Map<?, ?> corpo = (Map<?, ?>) resposta.getBody();
        assertThat(corpo.get("message")).isEqualTo("Erro interno no servidor.");
    }
}
