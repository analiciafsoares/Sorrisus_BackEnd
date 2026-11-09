package com.ifpb.sorrisus.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;

@Entity
@Table(name = "consultas", indexes = {
        @Index(name = "idx_consulta_dentista_datahora", columnList = "dentista_id, data_hora"),
        @Index(name = "idx_consulta_paciente_datahora", columnList = "paciente_id, data_hora")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Consulta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data_hora", nullable = false)
    private LocalDateTime dataHora;

    // VARCHAR em vez do ENUM nativo do MySQL, para que novos status não exijam alterar a coluna.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private StatusConsulta status = StatusConsulta.PENDENTE;

    @ManyToOne
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne
    @JoinColumn(name = "dentista_id", nullable = false)
    private Dentista dentista;

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "prontuario_id")
    private Prontuario prontuario;

    // Agendamento que originou a consulta (nulo quando a consulta é criada diretamente).
    @OneToOne
    @JoinColumn(name = "agendamento_id")
    private Agendamento agendamento;

    @Column(length = 1000)
    private String observacao;
}
