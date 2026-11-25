package com.ifpb.sorrisus.config;

import com.ifpb.sorrisus.model.Dentista;
import com.ifpb.sorrisus.model.Role;
import com.ifpb.sorrisus.repository.DentistaRepository;
import com.ifpb.sorrisus.service.DentistaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("Teste Unitário - DentistaInicialSeeder")
class DentistaInicialSeederTest {

    @Mock
    private DentistaRepository dentistaRepository;

    @Mock
    private DentistaService dentistaService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private DentistaInicialSeeder seeder(String email, String senha) {
        return new DentistaInicialSeeder(dentistaRepository, dentistaService, passwordEncoder,
                "Dentista Inicial", email, senha, "12345-PB", "Clínico Geral");
    }

    @Test
    @DisplayName("Deve criar o dentista inicial quando não há dentistas e as variáveis estão configuradas")
    void deveCriarDentistaInicial() {
        when(dentistaRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode("senhaInicial")).thenReturn("hash");

        seeder("inicial@clinica.local", "senhaInicial").run(null);

        ArgumentCaptor<Dentista> captor = ArgumentCaptor.forClass(Dentista.class);
        verify(dentistaService).salvar(captor.capture());
        Dentista criado = captor.getValue();
        assertThat(criado.getEmail()).isEqualTo("inicial@clinica.local");
        assertThat(criado.getSenha()).isEqualTo("hash");
        assertThat(criado.getRole()).isEqualTo(Role.ROLE_DENTISTA);
        assertThat(criado.getCro()).isEqualTo("12345-PB");
    }

    @Test
    @DisplayName("Não deve criar nada quando já existe dentista cadastrado")
    void naoDeveCriarQuandoJaExisteDentista() {
        when(dentistaRepository.count()).thenReturn(1L);

        seeder("inicial@clinica.local", "senhaInicial").run(null);

        verify(dentistaService, never()).salvar(any());
    }

    @Test
    @DisplayName("Não deve criar nada quando as variáveis não estão configuradas")
    void naoDeveCriarSemConfiguracao() {
        when(dentistaRepository.count()).thenReturn(0L);

        seeder("", "").run(null);

        verify(dentistaService, never()).salvar(any());
    }

    @Test
    @DisplayName("Não deve criar nada quando a senha inicial é curta")
    void naoDeveCriarComSenhaCurta() {
        when(dentistaRepository.count()).thenReturn(0L);

        seeder("inicial@clinica.local", "123").run(null);

        verify(dentistaService, never()).salvar(any());
    }
}
