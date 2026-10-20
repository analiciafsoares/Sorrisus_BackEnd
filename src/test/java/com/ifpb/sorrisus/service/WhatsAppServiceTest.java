package com.ifpb.sorrisus.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ifpb.sorrisus.exception.WhatsAppSendException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WhatsAppServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final WhatsAppService service = new WhatsAppService(objectMapper);

    @ParameterizedTest(name = "deve serializar mensagem com {0}")
    @MethodSource("mensagensParaSerializacao")
    void deveSerializarPayloadSemAlterarMensagem(String cenario, String mensagem) throws Exception {
        String telefone = "5583999999999";

        String json = service.criarPayload(telefone, mensagem);
        JsonNode payload = objectMapper.readTree(json);

        assertEquals(telefone, payload.get("phone").asText());
        assertEquals(mensagem, payload.get("message").asText());
    }

    @Test
    void deveClassificarFalhaDeSerializacaoComoPermanente() throws Exception {
        ObjectMapper objectMapperComFalha = mock(ObjectMapper.class);
        when(objectMapperComFalha.writeValueAsString(any()))
                .thenThrow(new JsonProcessingException("Falha simulada") { });
        WhatsAppService serviceComFalha = new WhatsAppService(objectMapperComFalha);

        WhatsAppSendException exception = assertThrows(
                WhatsAppSendException.class,
                () -> serviceComFalha.criarPayload("5583999999999", "Mensagem")
        );

        assertEquals(WhatsAppSendException.TipoFalha.PERMANENTE, exception.getTipoFalha());
    }

    @Test
    void deveClassificarSomenteErrosHttpTransitoriosComoRepetiveis() {
        assertEquals(WhatsAppSendException.TipoFalha.TRANSITORIA, WhatsAppService.classificarFalhaHttp(429));
        assertEquals(WhatsAppSendException.TipoFalha.TRANSITORIA, WhatsAppService.classificarFalhaHttp(500));
        assertEquals(WhatsAppSendException.TipoFalha.TRANSITORIA, WhatsAppService.classificarFalhaHttp(502));
        assertEquals(WhatsAppSendException.TipoFalha.TRANSITORIA, WhatsAppService.classificarFalhaHttp(503));
    }

    @Test
    void deveClassificarTimeoutsHttpComoAmbiguos() {
        assertEquals(WhatsAppSendException.TipoFalha.AMBIGUA, WhatsAppService.classificarFalhaHttp(408));
        assertEquals(WhatsAppSendException.TipoFalha.AMBIGUA, WhatsAppService.classificarFalhaHttp(504));
    }

    @Test
    void deveClassificarDemaisErrosHttpComoPermanentes() {
        assertEquals(WhatsAppSendException.TipoFalha.PERMANENTE, WhatsAppService.classificarFalhaHttp(400));
        assertEquals(WhatsAppSendException.TipoFalha.PERMANENTE, WhatsAppService.classificarFalhaHttp(401));
        assertEquals(WhatsAppSendException.TipoFalha.PERMANENTE, WhatsAppService.classificarFalhaHttp(403));
        assertEquals(WhatsAppSendException.TipoFalha.PERMANENTE, WhatsAppService.classificarFalhaHttp(404));
        assertEquals(WhatsAppSendException.TipoFalha.PERMANENTE, WhatsAppService.classificarFalhaHttp(405));
        assertEquals(WhatsAppSendException.TipoFalha.PERMANENTE, WhatsAppService.classificarFalhaHttp(415));
        assertEquals(WhatsAppSendException.TipoFalha.PERMANENTE, WhatsAppService.classificarFalhaHttp(422));
        assertEquals(WhatsAppSendException.TipoFalha.PERMANENTE, WhatsAppService.classificarFalhaHttp(501));
    }

    private static Stream<Arguments> mensagensParaSerializacao() {
        return Stream.of(
                Arguments.of("texto simples", "Consulta confirmada"),
                Arguments.of("aspas", "Paciente respondeu \"sim\""),
                Arguments.of("quebras de linha", "Primeira linha\n\nSegunda linha\r\nTerceira linha"),
                Arguments.of("caracteres acentuados", "Olá, José. Prevenção e saúde bucal.")
        );
    }
}
