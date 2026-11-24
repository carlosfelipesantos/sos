package br.com.sos.os;

import br.com.sos.cliente.Cliente;

import java.time.LocalDateTime;

/** OS resumida: resposta da abertura (POST /os) e de cada item da lista (GET /os). */
public record OrdemServicoResponse(
        Long id,
        Long numero,
        String descricao,
        StatusOS status,
        LocalDateTime dataAbertura,
        LocalDateTime dataFinalizacao,
        ClienteResumo cliente,
        PrestadorResponse prestador) {

    public record ClienteResumo(Long id, String nome, String cpfCnpj) {

        static ClienteResumo de(Cliente c) {
            return new ClienteResumo(c.getId(), c.getNomeCliente(), c.getCpf());
        }
    }

    public static OrdemServicoResponse de(OrdemServico os) {
        return new OrdemServicoResponse(os.getId(), os.getNumero(), os.getTipoServico(), os.getStatus(),
                os.getDataAbertura(), os.getDataFinalizacao(),
                ClienteResumo.de(os.getCliente()), PrestadorResponse.de(os.getPrestador()));
    }
}
