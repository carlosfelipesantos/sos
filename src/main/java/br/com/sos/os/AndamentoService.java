package br.com.sos.os;

import br.com.sos.auth.Usuario;
import br.com.sos.auth.UsuarioLogadoService;
import br.com.sos.comum.AcessoNegadoException;
import br.com.sos.comum.NaoEncontradoException;
import br.com.sos.comum.RegraNegocioException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

/**
 * Andamento (RF004), encerramento e cancelamento (RF005) da OS.
 * Fluxo: ABERTA -> AGUARDANDO_APROVACAO -> EM_EXECUCAO -> PRONTA -> ENCERRADA; CANCELADA a qualquer momento antes do fim.
 * Toda troca de status passa por {@link OrdemServicoService#mudarStatus}, que grava o Historico.
 * O Historico é imutável (RNF005): andamento registrado não é editado nem excluído.
 */
@Service
@RequiredArgsConstructor
public class AndamentoService {

    /** Depois de ENCERRADA ou CANCELADA a OS não muda mais. */
    private static final Set<StatusOS> FINALIZADOS = EnumSet.of(StatusOS.ENCERRADA, StatusOS.CANCELADA);

    private final OrdemServicoRepository ordemServicoRepository;
    private final OrdemServicoService ordemServicoService;
    private final HistoricoRepository historicoRepository;
    private final UsuarioLogadoService usuarioLogadoService;

    /**
     * Registra o andamento (RF004). Só o prestador responsável pela OS, com a OS em execução.
     * Sem status: grava só a observação (statusAnterior e statusNovo nulos).
     * Com status PRONTA: o serviço foi concluído e a OS fica pronta para o atendente encerrar.
     */
    @Transactional
    public Historico registrarAndamento(Long osId, RegistrarAndamentoRequest dados) {
        String observacao = textoOuNulo(dados.observacao());
        if (observacao == null) {
            throw new RegraNegocioException("observacao", "Descreva o andamento do serviço.");
        }
        if (dados.status() != null && dados.status() != StatusOS.PRONTA) {
            throw new RegraNegocioException("status", "No andamento, o status só pode mudar para \"Pronta\".");
        }

        Usuario prestador = usuarioLogadoService.atual();
        OrdemServico os = buscarParaAlterar(osId);
        if (!os.getPrestador().getId().equals(prestador.getId())) {
            throw new AcessoNegadoException("Somente o prestador responsável pode registrar o andamento desta OS.");
        }
        garantirNaoFinalizada(os);
        if (os.getStatus() != StatusOS.EM_EXECUCAO) {
            throw new RegraNegocioException("status", "Somente OS com status \"Em execução\" recebe andamento.");
        }

        if (dados.status() == StatusOS.PRONTA) {
            return ordemServicoService.mudarStatus(os, StatusOS.PRONTA, prestador, observacao);
        }
        Historico historico = new Historico();
        historico.setOs(os);
        historico.setUsuario(prestador);
        historico.setDescricao(observacao);
        historico.setDataHistorico(LocalDateTime.now());
        return historicoRepository.save(historico);
    }

    /** Encerra a OS (RF005): só OS PRONTA. Preenche a data de finalização e grava o histórico. */
    @Transactional
    public OrdemServico encerrar(Long osId, EncerrarOsRequest dados) {
        Usuario atendente = usuarioLogadoService.atual();
        OrdemServico os = buscarParaAlterar(osId);
        garantirNaoFinalizada(os);
        if (os.getStatus() != StatusOS.PRONTA) {
            throw new RegraNegocioException("status", "Somente OS com status \"Pronta\" pode ser encerrada.");
        }
        finalizar(os, StatusOS.ENCERRADA, atendente, dados == null ? null : dados.observacao());
        return os;
    }

    /**
     * Cancela a OS (RF005) em qualquer status que ainda não seja final. O motivo é obrigatório
     * e fica no histórico. Um orçamento pendente deixa de poder ser decidido (OrcamentoService.decidir).
     */
    @Transactional
    public OrdemServico cancelar(Long osId, CancelarOsRequest dados) {
        String motivo = textoOuNulo(dados.motivo());
        if (motivo == null) {
            throw new RegraNegocioException("motivo", "Informe o motivo do cancelamento.");
        }
        Usuario atendente = usuarioLogadoService.atual();
        OrdemServico os = buscarParaAlterar(osId);
        garantirNaoFinalizada(os);
        finalizar(os, StatusOS.CANCELADA, atendente, motivo);
        return os;
    }

    private void finalizar(OrdemServico os, StatusOS status, Usuario usuario, String observacao) {
        os.setDataFinalizacao(LocalDateTime.now());
        ordemServicoService.mudarStatus(os, status, usuario, observacao);
    }

    /** Trava a OS até o fim da transação: duas alterações ao mesmo tempo não passam pela mesma verificação de status. */
    private OrdemServico buscarParaAlterar(Long id) {
        return ordemServicoRepository.buscarParaAlterar(id)
                .orElseThrow(() -> new NaoEncontradoException("Ordem de serviço não encontrada."));
    }

    private static void garantirNaoFinalizada(OrdemServico os) {
        if (FINALIZADOS.contains(os.getStatus())) {
            throw new RegraNegocioException("status", "Esta OS já foi encerrada ou cancelada e não pode mais ser alterada.");
        }
    }

    private static String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
