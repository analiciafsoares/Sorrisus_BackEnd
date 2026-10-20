package com.ifpb.sorrisus.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ifpb.sorrisus.exception.WhatsAppSendException;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;

import static com.ifpb.sorrisus.exception.WhatsAppSendException.TipoFalha.AMBIGUA;
import static com.ifpb.sorrisus.exception.WhatsAppSendException.TipoFalha.PERMANENTE;
import static com.ifpb.sorrisus.exception.WhatsAppSendException.TipoFalha.TRANSITORIA;

@Service
public class WhatsAppService {

    @Value("${zapi.instance}")
    private String instance;

    @Value("${zapi.token}")
    private String token;

    @Value("${zapi.client-token}")
    private String clientToken;

    private final ObjectMapper objectMapper;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(10))
            .readTimeout(Duration.ofSeconds(30))
            .build();

    public WhatsAppService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public boolean isConfigured() {
        return possuiTexto(instance) && possuiTexto(token) && possuiTexto(clientToken);
    }

    public void sendMessage(String phone, String message) {

        if (!isConfigured()) {
            throw new WhatsAppSendException("Configuração da Z-API incompleta", PERMANENTE, null);
        }

        MediaType mediaType = MediaType.parse("application/json");

        String json = criarPayload(phone, message);

        RequestBody body = RequestBody.create(json, mediaType);

        Request request = new Request.Builder()
                .url("https://api.z-api.io/instances/" + instance + "/token/" + token + "/send-text")
                .post(body)
                .addHeader("client-token", clientToken)
                .addHeader("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {

            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Sem corpo de erro";
                int statusHttp = response.code();
                throw new WhatsAppSendException(
                        "Erro ao enviar WhatsApp (HTTP " + statusHttp + "): " + errorBody,
                        classificarFalhaHttp(statusHttp),
                        statusHttp
                );
            }

        } catch (IOException e) {
            throw new WhatsAppSendException("Erro de comunicação com a Z-API", e, AMBIGUA);
        }
    }

    String criarPayload(String phone, String message) {
        try {
            return objectMapper.writeValueAsString(new MensagemWhatsApp(phone, message));
        } catch (JsonProcessingException e) {
            throw new WhatsAppSendException("Erro ao serializar mensagem para a Z-API", e, PERMANENTE);
        }
    }

    static WhatsAppSendException.TipoFalha classificarFalhaHttp(int statusHttp) {
        if (statusHttp == 429 || statusHttp == 500 || statusHttp == 502 || statusHttp == 503) {
            return TRANSITORIA;
        }

        if (statusHttp == 408 || statusHttp == 504) {
            return AMBIGUA;
        }

        return PERMANENTE;
    }

    private boolean possuiTexto(String valor) {
        return valor != null && !valor.isBlank();
    }

    private record MensagemWhatsApp(String phone, String message) {
    }
}
