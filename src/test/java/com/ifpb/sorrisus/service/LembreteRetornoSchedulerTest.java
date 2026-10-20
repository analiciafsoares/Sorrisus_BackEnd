package com.ifpb.sorrisus.service;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LembreteRetornoSchedulerTest {

    @Test
    void deveUsarMesmoProcessamentoNaInicializacaoENoAgendamento() {
        LembreteRetornoService service = mock(LembreteRetornoService.class);
        LembreteRetornoScheduler scheduler = new LembreteRetornoScheduler(service);

        scheduler.executarAposInicializacao();
        scheduler.executarDiariamente();

        verify(service, times(2)).processarLembretesPendentes();
    }

    @Test
    void falhaNaoDeveEscaparNemImpedirNovaExecucao() {
        LembreteRetornoService service = mock(LembreteRetornoService.class);
        LembreteRetornoScheduler scheduler = new LembreteRetornoScheduler(service);
        doThrow(new RuntimeException("falha")).doNothing().when(service).processarLembretesPendentes();

        assertDoesNotThrow(scheduler::executarAposInicializacao);
        assertDoesNotThrow(scheduler::executarDiariamente);

        verify(service, times(2)).processarLembretesPendentes();
    }

    @Test
    void deveImpedirSobreposicaoNaMesmaInstancia() throws Exception {
        LembreteRetornoService service = mock(LembreteRetornoService.class);
        LembreteRetornoScheduler scheduler = new LembreteRetornoScheduler(service);
        CountDownLatch iniciou = new CountDownLatch(1);
        CountDownLatch liberar = new CountDownLatch(1);

        doAnswer(invocation -> {
            iniciou.countDown();
            assertTrue(liberar.await(5, TimeUnit.SECONDS));
            return null;
        }).when(service).processarLembretesPendentes();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            executor.submit(scheduler::executarAposInicializacao);
            assertTrue(iniciou.await(5, TimeUnit.SECONDS));

            scheduler.executarDiariamente();
            liberar.countDown();
        } finally {
            executor.shutdown();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }

        verify(service).processarLembretesPendentes();
    }
}
