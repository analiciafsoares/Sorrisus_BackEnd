package com.ifpb.sorrisus.controller;

import com.ifpb.sorrisus.dto.AnamneseDTO;
import com.ifpb.sorrisus.model.Anamnese;
import com.ifpb.sorrisus.service.AnamneseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/anamneses")
public class AnamneseController {

    private final AnamneseService anamneseService;

    public AnamneseController(AnamneseService anamneseService) {
        this.anamneseService = anamneseService;
    }

    @GetMapping("/paciente/{pacienteId}")
    public ResponseEntity<AnamneseDTO> buscarPorPaciente(@PathVariable Long pacienteId) {
        Anamnese anamnese = anamneseService.buscarExistentePorPacienteId(pacienteId);
        return ResponseEntity.ok(toDTO(anamnese));
    }

    private AnamneseDTO toDTO(Anamnese anamnese) {
        AnamneseDTO dto = new AnamneseDTO();
        dto.setId(anamnese.getId());
        dto.setPacienteId(anamnese.getPaciente().getId());
        dto.setAlergiaResposta(anamnese.getAlergiaResposta());
        dto.setAlergiaNotas(anamnese.getAlergiaNotas());
        dto.setAntibioticoResposta(anamnese.getAntibioticoResposta());
        dto.setAntibioticoNotas(anamnese.getAntibioticoNotas());
        dto.setAnestesicoResposta(anamnese.getAnestesicoResposta());
        dto.setAnestesicoNotas(anamnese.getAnestesicoNotas());
        dto.setSensibilidadeResposta(anamnese.getSensibilidadeResposta());
        dto.setSensibilidadeNotas(anamnese.getSensibilidadeNotas());
        dto.setPressaoResposta(anamnese.getPressaoResposta());
        dto.setPressaoNotas(anamnese.getPressaoNotas());
        dto.setMedicamentoResposta(anamnese.getMedicamentoResposta());
        dto.setMedicamentoNotas(anamnese.getMedicamentoNotas());
        dto.setProblemaSaudeResposta(anamnese.getProblemaSaudeResposta());
        dto.setProblemaSaudeNotas(anamnese.getProblemaSaudeNotas());
        return dto;
    }
}
