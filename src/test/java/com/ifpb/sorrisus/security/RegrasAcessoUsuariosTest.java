package com.ifpb.sorrisus.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ifpb.sorrisus.controller.DentistaController;
import com.ifpb.sorrisus.controller.PacienteController;
import com.ifpb.sorrisus.controller.RecepcionistaController;
import com.ifpb.sorrisus.controller.UsuarioController;
import com.ifpb.sorrisus.model.Dentista;
import com.ifpb.sorrisus.model.Paciente;
import com.ifpb.sorrisus.model.Recepcionista;
import com.ifpb.sorrisus.model.Usuario;
import com.ifpb.sorrisus.repository.UsuarioRepository;
import com.ifpb.sorrisus.service.DentistaService;
import com.ifpb.sorrisus.service.PacienteService;
import com.ifpb.sorrisus.service.RecepcionistaService;
import com.ifpb.sorrisus.service.UsuarioService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * Verifica, com a segurança por método do Spring ativa, quem pode consultar e alterar
 * cadastros de usuários. Os serviços são simulados; o que se testa são as regras de @PreAuthorize.
 */
@SpringJUnitConfig(RegrasAcessoUsuariosTest.Config.class)
@DisplayName("Teste - regras de acesso aos cadastros de usuários")
class RegrasAcessoUsuariosTest {

    @Configuration
    @EnableMethodSecurity
    static class Config {
        @Bean UsuarioRepository usuarioRepository() { return mock(UsuarioRepository.class); }
        @Bean PasswordEncoder passwordEncoder() { return mock(PasswordEncoder.class); }
        @Bean UsuarioService usuarioService() { return mock(UsuarioService.class); }
        @Bean PacienteService pacienteService() { return mock(PacienteService.class); }
        @Bean DentistaService dentistaService() { return mock(DentistaService.class); }
        @Bean RecepcionistaService recepcionistaService() { return mock(RecepcionistaService.class); }
        @Bean AcessoUsuario acessoUsuario(UsuarioRepository repository) { return new AcessoUsuario(repository); }
        @Bean UsuarioController usuarioController(UsuarioService s, PasswordEncoder e) { return new UsuarioController(s, e); }
        @Bean PacienteController pacienteController(PacienteService s, PasswordEncoder e) { return new PacienteController(s, e); }
        @Bean DentistaController dentistaController(DentistaService s, PasswordEncoder e) { return new DentistaController(s, e); }
        @Bean RecepcionistaController recepcionistaController(RecepcionistaService s, PasswordEncoder e) { return new RecepcionistaController(s, e); }
    }

    @Autowired UsuarioRepository usuarioRepository;
    @Autowired UsuarioController usuarioController;
    @Autowired PacienteController pacienteController;
    @Autowired DentistaController dentistaController;
    @Autowired RecepcionistaController recepcionistaController;

    private static final long PACIENTE_ID = 10L;
    private static final long OUTRO_PACIENTE_ID = 11L;
    private static final long RECEPCIONISTA_ID = 2L;

    @BeforeEach
    void cadastros() {
        reset(usuarioRepository);
        when(usuarioRepository.findById(PACIENTE_ID)).thenReturn(Optional.of(usuario(PACIENTE_ID, "paciente@exemplo.com")));
        when(usuarioRepository.findById(OUTRO_PACIENTE_ID)).thenReturn(Optional.of(usuario(OUTRO_PACIENTE_ID, "outro@exemplo.com")));
        when(usuarioRepository.findById(RECEPCIONISTA_ID)).thenReturn(Optional.of(usuario(RECEPCIONISTA_ID, "recepcao@exemplo.com")));
    }

    @AfterEach
    void limparAutenticacao() {
        SecurityContextHolder.clearContext();
    }

    private static Usuario usuario(long id, String email) {
        Usuario u = new Usuario();
        u.setId(id);
        u.setEmail(email);
        return u;
    }

