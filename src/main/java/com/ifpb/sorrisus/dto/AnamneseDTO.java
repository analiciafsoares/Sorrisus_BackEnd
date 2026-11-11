package com.ifpb.sorrisus.dto;

import lombok.Data;

@Data
public class AnamneseDTO {
    private Long id;
    private Long pacienteId;

    private String alergiaResposta;
    private String alergiaNotas;

    private String antibioticoResposta;
    private String antibioticoNotas;

    private String anestesicoResposta;
    private String anestesicoNotas;

    private String sensibilidadeResposta;
    private String sensibilidadeNotas;

    private String pressaoResposta;
    private String pressaoNotas;

    private String medicamentoResposta;
    private String medicamentoNotas;

    private String problemaSaudeResposta;
    private String problemaSaudeNotas;
}
