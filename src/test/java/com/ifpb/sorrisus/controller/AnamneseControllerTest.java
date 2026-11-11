package com.ifpb.sorrisus.controller;

import com.ifpb.sorrisus.dto.AnamneseDTO;
import com.ifpb.sorrisus.model.Anamnese;
import com.ifpb.sorrisus.model.Paciente;
import com.ifpb.sorrisus.service.AnamneseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnamneseControllerTest {

    @InjectMocks
    private AnamneseController controller;

    @Mock
    private AnamneseService service;

    @Test
    void deveRetornarAnamneseDoPaciente() {
        Paciente paciente = new Paciente();
        paciente.setId(8L);
        Anamnese anamnese = new Anamnese();
        anamnese.setId(15L);
        anamnese.setPaciente(paciente);
        anamnese.setAlergiaResposta("Sim");
        anamnese.setAlergiaNotas("Penicilina");

        when(service.buscarExistentePorPacienteId(8L)).thenReturn(anamnese);

        ResponseEntity<AnamneseDTO> response = controller.buscarPorPaciente(8L);

        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals(15L, response.getBody().getId());
        assertEquals(8L, response.getBody().getPacienteId());
        assertEquals("Sim", response.getBody().getAlergiaResposta());
        assertEquals("Penicilina", response.getBody().getAlergiaNotas());
        verify(service).buscarExistentePorPacienteId(8L);
    }
}
