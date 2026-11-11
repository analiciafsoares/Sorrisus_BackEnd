package com.ifpb.sorrisus.service;

import com.ifpb.sorrisus.dto.ProntuarioDTO;
import com.ifpb.sorrisus.exception.ResourceNotFoundException;
import com.ifpb.sorrisus.model.Anamnese;
import com.ifpb.sorrisus.model.Paciente;
import com.ifpb.sorrisus.repository.AnamneseRepository;
import com.ifpb.sorrisus.repository.PacienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnamneseServiceTest {

    @InjectMocks private AnamneseService service;
    @Mock private AnamneseRepository repository;
    @Mock private PacienteRepository pacienteRepository;

    @Test
    @DisplayName("Deve retornar nova anamnese se não encontrar no banco")
    void deveRetornarNovaSeVazio() {
        Paciente p = new Paciente();
        when(repository.findByPaciente(p)).thenReturn(Optional.empty());

        Anamnese a = service.buscarPorPaciente(p);
        assertNotNull(a);
        assertNull(a.getId()); 
    }

    @Test
    @DisplayName("Deve buscar anamnese existente pelo ID do paciente")
    void deveBuscarExistentePorPacienteId() {
        Paciente paciente = new Paciente();
        paciente.setId(7L);
        Anamnese anamnese = new Anamnese();
        anamnese.setId(12L);
        anamnese.setPaciente(paciente);

        when(pacienteRepository.findById(7L)).thenReturn(Optional.of(paciente));
        when(repository.findByPaciente(paciente)).thenReturn(Optional.of(anamnese));

        Anamnese resultado = service.buscarExistentePorPacienteId(7L);

        assertSame(anamnese, resultado);
    }

    @Test
    @DisplayName("Deve falhar ao buscar anamnese de paciente inexistente")
    void deveFalharParaPacienteInexistente() {
        when(pacienteRepository.findById(99L)).thenReturn(Optional.empty());

        ResourceNotFoundException erro = assertThrows(
                ResourceNotFoundException.class,
                () -> service.buscarExistentePorPacienteId(99L));

        assertEquals("Paciente não encontrado.", erro.getMessage());
        verify(repository, never()).findByPaciente(any());
    }

    @Test
    @DisplayName("Deve retornar 404 de domínio quando paciente ainda não possui anamnese")
    void deveFalharQuandoAnamneseNaoExiste() {
        Paciente paciente = new Paciente();
        paciente.setId(7L);
        when(pacienteRepository.findById(7L)).thenReturn(Optional.of(paciente));
        when(repository.findByPaciente(paciente)).thenReturn(Optional.empty());

        ResourceNotFoundException erro = assertThrows(
                ResourceNotFoundException.class,
                () -> service.buscarExistentePorPacienteId(7L));

        assertEquals("Anamnese não encontrada para este paciente.", erro.getMessage());
    }

    @Test
    @DisplayName("Deve salvar dados corretamente")
    void deveSalvarDados() {
        Paciente p = new Paciente();
        ProntuarioDTO dto = new ProntuarioDTO();
        dto.setAlergiaResposta("Sim");

        when(repository.findByPaciente(p)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Anamnese salva = service.salvarOuAtualizar(p, dto);
        
        assertEquals("Sim", salva.getAlergiaResposta());
        assertEquals(p, salva.getPaciente());
    }

    @Test
    @DisplayName("Deve atualizar a mesma anamnese existente")
    void deveAtualizarExistenteSemCriarOutra() {
        Paciente paciente = new Paciente();
        Anamnese existente = new Anamnese();
        existente.setId(33L);
        existente.setPaciente(paciente);
        existente.setAlergiaResposta("Não");

        ProntuarioDTO dto = new ProntuarioDTO();
        dto.setAlergiaResposta("Sim");

        when(repository.findByPaciente(paciente)).thenReturn(Optional.of(existente));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        Anamnese resultado = service.salvarOuAtualizar(paciente, dto);

        assertSame(existente, resultado);
        assertEquals(33L, resultado.getId());
        assertEquals("Sim", resultado.getAlergiaResposta());
        verify(repository).save(existente);
    }
}
