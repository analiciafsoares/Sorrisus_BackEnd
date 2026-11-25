package com.ifpb.sorrisus.config;

import com.ifpb.sorrisus.model.Dentista;
import com.ifpb.sorrisus.model.Role;
import com.ifpb.sorrisus.repository.DentistaRepository;
import com.ifpb.sorrisus.service.DentistaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DentistaInicialSeeder implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DentistaInicialSeeder.class);

    private final DentistaRepository dentistaRepository;
    private final DentistaService dentistaService;
    private final PasswordEncoder passwordEncoder;
    private final String nome;
    private final String email;
    private final String senha;
    private final String cro;
    private final String especialidade;

    public DentistaInicialSeeder(DentistaRepository dentistaRepository,
                                 DentistaService dentistaService,
                                 PasswordEncoder passwordEncoder,
                                 @Value("${sorrisus.dentista-inicial.nome:}") String nome,
                                 @Value("${sorrisus.dentista-inicial.email:}") String email,
                                 @Value("${sorrisus.dentista-inicial.senha:}") String senha,
                                 @Value("${sorrisus.dentista-inicial.cro:}") String cro,
                                 @Value("${sorrisus.dentista-inicial.especialidade:}") String especialidade) {
        this.dentistaRepository = dentistaRepository;
        this.dentistaService = dentistaService;
        this.passwordEncoder = passwordEncoder;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
        this.cro = cro;
        this.especialidade = especialidade;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (dentistaRepository.count() > 0) {
            return;
        }
        if (vazio(nome) || vazio(email) || vazio(senha) || vazio(cro) || vazio(especialidade)) {
            logger.warn("Nenhum dentista cadastrado. Configure as variáveis DENTISTA_INICIAL_NOME, "
                    + "DENTISTA_INICIAL_EMAIL, DENTISTA_INICIAL_SENHA, DENTISTA_INICIAL_CRO e "
                    + "DENTISTA_INICIAL_ESPECIALIDADE para criar a conta inicial.");
            return;
        }
        if (senha.length() < 6) {
            logger.error("Dentista inicial não criado: DENTISTA_INICIAL_SENHA deve ter pelo menos 6 caracteres.");
            return;
        }

        Dentista dentista = new Dentista();
        dentista.setNome(nome);
        dentista.setEmail(email);
        dentista.setCro(cro);
        dentista.setEspecialidade(especialidade);
        dentista.setRole(Role.ROLE_DENTISTA);
        dentista.setSenha(passwordEncoder.encode(senha));

        try {
            dentistaService.salvar(dentista);
            logger.info("Conta do dentista inicial criada. Recomenda-se alterar a senha no primeiro acesso.");
        } catch (RuntimeException e) {
            logger.error("Dentista inicial não criado: {}", e.getMessage());
        }
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }
}
