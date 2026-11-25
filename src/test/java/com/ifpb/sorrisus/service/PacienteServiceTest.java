package com.ifpb.sorrisus.service;

import com.ifpb.sorrisus.exception.CPFAlreadyExistsException;
import com.ifpb.sorrisus.exception.EmailAlreadyExistsException;
import com.ifpb.sorrisus.exception.InvalidCPFException;
import com.ifpb.sorrisus.exception.InvalidFieldException;
import com.ifpb.sorrisus.exception.ResourceNotFoundException;
import com.ifpb.sorrisus.model.Paciente;
import com.ifpb.sorrisus.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import jakarta.validation.Validation;
import jakarta.validation.Validator;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Teste Unitário - PacienteService")
class PacienteServiceTest {

    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    @Mock
    private PacienteRepository pacienteRepository;

    private PacienteService pacienteService;

    private Paciente paciente;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pacienteService = new PacienteService(pacienteRepository, VALIDATOR);
        paciente = new Paciente();
        paciente.setId(1L);
        paciente.setNome("João Silva");
        paciente.setEmail("joao@sorrisus.com");
        paciente.setTelefone("83999999999");
        paciente.setCpf("52998224725");
        paciente.setDataNascimento(LocalDate.parse("1990-01-01"));
    }

    @Test
    @DisplayName("Deve salvar paciente com sucesso")
    void deveSalvarPaciente() {
        when(pacienteRepository.existsByCpf("52998224725")).thenReturn(false);
        when(pacienteRepository.save(any(Paciente.class))).thenReturn(paciente);

        Paciente salvo = pacienteService.salvar(paciente);

        assertThat(salvo).isNotNull();
        assertThat(salvo.getNome()).isEqualTo("João Silva");
        assertThat(salvo.getCpf()).isEqualTo("52998224725");
        verify(pacienteRepository, times(1)).save(paciente);
    }

    @Test
    @DisplayName("Deve lançar exceção ao salvar paciente com CPF nulo")
    void deveLancarExcecaoAoSalvarComCpfNulo() {
        paciente.setCpf(null);

        assertThatThrownBy(() -> pacienteService.salvar(paciente))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("O CPF é obrigatório");

        verify(pacienteRepository, never()).save(any(Paciente.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao salvar paciente com CPF inválido")
    void deveLancarExcecaoAoSalvarComCpfInvalido() {
        paciente.setCpf("123");

        assertThatThrownBy(() -> pacienteService.salvar(paciente))
                .isInstanceOf(InvalidCPFException.class)
                .hasMessage("CPF inválido. Verifique o número informado.");

        verify(pacienteRepository, never()).save(any(Paciente.class));
    }

    @Test
    @DisplayName("Deve rejeitar CPF com onze dígitos e verificadores inválidos")
    void deveRejeitarCpfComDigitosVerificadoresInvalidos() {
        paciente.setCpf("12345678900");

        assertThatThrownBy(() -> pacienteService.salvar(paciente))
                .isInstanceOf(InvalidCPFException.class)
                .hasMessage("CPF inválido. Verifique o número informado.");

        verify(pacienteRepository, never()).existsByCpf(any());
        verify(pacienteRepository, never()).save(any(Paciente.class));
    }

    @Test
    @DisplayName("Deve normalizar CPF formatado antes de salvar")
    void deveNormalizarCpfFormatadoAoSalvar() {
        paciente.setCpf("529.982.247-25");
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));

        Paciente salvo = pacienteService.salvar(paciente);

        assertThat(salvo.getCpf()).isEqualTo("52998224725");
        verify(pacienteRepository).existsByCpf("52998224725");
        verify(pacienteRepository).save(paciente);
    }

    @Test
    @DisplayName("Deve lançar exceção ao salvar paciente com CPF já existente")
    void deveLancarExcecaoAoSalvarComCpfJaExistente() {
        when(pacienteRepository.existsByCpf("52998224725")).thenReturn(true);

        assertThatThrownBy(() -> pacienteService.salvar(paciente))
                .isInstanceOf(CPFAlreadyExistsException.class)
                .hasMessageContaining("52998224725");

        verify(pacienteRepository, never()).save(any(Paciente.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao salvar paciente com data de nascimento futura")
    void deveLancarExcecaoAoSalvarComDataFutura() {
        paciente.setDataNascimento(LocalDate.now().plusDays(1));
        when(pacienteRepository.existsByCpf("52998224725")).thenReturn(false);

        assertThatThrownBy(() -> pacienteService.salvar(paciente))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("A data de nascimento não pode ser futura");

        verify(pacienteRepository, never()).save(any(Paciente.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao salvar paciente com telefone nulo")
    void deveLancarExcecaoAoSalvarComTelefoneNulo() {
        paciente.setTelefone(null);
        when(pacienteRepository.existsByCpf("52998224725")).thenReturn(false);

        assertThatThrownBy(() -> pacienteService.salvar(paciente))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("O telefone é obrigatório");

        verify(pacienteRepository, never()).save(any(Paciente.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao salvar paciente com telefone em branco")
    void deveLancarExcecaoAoSalvarComTelefoneEmBranco() {
        paciente.setTelefone("   ");
        when(pacienteRepository.existsByCpf("52998224725")).thenReturn(false);

        assertThatThrownBy(() -> pacienteService.salvar(paciente))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("O telefone é obrigatório");

        verify(pacienteRepository, never()).save(any(Paciente.class));
    }

    @Test
    @DisplayName("Deve listar todos os pacientes")
    void deveListarPacientes() {
        when(pacienteRepository.findAll()).thenReturn(Arrays.asList(paciente));

        List<Paciente> lista = pacienteService.listarTodos();

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getEmail()).isEqualTo("joao@sorrisus.com");
        verify(pacienteRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve buscar paciente por ID com sucesso")
    void deveBuscarPacientePorId() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));

        Paciente resultado = pacienteService.buscarPorId(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getCpf()).isEqualTo("52998224725");
        assertThat(resultado.getTelefone()).isEqualTo("83999999999");
        verify(pacienteRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar paciente inexistente")
    void deveLancarExcecaoAoBuscarInexistente() {
        when(pacienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pacienteService.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Paciente com id 99 não encontrado");

        verify(pacienteRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("Deve atualizar paciente com sucesso")
    void deveAtualizarPacienteComSucesso() {
        Paciente atualizado = new Paciente();
        atualizado.setNome("João Atualizado");
        atualizado.setEmail("joao.atualizado@sorrisus.com");
        atualizado.setCpf("11144477735");
        atualizado.setTelefone("83888888888");
        atualizado.setDataNascimento(LocalDate.parse("1991-02-02"));

        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.existsByCpf("11144477735")).thenReturn(false);
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));

        Paciente resultado = pacienteService.atualizar(1L, atualizado);

        assertThat(resultado.getNome()).isEqualTo("João Atualizado");
        assertThat(resultado.getCpf()).isEqualTo("11144477735");
        assertThat(resultado.getTelefone()).isEqualTo("83888888888");
        assertThat(resultado.getDataNascimento()).isEqualTo(LocalDate.parse("1991-02-02"));
        verify(pacienteRepository, times(1)).findById(1L);
        verify(pacienteRepository, times(1)).save(any(Paciente.class));
    }

    @Test
    @DisplayName("Deve manter mesmo CPF ao atualizar paciente")
    void deveManterMesmoCpfAoAtualizar() {
        Paciente atualizado = new Paciente();
        atualizado.setNome("João Atualizado");
        atualizado.setEmail("joao@sorrisus.com");
        atualizado.setCpf("529.982.247-25");
        atualizado.setTelefone("83888888888");

        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(inv -> inv.getArgument(0));

        Paciente resultado = pacienteService.atualizar(1L, atualizado);

        assertThat(resultado.getCpf()).isEqualTo("52998224725");
        verify(pacienteRepository, never()).existsByEmail(any());
        verify(pacienteRepository, never()).existsByCpf(any());
        verify(pacienteRepository, times(1)).findById(1L);
        verify(pacienteRepository, times(1)).save(any(Paciente.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar paciente com CPF já existente")
    void deveLancarExcecaoAoAtualizarComCpfJaExistente() {
        Paciente atualizado = new Paciente();
        atualizado.setNome("João Atualizado");
        atualizado.setEmail("joao@sorrisus.com");
        atualizado.setCpf("11144477735");
        atualizado.setTelefone("83888888888");

        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.existsByCpf("11144477735")).thenReturn(true);

        assertThatThrownBy(() -> pacienteService.atualizar(1L, atualizado))
                .isInstanceOf(CPFAlreadyExistsException.class)
                .hasMessageContaining("11144477735");

        verify(pacienteRepository, never()).save(any(Paciente.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar paciente com CPF inválido")
    void deveLancarExcecaoAoAtualizarComCpfInvalido() {
        Paciente atualizado = new Paciente();
        atualizado.setNome("João Atualizado");
        atualizado.setCpf("123");
        atualizado.setTelefone("83888888888");

        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.existsByCpf("123")).thenReturn(false);

        assertThatThrownBy(() -> pacienteService.atualizar(1L, atualizado))
                .isInstanceOf(InvalidCPFException.class)
                .hasMessage("CPF inválido. Verifique o número informado.");

        verify(pacienteRepository, never()).save(any(Paciente.class));
    }

    @Test
    @DisplayName("Deve rejeitar atualização com CPF de verificadores inválidos")
    void deveRejeitarAtualizacaoComCpfDeVerificadoresInvalidos() {
        Paciente atualizado = new Paciente();
        atualizado.setNome("João Atualizado");
        atualizado.setEmail("joao@sorrisus.com");
        atualizado.setCpf("12345678900");
        atualizado.setTelefone("83888888888");

        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));

        assertThatThrownBy(() -> pacienteService.atualizar(1L, atualizado))
                .isInstanceOf(InvalidCPFException.class)
                .hasMessage("CPF inválido. Verifique o número informado.");

        verify(pacienteRepository, never()).existsByCpf(any());
        verify(pacienteRepository, never()).save(any(Paciente.class));
    }

    @Test
    @DisplayName("Deve rejeitar e-mail pertencente a outro paciente")
    void deveRejeitarEmailDeOutroPaciente() {
        Paciente atualizado = new Paciente();
        atualizado.setNome("João Atualizado");
        atualizado.setEmail("outro@sorrisus.com");
        atualizado.setCpf("52998224725");
        atualizado.setTelefone("83888888888");

        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        when(pacienteRepository.existsByEmail("outro@sorrisus.com")).thenReturn(true);

        assertThatThrownBy(() -> pacienteService.atualizar(1L, atualizado))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("outro@sorrisus.com");

        verify(pacienteRepository, never()).save(any(Paciente.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar atualizar paciente inexistente")
    void deveLancarExcecaoAoAtualizarInexistente() {
        Paciente atualizado = new Paciente();
        atualizado.setNome("Nome Qualquer");
        atualizado.setCpf("52998224725");
        atualizado.setTelefone("83999999999");

        when(pacienteRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pacienteService.atualizar(2L, atualizado))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Paciente com id 2 não encontrado");

        verify(pacienteRepository, times(1)).findById(2L);
        verify(pacienteRepository, never()).save(any(Paciente.class));
    }

    @Test
    @DisplayName("Deve deletar paciente com sucesso")
    void deveDeletarPaciente() {
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(paciente));
        doNothing().when(pacienteRepository).delete(paciente);

        pacienteService.deletar(1L);

        verify(pacienteRepository, times(1)).findById(1L);
        verify(pacienteRepository, times(1)).delete(paciente);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar deletar paciente inexistente")
    void deveLancarExcecaoAoDeletarInexistente() {
        when(pacienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pacienteService.deletar(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Paciente com id 99 não encontrado");

        verify(pacienteRepository, never()).delete(any(Paciente.class));
    }
}
