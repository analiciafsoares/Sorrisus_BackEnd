package com.ifpb.sorrisus.repository;

import com.ifpb.sorrisus.model.Agendamento;
import com.ifpb.sorrisus.model.Dentista;
import com.ifpb.sorrisus.model.Paciente;
import com.ifpb.sorrisus.model.StatusConsulta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {

    List<Agendamento> findByDentistaAndDataHoraBetween(Dentista dentista, LocalDateTime start, LocalDateTime end);

    boolean existsByPacienteAndDataHoraGreaterThanEqualAndConfirmadoFalse(
            Paciente paciente,
            LocalDateTime dataHora);

    boolean existsByDentistaAndDataHoraAndStatusNot(Dentista dentista, LocalDateTime dataHora, StatusConsulta status);

    boolean existsByDentistaAndDataHoraAndIdNotAndStatusNot(Dentista dentista, LocalDateTime dataHora, Long id,
            StatusConsulta status);

    List<Agendamento> findByPaciente(Paciente paciente);

    List<Agendamento> findByDentista(Dentista dentista);
}