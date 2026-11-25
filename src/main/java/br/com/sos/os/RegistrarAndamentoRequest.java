package br.com.sos.os;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Andamento da OS registrado pelo prestador responsável (RF004).
 * status é opcional: sem status, grava só a observação; com PRONTA, conclui o serviço.
 */
public record RegistrarAndamentoRequest(
        @NotBlank(message = "Descreva o andamento do serviço.")
        @Size(max = 2000, message = "A observação pode ter no máximo 2000 caracteres.")
        String observacao,

        StatusOS status) {
}
