package com.ifpb.sorrisus.service;

import com.ifpb.sorrisus.exception.BusinessException;
import com.ifpb.sorrisus.exception.EmailAlreadyExistsException;
import com.ifpb.sorrisus.exception.ResourceNotFoundException;
import com.ifpb.sorrisus.exception.InvalidFieldException;
import com.ifpb.sorrisus.model.Usuario;
import com.ifpb.sorrisus.repository.UsuarioRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario salvar(Usuario usuario) {

        if (usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            throw new InvalidFieldException("email", "O e-mail é obrigatório.");
        }

        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
            throw new EmailAlreadyExistsException(usuario.getEmail());
        }

        if (usuario.getRole() == null) {
            throw new InvalidFieldException("role", "A role é obrigatória.");
        }

        return usuarioRepository.save(usuario);
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Usuário com id " + id + " não encontrado.")
        );
    }

    public Usuario buscarPorEmail(String email) {
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário com email " + email + " não encontrado."));
    }

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public Usuario atualizar(Long id, Usuario usuarioAtualizado) {
        Usuario existente = buscarPorId(id);

        if (!existente.getEmail().equals(usuarioAtualizado.getEmail())
                && usuarioRepository.existsByEmail(usuarioAtualizado.getEmail())) {
            throw new EmailAlreadyExistsException(usuarioAtualizado.getEmail());
        }

        existente.setNome(usuarioAtualizado.getNome());
        existente.setEmail(usuarioAtualizado.getEmail());
        existente.setSenha(usuarioAtualizado.getSenha());
        existente.setRole(usuarioAtualizado.getRole());

        return usuarioRepository.save(existente);
    }

    public void alterarSenha(String email, String senhaAtual, String novaSenha) {
        Usuario usuario = buscarPorEmail(email);

        if (!passwordEncoder.matches(senhaAtual, usuario.getSenha())) {
            throw new BusinessException("A senha atual está incorreta.");
        }
        if (passwordEncoder.matches(novaSenha, usuario.getSenha())) {
            throw new BusinessException("A nova senha deve ser diferente da senha atual.");
        }

        usuario.setSenha(passwordEncoder.encode(novaSenha));
        usuarioRepository.save(usuario);
    }

    public void deletar(Long id) {
        Usuario usuario = buscarPorId(id);
        usuarioRepository.delete(usuario);
    }
}
