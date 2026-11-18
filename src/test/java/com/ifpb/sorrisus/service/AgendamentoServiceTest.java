package com.ifpb.sorrisus.service;

import com.ifpb.sorrisus.exception.BusinessException;
import com.ifpb.sorrisus.exception.ResourceNotFoundException;
import com.ifpb.sorrisus.model.*;
import com.ifpb.sorrisus.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    @InjectMocks
    private AgendamentoService service;

    @Mock
    private AgendamentoRepository agendamentoRepository;
    @Mock
    private PacienteRepository pacienteRepository;
    @Mock
    private DentistaRepository dentistaRepository;
    @Mock
    private ConsultaRepository consultaRepository;

    private Agendamento agendamento;
    private Dentista dentista;
    private Paciente paciente;

    @BeforeEach
    void setUp() {
        dentista = new Dentista();
        dentista.setId(1L);
        paciente = new Paciente();
        paciente.setId(1L);

        agendamento = new Agendamento();
        agendamento.setId(1L);
        agendamento.setDataHora(LocalDateTime.now().plusDays(1));
        agendamento.setDentista(dentista);
        agendamento.setPaciente(paciente);
        agendamento.setConfirmado(false);
        agendamento.setStatus(StatusConsulta.PENDENTE);
    }

    @Test
    @DisplayName("Deve criar agendamento com sucesso")
    void deveCriarAgendamentoSucesso() {
        when(dentistaRepository.findById(1L)).thenReturn(Optional.of(dentista));
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(agendamentoRepository.existsByDentistaAndDataHoraAndStatusNot(any(), any(), eq(StatusConsulta.CANCELADA)))
                .thenReturn(false);
        when(consultaRepository.existsByDentistaAndDataHoraAndStatusNot(any(), any(), eq(StatusConsulta.CANCELADA)))
                .thenReturn(false);
        when(agendamentoRepository.save(any())).thenReturn(agendamento);

        Agendamento salvo = service.criar(agendamento);

        assertNotNull(salvo);
        assertEquals(StatusConsulta.PENDENTE, salvo.getStatus());
        verify(agendamentoRepository).save(any(Agendamento.class));
    }

    @Test
    @DisplayName("Deve lançar erro ao criar com Dentista inexistente")
    void deveFalharDentistaNaoEncontrado() {
        when(dentistaRepository.findById(1L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.criar(agendamento));
    }

    @Test
    @DisplayName("Deve lançar erro de conflito de horário na criação")
    void deveLancarErroConflitoCriacao() {
        when(dentistaRepository.findById(1L)).thenReturn(Optional.of(dentista));
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(agendamentoRepository.existsByDentistaAndDataHoraAndStatusNot(any(), any(), eq(StatusConsulta.CANCELADA)))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> service.criar(agendamento));
    }

    @Test
    @DisplayName("Deve atualizar agendamento com sucesso")
    void deveAtualizarAgendamento() {
        Agendamento atualizado = new Agendamento();
        atualizado.setDentista(dentista);
        atualizado.setPaciente(paciente);
        atualizado.setDataHora(LocalDateTime.now().plusDays(3));

        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        when(dentistaRepository.findById(1L)).thenReturn(Optional.of(dentista));
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(agendamentoRepository.existsByDentistaAndDataHoraAndIdNotAndStatusNot(any(), any(), eq(1L),
                eq(StatusConsulta.CANCELADA))).thenReturn(false);
        when(consultaRepository.existsByDentistaAndDataHoraAndStatusNot(any(), any(), eq(StatusConsulta.CANCELADA)))
                .thenReturn(false);
        when(agendamentoRepository.save(any())).thenReturn(atualizado);

        Agendamento res = service.atualizar(1L, atualizado);

        assertEquals(atualizado.getDataHora(), res.getDataHora());
        verify(agendamentoRepository).save(any());
    }

    @Test
    @DisplayName("Não deve permitir atualizar agendamento já confirmado")
    void naoDeveAtualizarConfirmado() {
        agendamento.setConfirmado(true);
        agendamento.setStatus(StatusConsulta.CONFIRMADA);
        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));

        assertThrows(BusinessException.class, () -> service.atualizar(1L, agendamento));
    }

    @Test
    @DisplayName("Deve lançar erro de conflito ao atualizar para horário ocupado")
    void deveFalharAtualizacaoComConflito() {
        Agendamento novo = new Agendamento();
        novo.setDentista(dentista);
        novo.setPaciente(paciente);
        novo.setDataHora(LocalDateTime.now().plusDays(3));

        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        when(dentistaRepository.findById(1L)).thenReturn(Optional.of(dentista));
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(agendamentoRepository.existsByDentistaAndDataHoraAndIdNotAndStatusNot(any(), any(), eq(1L),
                eq(StatusConsulta.CANCELADA))).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.atualizar(1L, novo));
    }

    @Test
    @DisplayName("Deve confirmar agendamento e criar consulta")
    void deveConfirmarAgendamento() {
        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        when(consultaRepository.existsByDentistaAndDataHoraAndStatusNot(any(), any(), eq(StatusConsulta.CANCELADA)))
                .thenReturn(false);
        when(agendamentoRepository.save(any())).thenReturn(agendamento);

        service.confirmar(1L);

        assertTrue(agendamento.isConfirmado());
        assertEquals(StatusConsulta.CONFIRMADA, agendamento.getStatus());
        verify(consultaRepository).save(any(Consulta.class));
    }

    @Test
    @DisplayName("Deve impedir confirmação se já existe consulta no horário (Conflito tardio)")
    void deveFalharConfirmacaoSeConsultaExiste() {
        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        when(consultaRepository.existsByDentistaAndDataHoraAndStatusNot(any(), any(), eq(StatusConsulta.CANCELADA)))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> service.confirmar(1L));
    }

    @Test
    @DisplayName("Deve cancelar agendamento mantendo registro como CANCELADA")
    void deveCancelarAgendamento() {
        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        when(consultaRepository.findByDentistaAndDataHoraBetween(any(), any(), any())).thenReturn(List.of());
        when(agendamentoRepository.save(any())).thenReturn(agendamento);

        Agendamento cancelado = service.cancelar(1L);

        assertFalse(cancelado.isConfirmado());
        assertEquals(StatusConsulta.CANCELADA, cancelado.getStatus());
        verify(agendamentoRepository).save(agendamento);
    }

    @Test
    @DisplayName("Deve deletar agendamento")
    void deveDeletar() {
        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        service.deletar(1L);
        verify(agendamentoRepository).delete(agendamento);
    }

    @Test
    @DisplayName("Deve listar por paciente")
    void deveListarPorPaciente() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(agendamentoRepository.findByPaciente(paciente)).thenReturn(List.of(agendamento));

        List<Agendamento> lista = service.listarPorPaciente(1L);
        assertFalse(lista.isEmpty());
    }
}