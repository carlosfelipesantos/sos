package br.com.sos.os;

import br.com.sos.auth.Usuario;

/** Prestador para o select da abertura de OS (GET /prestadores) e para mostrar o responsável pela OS. */
public record PrestadorResponse(Long id, String nome, String tipoUsuario) {

    public static PrestadorResponse de(Usuario u) {
        return new PrestadorResponse(u.getId(), u.getNomeUsuario(), u.getTipoUsuario());
    }
}
