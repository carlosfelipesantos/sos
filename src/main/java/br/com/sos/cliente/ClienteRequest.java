package br.com.sos.cliente;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados para cadastrar ou editar um cliente (RF001).
 * Nome, CPF/CNPJ e telefone são obrigatórios; o e-mail é opcional, mas, se vier, precisa ser válido.
 * O CPF/CNPJ pode vir com ou sem máscara; os dígitos verificadores são conferidos no ClienteService.
 */
public record ClienteRequest(
        @NotBlank(message = "Informe o nome do cliente.")
        @Size(max = 255, message = "O nome pode ter no máximo 255 caracteres.")
        String nome,

        @NotBlank(message = "Informe o CPF ou o CNPJ do cliente.")
        @Size(max = 20, message = "CPF ou CNPJ inválido. Confira os números digitados.")
        String cpfCnpj,

        @NotBlank(message = "Informe o telefone do cliente.")
        @Size(max = 30, message = "O telefone pode ter no máximo 30 caracteres.")
        String telefone,

        @Email(message = "E-mail inválido. Use o formato nome@exemplo.com ou deixe em branco.")
        @Size(max = 255, message = "O e-mail pode ter no máximo 255 caracteres.")
        String email) {
}
