package com.ifpb.sorrisus.service;

import com.ifpb.sorrisus.exception.BusinessException;
import com.ifpb.sorrisus.exception.EmailAlreadyExistsException;
import com.ifpb.sorrisus.exception.InvalidFieldException;
import com.ifpb.sorrisus.exception.ResourceNotFoundException;
import com.ifpb.sorrisus.model.Role;
import com.ifpb.sorrisus.model.Usuario;
import com.ifpb.sorrisus.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Teste Unitário - UsuarioService")
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario usuarioBase;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        usuarioBase = new Usuario();
        usuarioBase.setId(1L);
        usuarioBase.setNome("Maria Souza");
        usuarioBase.setEmail("maria@sorrisus.com");
        usuarioBase.setSenha("123456");
        usuarioBase.setRole(Role.ROLE_RECEPCIONISTA);
    }

    @Test
    @DisplayName("Deve salvar um usuário com sucesso")
    void deveSalvarUsuario() {
        when(usuarioRepository.existsByEmail("maria@sorrisus.com")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioBase);

        Usuario salvo = usuarioService.salvar(usuarioBase);

        assertThat(salvo).isNotNull();
        assertThat(salvo.getNome()).isEqualTo("Maria Souza");
        assertThat(salvo.getEmail()).isEqualTo("maria@sorrisus.com");
        assertThat(salvo.getRole()).isEqualTo(Role.ROLE_RECEPCIONISTA);
        verify(usuarioRepository, times(1)).save(usuarioBase);
    }

    @Test
    @DisplayName("Deve lançar exceção ao salvar usuário com email nulo")
    void deveLancarExcecaoAoSalvarComEmailNulo() {
        usuarioBase.setEmail(null);

        assertThatThrownBy(() -> usuarioService.salvar(usuarioBase))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("O e-mail é obrigatório");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao salvar usuário com email em branco")
    void deveLancarExcecaoAoSalvarComEmailEmBranco() {
        usuarioBase.setEmail("   ");

        assertThatThrownBy(() -> usuarioService.salvar(usuarioBase))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("O e-mail é obrigatório");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao salvar usuário com email já existente")
    void deveLancarExcecaoAoSalvarComEmailJaExistente() {
        when(usuarioRepository.existsByEmail("maria@sorrisus.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.salvar(usuarioBase))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("maria@sorrisus.com");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao salvar usuário com role nula")
    void deveLancarExcecaoAoSalvarComRoleNula() {
        usuarioBase.setRole(null);
        when(usuarioRepository.existsByEmail("maria@sorrisus.com")).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.salvar(usuarioBase))
                .isInstanceOf(InvalidFieldException.class)
                .hasMessageContaining("A role é obrigatória");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve listar todos os usuários")
    void deveListarUsuarios() {
        when(usuarioRepository.findAll()).thenReturn(Arrays.asList(usuarioBase));

        List<Usuario> usuarios = usuarioService.listarTodos();

        assertThat(usuarios).hasSize(1);
        assertThat(usuarios.get(0).getEmail()).isEqualTo("maria@sorrisus.com");
        verify(usuarioRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("Deve buscar usuário por ID com sucesso")
    void deveBuscarPorIdComSucesso() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioBase));

        Usuario resultado = usuarioService.buscarPorId(1L);

        assertThat(resultado).isNotNull();
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNome()).isEqualTo("Maria Souza");
        verify(usuarioRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Deve lançar exceção ao buscar usuário inexistente")
    void deveLancarExcecaoAoBuscarUsuarioInexistente() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.buscarPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuário com id 99 não encontrado");

        verify(usuarioRepository, times(1)).findById(99L);
    }

    @Test
    @DisplayName("Deve atualizar usuário com sucesso")
    void deveAtualizarUsuarioComSucesso() {
        Usuario atualizado = new Usuario();
        atualizado.setNome("Maria Atualizada");
        atualizado.setEmail("maria.atualizada@sorrisus.com");
        atualizado.setSenha("654321");
        atualizado.setRole(Role.ROLE_RECEPCIONISTA);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioBase));
        when(usuarioRepository.existsByEmail("maria.atualizada@sorrisus.com")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = usuarioService.atualizar(1L, atualizado);

        assertThat(resultado.getNome()).isEqualTo("Maria Atualizada");
        assertThat(resultado.getEmail()).isEqualTo("maria.atualizada@sorrisus.com");
        assertThat(resultado.getSenha()).isEqualTo("654321");
        verify(usuarioRepository, times(1)).findById(1L);
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve manter mesmo email ao atualizar usuário")
    void deveManterMesmoEmailAoAtualizar() {
        Usuario atualizado = new Usuario();
        atualizado.setNome("Maria Souza");
        atualizado.setEmail("maria@sorrisus.com");
        atualizado.setSenha("654321");
        atualizado.setRole(Role.ROLE_RECEPCIONISTA);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioBase));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario resultado = usuarioService.atualizar(1L, atualizado);

        assertThat(resultado.getEmail()).isEqualTo("maria@sorrisus.com");
        verify(usuarioRepository, times(1)).findById(1L);
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao atualizar com email já existente")
    void deveLancarExcecaoAoAtualizarComEmailJaExistente() {
        Usuario atualizado = new Usuario();
        atualizado.setNome("Maria Atualizada");
        atualizado.setEmail("outro@sorrisus.com");
        atualizado.setSenha("654321");
        atualizado.setRole(Role.ROLE_RECEPCIONISTA);

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioBase));
        when(usuarioRepository.existsByEmail("outro@sorrisus.com")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.atualizar(1L, atualizado))
                .isInstanceOf(EmailAlreadyExistsException.class)
                .hasMessageContaining("outro@sorrisus.com");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar atualizar usuário inexistente")
    void deveLancarExcecaoAoAtualizarInexistente() {
        Usuario atualizado = new Usuario();
        atualizado.setNome("Nome Qualquer");

        when(usuarioRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.atualizar(2L, atualizado))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuário com id 2 não encontrado");

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve deletar usuário com sucesso")
    void deveDeletarUsuario() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuarioBase));
        doNothing().when(usuarioRepository).delete(usuarioBase);

        usuarioService.deletar(1L);

        verify(usuarioRepository, times(1)).findById(1L);
        verify(usuarioRepository, times(1)).delete(usuarioBase);
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar deletar usuário inexistente")
    void deveLancarExcecaoAoDeletarInexistente() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> usuarioService.deletar(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Usuário com id 99 não encontrado");

        verify(usuarioRepository, never()).delete(any(Usuario.class));
    }

    @Test
    @DisplayName("Deve alterar a senha quando a senha atual confere")
    void deveAlterarSenha() {
        usuarioBase.setSenha("hash-atual");
        when(usuarioRepository.findByEmail("maria@sorrisus.com")).thenReturn(Optional.of(usuarioBase));
        when(passwordEncoder.matches("senhaAtual", "hash-atual")).thenReturn(true);
        when(passwordEncoder.matches("novaSenha123", "hash-atual")).thenReturn(false);
        when(passwordEncoder.encode("novaSenha123")).thenReturn("hash-novo");

        usuarioService.alterarSenha("maria@sorrisus.com", "senhaAtual", "novaSenha123");

        assertThat(usuarioBase.getSenha()).isEqualTo("hash-novo");
        verify(usuarioRepository).save(usuarioBase);
    }

    @Test
    @DisplayName("Não deve alterar a senha quando a senha atual está incorreta")
    void naoDeveAlterarSenhaComSenhaAtualIncorreta() {
        usuarioBase.setSenha("hash-atual");
        when(usuarioRepository.findByEmail("maria@sorrisus.com")).thenReturn(Optional.of(usuarioBase));
        when(passwordEncoder.matches("errada", "hash-atual")).thenReturn(false);

        assertThatThrownBy(() -> usuarioService.alterarSenha("maria@sorrisus.com", "errada", "novaSenha123"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A senha atual está incorreta.");
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("Não deve aceitar nova senha igual à atual")
    void naoDeveAceitarNovaSenhaIgualAtual() {
        usuarioBase.setSenha("hash-atual");
        when(usuarioRepository.findByEmail("maria@sorrisus.com")).thenReturn(Optional.of(usuarioBase));
        when(passwordEncoder.matches("mesmaSenha", "hash-atual")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.alterarSenha("maria@sorrisus.com", "mesmaSenha", "mesmaSenha"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("A nova senha deve ser diferente da senha atual.");
        verify(usuarioRepository, never()).save(any());
    }
}
