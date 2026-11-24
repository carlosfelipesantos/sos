package br.com.sos.auth;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByLogin(String login);

    boolean existsByLogin(String login);

    /** Ex.: findByPapeisContainingOrderByNome(Papel.PRESTADOR) para o GET /prestadores. */
    List<Usuario> findByPapeisContainingOrderByNome(Papel papel);
}