    private static void logadoComo(String email, String perfil) {
        TestingAuthenticationToken auth = new TestingAuthenticationToken(email, null, perfil);
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Paciente não lista, não altera nem consulta usuários pela rota genérica")
    void pacienteNaoUsaRotaGenericaDeUsuarios() {
        logadoComo("paciente@exemplo.com", "ROLE_PACIENTE");

        assertThatThrownBy(() -> usuarioController.listarTodos()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> usuarioController.buscarPorId(PACIENTE_ID)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> usuarioController.atualizar(PACIENTE_ID, new Usuario())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Recepcionista também não usa a rota genérica de usuários")
    void recepcionistaNaoUsaRotaGenericaDeUsuarios() {
        logadoComo("recepcao@exemplo.com", "ROLE_RECEPCIONISTA");

        assertThatThrownBy(() -> usuarioController.atualizar(RECEPCIONISTA_ID, new Usuario())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Paciente consulta e edita apenas o próprio cadastro")
    void pacienteAcessaSomenteOProprioCadastro() {
        logadoComo("paciente@exemplo.com", "ROLE_PACIENTE");

        assertThatCode(() -> pacienteController.buscarPorId(PACIENTE_ID)).doesNotThrowAnyException();
        assertThatCode(() -> pacienteController.atualizar(PACIENTE_ID, new Paciente())).doesNotThrowAnyException();

        assertThatThrownBy(() -> pacienteController.buscarPorId(OUTRO_PACIENTE_ID)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> pacienteController.atualizar(OUTRO_PACIENTE_ID, new Paciente())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> pacienteController.listarTodos()).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> pacienteController.deletar(OUTRO_PACIENTE_ID)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("Equipe da clínica lista, consulta, edita e exclui pacientes")
    void equipeGerenciaPacientes() {
        logadoComo("recepcao@exemplo.com", "ROLE_RECEPCIONISTA");

        assertThatCode(() -> pacienteController.listarTodos()).doesNotThrowAnyException();
        assertThatCode(() -> pacienteController.buscarPorId(OUTRO_PACIENTE_ID)).doesNotThrowAnyException();
        assertThatCode(() -> pacienteController.atualizar(OUTRO_PACIENTE_ID, new Paciente())).doesNotThrowAnyException();
        assertThatCode(() -> pacienteController.deletar(OUTRO_PACIENTE_ID)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Apenas dentistas editam cadastros de dentistas; a lista continua disponível aos usuários autenticados")
    void somenteDentistaEditaDentista() {
        logadoComo("paciente@exemplo.com", "ROLE_PACIENTE");
        assertThatCode(() -> dentistaController.listarTodos()).doesNotThrowAnyException();
        assertThatThrownBy(() -> dentistaController.atualizar(1L, new Dentista())).isInstanceOf(AccessDeniedException.class);

        logadoComo("dentista@exemplo.com", "ROLE_DENTISTA");
        assertThatCode(() -> dentistaController.atualizar(1L, new Dentista())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("Recepcionista acessa o próprio cadastro; a lista de recepcionistas é restrita ao dentista")
    void recepcionistaAcessaSomenteOProprioCadastro() {
        logadoComo("recepcao@exemplo.com", "ROLE_RECEPCIONISTA");
        assertThatCode(() -> recepcionistaController.buscarPorId(RECEPCIONISTA_ID)).doesNotThrowAnyException();
        assertThatCode(() -> recepcionistaController.atualizar(RECEPCIONISTA_ID, new Recepcionista())).doesNotThrowAnyException();
        assertThatThrownBy(() -> recepcionistaController.listarTodos()).isInstanceOf(AccessDeniedException.class);

        logadoComo("paciente@exemplo.com", "ROLE_PACIENTE");
        assertThatThrownBy(() -> recepcionistaController.buscarPorId(RECEPCIONISTA_ID)).isInstanceOf(AccessDeniedException.class);

        logadoComo("dentista@exemplo.com", "ROLE_DENTISTA");
        assertThatCode(() -> recepcionistaController.listarTodos()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("A senha é aceita na entrada, mas nunca devolvida nas respostas da API")
    void senhaNaoApareceNoJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        Usuario u = usuario(1L, "dentista@exemplo.com");
        u.setSenha("hash-que-nao-pode-vazar");

        assertThat(mapper.writeValueAsString(u)).doesNotContain("senha").doesNotContain("hash-que-nao-pode-vazar");

        Paciente lido = mapper.readValue("{\"email\":\"novo@exemplo.com\",\"senha\":\"Teste@123\"}", Paciente.class);
        assertThat(lido.getSenha()).isEqualTo("Teste@123");
    }
}
