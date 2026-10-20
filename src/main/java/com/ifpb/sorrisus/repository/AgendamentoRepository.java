package com.ifpb.sorrisus.repository;

import com.ifpb.sorrisus.model.Agendamento;
import com.ifpb.sorrisus.model.Dentista;
import com.ifpb.sorrisus.model.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    boolean existsByDentistaAndDataHoraAndIdNot(Dentista dentista, LocalDateTime dataHora, Long id);

    List<Agendamento> findByDentistaAndDataHoraBetween(Dentista dentista, LocalDateTime start, LocalDateTime end);

    boolean existsByDentistaAndDataHora(Dentista dentista, LocalDateTime dataHora);

    boolean existsByPacienteAndDataHoraGreaterThanEqualAndConfirmadoFalse(
            Paciente paciente,
            LocalDateTime dataHora
    );

    List<Agendamento> findByPaciente(Paciente paciente);

    List<Agendamento> findByDentista(Dentista dentista);
}
