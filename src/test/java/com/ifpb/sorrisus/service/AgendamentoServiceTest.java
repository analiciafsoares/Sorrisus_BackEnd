package com.ifpb.sorrisus.service;

import com.ifpb.sorrisus.exception.BusinessException;
import com.ifpb.sorrisus.exception.ResourceNotFoundException;
import com.ifpb.sorrisus.model.*;
import com.ifpb.sorrisus.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgendamentoServiceTest {

    @InjectMocks
    private AgendamentoService service;

    @Mock private AgendamentoRepository agendamentoRepository;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private DentistaRepository dentistaRepository;
    @Mock private ConsultaRepository consultaRepository;
    @Mock private ServicoRepository servicoRepository;

    private Agendamento agendamento;
    private Dentista dentista;
    private Paciente paciente;

    @BeforeEach
    void setUp() {
        dentista = new Dentista(); dentista.setId(1L);
        paciente = new Paciente(); paciente.setId(1L);
        
        agendamento = new Agendamento();
        agendamento.setId(1L);
        agendamento.setDataHora(LocalDateTime.now().plusDays(1));
        agendamento.setDentista(dentista);
        agendamento.setPaciente(paciente);
        agendamento.setConfirmado(false);
    }

    @Test
    @DisplayName("Deve criar agendamento com sucesso")
    void deveCriarAgendamentoSucesso() {
        when(dentistaRepository.findById(1L)).thenReturn(Optional.of(dentista));
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(agendamentoRepository.existeAgendamentoAtivo(any(), any())).thenReturn(false);
        when(consultaRepository.existsByDentistaAndDataHoraAndStatusNot(any(), any(), any())).thenReturn(false);
        when(agendamentoRepository.save(any())).thenReturn(agendamento);

        Agendamento salvo = service.criar(agendamento);

        assertNotNull(salvo);
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
        when(agendamentoRepository.existeAgendamentoAtivo(any(), any())).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.criar(agendamento));
    }

    @Test
    @DisplayName("Não deve criar agendamento com serviço inativo")
    void naoDeveCriarComServicoInativo() {
        Servico servico = new Servico();
        servico.setId(5L);
        servico.setAtivo(false);
        agendamento.setServicoId(5L);

        when(dentistaRepository.findById(1L)).thenReturn(Optional.of(dentista));
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(servicoRepository.findById(5L)).thenReturn(Optional.of(servico));

        BusinessException erro = assertThrows(BusinessException.class, () -> service.criar(agendamento));

        assertTrue(erro.getMessage().contains("inativo"));
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve criar agendamento com serviço ativo")
    void deveCriarComServicoAtivo() {
        Servico servico = new Servico();
        servico.setId(5L);
        servico.setAtivo(true);
        agendamento.setServicoId(5L);

        when(dentistaRepository.findById(1L)).thenReturn(Optional.of(dentista));
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(servicoRepository.findById(5L)).thenReturn(Optional.of(servico));
        when(agendamentoRepository.existeAgendamentoAtivo(any(), any())).thenReturn(false);
        when(consultaRepository.existsByDentistaAndDataHoraAndStatusNot(any(), any(), any())).thenReturn(false);
        when(agendamentoRepository.save(any())).thenReturn(agendamento);

        Agendamento resultado = service.criar(agendamento);

        assertNotNull(resultado);
        verify(agendamentoRepository).save(agendamento);
    }

    @Test
    @DisplayName("Deve atualizar agendamento com sucesso")
    void deveAtualizarAgendamento() {
        Agendamento atualizado = new Agendamento();
        atualizado.setDentista(dentista);
        atualizado.setPaciente(paciente);
        atualizado.setDataHora(LocalDateTime.now().plusDays(3)); // Nova data

        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        when(dentistaRepository.findById(1L)).thenReturn(Optional.of(dentista));
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(agendamentoRepository.existeOutroAgendamentoAtivo(any(), any(), eq(1L))).thenReturn(false);
        when(agendamentoRepository.save(any())).thenReturn(atualizado);

        Agendamento res = service.atualizar(1L, atualizado);
        
        assertEquals(atualizado.getDataHora(), res.getDataHora());
        verify(agendamentoRepository).save(any());
    }

    @Test
    @DisplayName("Deve preservar serviço histórico inativo ao editar agendamento")
    void devePreservarServicoHistoricoInativo() {
        agendamento.setServicoId(5L);

        Agendamento atualizado = new Agendamento();
        atualizado.setDentista(dentista);
        atualizado.setPaciente(paciente);
        atualizado.setDataHora(agendamento.getDataHora());
        atualizado.setServicoId(5L);

        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        when(dentistaRepository.findById(1L)).thenReturn(Optional.of(dentista));
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(agendamentoRepository.save(any())).thenReturn(agendamento);

        service.atualizar(1L, atualizado);

        verify(servicoRepository, never()).findById(anyLong());
        verify(agendamentoRepository).save(agendamento);
    }

    @Test
    @DisplayName("Não deve trocar agendamento para outro serviço inativo")
    void naoDeveTrocarParaOutroServicoInativo() {
        agendamento.setServicoId(5L);

        Agendamento atualizado = new Agendamento();
        atualizado.setServicoId(6L);

        Servico servicoInativo = new Servico();
        servicoInativo.setId(6L);
        servicoInativo.setAtivo(false);

        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        when(servicoRepository.findById(6L)).thenReturn(Optional.of(servicoInativo));

        assertThrows(BusinessException.class, () -> service.atualizar(1L, atualizado));

        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve permitir atualizar agendamento já confirmado")
    void naoDeveAtualizarConfirmado() {
        agendamento.setConfirmado(true);
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
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
        when(agendamentoRepository.existeOutroAgendamentoAtivo(any(), any(), eq(1L))).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.atualizar(1L, novo));
    }

    @Test
    @DisplayName("Deve confirmar agendamento e criar consulta")
    void deveConfirmarAgendamento() {
        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        when(consultaRepository.existsByDentistaAndDataHoraAndStatusNot(any(), any(), any())).thenReturn(false);
        when(agendamentoRepository.save(any())).thenReturn(agendamento);

        service.confirmar(1L);

        assertTrue(agendamento.isConfirmado());
        assertEquals(StatusAgendamento.CONFIRMADO, agendamento.getStatus());
        ArgumentCaptor<Consulta> consultaCaptor = ArgumentCaptor.forClass(Consulta.class);
        verify(consultaRepository).save(consultaCaptor.capture());
        assertEquals(StatusConsulta.CONFIRMADA, consultaCaptor.getValue().getStatus());
        assertEquals(agendamento, consultaCaptor.getValue().getAgendamento());
    }

    @Test
    @DisplayName("Não deve confirmar agendamento cancelado")
    void naoDeveConfirmarCancelado() {
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));

        assertThrows(BusinessException.class, () -> service.confirmar(1L));
        verify(consultaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve cancelar agendamento pendente sem excluí-lo")
    void deveCancelarAgendamentoPendente() {
        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        when(agendamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Agendamento cancelado = service.cancelar(1L);

        assertEquals(StatusAgendamento.CANCELADO, cancelado.getStatus());
        verify(agendamentoRepository, never()).delete(any());
        verify(consultaRepository, never()).findByAgendamento(any());
    }

    @Test
    @DisplayName("Deve cancelar também a consulta ao cancelar agendamento confirmado")
    void deveCancelarConsultaAoCancelarAgendamentoConfirmado() {
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        Consulta consulta = new Consulta();
        consulta.setStatus(StatusConsulta.CONFIRMADA);
        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        when(consultaRepository.findByAgendamento(agendamento)).thenReturn(Optional.of(consulta));
        when(agendamentoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.cancelar(1L);

        assertEquals(StatusAgendamento.CANCELADO, agendamento.getStatus());
        assertEquals(StatusConsulta.CANCELADA, consulta.getStatus());
        verify(consultaRepository).save(consulta);
    }

    @Test
    @DisplayName("Não deve cancelar agendamento cujo atendimento já foi realizado")
    void naoDeveCancelarComAtendimentoRealizado() {
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        Consulta consulta = new Consulta();
        consulta.setStatus(StatusConsulta.REALIZADA);
        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        when(consultaRepository.findByAgendamento(agendamento)).thenReturn(Optional.of(consulta));

        assertThrows(BusinessException.class, () -> service.cancelar(1L));
        assertEquals(StatusAgendamento.CONFIRMADO, agendamento.getStatus());
        verify(agendamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve cancelar agendamento já cancelado")
    void naoDeveCancelarDuasVezes() {
        agendamento.setStatus(StatusAgendamento.CANCELADO);
        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));

        assertThrows(BusinessException.class, () -> service.cancelar(1L));
    }

    @Test
    @DisplayName("Deve impedir confirmação se já existe consulta no horário (Conflito tardio)")
    void deveFalharConfirmacaoSeConsultaExiste() {
        when(agendamentoRepository.findById(1L)).thenReturn(Optional.of(agendamento));
        when(consultaRepository.existsByDentistaAndDataHoraAndStatusNot(any(), any(), any())).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.confirmar(1L));
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
