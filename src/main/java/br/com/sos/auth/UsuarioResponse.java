package br.com.sos.auth;

import java.util.Set;

public record UsuarioResponse(Long id, String nomeUsuario, String login, Set<String> cargos) {

    public static UsuarioResponse de(UsuarioAutenticado u) {
        return new UsuarioResponse(u.getId(), u.getNomeUsuario(), u.getLogin(), u.getCargos());
    }
}
