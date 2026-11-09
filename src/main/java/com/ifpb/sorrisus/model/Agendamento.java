package com.ifpb.sorrisus.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
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

    // Coluna sem NOT NULL para que bancos já existentes recebam a coluna nova sem erro;
    // registros antigos têm o status derivado de "confirmado" ao serem carregados.
    // VARCHAR em vez do ENUM nativo do MySQL, para que novos status não exijam alterar a coluna.
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 20)
    private StatusAgendamento status = StatusAgendamento.PENDENTE;

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

    @PostLoad
    void preencherStatusLegado() {
        if (status == null) {
            status = confirmado ? StatusAgendamento.CONFIRMADO : StatusAgendamento.PENDENTE;
        }
    }
}
