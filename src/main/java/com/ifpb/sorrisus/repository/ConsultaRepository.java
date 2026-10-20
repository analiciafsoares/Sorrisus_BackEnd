package com.ifpb.sorrisus.repository;

import com.ifpb.sorrisus.model.Consulta;
import com.ifpb.sorrisus.model.Dentista;
import com.ifpb.sorrisus.model.Paciente;
import com.ifpb.sorrisus.model.StatusConsulta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ConsultaRepository extends JpaRepository<Consulta, Long> {

    List<Consulta> findByDentistaAndDataHoraBetween(Dentista dentista, LocalDateTime start, LocalDateTime end);

    boolean existsByDentistaAndDataHora(Dentista dentista, LocalDateTime dataHora);

    boolean existsByDentistaAndDataHoraAndIdNot(Dentista dentista, LocalDateTime dataHora, Long id);

    List<Consulta> findByPaciente(Paciente paciente);

    boolean existsByPacienteAndDataHoraGreaterThanEqualAndStatusIn(
            Paciente paciente,
            LocalDateTime dataHora,
            Collection<StatusConsulta> status
    );

    @Query("SELECT c FROM Consulta c WHERE c.paciente = :paciente AND c.status = :status ORDER BY c.dataHora DESC")
    List<Consulta> findLastConsultasByPacienteAndStatus(@Param("paciente") Paciente paciente, @Param("status") StatusConsulta status);

    @Query("SELECT DISTINCT c.paciente FROM Consulta c WHERE c.status = 'REALIZADA' " +
            "AND c.dataHora < :dataLimite " +
            "AND c.paciente.id NOT IN (" +
            "   SELECT c2.paciente.id FROM Consulta c2 " +
            "   WHERE c2.status = 'REALIZADA' AND c2.dataHora >= :dataLimite" +
            ")")
    List<Paciente> findPacientesComUltimaConsultaAntesDe(@Param("dataLimite") LocalDateTime dataLimite);

    @Query("SELECT c FROM Consulta c WHERE c.paciente = :paciente AND c.status = 'REALIZADA' ORDER BY c.dataHora DESC")
    List<Consulta> findUltimaConsultaRealizadaPorPaciente(@Param("paciente") Paciente paciente);

}
