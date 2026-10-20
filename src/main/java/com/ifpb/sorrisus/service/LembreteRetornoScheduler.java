package com.ifpb.sorrisus.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(
        name = "sorrisus.lembretes-retorno.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class LembreteRetornoScheduler {

    private static final Logger logger = LoggerFactory.getLogger(LembreteRetornoScheduler.class);

    private final LembreteRetornoService lembreteRetornoService;
    private final AtomicBoolean emExecucao = new AtomicBoolean(false);

    public LembreteRetornoScheduler(LembreteRetornoService lembreteRetornoService) {
        this.lembreteRetornoService = lembreteRetornoService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void executarAposInicializacao() {
        executar("inicialização");
    }

    @Scheduled(
            cron = "${sorrisus.lembretes-retorno.cron:0 0 9 * * *}",
            zone = "${sorrisus.lembretes-retorno.zone:America/Fortaleza}"
    )
    public void executarDiariamente() {
        executar("agendamento diário");
    }

    private void executar(String origem) {
        if (!emExecucao.compareAndSet(false, true)) {
            logger.info("Verificação de retornos por {} ignorada: já existe uma execução em andamento", origem);
            return;
        }

        try {
            logger.info("Iniciando verificação de retornos por {}", origem);
            lembreteRetornoService.processarLembretesPendentes();
        } catch (RuntimeException e) {
            logger.error("Falha inesperada na verificação de retornos por {}", origem, e);
        } finally {
            emExecucao.set(false);
        }
    }
}
