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
    private final ServicoRepository servicoRepository;

    public AgendamentoService(AgendamentoRepository agendamentoRepository,
                              PacienteRepository pacienteRepository,
                              DentistaRepository dentistaRepository,
                              ConsultaRepository consultaRepository,
                              ServicoRepository servicoRepository) {
        this.agendamentoRepository = agendamentoRepository;
        this.pacienteRepository = pacienteRepository;
        this.dentistaRepository = dentistaRepository;
        this.consultaRepository = consultaRepository;
        this.servicoRepository = servicoRepository;
    }

    private void validarServicoDisponivel(Long servicoId, Long servicoAtualId) {
        if (servicoId == null || servicoId.equals(servicoAtualId)) {
            return;
        }

        Servico servico = servicoRepository.findById(servicoId)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado."));

        if (!servico.isAtivo()) {
            throw new BusinessException("Serviço inativo não pode ser usado em novos agendamentos.");
        }
    }

    private void validarConflito(Dentista dentista, LocalDateTime dataHora, Long agendamentoId) {
        boolean conflitoAgendamento;
        
        if (agendamentoId == null) {
            conflitoAgendamento = agendamentoRepository.existeAgendamentoAtivo(dentista, dataHora);
        } else {
            conflitoAgendamento = agendamentoRepository.existeOutroAgendamentoAtivo(dentista, dataHora, agendamentoId);
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

        Dentista dentista = dentistaRepository.findById(agendamento.getDentista().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Dentista não encontrado."));
        Paciente paciente = pacienteRepository.findById(agendamento.getPaciente().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado."));

        validarServicoDisponivel(agendamento.getServicoId(), null);
        
        validarConflito(dentista, agendamento.getDataHora(), null);

        agendamento.setDentista(dentista);
        agendamento.setPaciente(paciente);
        agendamento.setConfirmado(false);
        agendamento.setStatus(StatusAgendamento.PENDENTE);

        return agendamentoRepository.save(agendamento);
    }

    @Transactional
    public Agendamento atualizar(Long id, Agendamento atualizado) {
        Agendamento existente = buscarPorId(id);

        if (existente.getStatus() != StatusAgendamento.PENDENTE) {
            throw new BusinessException("Somente agendamentos pendentes podem ser editados.");
        }

        validarServicoDisponivel(atualizado.getServicoId(), existente.getServicoId());

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

        if (agendamento.getStatus() != StatusAgendamento.PENDENTE) {
            throw new BusinessException("Somente agendamentos pendentes podem ser confirmados.");
        }

        if (consultaRepository.existsByDentistaAndDataHoraAndStatusNot(
                agendamento.getDentista(), agendamento.getDataHora(), StatusConsulta.CANCELADA)) {
             throw new BusinessException("Já existe uma consulta registrada para este horário.");
        }

        agendamento.setConfirmado(true);
        agendamento.setStatus(StatusAgendamento.CONFIRMADO);
        Agendamento salvo = agendamentoRepository.save(agendamento);

        Consulta consulta = new Consulta();
        consulta.setDataHora(agendamento.getDataHora());
        consulta.setDentista(agendamento.getDentista());
        consulta.setPaciente(agendamento.getPaciente());
        consulta.setStatus(StatusConsulta.CONFIRMADA);
        consulta.setObservacao("Criada a partir do agendamento id=" + agendamento.getId());
        consulta.setAgendamento(salvo);

        consultaRepository.save(consulta);

        return salvo;
    }

    /**
     * Cancela o agendamento sem excluí-lo. Se ele já tiver sido confirmado, a consulta
     * gerada também é cancelada, desde que o atendimento ainda não tenha sido encerrado.
     */
    @Transactional
    public Agendamento cancelar(Long id) {
        Agendamento agendamento = buscarPorId(id);

        if (agendamento.getStatus() == StatusAgendamento.CANCELADO) {
            throw new BusinessException("Agendamento já cancelado.");
        }

        if (agendamento.getStatus() == StatusAgendamento.CONFIRMADO) {
            consultaRepository.findByAgendamento(agendamento).ifPresent(consulta -> {
                if (consulta.getStatus() == StatusConsulta.REALIZADA
                        || consulta.getStatus() == StatusConsulta.NAO_COMPARECEU) {
                    throw new BusinessException(
                            "Não é possível cancelar: o atendimento deste agendamento já foi encerrado.");
                }
                consulta.setStatus(StatusConsulta.CANCELADA);
                consultaRepository.save(consulta);
            });
        }

        agendamento.setStatus(StatusAgendamento.CANCELADO);
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
