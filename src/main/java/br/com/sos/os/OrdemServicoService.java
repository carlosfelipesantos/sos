package br.com.sos.os;

import br.com.sos.auth.Cargo;
import br.com.sos.auth.Usuario;
import br.com.sos.auth.UsuarioAutenticado;
import br.com.sos.auth.UsuarioRepository;
import br.com.sos.cliente.Cliente;
import br.com.sos.cliente.ClienteService;
import br.com.sos.comum.AcessoNegadoException;
import br.com.sos.comum.NaoEncontradoException;
import br.com.sos.comum.RegraNegocioException;
import br.com.sos.orcamento.OrcamentoRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Abertura e consulta de OS (RF002, RF006) e a troca de status usada por todo o sistema.
 * Para mudar o status de uma OS, use sempre {@link #mudarStatus}: ele grava o Historico.
 */
@Service
@RequiredArgsConstructor
public class OrdemServicoService {

    private final OrdemServicoRepository ordemServicoRepository;
    private final HistoricoRepository historicoRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteService clienteService;

    /**
     * Troca o status da OS e grava um Historico com o status anterior, o novo, o usuário e a data e hora.
     * Toda mudança de status do sistema passa por aqui (orçamento, andamento, encerramento).
     * Quem chama confere antes se a mudança é permitida (ex.: OS precisa estar ABERTA para gerar orçamento).
     */
    @Transactional
    public Historico mudarStatus(OrdemServico os, StatusOS novo, Usuario usuario, String observacao) {
        if (novo == null) {
            throw new IllegalArgumentException("O novo status é obrigatório.");
        }
        if (usuario == null) {
            throw new IllegalArgumentException("O usuário que muda o status é obrigatório.");
        }
        StatusOS anterior = os.getStatus();
        os.setStatus(novo);
        ordemServicoRepository.save(os);

        Historico historico = new Historico();
        historico.setOs(os);
        historico.setStatusAnterior(anterior);
        historico.setStatusNovo(novo);
        historico.setUsuario(usuario);
        historico.setDescricao(observacao == null || observacao.isBlank() ? null : observacao.trim());
        historico.setDataHistorico(LocalDateTime.now());
        return historicoRepository.save(historico);
    }

    /**
     * Abre a OS (RF002): cliente e prestador precisam existir. O número vem da sequence do banco,
     * que nunca repete, mesmo com duas OS abertas ao mesmo tempo (RNF008).
     */
    @Transactional
    public OrdemServico abrir(AbrirOsRequest dados) {
        if (dados.descricao() == null || dados.descricao().isBlank()) {
            throw new RegraNegocioException("descricao", "A descrição do serviço é obrigatória.");
        }
        Cliente cliente = clienteService.buscarPorId(dados.clienteId());
        Usuario prestador = buscarPrestador(dados.prestadorId());

        OrdemServico os = new OrdemServico();
        os.setNumero(ordemServicoRepository.proximoNumero());
        os.setTipoServico(dados.descricao().trim());
        os.setStatus(StatusOS.ABERTA);
        os.setDataAbertura(LocalDateTime.now());
        os.setCliente(cliente);
        os.setPrestador(prestador);
        return ordemServicoRepository.save(os);
    }

    /**
     * Lista as OS (RF006). Filtros opcionais e combináveis:
     * numero exato, clienteBusca (parte do nome ou do CPF/CNPJ) e status.
     * O atendente vê todas; o prestador vê só as dele. Mais recentes primeiro.
     */
    @Transactional(readOnly = true)
    public List<OrdemServico> listar(Long numero, String clienteBusca, StatusOS status, UsuarioAutenticado usuarioLogado) {
        Specification<OrdemServico> filtro = (raiz, consulta, cb) -> {
            List<Predicate> condicoes = new ArrayList<>();
            if (numero != null) {
                condicoes.add(cb.equal(raiz.get("numero"), numero));
            }
            if (status != null) {
                condicoes.add(cb.equal(raiz.get("status"), status));
            }
            if (clienteBusca != null && !clienteBusca.isBlank()) {
                Join<OrdemServico, Cliente> cliente = raiz.join("cliente");
                String busca = clienteBusca.trim();
                String digitos = busca.replaceAll("\\D", "");
                Predicate porNome = cb.like(cb.lower(cliente.get("nomeCliente")), "%" + busca.toLowerCase() + "%");
                condicoes.add(digitos.isEmpty()
                        ? porNome
                        : cb.or(porNome, cb.like(cliente.get("cpf"), "%" + digitos + "%")));
            }
            if (!podeVerTodas(usuarioLogado)) {
                condicoes.add(cb.equal(raiz.get("prestador").get("id"), usuarioLogado.getId()));
            }
            return cb.and(condicoes.toArray(Predicate[]::new));
        };
        return ordemServicoRepository.findAll(filtro, Sort.by(Sort.Direction.DESC, "numero"));
    }

    /**
     * Detalhe completo da OS (RF006): cliente, prestador, todos os orçamentos (inclusive os recusados)
     * e o histórico em ordem de data. O prestador não pode ver OS de outro prestador (403).
     */
    @Transactional(readOnly = true)
    public OrdemServicoDetalheResponse detalhar(Long id, UsuarioAutenticado usuarioLogado) {
        OrdemServico os = buscarPorId(id);
        if (!podeVerTodas(usuarioLogado) && !os.getPrestador().getId().equals(usuarioLogado.getId())) {
            throw new AcessoNegadoException("Você não pode ver a OS de outro prestador.");
        }
        // Ordem de data; se dois registros tiverem a mesma data e hora, o mais antigo (menor id) vem primeiro.
        List<Historico> historico = historicoRepository.findByOsIdOrderByDataHistoricoAsc(id).stream()
                .sorted(Comparator.comparing(Historico::getDataHistorico).thenComparing(Historico::getId))
                .toList();
        return OrdemServicoDetalheResponse.de(os, orcamentoRepository.findByOsIdOrderByIdAsc(id), historico);
    }

    /** Busca a OS pelo id. Se não existir, lança 404. */
    @Transactional(readOnly = true)
    public OrdemServico buscarPorId(Long id) {
        return ordemServicoRepository.findById(id)
                .orElseThrow(() -> new NaoEncontradoException("Ordem de serviço não encontrada."));
    }

    /** Prestadores para escolher na abertura da OS (GET /prestadores), em ordem de nome. */
    @Transactional(readOnly = true)
    public List<Usuario> listarPrestadores() {
        return usuarioRepository.findByCargosTipoCargoOrderByNomeUsuario(Cargo.PRESTADOR);
    }

    private Usuario buscarPrestador(Long prestadorId) {
        return usuarioRepository.findById(prestadorId)
                .filter(u -> u.temCargo(Cargo.PRESTADOR))
                .orElseThrow(() -> new NaoEncontradoException("Prestador não encontrado."));
    }

    /** Quem tem cargo de atendente (inclusive o autônomo, que tem os dois) vê todas as OS. */
    private boolean podeVerTodas(UsuarioAutenticado usuario) {
        return usuario.temCargo(Cargo.ATENDENTE);
    }
}
