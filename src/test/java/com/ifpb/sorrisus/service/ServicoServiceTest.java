package com.ifpb.sorrisus.service;

import com.ifpb.sorrisus.exception.ResourceNotFoundException;
import com.ifpb.sorrisus.model.Servico;
import com.ifpb.sorrisus.repository.ServicoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicoServiceTest {

    @InjectMocks
    private ServicoService service;

    @Mock
    private ServicoRepository servicoRepository;

    private Servico servico;

    @BeforeEach
    void setUp() {
        servico = new Servico();
        servico.setId(1L);
        servico.setNome("Limpeza");
        servico.setAtivo(true);
    }

    @Test
    @DisplayName("Deve desativar serviço sem remover o registro")
    void deveDesativarSemExcluirFisicamente() {
        when(servicoRepository.findById(1L)).thenReturn(Optional.of(servico));

        service.desativar(1L);

        assertFalse(servico.isAtivo());
        verify(servicoRepository).save(servico);
        verify(servicoRepository, never()).delete(any(Servico.class));
        verify(servicoRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Deve falhar ao desativar serviço inexistente")
    void deveFalharAoDesativarServicoInexistente() {
        when(servicoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.desativar(99L));

        verify(servicoRepository, never()).save(any(Servico.class));
        verify(servicoRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Deve permitir reativar serviço pela edição")
    void deveReativarServicoPelaEdicao() {
        servico.setAtivo(false);
        Servico atualizacao = new Servico();
        atualizacao.setNome("Limpeza");
        atualizacao.setAtivo(true);

        when(servicoRepository.findById(1L)).thenReturn(Optional.of(servico));
        when(servicoRepository.save(servico)).thenReturn(servico);

        Servico resultado = service.atualizar(1L, atualizacao);

        assertTrue(resultado.isAtivo());
        verify(servicoRepository).save(servico);
    }
}
