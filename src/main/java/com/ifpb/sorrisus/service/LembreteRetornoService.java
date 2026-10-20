package com.ifpb.sorrisus.service;

import com.ifpb.sorrisus.exception.WhatsAppSendException;
import com.ifpb.sorrisus.model.LembreteRetorno;
import com.ifpb.sorrisus.model.Paciente;
import com.ifpb.sorrisus.model.RetornoPendente;
import com.ifpb.sorrisus.model.StatusLembreteRetorno;
import com.ifpb.sorrisus.repository.LembreteRetornoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LembreteRetornoService {

    private static final Logger logger = LoggerFactory.getLogger(LembreteRetornoService.class);
    private static final int TAMANHO_MAXIMO_ERRO = 1000;
    private static final String MENSAGEM = "Olá, %s.\n\n"
            + "Já faz algum tempo desde sua última consulta odontológica. "
            + "Manter o acompanhamento periódico é importante para a prevenção e o cuidado com a saúde bucal.\n\n"
            + "Entre em contato com a clínica para verificar a possibilidade de agendar seu retorno.\n\n"
            + "Atenciosamente,\n"
            + "Equipe Sorrisus";

    private final ConsultaService consultaService;
    private final WhatsAppService whatsAppService;
    private final LembreteRetornoRepository lembreteRetornoRepository;

    public LembreteRetornoService(ConsultaService consultaService,
                                  WhatsAppService whatsAppService,
                                  LembreteRetornoRepository lembreteRetornoRepository) {
        this.consultaService = consultaService;
        this.whatsAppService = whatsAppService;
        this.lembreteRetornoRepository = lembreteRetornoRepository;
    }

    public void processarLembretesPendentes() {
        if (!whatsAppService.isConfigured()) {
            logger.error("Verificação de retornos encerrada: configuração da Z-API incompleta");
            return;
        }

        List<RetornoPendente> retornos = consultaService.verificarRetornosPendentes();
        int enviados = 0;
        int ignorados = 0;
        int falhos = 0;

        for (RetornoPendente retorno : retornos) {
            ResultadoProcessamento resultado = processarRetorno(retorno);

            switch (resultado) {
                case ENVIADO -> enviados++;
                case IGNORADO -> ignorados++;
                case FALHA -> falhos++;
            }
        }

        logger.info(
                "Verificação de retornos concluída: {} elegíveis, {} enviados, {} ignorados e {} falhos",
                retornos.size(), enviados, ignorados, falhos
        );
    }

    private ResultadoProcessamento processarRetorno(RetornoPendente retorno) {
        Paciente paciente = retorno.getPaciente();

        Optional<String> telefone = normalizarTelefone(paciente.getTelefone());
        if (telefone.isEmpty()) {
            logger.warn("Lembrete de retorno ignorado: telefone ausente ou inválido para paciente id={}", paciente.getId());
            return ResultadoProcessamento.IGNORADO;
        }

        Optional<LembreteRetorno> registroExistente = lembreteRetornoRepository
                .findByPacienteIdAndUltimaConsultaId(paciente.getId(), retorno.getUltimaConsulta().getId());

        if (registroExistente.isPresent()
                && registroExistente.get().getStatus() != StatusLembreteRetorno.FALHA_TRANSITORIA) {
            return ResultadoProcessamento.IGNORADO;
        }

        LembreteRetorno registro = registroExistente.orElseGet(LembreteRetorno::new);
        registro.setPaciente(paciente);
        registro.setUltimaConsulta(retorno.getUltimaConsulta());
        registro.setStatus(StatusLembreteRetorno.PROCESSANDO);
        registro.setTentativas(registro.getTentativas() + 1);
        registro.setDataUltimaTentativa(LocalDateTime.now());
        registro.setErro(null);

        try {
            registro = lembreteRetornoRepository.saveAndFlush(registro);
        } catch (DataIntegrityViolationException | OptimisticLockingFailureException e) {
            logger.info("Lembrete de retorno já reservado por outra execução para paciente id={}", paciente.getId());
            return ResultadoProcessamento.IGNORADO;
        }

        try {
            whatsAppService.sendMessage(telefone.get(), MENSAGEM.formatted(paciente.getNome()));
            registro.setStatus(StatusLembreteRetorno.ENVIADO);
            registro.setDataEnvio(LocalDateTime.now());
            registro.setErro(null);
            registro = lembreteRetornoRepository.saveAndFlush(registro);

            logger.info("Lembrete preventivo aceito pela Z-API para paciente id={}", paciente.getId());
            return ResultadoProcessamento.ENVIADO;
        } catch (WhatsAppSendException e) {
            registro.setStatus(mapearStatus(e.getTipoFalha()));
            registro.setErro(resumirErro(e.getMessage()));
            salvarFalha(registro, paciente.getId());
            return ResultadoProcessamento.FALHA;
        } catch (RuntimeException e) {
            registro.setStatus(StatusLembreteRetorno.FALHA_PERMANENTE);
            registro.setErro(resumirErro(e.getMessage()));
            salvarFalha(registro, paciente.getId());
            return ResultadoProcessamento.FALHA;
        }
    }

    Optional<String> normalizarTelefone(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            return Optional.empty();
        }

        String apenasDigitos = telefone.replaceAll("\\D", "");

        if (apenasDigitos.length() == 10 || apenasDigitos.length() == 11) {
            return Optional.of("55" + apenasDigitos);
        }

        if ((apenasDigitos.length() == 12 || apenasDigitos.length() == 13)
                && apenasDigitos.startsWith("55")) {
            return Optional.of(apenasDigitos);
        }

        return Optional.empty();
    }

    private StatusLembreteRetorno mapearStatus(WhatsAppSendException.TipoFalha tipoFalha) {
        return switch (tipoFalha) {
            case TRANSITORIA -> StatusLembreteRetorno.FALHA_TRANSITORIA;
            case PERMANENTE -> StatusLembreteRetorno.FALHA_PERMANENTE;
            case AMBIGUA -> StatusLembreteRetorno.FALHA_AMBIGUA;
        };
    }

    private void salvarFalha(LembreteRetorno registro, Long pacienteId) {
        try {
            lembreteRetornoRepository.saveAndFlush(registro);
        } catch (RuntimeException persistenciaException) {
            logger.error(
                    "Não foi possível registrar a falha do lembrete para paciente id={}",
                    pacienteId,
                    persistenciaException
            );
        }

        logger.error(
                "Falha no lembrete preventivo para paciente id={}, status={}: {}",
                pacienteId,
                registro.getStatus(),
                registro.getErro()
        );
    }

    private String resumirErro(String erro) {
        if (erro == null || erro.isBlank()) {
            return "Erro sem detalhes";
        }

        return erro.length() <= TAMANHO_MAXIMO_ERRO
                ? erro
                : erro.substring(0, TAMANHO_MAXIMO_ERRO);
    }

    private enum ResultadoProcessamento {
        ENVIADO,
        IGNORADO,
        FALHA
    }
}
