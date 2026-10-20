package com.ifpb.sorrisus.repository;

import com.ifpb.sorrisus.model.LembreteRetorno;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LembreteRetornoRepository extends JpaRepository<LembreteRetorno, Long> {

    Optional<LembreteRetorno> findByPacienteIdAndUltimaConsultaId(Long pacienteId, Long ultimaConsultaId);
}
