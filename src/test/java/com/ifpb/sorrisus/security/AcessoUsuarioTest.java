package com.ifpb.sorrisus.security;

import com.ifpb.sorrisus.model.Usuario;
import com.ifpb.sorrisus.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("Teste Unitário - AcessoUsuario")
class AcessoUsuarioTest {

    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final AcessoUsuario acesso = new AcessoUsuario(repository);

    private Usuario usuario(long id, String email) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail(email);
        return u;
    }

    @Test
    @DisplayName("Reconhece o próprio cadastro pelo e-mail do usuário autenticado")
    void reconheceOProprioCadastro() {
        when(repository.findById(5L)).thenReturn(Optional.of(usuario(5L, "paciente@exemplo.com")));

        assertThat(acesso.ehOProprio(5L, new TestingAuthenticationToken("Paciente@Exemplo.com", null))).isTrue();
    }

    @Test
    @DisplayName("Nega acesso ao cadastro de outro usuário")
    void negaCadastroDeOutroUsuario() {
        when(repository.findById(6L)).thenReturn(Optional.of(usuario(6L, "outro@exemplo.com")));

        assertThat(acesso.ehOProprio(6L, new TestingAuthenticationToken("paciente@exemplo.com", null))).isFalse();
    }

    @Test
    @DisplayName("Nega acesso quando o cadastro não existe ou não há autenticação")
    void negaSemCadastroOuSemAutenticacao() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        assertThat(acesso.ehOProprio(7L, new TestingAuthenticationToken("paciente@exemplo.com", null))).isFalse();
        assertThat(acesso.ehOProprio(7L, null)).isFalse();
        assertThat(acesso.ehOProprio(null, new TestingAuthenticationToken("paciente@exemplo.com", null))).isFalse();
    }
}
