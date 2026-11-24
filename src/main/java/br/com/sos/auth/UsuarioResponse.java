package br.com.sos.auth;

import java.util.Set;

public record UsuarioResponse(Long id, String nome, String login, Set<Papel> papeis) {

    public static UsuarioResponse de(UsuarioAutenticado u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getLogin(), u.getPapeis());
    }
}
