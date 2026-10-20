package com.ifpb.sorrisus.service;

import com.ifpb.sorrisus.exception.WhatsAppSendException;
import com.ifpb.sorrisus.model.Consulta;
import com.ifpb.sorrisus.model.LembreteRetorno;
import com.ifpb.sorrisus.model.Paciente;
import com.ifpb.sorrisus.model.RetornoPendente;
import com.ifpb.sorrisus.model.StatusLembreteRetorno;
import com.ifpb.sorrisus.repository.LembreteRetornoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LembreteRetornoServiceTest {

    @Mock private ConsultaService consultaService;
    @Mock private WhatsAppService whatsAppService;
    @Mock private LembreteRetornoRepository lembreteRetornoRepository;

    private LembreteRetornoService service;
    private Paciente paciente;
    private Consulta ultimaConsulta;
    private RetornoPendente retorno;

    @BeforeEach
    void setUp() {
        service = new LembreteRetornoService(consultaService, whatsAppService, lembreteRetornoRepository);

        paciente = new Paciente();
        paciente.setId(1L);
        paciente.setNome("Maria");
        paciente.setTelefone("(83) 99999-9999");

        ultimaConsulta = new Consulta();
        ultimaConsulta.setId(10L);
        ultimaConsulta.setPaciente(paciente);

        retorno = new RetornoPendente();
        retorno.setPaciente(paciente);
        retorno.setUltimaConsulta(ultimaConsulta);
    }

    @Test
    @DisplayName("Deve enviar lembrete, normalizar telefone e registrar sucesso")
    void deveEnviarERegistrarSucesso() {
        List<StatusLembreteRetorno> statusSalvos = prepararProcessamento(List.of(retorno));
        when(lembreteRetornoRepository.findByPacienteIdAndUltimaConsultaId(1L, 10L))
                .thenReturn(Optional.empty());

        service.processarLembretesPendentes();

        verify(whatsAppService).sendMessage(
                eq("5583999999999"),
                org.mockito.ArgumentMatchers.contains("Olá, Maria")
        );
        assertEquals(List.of(StatusLembreteRetorno.PROCESSANDO, StatusLembreteRetorno.ENVIADO), statusSalvos);
    }

    @Test
    @DisplayName("Deve ignorar telefone inválido sem consumir o controle de duplicidade")
    void deveIgnorarTelefoneInvalido() {
        paciente.setTelefone("123");
        when(whatsAppService.isConfigured()).thenReturn(true);
        when(consultaService.verificarRetornosPendentes()).thenReturn(List.of(retorno));

        service.processarLembretesPendentes();

        verify(whatsAppService, never()).sendMessage(any(), any());
        verify(lembreteRetornoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Deve ignorar lembrete já enviado")
    void deveIgnorarLembreteJaEnviado() {
        when(whatsAppService.isConfigured()).thenReturn(true);
        when(consultaService.verificarRetornosPendentes()).thenReturn(List.of(retorno));
        LembreteRetorno existente = criarRegistro(StatusLembreteRetorno.ENVIADO);
        when(lembreteRetornoRepository.findByPacienteIdAndUltimaConsultaId(1L, 10L))
                .thenReturn(Optional.of(existente));

        service.processarLembretesPendentes();

        verify(whatsAppService, never()).sendMessage(any(), any());
    }

    @Test
    @DisplayName("Não deve repetir falhas permanentes ou ambíguas")
    void naoDeveRepetirFalhasPermanentesOuAmbiguas() {
        when(whatsAppService.isConfigured()).thenReturn(true);
        when(consultaService.verificarRetornosPendentes()).thenReturn(List.of(retorno));
        when(lembreteRetornoRepository.findByPacienteIdAndUltimaConsultaId(1L, 10L))
                .thenReturn(
                        Optional.of(criarRegistro(StatusLembreteRetorno.FALHA_PERMANENTE)),
                        Optional.of(criarRegistro(StatusLembreteRetorno.FALHA_AMBIGUA))
                );

        service.processarLembretesPendentes();
        service.processarLembretesPendentes();

        verify(whatsAppService, never()).sendMessage(any(), any());
        verify(lembreteRetornoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Deve permitir nova tentativa somente após falha transitória")
    void deveRepetirFalhaTransitoria() {
        List<StatusLembreteRetorno> statusSalvos = prepararProcessamento(List.of(retorno));
        LembreteRetorno existente = criarRegistro(StatusLembreteRetorno.FALHA_TRANSITORIA);
        existente.setTentativas(1);
        when(lembreteRetornoRepository.findByPacienteIdAndUltimaConsultaId(1L, 10L))
                .thenReturn(Optional.of(existente));

        service.processarLembretesPendentes();

        verify(whatsAppService).sendMessage(any(), any());
        assertEquals(2, existente.getTentativas());
        assertEquals(List.of(StatusLembreteRetorno.PROCESSANDO, StatusLembreteRetorno.ENVIADO), statusSalvos);
    }

    @Test
    @DisplayName("Deve registrar falha HTTP transitória como repetível")
    void deveRegistrarFalhaTransitoria() {
        List<StatusLembreteRetorno> statusSalvos = prepararProcessamento(List.of(retorno));
        when(lembreteRetornoRepository.findByPacienteIdAndUltimaConsultaId(1L, 10L))
                .thenReturn(Optional.empty());
        doThrow(new WhatsAppSendException(
                "indisponível",
                WhatsAppSendException.TipoFalha.TRANSITORIA,
                503
        )).when(whatsAppService).sendMessage(any(), any());

        service.processarLembretesPendentes();

        assertEquals(
                List.of(StatusLembreteRetorno.PROCESSANDO, StatusLembreteRetorno.FALHA_TRANSITORIA),
                statusSalvos
        );
    }

    @Test
    @DisplayName("Deve registrar timeout como ambíguo e não repetível")
    void deveRegistrarFalhaAmbigua() {
        List<StatusLembreteRetorno> statusSalvos = prepararProcessamento(List.of(retorno));
        when(lembreteRetornoRepository.findByPacienteIdAndUltimaConsultaId(1L, 10L))
                .thenReturn(Optional.empty());
        doThrow(new WhatsAppSendException(
                "timeout",
                new java.io.IOException("timeout"),
                WhatsAppSendException.TipoFalha.AMBIGUA
        )).when(whatsAppService).sendMessage(any(), any());

        service.processarLembretesPendentes();

        assertEquals(
                List.of(StatusLembreteRetorno.PROCESSANDO, StatusLembreteRetorno.FALHA_AMBIGUA),
                statusSalvos
        );
    }

    @Test
    @DisplayName("Deve continuar processando os demais pacientes após uma falha")
    void deveContinuarAposFalhaIndividual() {
        Paciente segundoPaciente = new Paciente();
        segundoPaciente.setId(2L);
        segundoPaciente.setNome("José");
        segundoPaciente.setTelefone("83988887777");
        Consulta segundaConsulta = new Consulta();
        segundaConsulta.setId(20L);
        segundaConsulta.setPaciente(segundoPaciente);
        RetornoPendente segundoRetorno = new RetornoPendente();
        segundoRetorno.setPaciente(segundoPaciente);
        segundoRetorno.setUltimaConsulta(segundaConsulta);

        prepararProcessamento(List.of(retorno, segundoRetorno));
        when(lembreteRetornoRepository.findByPacienteIdAndUltimaConsultaId(any(), any()))
                .thenReturn(Optional.empty());
        doThrow(new WhatsAppSendException(
                "requisição inválida",
                WhatsAppSendException.TipoFalha.PERMANENTE,
                400
        )).doNothing().when(whatsAppService).sendMessage(any(), any());

        service.processarLembretesPendentes();

        verify(whatsAppService).sendMessage("5583999999999", "Olá, Maria.\n\n"
                + "Já faz algum tempo desde sua última consulta odontológica. "
                + "Manter o acompanhamento periódico é importante para a prevenção e o cuidado com a saúde bucal.\n\n"
                + "Entre em contato com a clínica para verificar a possibilidade de agendar seu retorno.\n\n"
                + "Atenciosamente,\nEquipe Sorrisus");
        verify(whatsAppService).sendMessage(eq("5583988887777"), any());
    }

    @Test
    @DisplayName("Não deve consultar retornos quando a Z-API não está configurada")
    void naoDeveProcessarSemConfiguracao() {
        when(whatsAppService.isConfigured()).thenReturn(false);

        service.processarLembretesPendentes();

        verify(consultaService, never()).verificarRetornosPendentes();
        verify(lembreteRetornoRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Não deve enviar nem persistir quando não há retornos pendentes")
    void naoDeveProcessarListaVazia() {
        when(whatsAppService.isConfigured()).thenReturn(true);
        when(consultaService.verificarRetornosPendentes()).thenReturn(List.of());

        service.processarLembretesPendentes();

        verify(whatsAppService, never()).sendMessage(any(), any());
        verify(lembreteRetornoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveNormalizarSomenteTelefonesBrasileirosValidos() {
        assertEquals(Optional.of("5583999999999"), service.normalizarTelefone("(83) 99999-9999"));
        assertEquals(Optional.of("5583999999999"), service.normalizarTelefone("5583999999999"));
        assertTrue(service.normalizarTelefone(null).isEmpty());
        assertTrue(service.normalizarTelefone("123").isEmpty());
        assertTrue(service.normalizarTelefone("4483999999999").isEmpty());
    }

    private List<StatusLembreteRetorno> prepararProcessamento(List<RetornoPendente> retornos) {
        when(whatsAppService.isConfigured()).thenReturn(true);
        when(consultaService.verificarRetornosPendentes()).thenReturn(retornos);

        List<StatusLembreteRetorno> statusSalvos = new ArrayList<>();
        doAnswer(invocation -> {
            LembreteRetorno registro = invocation.getArgument(0);
            statusSalvos.add(registro.getStatus());
            return registro;
        }).when(lembreteRetornoRepository).saveAndFlush(any(LembreteRetorno.class));
        return statusSalvos;
    }

    private LembreteRetorno criarRegistro(StatusLembreteRetorno status) {
        LembreteRetorno registro = new LembreteRetorno();
        registro.setPaciente(paciente);
        registro.setUltimaConsulta(ultimaConsulta);
        registro.setStatus(status);
        return registro;
    }
}
