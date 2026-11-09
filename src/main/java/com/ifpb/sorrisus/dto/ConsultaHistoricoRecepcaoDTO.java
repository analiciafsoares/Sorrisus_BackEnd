package com.ifpb.sorrisus.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ConsultaHistoricoRecepcaoDTO {

    private Long id;
    private LocalDateTime dataHora;
    private String nomePaciente;
    private String nomeDentista;
    private String status;
    private String observacao;
}
