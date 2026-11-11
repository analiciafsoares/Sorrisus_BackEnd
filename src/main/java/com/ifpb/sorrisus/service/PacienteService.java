package com.ifpb.sorrisus.service;

import com.ifpb.sorrisus.exception.*;
import com.ifpb.sorrisus.model.Paciente;
import com.ifpb.sorrisus.repository.PacienteRepository;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;
    private final Validator validator;

    public PacienteService(PacienteRepository pacienteRepository, Validator validator) {
        this.pacienteRepository = pacienteRepository;
        this.validator = validator;
    }

    private boolean cpfValido(String cpf) {
        return cpf != null
                && cpf.matches("\\d{11}")
                && validator.validateValue(Paciente.class, "cpf", cpf).isEmpty();
    }

    private String normalizarCpf(String cpf) {
        return cpf == null ? null : cpf.replaceAll("\\D", "");
    }

    public Paciente salvar(Paciente paciente) {

        paciente.setCpf(normalizarCpf(paciente.getCpf()));

        if (paciente.getEmail() == null || paciente.getEmail().isBlank()) {
            throw new InvalidFieldException("email", "O email é obrigatório.");
        }
        if (pacienteRepository.existsByEmail(paciente.getEmail())) {
            throw new EmailAlreadyExistsException(paciente.getEmail());
        }

        if (paciente.getCpf() == null) {
            throw new InvalidFieldException("cpf", "O CPF é obrigatório.");
        }

        if (!cpfValido(paciente.getCpf())) {
            throw new InvalidCPFException();
        }

        if (pacienteRepository.existsByCpf(paciente.getCpf())) {
            throw new CPFAlreadyExistsException(paciente.getCpf());
        }

        if (paciente.getDataNascimento() != null &&
                paciente.getDataNascimento().isAfter(LocalDate.now())) {
            throw new InvalidFieldException("dataNascimento", "A data de nascimento não pode ser futura.");
        }

        if (paciente.getTelefone() == null || paciente.getTelefone().isBlank()) {
            throw new InvalidFieldException("telefone", "O telefone é obrigatório.");
        }

        return pacienteRepository.save(paciente);
    }

    public Paciente buscarPorId(Long id) {
        return pacienteRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Paciente com id " + id + " não encontrado.")
        );
    }

    public List<Paciente> listarTodos() {
        return pacienteRepository.findAll();
    }

    public Paciente atualizar(Long id, Paciente atualizado) {
        Paciente existente = buscarPorId(id);
        String cpfNormalizado = normalizarCpf(atualizado.getCpf());

        if (!cpfValido(cpfNormalizado)) {
            throw new InvalidCPFException();
        }

        if (!Objects.equals(existente.getEmail(), atualizado.getEmail())
                && pacienteRepository.existsByEmail(atualizado.getEmail())) {
            throw new EmailAlreadyExistsException(atualizado.getEmail());
        }

        if (!Objects.equals(existente.getCpf(), cpfNormalizado)
                && pacienteRepository.existsByCpf(cpfNormalizado)) {
            throw new CPFAlreadyExistsException(cpfNormalizado);
        }

        existente.setNome(atualizado.getNome());
        existente.setCpf(cpfNormalizado);
        existente.setTelefone(atualizado.getTelefone());
        existente.setDataNascimento(atualizado.getDataNascimento());
        existente.setEmail(atualizado.getEmail());

        return pacienteRepository.save(existente);
    }

    public void deletar(Long id) {
        Paciente paciente = buscarPorId(id);
        pacienteRepository.delete(paciente);
    }
}
