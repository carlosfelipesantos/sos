package br.com.sos.cliente;

/** Cliente devolvido para o front. O CPF/CNPJ vem só com números; a máscara fica por conta da tela. */
public record ClienteResponse(Long id, String nome, String cpfCnpj, String telefone, String email, boolean ativo) {

    public static ClienteResponse de(Cliente c) {
        return new ClienteResponse(c.getId(), c.getNomeCliente(), c.getCpf(), c.getTelefone(), c.getEmail(),
                c.isStatusCliente());
    }
}
