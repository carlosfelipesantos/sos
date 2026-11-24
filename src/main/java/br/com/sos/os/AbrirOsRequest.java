package br.com.sos.os;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Dados para abrir uma OS (RF002).
 * A descrição é obrigatória; cliente e prestador precisam existir (conferido no OrdemServicoService).
 */
public record AbrirOsRequest(
        @NotNull(message = "Selecione o cliente.")
        Long clienteId,

        @NotBlank(message = "A descrição do serviço é obrigatória.")
        @Size(max = 2000, message = "A descrição pode ter no máximo 2000 caracteres.")
        String descricao,

        @NotNull(message = "Selecione o prestador responsável.")
        Long prestadorId) {
}
