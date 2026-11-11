package com.ifpb.sorrisus.service;

import com.ifpb.sorrisus.dto.ProntuarioDTO;
import com.ifpb.sorrisus.exception.ResourceNotFoundException;
import com.ifpb.sorrisus.model.*;
import com.ifpb.sorrisus.repository.ConsultaRepository;
import com.ifpb.sorrisus.repository.ProntuarioRepository;
import org.junit.jupiter.api.BeforeEach;
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
class ProntuarioServiceTest {

    @InjectMocks
    private ProntuarioService service;

    @Mock private ProntuarioRepository prontuarioRepository;
    @Mock private ConsultaRepository consultaRepository;
    @Mock private AnamneseService anamneseService;

    private Consulta consulta;
    private Paciente paciente;
    private ProntuarioDTO dto;
    private Anamnese anamneseMock;

    @BeforeEach
    void setUp() {
        paciente = new Paciente();
        paciente.setId(1L);

        consulta = new Consulta();
        consulta.setId(10L);
        consulta.setPaciente(paciente);

        dto = new ProntuarioDTO();
        dto.setObservacoes("Paciente com dor");
        dto.setOdontogramaJson("{dente:16}");
        dto.setAlergiaResposta("Não");

        anamneseMock = new Anamnese();
        anamneseMock.setAlergiaResposta("Não");
    }

    @Test
    @DisplayName("Deve falhar se ID da consulta for nulo")
    void deveFalharSemConsultaId() {
        assertThrows(IllegalArgumentException.class, () -> service.salvarFichaClinica(dto, null));
    }

    @Test
    @DisplayName("Deve falhar se consulta não for encontrada")
    void deveFalharConsultaInexistente() {
        when(consultaRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.salvarFichaClinica(dto, 99L));
    }

    @Test
    @DisplayName("Deve criar prontuário e vinculá-lo à consulta correta")
    void deveCriarProntuarioParaConsulta() {
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(anamneseService.salvarOuAtualizar(paciente, dto)).thenReturn(anamneseMock);
        when(prontuarioRepository.save(any())).thenAnswer(inv -> {
            Prontuario prontuario = inv.getArgument(0);
            prontuario.setId(77L);
            return prontuario;
        });

        ProntuarioDTO resultado = service.salvarFichaClinica(dto, 10L);

        assertEquals(77L, resultado.getId());
        assertNotNull(consulta.getProntuario());
        assertSame(consulta, consulta.getProntuario().getConsulta());
        assertEquals("Paciente com dor", consulta.getProntuario().getObservacoes());
        assertEquals("{dente:16}", consulta.getProntuario().getOdontogramaJson());
        verify(consultaRepository).save(consulta);
    }

    @Test
    @DisplayName("Deve salvar prontuário em consulta que já possui prontuário (Atualização)")
    void deveAtualizarProntuarioExistente() {
        Prontuario pExistente = new Prontuario();
        pExistente.setId(55L);
        consulta.setProntuario(pExistente);

        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(anamneseService.salvarOuAtualizar(any(), any())).thenReturn(anamneseMock);
        when(prontuarioRepository.save(any())).thenReturn(pExistente);

        ProntuarioDTO res = service.salvarFichaClinica(dto, 10L);

        assertEquals(55L, res.getId());
        verify(consultaRepository, never()).save(consulta); 
    }

    @Test
    @DisplayName("Deve criar ou atualizar a anamnese ao salvar um prontuário legado sem anamnese")
    void deveSalvarAnamneseParaProntuarioLegado() {
        Prontuario prontuarioLegado = new Prontuario();
        prontuarioLegado.setId(56L);
        prontuarioLegado.setConsulta(consulta);
        consulta.setProntuario(prontuarioLegado);

        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(anamneseService.salvarOuAtualizar(paciente, dto)).thenReturn(anamneseMock);
        when(prontuarioRepository.save(prontuarioLegado)).thenReturn(prontuarioLegado);

        service.salvarFichaClinica(dto, 10L);

        verify(anamneseService).salvarOuAtualizar(paciente, dto);
        verify(prontuarioRepository).save(prontuarioLegado);
    }

    @Test
    @DisplayName("Deve buscar ficha e retornar anamnese vazia se não existir")
    void deveBuscarComAnamneseVazia() {
        Prontuario p = new Prontuario();
        p.setId(99L);
        consulta.setProntuario(p);

        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(anamneseService.buscarPorPaciente(paciente)).thenReturn(new Anamnese());

        ProntuarioDTO resultado = service.buscarPorConsultaId(10L);
        
        assertNotNull(resultado);
        assertNull(resultado.getAlergiaResposta());
    }

    @Test
    @DisplayName("Deve retornar não encontrado quando a consulta ainda não possui prontuário")
    void deveFalharBuscaSemProntuario() {
        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));

        ResourceNotFoundException erro = assertThrows(
                ResourceNotFoundException.class,
                () -> service.buscarPorConsultaId(10L));

        assertEquals("Nenhum prontuário registrado para esta consulta.", erro.getMessage());
        verify(anamneseService, never()).buscarPorPaciente(any());
    }

    @Test
    @DisplayName("Deve manter prontuários separados para consultas diferentes do mesmo paciente")
    void deveManterProntuariosSeparadosPorConsulta() {
        Consulta segundaConsulta = new Consulta();
        segundaConsulta.setId(20L);
        segundaConsulta.setPaciente(paciente);

        when(consultaRepository.findById(10L)).thenReturn(Optional.of(consulta));
        when(consultaRepository.findById(20L)).thenReturn(Optional.of(segundaConsulta));
        when(anamneseService.salvarOuAtualizar(eq(paciente), any())).thenReturn(anamneseMock);
        when(prontuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ProntuarioDTO primeiraFicha = new ProntuarioDTO();
        primeiraFicha.setObservacoes("Consulta 1");
        primeiraFicha.setOdontogramaJson("{\"11\":\"treated\"}");
        ProntuarioDTO segundaFicha = new ProntuarioDTO();
        segundaFicha.setObservacoes("Consulta 2");
        segundaFicha.setOdontogramaJson("{\"11\":\"decayed\"}");

        service.salvarFichaClinica(primeiraFicha, 10L);
        service.salvarFichaClinica(segundaFicha, 20L);

        assertNotSame(consulta.getProntuario(), segundaConsulta.getProntuario());
        assertSame(consulta, consulta.getProntuario().getConsulta());
        assertSame(segundaConsulta, segundaConsulta.getProntuario().getConsulta());
        assertEquals("{\"11\":\"treated\"}", consulta.getProntuario().getOdontogramaJson());
        assertEquals("{\"11\":\"decayed\"}", segundaConsulta.getProntuario().getOdontogramaJson());
    }

    @Test
    @DisplayName("Deve deletar prontuário e desvincular da consulta")
    void deveDeletarProntuario() {
        Prontuario p = new Prontuario();
        p.setId(55L);
        p.setConsulta(consulta);
        
        when(prontuarioRepository.findById(55L)).thenReturn(Optional.of(p));

        service.deletar(55L);

        assertNull(consulta.getProntuario()); 
        verify(consultaRepository).save(consulta); 
        verify(prontuarioRepository).delete(p); 
    }
}
