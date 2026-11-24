package br.com.sos.auth;

import lombok.Getter;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Usuário logado, guardado na sessão.
 * Nos controllers: {@code @AuthenticationPrincipal UsuarioAutenticado usuario}.
 */
@Getter
public class UsuarioAutenticado implements UserDetails, CredentialsContainer {

    private final Long id;
    private final String nomeUsuario;
    private final String login;
    private final Set<String> cargos;
    private String senhaHash;

    public UsuarioAutenticado(Usuario usuario) {
        this.id = usuario.getId();
        this.nomeUsuario = usuario.getNomeUsuario();
        this.login = usuario.getLogin();
        this.cargos = usuario.getCargos().stream().map(Cargo::getTipoCargo).collect(Collectors.toUnmodifiableSet());
        this.senhaHash = usuario.getSenhaHash();
    }

    public boolean temCargo(String tipoCargo) {
        return cargos.contains(tipoCargo);
    }

    /** Cargo ATENDENTE vira ROLE_ATENDENTE, usado em @PreAuthorize("hasRole('ATENDENTE')"). */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return cargos.stream().map(c -> new SimpleGrantedAuthority("ROLE_" + c)).toList();
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
