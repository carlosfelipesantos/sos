package br.com.sos.orcamento;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Dados para gerar um orçamento (RF003).
 * O Prestador informa a descrição do serviço, o valor e o prazo em dias.
 */
public record OrcamentoRequest(
        @NotBlank(message = "A descrição do serviço é obrigatória.")
        @Size(max = 2000, message = "A descrição pode ter no máximo 2000 caracteres.")
        String descricao,

        @NotNull(message = "O valor do orçamento deve ser maior que zero.")
        BigDecimal valor,

        @NotNull(message = "O prazo deve ser informado.")
        Integer prazoDias) {
}
