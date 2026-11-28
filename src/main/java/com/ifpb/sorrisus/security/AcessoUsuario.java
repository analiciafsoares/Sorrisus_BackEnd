package com.ifpb.sorrisus.security;

import com.ifpb.sorrisus.repository.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("acessoUsuario")
public class AcessoUsuario {

    private final UsuarioRepository usuarioRepository;

    public AcessoUsuario(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /** Indica se o usuário autenticado é o dono do cadastro com o id informado. */
    public boolean ehOProprio(Long id, Authentication authentication) {
        if (id == null || authentication == null || authentication.getName() == null) {
            return false;
        }
        return usuarioRepository.findById(id)
                .map(usuario -> authentication.getName().equalsIgnoreCase(usuario.getEmail()))
                .orElse(false);
    }
}
