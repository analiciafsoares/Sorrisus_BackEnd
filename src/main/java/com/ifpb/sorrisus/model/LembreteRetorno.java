package com.ifpb.sorrisus.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "lembretes_retorno", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_lembrete_retorno_paciente_consulta",
                columnNames = {"paciente_id", "ultima_consulta_id"}
        )
})
@Getter
@Setter
@NoArgsConstructor
public class LembreteRetorno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ultima_consulta_id", nullable = false)
    private Consulta ultimaConsulta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StatusLembreteRetorno status;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "data_ultima_tentativa")
    private LocalDateTime dataUltimaTentativa;

    @Column(name = "data_envio")
    private LocalDateTime dataEnvio;

    @Column(length = 1000)
    private String erro;

    @Version
    private Long version;
}
