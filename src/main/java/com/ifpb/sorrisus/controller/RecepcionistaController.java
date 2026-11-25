package com.ifpb.sorrisus.controller;

import com.ifpb.sorrisus.exception.InvalidFieldException;
import com.ifpb.sorrisus.model.Recepcionista;
import com.ifpb.sorrisus.model.Role;
import com.ifpb.sorrisus.service.RecepcionistaService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/recepcionistas")
public class RecepcionistaController {

    private final RecepcionistaService service;
    private final PasswordEncoder passwordEncoder;

    public RecepcionistaController(RecepcionistaService service, PasswordEncoder passwordEncoder) {
        this.service = service;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<Recepcionista> listarTodos() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Recepcionista> buscarPorId(@PathVariable Long id) {
        Recepcionista recepcionista = service.buscarPorId(id);
        return ResponseEntity.ok(recepcionista);
    }

    @PostMapping("/cadastro")
    @PreAuthorize("hasAuthority('ROLE_DENTISTA')")
    public ResponseEntity<Recepcionista> criar(@RequestBody Recepcionista recepcionista) {
        recepcionista.setRole(Role.ROLE_RECEPCIONISTA);
        if (recepcionista.getSenha() == null || recepcionista.getSenha().length() < 6) {
            throw new InvalidFieldException("senha", "Informe uma senha provisória com pelo menos 6 caracteres.");
        }
        recepcionista.setSenha(passwordEncoder.encode(recepcionista.getSenha()));
        return ResponseEntity.ok(service.salvar(recepcionista));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Recepcionista> atualizar(@PathVariable Long id, @RequestBody Recepcionista recepcionista) {
        return ResponseEntity.ok(service.atualizar(id, recepcionista));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_DENTISTA')")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }
}
