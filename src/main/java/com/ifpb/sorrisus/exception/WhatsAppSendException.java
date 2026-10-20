package com.ifpb.sorrisus.exception;

public class WhatsAppSendException extends RuntimeException {

    public enum TipoFalha {
        TRANSITORIA,
        PERMANENTE,
        AMBIGUA
    }

    private final TipoFalha tipoFalha;
    private final Integer statusHttp;

    public WhatsAppSendException(String message, TipoFalha tipoFalha, Integer statusHttp) {
        super(message);
        this.tipoFalha = tipoFalha;
        this.statusHttp = statusHttp;
    }

    public WhatsAppSendException(String message, Throwable cause, TipoFalha tipoFalha) {
        super(message, cause);
        this.tipoFalha = tipoFalha;
        this.statusHttp = null;
    }

    public TipoFalha getTipoFalha() {
        return tipoFalha;
    }

    public Integer getStatusHttp() {
        return statusHttp;
    }
}
