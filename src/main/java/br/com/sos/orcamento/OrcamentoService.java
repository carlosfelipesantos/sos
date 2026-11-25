package br.com.sos.orcamento;

import br.com.sos.auth.Cargo;
import br.com.sos.auth.Usuario;
import br.com.sos.auth.UsuarioAutenticado;
import br.com.sos.auth.UsuarioRepository;
import br.com.sos.comum.AcessoNegadoException;
import br.com.sos.comum.NaoEncontradoException;
import br.com.sos.comum.RegraNegocioException;
import br.com.sos.os.OrdemServico;
import br.com.sos.os.OrdemServicoService;
import br.com.sos.os.StatusOS;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Regras do orçamento (RF003): geração pelo Prestador e decisão (aprovação/recusa) pelo Atendente.
 */
@Service
@RequiredArgsConstructor
public class OrcamentoService {

    private final OrcamentoRepository orcamentoRepository;
    private final OrdemServicoService ordemServicoService;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public Orcamento gerar(Long osId, OrcamentoRequest dados, Object prestadorLogado) {
        Usuario usuarioPrestador = toUsuario(prestadorLogado);
        if (usuarioPrestador != null && !usuarioPrestador.temCargo(Cargo.PRESTADOR)) {
            throw new AcessoNegadoException("Você não tem permissão para usar esta função.");
        }
        if (osId == null) {
            throw new RegraNegocioException("osId", "A OS é obrigatória.");
        }
        if (dados == null) {
            throw new RegraNegocioException("dados", "Os dados do orçamento são obrigatórios.");
        }
        OrdemServico os = ordemServicoService.buscarPorId(osId);
        if (os.getStatus() != StatusOS.ABERTA) {
            throw new RegraNegocioException("status", "Somente OS com status \"Aberta\" recebe orçamento.");
        }
        if (dados.descricao() == null || dados.descricao().isBlank()) {
            throw new RegraNegocioException("descricao", "A descrição do serviço é obrigatória.");
        }
        if (dados.valor() == null || dados.valor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegraNegocioException("valor", "O valor do orçamento deve ser maior que zero.");
        }
        if (dados.prazoDias() == null) {
            throw new RegraNegocioException("prazoDias", "O prazo deve ser informado.");
        }

        Orcamento orcamento = new Orcamento();
        orcamento.setOs(os);
        orcamento.setDescricao(dados.descricao().trim());
        orcamento.setValorOrcamento(dados.valor());
        orcamento.setPrazoDias(dados.prazoDias());
        orcamento.setStatus(StatusOrcamento.PENDENTE);
        Orcamento salvo = orcamentoRepository.save(orcamento);

        ordemServicoService.mudarStatus(os, StatusOS.AGUARDANDO_APROVACAO,
                usuarioPrestador == null ? os.getPrestador() : usuarioPrestador, null);
        return salvo;
    }

    @Transactional
    public Orcamento decidir(Long orcamentoId, boolean aprovado, Object atendenteLogado) {
        if (orcamentoId == null) {
            throw new RegraNegocioException("id", "O orçamento é obrigatório.");
        }
        Orcamento orcamento = orcamentoRepository.findById(orcamentoId)
                .orElseThrow(() -> new NaoEncontradoException("Orçamento não encontrado."));
        if (orcamento.getStatus() != StatusOrcamento.PENDENTE) {
            throw new RegraNegocioException("status", "O orçamento precisa estar pendente para decisão.");
        }
        // OS cancelada (RF005) com orçamento pendente: a decisão não pode mais tirar a OS de CANCELADA.
        if (orcamento.getOs().getStatus() != StatusOS.AGUARDANDO_APROVACAO) {
            throw new RegraNegocioException("status", "A OS deste orçamento não está mais aguardando aprovação.");
        }

        Usuario atendente = toUsuario(atendenteLogado);
        if (atendente == null) {
            throw new AcessoNegadoException("Usuário não autenticado.");
        }
        if (!atendente.temCargo(Cargo.ATENDENTE)) {
            throw new AcessoNegadoException("Você não tem permissão para usar esta função.");
        }

        orcamento.setDecididoPor(atendente);
        orcamento.setDataDecisao(LocalDateTime.now());

        OrdemServico os = orcamento.getOs();
        if (aprovado) {
            orcamento.setStatus(StatusOrcamento.APROVADO);
            orcamentoRepository.save(orcamento);
            ordemServicoService.mudarStatus(os, StatusOS.EM_EXECUCAO, atendente, null);
        } else {
            orcamento.setStatus(StatusOrcamento.RECUSADO);
            orcamentoRepository.save(orcamento);
            ordemServicoService.mudarStatus(os, StatusOS.ABERTA, atendente, null);
        }
        return orcamento;
    }

    private Usuario toUsuario(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Usuario u) {
            return u;
        }
        if (o instanceof UsuarioAutenticado ua) {
            return usuarioRepository.findById(ua.getId())
                    .orElseThrow(() -> new NaoEncontradoException("Usuário não encontrado."));
        }
        throw new NaoEncontradoException("Usuário não encontrado.");
    }
}
