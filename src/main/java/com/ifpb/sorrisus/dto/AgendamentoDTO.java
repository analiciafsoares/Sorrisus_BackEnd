package com.ifpb.sorrisus.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
public class AgendamentoDTO {
    private Long id;

    @NotNull
    private LocalDateTime dataHora;

    @NotNull
    private Long pacienteId;

    @NotNull
    private Long dentistaId;

    private Long servicoId;
    private Long recepcionistaId;
    private Boolean confirmado;

    private String status;
    private String observacao;
}
