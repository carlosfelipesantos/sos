package br.com.sos.os;

import br.com.sos.auth.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * Abertura e consulta de OS (RF002, RF006).
 * POST /os                           -> abre a OS (ATENDENTE)
 * GET  /os?numero=&cliente=&status=  -> lista com filtros (os dois; prestador vê só as dele)
 * GET  /os/{id}                      -> detalhe completo (os dois; prestador só das dele)
 * GET  /prestadores                  -> prestadores para escolher na abertura (ATENDENTE)
 */
@Tag(name = "Ordens de serviço", description = "Abertura e consulta de OS (RF002, RF006).")
@RestController
@RequiredArgsConstructor
public class OrdemServicoController {

    private final OrdemServicoService ordemServicoService;

    @Operation(summary = "Abre uma OS",
            description = "Status ABERTA, número e data de abertura gerados pelo sistema. "
                    + "400 sem descrição; 404 se o cliente ou o prestador não existir.")
    @PostMapping("/os")
    @PreAuthorize("hasRole('ATENDENTE')")
    public ResponseEntity<OrdemServicoResponse> abrir(@Valid @RequestBody AbrirOsRequest dados) {
        OrdemServico os = ordemServicoService.abrir(dados);
        URI endereco = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(os.getId()).toUri();
        return ResponseEntity.created(endereco).body(OrdemServicoResponse.de(os));
    }

    @Operation(summary = "Lista as OS com filtros",
            description = "Filtros opcionais e combináveis. O atendente vê todas; o prestador vê só as dele. "
                    + "Mais recentes primeiro.")
    @GetMapping("/os")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'PRESTADOR')")
    public List<OrdemServicoResponse> listar(
            @Parameter(description = "Número exato da OS")
            @RequestParam(name = "numero", required = false) Long numero,
            @Parameter(description = "Parte do nome ou do CPF/CNPJ do cliente")
            @RequestParam(name = "cliente", required = false) String cliente,
            @Parameter(description = "Status da OS")
            @RequestParam(name = "status", required = false) StatusOS status,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ordemServicoService.listar(numero, cliente, status, usuario).stream()
                .map(OrdemServicoResponse::de).toList();
    }

    @Operation(summary = "Detalhe completo da OS",
            description = "Cliente, prestador, todos os orçamentos (inclusive recusados) e o histórico em ordem de data. "
                    + "404 se não existir; 403 se for OS de outro prestador.")
    @GetMapping("/os/{id}")
    @PreAuthorize("hasAnyRole('ATENDENTE', 'PRESTADOR')")
    public OrdemServicoDetalheResponse detalhar(@PathVariable("id") Long id,
                                                @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ordemServicoService.detalhar(id, usuario);
    }

    @Operation(summary = "Lista os prestadores", description = "Para escolher o responsável na abertura da OS.")
    @GetMapping("/prestadores")
    @PreAuthorize("hasRole('ATENDENTE')")
    public List<PrestadorResponse> prestadores() {
        return ordemServicoService.listarPrestadores().stream().map(PrestadorResponse::de).toList();
    }
}
