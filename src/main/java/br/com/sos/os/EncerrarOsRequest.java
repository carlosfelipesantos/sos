package br.com.sos.os;

import jakarta.validation.constraints.Size;

/**
 * Encerramento da OS pelo atendente (RF005). A observação é opcional (ex.: "Cliente retirou o equipamento").
 */
public record EncerrarOsRequest(
        @Size(max = 2000, message = "A observação pode ter no máximo 2000 caracteres.")
        String observacao) {
}
