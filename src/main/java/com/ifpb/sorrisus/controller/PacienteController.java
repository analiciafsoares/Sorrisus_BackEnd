package com.ifpb.sorrisus.controller;

import com.ifpb.sorrisus.model.Paciente;
import com.ifpb.sorrisus.model.Role;
import com.ifpb.sorrisus.service.PacienteService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {

    private final PacienteService service;
    private final PasswordEncoder passwordEncoder;


    public PacienteController(PacienteService service, PasswordEncoder passwordEncoder) {
        this.service = service;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<Paciente> listarTodos() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Paciente> buscarPorId(@PathVariable Long id) {
        Paciente paciente = service.buscarPorId(id);
        return ResponseEntity.ok(paciente);
    }

    @PostMapping("/cadastro")
    @PreAuthorize("hasAnyAuthority('ROLE_DENTISTA', 'ROLE_RECEPCIONISTA')")
    public ResponseEntity<Paciente> criar(@RequestBody Paciente paciente) {
        paciente.setRole(Role.ROLE_PACIENTE);
        String senhaInicial = paciente.getSenha() == null || paciente.getSenha().isBlank()
                ? paciente.getCpf()
                : paciente.getSenha();
        paciente.setSenha(senhaInicial == null ? null : passwordEncoder.encode(senhaInicial));
        return ResponseEntity.ok(service.salvar(paciente));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Paciente> atualizar(@PathVariable Long id, @RequestBody Paciente paciente) {
        return ResponseEntity.ok(service.atualizar(id, paciente));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
