package com.ifpb.sorrisus.exception;

public class InvalidCPFException extends BusinessException {
    public InvalidCPFException() {
        super("CPF inválido. Verifique o número informado.");
    }
}
