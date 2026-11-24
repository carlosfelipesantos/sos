package br.com.sos.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Cria os usuários de teste na primeira vez que o sistema sobe. Todos com senha 123. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DadosIniciais implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        criar("atendente", "Atendente Teste", Set.of(Papel.ATENDENTE));
        criar("prestador", "Prestador Teste", Set.of(Papel.PRESTADOR));
        criar("prestador2", "Segundo Prestador", Set.of(Papel.PRESTADOR));
        criar("autonomo", "Prestador Autônomo", Set.of(Papel.ATENDENTE, Papel.PRESTADOR));
    }

    private void criar(String login, String nome, Set<Papel> papeis) {
        if (usuarioRepository.existsByLogin(login)) {
            return;
        }
        Usuario u = new Usuario();
        u.setLogin(login);
        u.setNome(nome);
        u.setSenhaHash(passwordEncoder.encode("123"));
        u.getPapeis().addAll(papeis);
        usuarioRepository.save(u);
        log.info("Usuário de teste criado: {} / 123 ({})", login, papeis);
    }
}
