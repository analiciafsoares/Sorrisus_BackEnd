package com.ifpb.sorrisus.controller;

import com.ifpb.sorrisus.exception.InvalidFieldException;
import com.ifpb.sorrisus.model.Dentista;
import com.ifpb.sorrisus.model.Role;
import com.ifpb.sorrisus.service.DentistaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dentistas")
public class DentistaController {

    private final DentistaService service;
    private final PasswordEncoder passwordEncoder;

    public DentistaController(DentistaService service, PasswordEncoder passwordEncoder) {
        this.service = service;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<Dentista> listarTodos() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Dentista> buscarPorId(@PathVariable Long id) {
        Dentista dentista = service.buscarPorId(id);
        return ResponseEntity.ok(dentista);
    }

    @PostMapping("/cadastro")
    @PreAuthorize("hasAuthority('ROLE_DENTISTA')")
    public ResponseEntity<Dentista> criar(@RequestBody Dentista dentista) {
        dentista.setRole(Role.ROLE_DENTISTA);
        if (dentista.getSenha() == null || dentista.getSenha().length() < 6) {
            throw new InvalidFieldException("senha", "Informe uma senha provisória com pelo menos 6 caracteres.");
        }
        dentista.setSenha(passwordEncoder.encode(dentista.getSenha()));
        return ResponseEntity.ok(service.salvar(dentista));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Dentista> atualizar(@PathVariable Long id, @RequestBody Dentista dentista) {
        return ResponseEntity.ok(service.atualizar(id, dentista));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DENTISTA')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
