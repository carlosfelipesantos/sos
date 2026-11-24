package br.com.sos.auth;

import br.com.sos.comum.NaoEncontradoException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Para os services pegarem o usuário logado como entidade (ex.: gravar no Historico).
 * Uso: {@code Usuario usuario = usuarioLogadoService.atual();}
 */
@Service
@RequiredArgsConstructor
public class UsuarioLogadoService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioAutenticado autenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UsuarioAutenticado usuario)) {
            throw new IllegalStateException("Nenhum usuário logado");
        }
        return usuario;
    }

    public Usuario atual() {
        return usuarioRepository.findById(autenticado().getId())
                .orElseThrow(() -> new NaoEncontradoException("Usuário logado não encontrado."));
    }
}
