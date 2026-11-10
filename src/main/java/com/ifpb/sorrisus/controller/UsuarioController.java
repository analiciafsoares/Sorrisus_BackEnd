package com.ifpb.sorrisus.controller;

import com.ifpb.sorrisus.dto.AlterarSenhaDTO;
import com.ifpb.sorrisus.model.Usuario;
import com.ifpb.sorrisus.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService service;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioService service, PasswordEncoder passwordEncoder) {
        this.service = service;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<Usuario> listarTodos() {
        return service.listarTodos();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Usuario> buscarPorId(@PathVariable Long id) {
        Usuario usuario = service.buscarPorId(id);
        return ResponseEntity.ok(usuario);
    }

    @PostMapping("/cadastro")
    public ResponseEntity<Usuario> criar(@RequestBody Usuario usuario) {
        usuario.setSenha(passwordEncoder.encode(usuario.getSenha()));
        return ResponseEntity.ok(service.salvar(usuario));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Usuario> atualizar(@PathVariable Long id, @RequestBody Usuario usuario) {
        return ResponseEntity.ok(service.atualizar(id, usuario));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        service.deletar(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/me/senha")
    public ResponseEntity<Void> alterarSenha(@Valid @RequestBody AlterarSenhaDTO dto, Authentication authentication) {
        service.alterarSenha(authentication.getName(), dto.getSenhaAtual(), dto.getNovaSenha());
        return ResponseEntity.noContent().build();
    }
}
