package com.ifpb.sorrisus.service;

import com.ifpb.sorrisus.controller.ConsultaController;
import com.ifpb.sorrisus.dto.ConsultaHistoricoRecepcaoDTO;
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
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConsultaServiceTest {

    @InjectMocks
    private ConsultaService service;

    @Mock private ConsultaRepository consultaRepository;
    @Mock private PacienteRepository pacienteRepository;
    @Mock private DentistaRepository dentistaRepository;
    @Mock private ProntuarioRepository prontuarioRepository;
    @Mock private AgendamentoRepository agendamentoRepository;

    private Consulta consulta;
    private Dentista dentista;
    private Paciente paciente;

    @BeforeEach
    void setUp() {
        dentista = new Dentista(); dentista.setId(1L);
        paciente = new Paciente(); paciente.setId(1L);
        
        consulta = new Consulta();
        consulta.setId(10L);
        consulta.setDataHora(LocalDateTime.now().plusDays(2));
        consulta.setDentista(dentista);
        consulta.setPaciente(paciente);
        consulta.setStatus(StatusConsulta.CONFIRMADA);
    }

    @Test
    @DisplayName("Deve criar consulta manual com sucesso e salvar prontuário se vier junto")
    void deveCriarConsultaComProntuario() {
        consulta.setProntuario(new Prontuario()); 

        when(dentistaRepository.findById(1L)).thenReturn(Optional.of(dentista));
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(consultaRepository.existsByDentistaAndDataHora(any(), any())).thenReturn(false);
        when(agendamentoRepository.existsByDentistaAndDataHora(any(), any())).thenReturn(false);
        when(prontuarioRepository.save(any())).thenReturn(new Prontuario()); 
        when(consultaRepository.save(any())).thenReturn(consulta);

        Consulta salva = service.criar(consulta);
        assertNotNull(salva);
        verify(prontuarioRepository).save(any()); 
    }

    @Test
    @DisplayName("Deve falhar ao criar se paciente não existe")
    void deveFalharPacienteInexistente() {
        when(dentistaRepository.findById(1L)).thenReturn(Optional.of(dentista));
        when(pacienteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.criar(consulta));
    }

    @Test
    @DisplayName("Deve atualizar data da consulta (sem conflito)")
    void deveAtualizarDataConsulta() {
        Consulta atualizacao = new Consulta();
        atualizacao.setDataHora(LocalDateTime.now().plusDays(5)); 

        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(consultaRepository.existsByDentistaAndDataHoraAndIdNot(any(), any(), eq(10L))).thenReturn(false);
        when(consultaRepository.save(any())).thenReturn(consulta);

        service.atualizar(10L, atualizacao);

        assertEquals(atualizacao.getDataHora(), consulta.getDataHora());
        verify(consultaRepository).save(consulta);
    }

    @Test
    @DisplayName("Deve falhar ao mudar data para horário ocupado")
    void deveFalharAtualizacaoDataOcupada() {
        Consulta atualizacao = new Consulta();
        atualizacao.setDataHora(LocalDateTime.now().plusDays(5));

        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(consultaRepository.existsByDentistaAndDataHoraAndIdNot(any(), any(), eq(10L))).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.atualizar(10L, atualizacao));
    }

    @Test
    @DisplayName("Não deve permitir editar consulta cancelada")
    void naoDeveEditarCancelada() {
        consulta.setStatus(StatusConsulta.CANCELADA);
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));

        assertThrows(BusinessException.class, () -> service.atualizar(10L, new Consulta()));
    }

    @Test
    @DisplayName("Deve cancelar consulta")
    void deveCancelarConsulta() {
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        service.cancelar(10L);
        assertEquals(StatusConsulta.CANCELADA, consulta.getStatus());
    }
    
    @Test
    @DisplayName("Deve lançar erro ao buscar consulta inexistente")
    void deveFalharBuscaInexistente() {
        when(consultaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.buscarPorId(99L));
    }

    @Test
    @DisplayName("Deve listar histórico da recepção por data e id decrescentes")
    void deveListarHistoricoRecepcaoOrdenado() {
        when(consultaRepository.findAll(any(Sort.class))).thenReturn(List.of(consulta));

        List<Consulta> resultado = service.listarParaRecepcao();

        assertEquals(List.of(consulta), resultado);

        ArgumentCaptor<Sort> sortCaptor = ArgumentCaptor.forClass(Sort.class);
        verify(consultaRepository).findAll(sortCaptor.capture());
        Sort ordenacao = sortCaptor.getValue();
        assertEquals(Sort.Direction.DESC, ordenacao.getOrderFor("dataHora").getDirection());
        assertEquals(Sort.Direction.DESC, ordenacao.getOrderFor("id").getDirection());
    }

    @Test
    @DisplayName("Endpoint da recepção deve ser exclusivo e não expor prontuário")
    void endpointRecepcaoDeveSerRestritoESemProntuario() throws NoSuchMethodException {
        paciente.setNome("Paciente Teste");
        dentista.setNome("Dentista Teste");
        consulta.setObservacao("Observação permitida");
        consulta.setProntuario(new Prontuario());

        ConsultaService serviceMock = mock(ConsultaService.class);
        when(serviceMock.listarParaRecepcao()).thenReturn(List.of(consulta));
        ConsultaController controller = new ConsultaController(serviceMock);

        List<ConsultaHistoricoRecepcaoDTO> resposta = controller
                .listarHistoricoParaRecepcao()
                .getBody();

        assertNotNull(resposta);
        assertEquals(1, resposta.size());
        assertEquals("Paciente Teste", resposta.get(0).getNomePaciente());
        assertEquals("Dentista Teste", resposta.get(0).getNomeDentista());
        assertTrue(Arrays.stream(ConsultaHistoricoRecepcaoDTO.class.getDeclaredFields())
                .noneMatch(field -> field.getName().equals("prontuario")));

        Method endpoint = ConsultaController.class.getMethod("listarHistoricoParaRecepcao");
        PreAuthorize regra = endpoint.getAnnotation(PreAuthorize.class);
        assertNotNull(regra);
        assertEquals("hasAuthority('ROLE_RECEPCIONISTA')", regra.value());
    }

    @Test
    @DisplayName("Deve manter retorno pendente quando não há acompanhamento futuro ativo")
    void deveManterRetornoSemAcompanhamentoFuturo() {
        Consulta ultimaConsulta = criarUltimaConsultaRealizada();
        when(consultaRepository.findPacientesComUltimaConsultaAntesDe(any())).thenReturn(List.of(paciente));
        when(agendamentoRepository.existsByPacienteAndDataHoraGreaterThanEqualAndConfirmadoFalse(eq(paciente), any()))
                .thenReturn(false);
        when(consultaRepository.existsByPacienteAndDataHoraGreaterThanEqualAndStatusIn(eq(paciente), any(), any()))
                .thenReturn(false);
        when(consultaRepository.findUltimaConsultaRealizadaPorPaciente(paciente)).thenReturn(List.of(ultimaConsulta));

        List<RetornoPendente> retornos = service.verificarRetornosPendentes();

        assertEquals(1, retornos.size());
        assertEquals(ultimaConsulta, retornos.get(0).getUltimaConsulta());
    }

    @Test
    @DisplayName("Deve excluir retorno quando há agendamento futuro não confirmado")
    void deveExcluirRetornoComAgendamentoFuturoNaoConfirmado() {
        when(consultaRepository.findPacientesComUltimaConsultaAntesDe(any())).thenReturn(List.of(paciente));
        when(agendamentoRepository.existsByPacienteAndDataHoraGreaterThanEqualAndConfirmadoFalse(eq(paciente), any()))
                .thenReturn(true);

        List<RetornoPendente> retornos = service.verificarRetornosPendentes();

        assertTrue(retornos.isEmpty());
        verify(consultaRepository, never()).findUltimaConsultaRealizadaPorPaciente(any());
    }

    @Test
    @DisplayName("Deve excluir retorno somente para consultas futuras pendentes ou confirmadas")
    void deveConsiderarSomenteStatusAtivosNoAcompanhamentoFuturo() {
        when(consultaRepository.findPacientesComUltimaConsultaAntesDe(any())).thenReturn(List.of(paciente));
        when(agendamentoRepository.existsByPacienteAndDataHoraGreaterThanEqualAndConfirmadoFalse(eq(paciente), any()))
                .thenReturn(false);
        when(consultaRepository.existsByPacienteAndDataHoraGreaterThanEqualAndStatusIn(eq(paciente), any(), any()))
                .thenReturn(true);

        List<RetornoPendente> retornos = service.verificarRetornosPendentes();

        assertTrue(retornos.isEmpty());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Collection<StatusConsulta>> statusCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(consultaRepository).existsByPacienteAndDataHoraGreaterThanEqualAndStatusIn(
                eq(paciente),
                any(),
                statusCaptor.capture()
        );
        assertEquals(List.of(StatusConsulta.PENDENTE, StatusConsulta.CONFIRMADA), List.copyOf(statusCaptor.getValue()));
        assertFalse(statusCaptor.getValue().contains(StatusConsulta.CANCELADA));
        assertFalse(statusCaptor.getValue().contains(StatusConsulta.REALIZADA));
    }

    private Consulta criarUltimaConsultaRealizada() {
        Consulta ultimaConsulta = new Consulta();
        ultimaConsulta.setId(20L);
        ultimaConsulta.setPaciente(paciente);
        ultimaConsulta.setDentista(dentista);
        ultimaConsulta.setStatus(StatusConsulta.REALIZADA);
        ultimaConsulta.setDataHora(LocalDateTime.now().minusMonths(7));
        return ultimaConsulta;
    }
}
