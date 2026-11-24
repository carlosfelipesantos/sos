package br.com.sos.orcamento;

import br.com.sos.auth.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Orçamento da OS (RF003).
 * POST /os/{id}/orcamentos        -> gera orçamento (PRESTADOR)
 * POST /orcamentos/{id}/decisao   -> aprova ou recusa (ATENDENTE)
 */
@Tag(name = "Orçamentos", description = "Geração e decisão de orçamento (RF003).")
@RestController
@RequiredArgsConstructor
public class OrcamentoController {

    private final OrcamentoService orcamentoService;

    @Operation(summary = "Gera um orçamento para a OS",
            description = "A OS precisa estar Aberta. Somente o PRESTADOR usa. Retorna 201 com o orçamento pendente.")
    @PostMapping("/os/{id}/orcamentos")
    @PreAuthorize("hasRole('PRESTADOR')")
    public ResponseEntity<OrcamentoResponse> gerar(@PathVariable("id") Long id,
                                                  @Valid @RequestBody OrcamentoRequest dados,
                                                  @AuthenticationPrincipal UsuarioAutenticado usuario) {
        Orcamento orcamento = orcamentoService.gerar(id, dados, usuario);
        URI endereco = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/orcamentos/{id}").buildAndExpand(orcamento.getId()).toUri();
        return ResponseEntity.created(endereco).body(OrcamentoResponse.de(orcamento));
    }

    @Operation(summary = "Registra a decisão do cliente sobre o orçamento",
            description = "Aprovado: orçamento APROVADO e OS EM_EXECUCAO. Recusado: orçamento RECUSADO e OS volta a ABERTA. Somente o ATENDENTE usa.")
    @PostMapping("/orcamentos/{id}/decisao")
    @PreAuthorize("hasRole('ATENDENTE')")
    public ResponseEntity<OrcamentoResponse> decidir(@PathVariable("id") Long id,
                                                    @Valid @RequestBody DecisaoOrcamentoRequest dados,
                                                    @AuthenticationPrincipal UsuarioAutenticado usuario) {
        Orcamento orcamento = orcamentoService.decidir(id, dados.aprovado(), usuario);
        return ResponseEntity.ok(OrcamentoResponse.de(orcamento));
    }
}
