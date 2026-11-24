package br.com.sos.orcamento;

import br.com.sos.auth.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Orçamento devolvido pela API (RF003).
 */
public record OrcamentoResponse(
        Long id,
        String descricao,
        BigDecimal valor,
        Integer prazoDias,
        StatusOrcamento status,
        LocalDateTime dataCriacao,
        LocalDateTime dataDecisao,
        UsuarioResumo decididoPor) {

    public record UsuarioResumo(Long id, String nome) {

        static UsuarioResumo de(Usuario u) {
            return u == null ? null : new UsuarioResumo(u.getId(), u.getNomeUsuario());
        }
    }

    public static OrcamentoResponse de(Orcamento o) {
        return new OrcamentoResponse(o.getId(), o.getDescricao(), o.getValorOrcamento(), o.getPrazoDias(),
                o.getStatus(), o.getDataCriacao(), o.getDataDecisao(), UsuarioResumo.de(o.getDecididoPor()));
    }
}
