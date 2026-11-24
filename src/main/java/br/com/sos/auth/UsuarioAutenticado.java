package br.com.sos.auth;

import lombok.Getter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;

/**
 * Usuário logado, guardado na sessão.
 * Nos controllers: {@code @AuthenticationPrincipal UsuarioAutenticado usuario}.
 */
@Getter
public class UsuarioAutenticado implements UserDetails, CredentialsContainer {

    private final Long id;
    private final String nome;
    private final String login;
    private final Set<Papel> papeis;
    private String senhaHash;

    public UsuarioAutenticado(Usuario usuario) {
        this.id = usuario.getId();
        this.nome = usuario.getNome();
        this.login = usuario.getLogin();
        this.papeis = Set.copyOf(usuario.getPapeis());
        this.senhaHash = usuario.getSenhaHash();
    }

    public boolean temPapel(Papel papel) {
        return papeis.contains(papel);
    }

    /** Papel ATENDENTE vira ROLE_ATENDENTE, usado em @PreAuthorize("hasRole('ATENDENTE')"). */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return papeis.stream().map(p -> new SimpleGrantedAuthority("ROLE_" + p.name())).toList();
    }

    @Override
    public String getPassword() {
        return senhaHash;
    }

    @Override
    public String getUsername() {
        return login;
    }

    /** Remove o hash da memória depois do login, para não ficar na sessão. */
    @Override
    public void eraseCredentials() {
        this.senhaHash = null;
    }
}
