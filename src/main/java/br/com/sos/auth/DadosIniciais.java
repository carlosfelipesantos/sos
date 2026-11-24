package br.com.sos.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

/** Cria os cargos e os usuários de teste na primeira vez que o sistema sobe. Todos com senha 123. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DadosIniciais implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final CargoRepository cargoRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        Cargo atendente = cargo(Cargo.ATENDENTE);
        Cargo prestador = cargo(Cargo.PRESTADOR);

        criar("atendente", "Atendente Teste", Usuario.TIPO_PESSOA, Set.of(atendente));
        criar("prestador", "Prestador Teste", Usuario.TIPO_PESSOA, Set.of(prestador));
        criar("prestador2", "Equipe de Manutenção", Usuario.TIPO_EQUIPE, Set.of(prestador));
        criar("autonomo", "Prestador Autônomo", Usuario.TIPO_PESSOA, Set.of(atendente, prestador));
    }

    private Cargo cargo(String tipoCargo) {
        return cargoRepository.findByTipoCargo(tipoCargo)
                .orElseGet(() -> cargoRepository.save(new Cargo(tipoCargo)));
    }

    private void criar(String login, String nome, String tipoUsuario, Set<Cargo> cargos) {
        if (usuarioRepository.existsByLogin(login)) {
            return;
        }
        Usuario u = new Usuario();
        u.setLogin(login);
        u.setNomeUsuario(nome);
        u.setTipoUsuario(tipoUsuario);
        u.setEmail(login + "@sos.local");
        u.setSenhaHash(passwordEncoder.encode("123"));
        u.getCargos().addAll(cargos);
        usuarioRepository.save(u);
        log.info("Usuário de teste criado: {} / 123", login);
    }
}
