package com.ifpb.sorrisus.service;

import com.ifpb.sorrisus.exception.*;
import com.ifpb.sorrisus.model.*;
import com.ifpb.sorrisus.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class ConsultaService {

    private final ConsultaRepository consultaRepository;
    private final PacienteRepository pacienteRepository;
    private final DentistaRepository dentistaRepository;
    private final ProntuarioRepository prontuarioRepository;
    private final AgendamentoRepository agendamentoRepository;

    public ConsultaService(ConsultaRepository consultaRepository,
                           PacienteRepository pacienteRepository,
                           DentistaRepository dentistaRepository,
                           ProntuarioRepository prontuarioRepository,
                           AgendamentoRepository agendamentoRepository) {
        this.consultaRepository = consultaRepository;
        this.pacienteRepository = pacienteRepository;
        this.dentistaRepository = dentistaRepository;
        this.prontuarioRepository = prontuarioRepository;
        this.agendamentoRepository = agendamentoRepository;
    }

    private void validarConflito(Dentista dentista, LocalDateTime dataHora, Long consultaId) {
        boolean conflitoConsulta;

        if (consultaId == null) {
            conflitoConsulta = consultaRepository.existsByDentistaAndDataHora(dentista, dataHora);
        } else {
            conflitoConsulta = consultaRepository.existsByDentistaAndDataHoraAndIdNot(dentista, dataHora, consultaId);
        }

        boolean conflitoAgendamento = agendamentoRepository.existsByDentistaAndDataHora(dentista, dataHora);

        if (conflitoConsulta || conflitoAgendamento) {
            throw new BusinessException("Horário indisponível. Existe um Agendamento ou Consulta para este dentista.");
        }
    }

    @Transactional
    public Consulta criar(Consulta consulta) {
        if (consulta.getPaciente() == null || consulta.getDentista() == null) {
            throw new InvalidFieldException("paciente/dentista", "Paciente e Dentista são obrigatórios.");
        }

        Dentista dentista = dentistaRepository.findById(consulta.getDentista().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Dentista não encontrado."));
        Paciente paciente = pacienteRepository.findById(consulta.getPaciente().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado."));

        validarConflito(dentista, consulta.getDataHora(), null);

        consulta.setDentista(dentista);
        consulta.setPaciente(paciente);

        if (consulta.getProntuario() != null) {
            Prontuario salvo = prontuarioRepository.save(consulta.getProntuario());
            consulta.setProntuario(salvo);
        }

        return consultaRepository.save(consulta);
    }

    @Transactional
    public Consulta atualizar(Long id, Consulta atualizado) {
        Consulta existente = buscarPorId(id);

        if (existente.getStatus() == StatusConsulta.CANCELADA) {
             throw new BusinessException("Não é possível editar uma consulta cancelada.");
        }

        if (atualizado.getDataHora() != null && !atualizado.getDataHora().equals(existente.getDataHora())) {
             validarConflito(existente.getDentista(), atualizado.getDataHora(), id);
             existente.setDataHora(atualizado.getDataHora());
        }

        if (atualizado.getStatus() != null) {
            existente.setStatus(atualizado.getStatus());
        }

        if (atualizado.getObservacao() != null) {
            existente.setObservacao(atualizado.getObservacao());
        }

        if (atualizado.getProntuario() != null) {
            Prontuario p = existente.getProntuario();
            if (p == null) {
                p = new Prontuario();
                p.setObservacoes(atualizado.getProntuario().getObservacoes());
                p = prontuarioRepository.save(p);
                existente.setProntuario(p);
            } else {
                p.setObservacoes(atualizado.getProntuario().getObservacoes());
                prontuarioRepository.save(p);
            }
        }

        return consultaRepository.save(existente);
    }

    @Transactional
    public void cancelar(Long id) {
        Consulta consulta = buscarPorId(id);
        consulta.setStatus(StatusConsulta.CANCELADA);
        consultaRepository.save(consulta);
    }

    public Consulta buscarPorId(Long id) {
        return consultaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Consulta não encontrada."));
    }

    public List<Consulta> listarPorPaciente(Long pacienteId) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado."));
        return consultaRepository.findByPaciente(paciente);
    }

    public List<Consulta> listarPorDentista(Long dentistaId) {
         Dentista dentista = dentistaRepository.findById(dentistaId)
                .orElseThrow(() -> new ResourceNotFoundException("Dentista não encontrado."));
        return consultaRepository.findByDentistaAndDataHoraBetween(dentista, LocalDateTime.now().minusYears(1), LocalDateTime.now().plusYears(1));
    }

    public List<RetornoPendente> verificarRetornosPendentes() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime seisMesesAtras = agora.minusMonths(6);
        List<Paciente> pacientesComRetornoPendente = consultaRepository.findPacientesComUltimaConsultaAntesDe(seisMesesAtras);

        List<RetornoPendente> retornosPendentes = new ArrayList<>();

        for (Paciente paciente : pacientesComRetornoPendente) {
            if (possuiAcompanhamentoFuturo(paciente, agora)) {
                continue;
            }

            List<Consulta> ultimasConsultas = consultaRepository.findUltimaConsultaRealizadaPorPaciente(paciente);

            if (!ultimasConsultas.isEmpty()) {
                Consulta ultimaConsulta = ultimasConsultas.get(0);

                RetornoPendente retornoPendente = new RetornoPendente();
                retornoPendente.setPaciente(paciente);
                retornoPendente.setUltimaConsulta(ultimaConsulta);
                retornoPendente.setDataUltimaConsulta(ultimaConsulta.getDataHora());
                retornoPendente.setDiasDesdeUltimaConsulta(calcularDiasDesdeUltimaConsulta(ultimaConsulta.getDataHora(), agora));
                retornoPendente.setPrioridade(calcularPrioridade(ultimaConsulta.getDataHora(), agora));

                retornosPendentes.add(retornoPendente);
            }
        }

        retornosPendentes.sort((r1, r2) -> Integer.compare(r2.getDiasDesdeUltimaConsulta(), r1.getDiasDesdeUltimaConsulta()));

        return retornosPendentes;
    }

    private boolean possuiAcompanhamentoFuturo(Paciente paciente, LocalDateTime agora) {
        boolean possuiAgendamentoNaoConfirmado = agendamentoRepository
                .existsByPacienteAndDataHoraGreaterThanEqualAndConfirmadoFalse(paciente, agora);

        if (possuiAgendamentoNaoConfirmado) {
            return true;
        }

        return consultaRepository.existsByPacienteAndDataHoraGreaterThanEqualAndStatusIn(
                paciente,
                agora,
                List.of(StatusConsulta.PENDENTE, StatusConsulta.CONFIRMADA)
        );
    }

    public List<RetornoPendente> verificarRetornosPendentesPorDentista(Long dentistaId) {
        Dentista dentista = dentistaRepository.findById(dentistaId)
                .orElseThrow(() -> new ResourceNotFoundException("Dentista não encontrado."));

        List<RetornoPendente> todosRetornos = verificarRetornosPendentes();

        return todosRetornos.stream()
                .filter(retorno -> retorno.getUltimaConsulta().getDentista().getId().equals(dentistaId))
                .toList();
    }

    public List<Paciente> buscarPacientesSemConsulta() {
        List<Paciente> todosPacientes = pacienteRepository.findAll();
        List<Paciente> pacientesSemConsulta = new ArrayList<>();

        for (Paciente paciente : todosPacientes) {
            List<Consulta> consultasRealizadas = consultaRepository.findUltimaConsultaRealizadaPorPaciente(paciente);
            if (consultasRealizadas.isEmpty()) {
                pacientesSemConsulta.add(paciente);
            }
        }

        return pacientesSemConsulta;
    }

    private int calcularDiasDesdeUltimaConsulta(LocalDateTime dataUltimaConsulta, LocalDateTime agora) {
        return (int) java.time.Duration.between(dataUltimaConsulta, agora).toDays();
    }

    private String calcularPrioridade(LocalDateTime dataUltimaConsulta, LocalDateTime agora) {
        int dias = calcularDiasDesdeUltimaConsulta(dataUltimaConsulta, agora);

        if (dias >= 365) {
            return "ALTA";
        } else if (dias >= 270) {
            return "MÉDIA";
        } else {
            return "BAIXA";
        }
    }
}
