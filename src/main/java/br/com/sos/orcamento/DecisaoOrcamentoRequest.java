package br.com.sos.orcamento;

import jakarta.validation.constraints.NotNull;

/**
 * Decisão do cliente sobre um orçamento (RF003), registrada pelo Atendente.
 */
public record DecisaoOrcamentoRequest(
        @NotNull(message = "Informe a decisão do cliente.")
        Boolean aprovado) {
}
