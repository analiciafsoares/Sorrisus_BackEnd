package com.ifpb.sorrisus.repository;

import com.ifpb.sorrisus.model.Agendamento;
import com.ifpb.sorrisus.model.Dentista;
import com.ifpb.sorrisus.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    // Agendamentos cancelados não ocupam o horário. Status nulo corresponde a registros
    // anteriores à coluna "status" e é tratado como ativo.
    @Query("SELECT COUNT(a) > 0 FROM Agendamento a WHERE a.dentista = :dentista AND a.dataHora = :dataHora " +
            "AND (a.status IS NULL OR a.status <> com.ifpb.sorrisus.model.StatusAgendamento.CANCELADO)")
    boolean existeAgendamentoAtivo(@Param("dentista") Dentista dentista,
                                   @Param("dataHora") LocalDateTime dataHora);

    @Query("SELECT COUNT(a) > 0 FROM Agendamento a WHERE a.dentista = :dentista AND a.dataHora = :dataHora " +
            "AND a.id <> :id " +
            "AND (a.status IS NULL OR a.status <> com.ifpb.sorrisus.model.StatusAgendamento.CANCELADO)")
    boolean existeOutroAgendamentoAtivo(@Param("dentista") Dentista dentista,
                                        @Param("dataHora") LocalDateTime dataHora,
                                        @Param("id") Long id);

    @Query("SELECT COUNT(a) > 0 FROM Agendamento a WHERE a.paciente = :paciente AND a.dataHora >= :dataHora " +
            "AND (a.status = com.ifpb.sorrisus.model.StatusAgendamento.PENDENTE " +
            "OR (a.status IS NULL AND a.confirmado = false))")
    boolean existeAgendamentoPendenteFuturo(@Param("paciente") Paciente paciente,
                                            @Param("dataHora") LocalDateTime dataHora);

    List<Agendamento> findByDentistaAndDataHoraBetween(Dentista dentista, LocalDateTime start, LocalDateTime end);

    List<Agendamento> findByPaciente(Paciente paciente);

    List<Agendamento> findByDentista(Dentista dentista);
}
