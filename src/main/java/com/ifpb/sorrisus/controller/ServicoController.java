package com.ifpb.sorrisus.controller;

import com.ifpb.sorrisus.dto.ServicoDTO;
import com.ifpb.sorrisus.exception.ResourceNotFoundException;
import com.ifpb.sorrisus.model.Servico;
import com.ifpb.sorrisus.service.ServicoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/servicos")
public class ServicoController {

    private final ServicoService service;

    public ServicoController(ServicoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<ServicoDTO>> listar() {
        List<ServicoDTO> list = service.listarTodos().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ServicoDTO> buscarPorId(@PathVariable Long id) {
        Servico s = service.buscarPorId(id);
        return ResponseEntity.ok(toDTO(s));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('ROLE_DENTISTA', 'ROLE_RECEPCIONISTA')")
    public ResponseEntity<ServicoDTO> criar(@Valid @RequestBody ServicoDTO dto) {
        Servico s = new Servico();
        s.setNome(dto.getNome());
        s.setDescricao(dto.getDescricao());
        s.setPreco(dto.getPreco());
        s.setAtivo(dto.isAtivo());

        Servico salvo = service.salvar(s);

        return ResponseEntity.created(URI.create("/api/servicos/" + salvo.getId())).body(toDTO(salvo));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DENTISTA', 'ROLE_RECEPCIONISTA')")
    public ResponseEntity<ServicoDTO> atualizar(@PathVariable Long id, @Valid @RequestBody ServicoDTO dto) {
        Servico s = new Servico();
        s.setNome(dto.getNome());
        s.setDescricao(dto.getDescricao());
        s.setPreco(dto.getPreco());
        s.setAtivo(dto.isAtivo());

        Servico atualizado = service.atualizar(id, s);
        return ResponseEntity.ok(toDTO(atualizado));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('ROLE_DENTISTA', 'ROLE_RECEPCIONISTA')")
    public ResponseEntity<Void> desativar(@PathVariable Long id) {
        service.desativar(id);
        return ResponseEntity.noContent().build();
    }

    private ServicoDTO toDTO(Servico s) {
        ServicoDTO dto = new ServicoDTO();
        dto.setId(s.getId());
        dto.setNome(s.getNome());
        dto.setDescricao(s.getDescricao());
        dto.setPreco(s.getPreco());
        dto.setAtivo(s.isAtivo());
        return dto;
    }
}
