package com.ifpb.sorrisus.service;

import com.ifpb.sorrisus.exception.ResourceNotFoundException;
import com.ifpb.sorrisus.model.Servico;
import com.ifpb.sorrisus.repository.ServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ServicoService {

    private final ServicoRepository servicoRepository;

    public ServicoService(ServicoRepository servicoRepository) {
        this.servicoRepository = servicoRepository;
    }

    public List<Servico> listarTodos() {
        return servicoRepository.findAll();
    }

    public List<Servico> listarAtivos() {
        return servicoRepository.findByAtivoTrue();
    }

    public Servico buscarPorId(Long id) {
        return servicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado."));
    }

    @Transactional
    public Servico salvar(Servico servico) {
        if (servico.getId() == null) {
            servico.setAtivo(true);
        }
        return servicoRepository.save(servico);
    }

    @Transactional
    public Servico atualizar(Long id, Servico atualizado) {
        Servico existente = buscarPorId(id);

        existente.setNome(atualizado.getNome());
        existente.setDescricao(atualizado.getDescricao());
        existente.setPreco(atualizado.getPreco());
        existente.setAtivo(atualizado.isAtivo());

        return servicoRepository.save(existente);
    }

    @Transactional
    public void desativar(Long id) {
        Servico servico = buscarPorId(id);
        servico.setAtivo(false);
        servicoRepository.save(servico);
    }
}
