package br.com.sos.os;

import br.com.sos.auth.Usuario;
import br.com.sos.cliente.ClienteResponse;
import br.com.sos.orcamento.Orcamento;
import br.com.sos.orcamento.StatusOrcamento;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Detalhe completo da OS (GET /os/{id}, RF006): cliente, prestador,
 * todos os orçamentos (inclusive os recusados) e o histórico em ordem de data.
 */
public record OrdemServicoDetalheResponse(
        Long id,
        Long numero,
        String descricao,
        StatusOS status,
        LocalDateTime dataAbertura,
        LocalDateTime dataFinalizacao,
        ClienteResponse cliente,
        PrestadorResponse prestador,
        List<OrcamentoItem> orcamentos,
        List<HistoricoItem> historico) {

    /** Usuário que fez um registro (decisão de orçamento ou histórico). */
    public record UsuarioResumo(Long id, String nome) {

        static UsuarioResumo de(Usuario u) {
            return u == null ? null : new UsuarioResumo(u.getId(), u.getNomeUsuario());
        }
    }

    public record OrcamentoItem(
            Long id,
            String descricao,
            BigDecimal valor,
            Integer prazoDias,
            StatusOrcamento status,
            LocalDateTime dataCriacao,
            LocalDateTime dataDecisao,
            UsuarioResumo decididoPor) {

        static OrcamentoItem de(Orcamento o) {
            return new OrcamentoItem(o.getId(), o.getDescricao(), o.getValorOrcamento(), o.getPrazoDias(),
                    o.getStatus(), o.getDataCriacao(), o.getDataDecisao(), UsuarioResumo.de(o.getDecididoPor()));
        }
    }

    /** statusAnterior e statusNovo ficam nulos quando o registro é só uma observação de andamento. */
    public record HistoricoItem(
            Long id,
            String observacao,
            StatusOS statusAnterior,
            StatusOS statusNovo,
            LocalDateTime dataHora,
            UsuarioResumo usuario) {

        static HistoricoItem de(Historico h) {
            return new HistoricoItem(h.getId(), h.getDescricao(), h.getStatusAnterior(), h.getStatusNovo(),
                    h.getDataHistorico(), UsuarioResumo.de(h.getUsuario()));
        }
    }

    public static OrdemServicoDetalheResponse de(OrdemServico os, List<Orcamento> orcamentos, List<Historico> historico) {
        return new OrdemServicoDetalheResponse(os.getId(), os.getNumero(), os.getTipoServico(), os.getStatus(),
                os.getDataAbertura(), os.getDataFinalizacao(),
                ClienteResponse.de(os.getCliente()), PrestadorResponse.de(os.getPrestador()),
                orcamentos.stream().map(OrcamentoItem::de).toList(),
                historico.stream().map(HistoricoItem::de).toList());
    }
}
