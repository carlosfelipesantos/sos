package br.com.sos.os;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

/**
 * Andamento e encerramento da OS (RF004, RF005).
 * POST /os/{id}/andamentos    -> registra o andamento; com status PRONTA conclui o serviço (PRESTADOR responsável)
 * POST /os/{id}/encerramento  -> encerra a OS pronta (ATENDENTE)
 * POST /os/{id}/cancelamento  -> cancela a OS informando o motivo (ATENDENTE)
 * O histórico gravado aparece no GET /os/{id}.
 */
@Tag(name = "Andamento da OS", description = "Andamento, encerramento e cancelamento da OS (RF004, RF005).")
@RestController
@RequiredArgsConstructor
public class AndamentoController {

    private final AndamentoService andamentoService;

    @Operation(summary = "Registra o andamento da OS",
            description = "Somente o prestador responsável, com a OS EM_EXECUCAO. Sem status grava só a observação; "
                    + "com status PRONTA conclui o serviço. 400 sem observação, status diferente de PRONTA ou OS fora de execução; "
                    + "403 se a OS for de outro prestador; 404 se não existir.")
    @PostMapping("/os/{id}/andamentos")
    @PreAuthorize("hasRole('PRESTADOR')")
    public ResponseEntity<OrdemServicoDetalheResponse.HistoricoItem> registrarAndamento(
            @PathVariable("id") Long id, @Valid @RequestBody RegistrarAndamentoRequest dados) {
        Historico historico = andamentoService.registrarAndamento(id, dados);
        URI endereco = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/os/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(endereco).body(OrdemServicoDetalheResponse.HistoricoItem.de(historico));
    }

    @Operation(summary = "Encerra a OS",
            description = "Somente OS PRONTA. Status ENCERRADA e data de finalização preenchida. Observação opcional. "
                    + "400 se a OS não estiver pronta ou já tiver sido encerrada/cancelada; 404 se não existir.")
    @PostMapping("/os/{id}/encerramento")
    @PreAuthorize("hasRole('ATENDENTE')")
    public OrdemServicoResponse encerrar(@PathVariable("id") Long id,
                                         @Valid @RequestBody(required = false) EncerrarOsRequest dados) {
        return OrdemServicoResponse.de(andamentoService.encerrar(id, dados));
    }

    @Operation(summary = "Cancela a OS",
            description = "Qualquer status que ainda não seja ENCERRADA ou CANCELADA. Motivo obrigatório, gravado no histórico. "
                    + "Status CANCELADA e data de finalização preenchida. 400 sem motivo ou OS já finalizada; 404 se não existir.")
    @PostMapping("/os/{id}/cancelamento")
    @PreAuthorize("hasRole('ATENDENTE')")
    public OrdemServicoResponse cancelar(@PathVariable("id") Long id, @Valid @RequestBody CancelarOsRequest dados) {
        return OrdemServicoResponse.de(andamentoService.cancelar(id, dados));
    }
}
