package com.ifpb.sorrisus.service;

import com.ifpb.sorrisus.exception.*;
import com.ifpb.sorrisus.model.*;
import com.ifpb.sorrisus.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AgendamentoService {

    private final AgendamentoRepository agendamentoRepository;
    private final PacienteRepository pacienteRepository;
    private final DentistaRepository dentistaRepository;
    private final ConsultaRepository consultaRepository;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
            PacienteRepository pacienteRepository,
            DentistaRepository dentistaRepository,
            ConsultaRepository consultaRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.pacienteRepository = pacienteRepository;
        this.dentistaRepository = dentistaRepository;
        this.consultaRepository = consultaRepository;
    }

    private void validarConflito(Dentista dentista, LocalDateTime dataHora, Long agendamentoId) {
        boolean conflitoAgendamento;

        if (agendamentoId == null) {
            conflitoAgendamento = agendamentoRepository.existsByDentistaAndDataHoraAndStatusNot(
                    dentista, dataHora, StatusConsulta.CANCELADA);
        } else {
            conflitoAgendamento = agendamentoRepository.existsByDentistaAndDataHoraAndIdNotAndStatusNot(
                    dentista, dataHora, agendamentoId, StatusConsulta.CANCELADA);
        }

        boolean conflitoConsulta = consultaRepository.existsByDentistaAndDataHoraAndStatusNot(
                dentista, dataHora, StatusConsulta.CANCELADA);

        if (conflitoAgendamento || conflitoConsulta) {
            throw new BusinessException("Dentista já possui compromisso (Agendamento ou Consulta) neste horário.");
        }
    }

    @Transactional
    public Agendamento criar(Agendamento agendamento) {
        if (agendamento.getPaciente() == null || agendamento.getDentista() == null) {
            throw new InvalidFieldException("paciente/dentista", "Paciente e Dentista são obrigatórios.");
        }
        if (agendamento.getDataHora() == null) {
            throw new InvalidFieldException("dataHora", "Data e hora são obrigatórias.");
        }
        if (agendamento.getDataHora().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Não é permitido criar agendamento em data ou horário passado.");
        }

        Dentista dentista = dentistaRepository.findById(agendamento.getDentista().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Dentista não encontrado."));
        Paciente paciente = pacienteRepository.findById(agendamento.getPaciente().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado."));

        validarConflito(dentista, agendamento.getDataHora(), null);

        agendamento.setDentista(dentista);
        agendamento.setPaciente(paciente);
        agendamento.setConfirmado(false);
        agendamento.setStatus(StatusConsulta.PENDENTE);

        return agendamentoRepository.save(agendamento);
    }

    @Transactional
    public Agendamento atualizar(Long id, Agendamento atualizado) {
        Agendamento existente = buscarPorId(id);

        if (existente.isConfirmado() || existente.getStatus() == StatusConsulta.CONFIRMADA) {
            throw new BusinessException("Não é permitido editar agendamento já confirmado.");
        }
        if (existente.getStatus() == StatusConsulta.CANCELADA) {
            throw new BusinessException("Não é permitido editar agendamento cancelado.");
        }
        if (atualizado.getDataHora() != null && atualizado.getDataHora().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Não é permitido reagendar para data ou horário passado.");
        }

        Dentista dentista = dentistaRepository.findById(atualizado.getDentista().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Dentista não encontrado."));
        Paciente paciente = pacienteRepository.findById(atualizado.getPaciente().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado."));

        if (!existente.getDataHora().equals(atualizado.getDataHora()) || !existente.getDentista().equals(dentista)) {
            validarConflito(dentista, atualizado.getDataHora(), id);
        }

        existente.setDataHora(atualizado.getDataHora());
        existente.setDentista(dentista);
        existente.setPaciente(paciente);
        existente.setObservacao(atualizado.getObservacao());
        existente.setServicoId(atualizado.getServicoId());

        return agendamentoRepository.save(existente);
    }

    @Transactional
    public Agendamento confirmar(Long id) {
        Agendamento agendamento = buscarPorId(id);

        if (agendamento.getStatus() == StatusConsulta.CANCELADA) {
            throw new BusinessException("Não é possível confirmar um agendamento cancelado.");
        }
        if (agendamento.isConfirmado() || agendamento.getStatus() == StatusConsulta.CONFIRMADA) {
            throw new BusinessException("Agendamento já confirmado.");
        }

        if (consultaRepository.existsByDentistaAndDataHoraAndStatusNot(
                agendamento.getDentista(), agendamento.getDataHora(), StatusConsulta.CANCELADA)) {
            throw new BusinessException("Já existe uma consulta registrada para este horário.");
        }

        agendamento.setConfirmado(true);
        agendamento.setStatus(StatusConsulta.CONFIRMADA);
        Agendamento salvo = agendamentoRepository.save(agendamento);

        Consulta consulta = new Consulta();
        consulta.setDataHora(agendamento.getDataHora());
        consulta.setDentista(agendamento.getDentista());
        consulta.setPaciente(agendamento.getPaciente());
        consulta.setStatus(StatusConsulta.CONFIRMADA);
        consulta.setObservacao("Criada a partir do agendamento id=" + agendamento.getId());

        consultaRepository.save(consulta);

        return salvo;
    }

    @Transactional
    public Agendamento cancelar(Long id) {
        Agendamento agendamento = buscarPorId(id);

        if (agendamento.getStatus() == StatusConsulta.CANCELADA) {
            throw new BusinessException("Agendamento já está cancelado.");
        }

        List<Consulta> consultas = consultaRepository.findByDentistaAndDataHoraBetween(
                agendamento.getDentista(),
                agendamento.getDataHora(),
                agendamento.getDataHora());
        for (Consulta c : consultas) {
            if (c.getPaciente().getId().equals(agendamento.getPaciente().getId())
                    && c.getStatus() != StatusConsulta.CANCELADA) {
                if (c.getStatus() == StatusConsulta.REALIZADA) {
                    throw new BusinessException(
                            "Não é possível cancelar um agendamento cuja consulta já foi realizada.");
                }
                c.setStatus(StatusConsulta.CANCELADA);
                consultaRepository.save(c);
            }
        }

        agendamento.setConfirmado(false);
        agendamento.setStatus(StatusConsulta.CANCELADA);

        return agendamentoRepository.save(agendamento);
    }

    public Agendamento buscarPorId(Long id) {
        return agendamentoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agendamento não encontrado."));
    }

    @Transactional
    public void deletar(Long id) {
        Agendamento agendamento = buscarPorId(id);
        agendamentoRepository.delete(agendamento);
    }

    public List<Agendamento> listarPorPaciente(Long pacienteId) {
        Paciente paciente = pacienteRepository.findById(pacienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado."));
        return agendamentoRepository.findByPaciente(paciente);
    }

    public List<Agendamento> listarPorDentista(Long dentistaId) {
        Dentista dentista = dentistaRepository.findById(dentistaId)
                .orElseThrow(() -> new ResourceNotFoundException("Dentista não encontrado."));
        return agendamentoRepository.findByDentista(dentista);
    }
}