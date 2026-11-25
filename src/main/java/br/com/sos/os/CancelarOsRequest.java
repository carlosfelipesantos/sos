package br.com.sos.os;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cancelamento da OS pelo atendente (RF005). O motivo é obrigatório e fica gravado no histórico.
 */
public record CancelarOsRequest(
        @NotBlank(message = "Informe o motivo do cancelamento.")
        @Size(max = 2000, message = "O motivo pode ter no máximo 2000 caracteres.")
        String motivo) {
}
