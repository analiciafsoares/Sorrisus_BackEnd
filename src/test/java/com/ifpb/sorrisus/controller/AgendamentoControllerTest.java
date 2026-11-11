package com.ifpb.sorrisus.controller;

import com.ifpb.sorrisus.dto.AgendamentoDTO;
import com.ifpb.sorrisus.exception.BusinessException;
import com.ifpb.sorrisus.exception.WhatsAppSendException;
import com.ifpb.sorrisus.model.Agendamento;
import com.ifpb.sorrisus.model.Dentista;
import com.ifpb.sorrisus.model.Paciente;
import com.ifpb.sorrisus.model.StatusAgendamento;
import com.ifpb.sorrisus.repository.DentistaRepository;
import com.ifpb.sorrisus.repository.PacienteRepository;
import com.ifpb.sorrisus.service.AgendamentoService;
import com.ifpb.sorrisus.service.WhatsAppService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgendamentoControllerTest {

    @InjectMocks
    private AgendamentoController controller;

    @Mock private AgendamentoService service;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private DentistaRepository dentistaRepository;
    @Mock private WhatsAppService whatsAppService;

    private AgendamentoDTO dto;
    private Paciente paciente;
    private Dentista dentista;
    private Agendamento salvo;

    @BeforeEach
    void setUp() {
        LocalDateTime dataHora = LocalDateTime.of(2026, 11, 20, 14, 30);

        dto = new AgendamentoDTO();
        dto.setDataHora(dataHora);
        dto.setPacienteId(7L);
        dto.setDentistaId(8L);
        dto.setServicoId(9L);
        dto.setObservacao("Retorno");

        paciente = new Paciente();
        paciente.setId(7L);
        paciente.setNome("Maria");
        paciente.setTelefone("5583999999999");

        dentista = new Dentista();
        dentista.setId(8L);

        salvo = new Agendamento();
        salvo.setId(50L);
        salvo.setDataHora(dataHora);
        salvo.setPaciente(paciente);
        salvo.setDentista(dentista);
        salvo.setServicoId(9L);
        salvo.setObservacao("Retorno");
        salvo.setStatus(StatusAgendamento.PENDENTE);
        salvo.setConfirmado(false);

        when(pacienteRepository.findById(7L)).thenReturn(Optional.of(paciente));
        when(dentistaRepository.findById(8L)).thenReturn(Optional.of(dentista));
    }

    @Test
    @DisplayName("Deve criar agendamento e retornar 201 com DTO e Location")
    void deveCriarAgendamentoComSucesso() {
        configurarCriacaoComSucesso();

        ResponseEntity<AgendamentoDTO> response = controller.criar(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(URI.create("/api/agendamentos/50"), response.getHeaders().getLocation());
        assertNotNull(response.getBody());
        assertEquals(50L, response.getBody().getId());
        assertEquals("PENDENTE", response.getBody().getStatus());
        verify(service, times(1)).criar(any(Agendamento.class));
        verify(whatsAppService).sendMessage(eq("5583999999999"), contains("20/11/2026 às 14:30"));
    }

    @Test
    @DisplayName("Deve retornar 201 quando a notificação do WhatsApp falhar")
    void deveManterSucessoQuandoWhatsAppFalhar() {
        configurarCriacaoComSucesso();
        doThrow(new WhatsAppSendException(
                "Z-API indisponível",
                WhatsAppSendException.TipoFalha.TRANSITORIA,
                503))
                .when(whatsAppService).sendMessage(any(), any());

        ResponseEntity<AgendamentoDTO> response = controller.criar(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(50L, response.getBody().getId());
        verify(service, times(1)).criar(any(Agendamento.class));
    }

    @Test
    @DisplayName("Deve retornar 201 quando ocorrer falha inesperada na notificação")
    void deveManterSucessoQuandoNotificacaoFalharInesperadamente() {
        configurarCriacaoComSucesso();
        doThrow(new IllegalStateException("falha inesperada"))
                .when(whatsAppService).sendMessage(any(), any());

        ResponseEntity<AgendamentoDTO> response = controller.criar(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(50L, response.getBody().getId());
        verify(service, times(1)).criar(any(Agendamento.class));
    }

    @Test
    @DisplayName("Não deve tentar notificar paciente sem telefone")
    void naoDeveNotificarSemTelefone() {
        configurarCriacaoComSucesso();
        paciente.setTelefone(" ");

        ResponseEntity<AgendamentoDTO> response = controller.criar(dto);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        verify(whatsAppService, never()).sendMessage(any(), any());
    }

    @Test
    @DisplayName("Deve propagar erro real da criação sem enviar notificação")
    void devePropagarErroRealDaCriacao() {
        when(service.criar(any(Agendamento.class)))
                .thenThrow(new BusinessException("Horário indisponível."));

        BusinessException error = assertThrows(BusinessException.class, () -> controller.criar(dto));

        assertEquals("Horário indisponível.", error.getMessage());
        verify(service, times(1)).criar(any(Agendamento.class));
        verifyNoInteractions(whatsAppService);
    }

    private void configurarCriacaoComSucesso() {
        when(service.criar(any(Agendamento.class))).thenReturn(salvo);
    }
}
