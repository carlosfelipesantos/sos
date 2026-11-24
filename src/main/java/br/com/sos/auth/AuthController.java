package br.com.sos.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

/**
 * POST /auth/login  -> faz login e cria a sessão (cookie JSESSIONID)
 * POST /auth/logout -> encerra a sessão (tratado pelo SecurityConfig, responde 204)
 * GET  /auth/me     -> dados e papéis do usuário logado
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    @PostMapping("/login")
    public UsuarioResponse login(@Valid @RequestBody LoginRequest dados,
                                 HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(dados.login(), dados.senha()));

        if (request.getSession(false) != null) {
            request.changeSessionId(); // evita fixação de sessão
        }
        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(auth);
        SecurityContextHolder.setContext(contexto);
        securityContextRepository.saveContext(contexto, request, response);

        return UsuarioResponse.de((UsuarioAutenticado) auth.getPrincipal());
    }

    @GetMapping("/me")
    public UsuarioResponse me(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return UsuarioResponse.de(usuario);
    }
}
