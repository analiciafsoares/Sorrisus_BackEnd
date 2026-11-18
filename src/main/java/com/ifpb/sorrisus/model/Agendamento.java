package com.ifpb.sorrisus.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "agendamentos", indexes = {
        @Index(name = "idx_agendamento_dentista_datahora", columnList = "dentista_id, data_hora"),
        @Index(name = "idx_agendamento_paciente_datahora", columnList = "paciente_id, data_hora")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Agendamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    @Column(nullable = false)
    private boolean confirmado = false;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private StatusConsulta status = StatusConsulta.PENDENTE;

    @ManyToOne
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne
    @JoinColumn(name = "dentista_id", nullable = false)
    private Dentista dentista;

    @Column(name = "servico_id")
    private Long servicoId;

    @ManyToOne
    @JoinColumn(name = "recepcionista_id")
    private Recepcionista criadoPor;

    @Column(length = 1000)
    private String observacao;
}