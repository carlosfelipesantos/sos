package br.com.sos.os;

import br.com.sos.auth.Usuario;
import br.com.sos.auth.UsuarioAutenticado;
import br.com.sos.auth.UsuarioRepository;
import br.com.sos.cliente.Cliente;
import br.com.sos.cliente.ClienteRepository;
import br.com.sos.comum.RegraNegocioException;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF004 (andamento da OS) e RF005 (encerrar e cancelar a OS). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AndamentoControllerTest {

    /** CPFs começando com 8, para não coincidir com os gerados nos outros testes. */
    private static final AtomicLong SEQUENCIA = new AtomicLong(ThreadLocalRandom.current().nextLong(1_000_000));

    @Autowired
    MockMvc mvc;
    @Autowired
    AndamentoService andamentoService;
    @Autowired
    OrdemServicoService ordemServicoService;
    @Autowired
    OrdemServicoRepository ordemServicoRepository;
    @Autowired
    HistoricoRepository historicoRepository;
    @Autowired
    ClienteRepository clienteRepository;
    @Autowired
    UsuarioRepository usuarioRepository;

    // ---------- andamento (RF004) ----------

    @Test
    void prestadorRegistraObservacaoDeAndamento() throws Exception {
        long id = osEmExecucao("prestador");

        mvc.perform(post("/os/" + id + "/andamentos").session(login("prestador")).contentType(MediaType.APPLICATION_JSON)
                        .content(andamento("  Peça trocada, testando  ", null)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/os/" + id)))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.observacao").value("Peça trocada, testando"))
                .andExpect(jsonPath("$.statusAnterior").doesNotExist())
                .andExpect(jsonPath("$.statusNovo").doesNotExist())
                .andExpect(jsonPath("$.dataHora").isNotEmpty())
                .andExpect(jsonPath("$.usuario.nome").value("Prestador Teste"));

        // A observação não muda o status e aparece no detalhe da OS (RF006).
        mvc.perform(get("/os/" + id).session(login("atendente")))
                .andExpect(jsonPath("$.status").value("EM_EXECUCAO"))
                .andExpect(jsonPath("$.historico[-1:].observacao").value(hasItem("Peça trocada, testando")));
    }

    @Test
    void prestadorConcluiServicoEOsFicaPronta() throws Exception {
        long id = osEmExecucao("prestador");

        mvc.perform(post("/os/" + id + "/andamentos").session(login("prestador")).contentType(MediaType.APPLICATION_JSON)
                        .content(andamento("Serviço concluído", "PRONTA")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusAnterior").value("EM_EXECUCAO"))
                .andExpect(jsonPath("$.statusNovo").value("PRONTA"))
                .andExpect(jsonPath("$.observacao").value("Serviço concluído"));

        OrdemServico os = ordemServicoRepository.findById(id).orElseThrow();
        assertThat(os.getStatus()).isEqualTo(StatusOS.PRONTA);
        assertThat(os.getDataFinalizacao()).isNull();
    }

    @Test
    void andamentoSemObservacaoDa400() throws Exception {
        long id = osEmExecucao("prestador");

        mvc.perform(post("/os/" + id + "/andamentos").session(login("prestador")).contentType(MediaType.APPLICATION_JSON)
                        .content(andamento("   ", "PRONTA")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("observacao"))
                .andExpect(jsonPath("$.mensagem").value("Descreva o andamento do serviço."));
        assertThat(statusDa(id)).isEqualTo(StatusOS.EM_EXECUCAO);
    }

    @Test
    void andamentoSoPodeMudarParaPronta() throws Exception {
        long id = osEmExecucao("prestador");
        MockHttpSession prestador = login("prestador");

        mvc.perform(post("/os/" + id + "/andamentos").session(prestador).contentType(MediaType.APPLICATION_JSON)
                        .content(andamento("Tentando encerrar", "ENCERRADA")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("status"))
                .andExpect(jsonPath("$.mensagem").value("No andamento, o status só pode mudar para \"Pronta\"."));
        mvc.perform(post("/os/" + id + "/andamentos").session(prestador).contentType(MediaType.APPLICATION_JSON)
                        .content(andamento("Status que não existe", "QUALQUER")))
                .andExpect(status().isBadRequest());
        assertThat(statusDa(id)).isEqualTo(StatusOS.EM_EXECUCAO);
    }

    @Test
    void andamentoSoComOsEmExecucao() throws Exception {
        long aberta = osAberta("prestador").getId();
        long pronta = osEmExecucao("prestador");
        ordemServicoService.mudarStatus(ordemServicoService.buscarPorId(pronta), StatusOS.PRONTA, usuario("prestador"), null);
        MockHttpSession prestador = login("prestador");

        for (long id : new long[]{aberta, pronta}) {
            mvc.perform(post("/os/" + id + "/andamentos").session(prestador).contentType(MediaType.APPLICATION_JSON)
                            .content(andamento("Andamento", null)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campo").value("status"))
                    .andExpect(jsonPath("$.mensagem").value("Somente OS com status \"Em execução\" recebe andamento."));
        }
    }

    @Test
    void prestadorNaoRegistraAndamentoNaOsDeOutro() throws Exception {
        long id = osEmExecucao("prestador");

        mvc.perform(post("/os/" + id + "/andamentos").session(login("prestador2")).contentType(MediaType.APPLICATION_JSON)
                        .content(andamento("Não é minha", "PRONTA")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Somente o prestador responsável pode registrar o andamento desta OS."));
        // O autônomo também é atendente, mas o andamento é só do prestador responsável.
        mvc.perform(post("/os/" + id + "/andamentos").session(login("autonomo")).contentType(MediaType.APPLICATION_JSON)
                        .content(andamento("Não é minha", null)))
                .andExpect(status().isForbidden());
        assertThat(statusDa(id)).isEqualTo(StatusOS.EM_EXECUCAO);
    }

    @Test
    void atendenteNaoRegistraAndamento() throws Exception {
        long id = osEmExecucao("prestador");

        mvc.perform(post("/os/" + id + "/andamentos").session(login("atendente")).contentType(MediaType.APPLICATION_JSON)
                        .content(andamento("Atendente tentando", null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Você não tem permissão para usar esta função."));
    }

    @Test
    void autonomoRegistraAndamentoNaSuaOs() throws Exception {
        long id = osEmExecucao("autonomo");

        mvc.perform(post("/os/" + id + "/andamentos").session(login("autonomo")).contentType(MediaType.APPLICATION_JSON)
                        .content(andamento("Feito", "PRONTA")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.usuario.nome").value("Prestador Autônomo"));
    }

    @Test
    void historicoGravadoNaoMuda() throws Exception {
        long id = osEmExecucao("prestador");
        String resposta = mvc.perform(post("/os/" + id + "/andamentos").session(login("prestador"))
                        .contentType(MediaType.APPLICATION_JSON).content(andamento("Original", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long historicoId = ((Number) JsonPath.read(resposta, "$.id")).longValue();

        Historico h = historicoRepository.findById(historicoId).orElseThrow();
        h.setDescricao("Alterado");
        historicoRepository.saveAndFlush(h); // @Immutable: o Hibernate ignora a alteração

        assertThat(historicoRepository.findById(historicoId).orElseThrow().getDescricao()).isEqualTo("Original");
    }

    @Test
    void andamentoSemLoginDa401() throws Exception {
        mvc.perform(post("/os/1/andamentos").contentType(MediaType.APPLICATION_JSON).content(andamento("X", null)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/os/1/encerramento")).andExpect(status().isUnauthorized());
        mvc.perform(post("/os/1/cancelamento").contentType(MediaType.APPLICATION_JSON).content(cancelamento("X")))
                .andExpect(status().isUnauthorized());
    }

    // ---------- encerrar (RF005) ----------

    @Test
    void atendenteEncerraOsPronta() throws Exception {
        long id = osPronta("prestador");

        mvc.perform(post("/os/" + id + "/encerramento").session(login("atendente")).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observacao\":\"  Cliente retirou o equipamento  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("ENCERRADA"))
                .andExpect(jsonPath("$.dataFinalizacao").isNotEmpty());

        Historico ultimo = ultimoHistorico(id);
        assertThat(ultimo.getStatusAnterior()).isEqualTo(StatusOS.PRONTA);
        assertThat(ultimo.getStatusNovo()).isEqualTo(StatusOS.ENCERRADA);
        assertThat(ultimo.getDescricao()).isEqualTo("Cliente retirou o equipamento");
        assertThat(ultimo.getUsuario().getNomeUsuario()).isEqualTo("Atendente Teste");
    }

    @Test
    void encerraSemCorpo() throws Exception {
        long id = osPronta("prestador");

        mvc.perform(post("/os/" + id + "/encerramento").session(login("atendente")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENCERRADA"));
        assertThat(ultimoHistorico(id).getDescricao()).isNull();
    }

    @Test
    void soOsProntaPodeSerEncerrada() throws Exception {
        long aberta = osAberta("prestador").getId();
        long emExecucao = osEmExecucao("prestador");
        MockHttpSession atendente = login("atendente");

        for (long id : new long[]{aberta, emExecucao}) {
            mvc.perform(post("/os/" + id + "/encerramento").session(atendente))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.campo").value("status"))
                    .andExpect(jsonPath("$.mensagem").value("Somente OS com status \"Pronta\" pode ser encerrada."));
            assertThat(ordemServicoRepository.findById(id).orElseThrow().getDataFinalizacao()).isNull();
        }
    }

    // ---------- cancelar (RF005) ----------

    @Test
    void atendenteCancelaOsComMotivo() throws Exception {
        long id = osAberta("prestador").getId();

        mvc.perform(post("/os/" + id + "/cancelamento").session(login("atendente")).contentType(MediaType.APPLICATION_JSON)
                        .content(cancelamento("  Cliente desistiu do serviço  ")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADA"))
                .andExpect(jsonPath("$.dataFinalizacao").isNotEmpty());

        Historico ultimo = ultimoHistorico(id);
        assertThat(ultimo.getStatusAnterior()).isEqualTo(StatusOS.ABERTA);
        assertThat(ultimo.getStatusNovo()).isEqualTo(StatusOS.CANCELADA);
        assertThat(ultimo.getDescricao()).isEqualTo("Cliente desistiu do serviço");
        assertThat(ultimo.getUsuario().getNomeUsuario()).isEqualTo("Atendente Teste");
    }

    @Test
    void cancelaEmExecucaoOuPronta() throws Exception {
        long emExecucao = osEmExecucao("prestador");
        long pronta = osPronta("prestador");
        MockHttpSession atendente = login("atendente");

        for (long id : new long[]{emExecucao, pronta}) {
            mvc.perform(post("/os/" + id + "/cancelamento").session(atendente).contentType(MediaType.APPLICATION_JSON)
                            .content(cancelamento("Peça indisponível")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELADA"));
        }
    }

    @Test
    void cancelarSemMotivoDa400() throws Exception {
        long id = osAberta("prestador").getId();

        mvc.perform(post("/os/" + id + "/cancelamento").session(login("atendente")).contentType(MediaType.APPLICATION_JSON)
                        .content(cancelamento("  ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("motivo"))
                .andExpect(jsonPath("$.mensagem").value("Informe o motivo do cancelamento."));
        mvc.perform(post("/os/" + id + "/cancelamento").session(login("atendente")))
                .andExpect(status().isBadRequest());
        assertThat(statusDa(id)).isEqualTo(StatusOS.ABERTA);
    }

    @Test
    void osEncerradaOuCanceladaNaoMudaMais() throws Exception {
        long encerrada = osPronta("prestador");
        long cancelada = osAberta("prestador").getId();
        MockHttpSession atendente = login("atendente");
        mvc.perform(post("/os/" + encerrada + "/encerramento").session(atendente)).andExpect(status().isOk());
        mvc.perform(post("/os/" + cancelada + "/cancelamento").session(atendente).contentType(MediaType.APPLICATION_JSON)
                .content(cancelamento("Desistiu"))).andExpect(status().isOk());
        String mensagem = "Esta OS já foi encerrada ou cancelada e não pode mais ser alterada.";

        for (long id : new long[]{encerrada, cancelada}) {
            mvc.perform(post("/os/" + id + "/encerramento").session(atendente))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensagem").value(mensagem));
            mvc.perform(post("/os/" + id + "/cancelamento").session(atendente).contentType(MediaType.APPLICATION_JSON)
                            .content(cancelamento("De novo")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensagem").value(mensagem));
            mvc.perform(post("/os/" + id + "/andamentos").session(login("prestador")).contentType(MediaType.APPLICATION_JSON)
                            .content(andamento("Depois do fim", null)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensagem").value(mensagem));
        }
        assertThat(statusDa(encerrada)).isEqualTo(StatusOS.ENCERRADA);
        assertThat(statusDa(cancelada)).isEqualTo(StatusOS.CANCELADA);
    }

    @Test
    void prestadorNaoEncerraNemCancela() throws Exception {
        long id = osPronta("prestador");
        MockHttpSession prestador = login("prestador");

        mvc.perform(post("/os/" + id + "/encerramento").session(prestador)).andExpect(status().isForbidden());
        mvc.perform(post("/os/" + id + "/cancelamento").session(prestador).contentType(MediaType.APPLICATION_JSON)
                .content(cancelamento("Motivo"))).andExpect(status().isForbidden());
        assertThat(statusDa(id)).isEqualTo(StatusOS.PRONTA);
    }

    @Test
    void osInexistenteDa404() throws Exception {
        mvc.perform(post("/os/999999/andamentos").session(login("prestador")).contentType(MediaType.APPLICATION_JSON)
                        .content(andamento("X", null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Ordem de serviço não encontrada."));
        MockHttpSession atendente = login("atendente");
        mvc.perform(post("/os/999999/encerramento").session(atendente))
                .andExpect(status().isNotFound());
        mvc.perform(post("/os/999999/cancelamento").session(atendente).contentType(MediaType.APPLICATION_JSON)
                        .content(cancelamento("X")))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelarOsAguardandoAprovacaoImpedeDecidirOOrcamento() throws Exception {
        long id = osAberta("prestador").getId();
        long orcamentoId = gerarOrcamento(id);
        MockHttpSession atendente = login("atendente");

        mvc.perform(post("/os/" + id + "/cancelamento").session(atendente).contentType(MediaType.APPLICATION_JSON)
                .content(cancelamento("Cliente não quer esperar"))).andExpect(status().isOk());
        mvc.perform(post("/orcamentos/" + orcamentoId + "/decisao").session(atendente).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"aprovado\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A OS deste orçamento não está mais aguardando aprovação."));
        assertThat(statusDa(id)).isEqualTo(StatusOS.CANCELADA);
    }

    @Test
    void encerrarECancelarAoMesmoTempoSoUmGanha() throws Exception {
        Usuario atendente = usuario("atendente");
        int quantidade = 8;
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            ids.add(osPronta("prestador"));
        }

        try (ExecutorService pool = Executors.newFixedThreadPool(quantidade * 2)) {
            List<Future<Boolean>> tarefas = new ArrayList<>();
            for (long id : ids) {
                tarefas.add(pool.submit(comoUsuario(atendente, () -> andamentoService.encerrar(id, null))));
                tarefas.add(pool.submit(comoUsuario(atendente,
                        () -> andamentoService.cancelar(id, new CancelarOsRequest("Ao mesmo tempo")))));
            }
            for (int i = 0; i < ids.size(); i++) {
                boolean encerrou = tarefas.get(2 * i).get();
                boolean cancelou = tarefas.get(2 * i + 1).get();
                assertThat(encerrou ^ cancelou).as("só uma das duas operações pode vencer").isTrue();
            }
        }
        for (long id : ids) {
            long finais = historicoRepository.findByOsIdOrderByDataHistoricoAsc(id).stream()
                    .filter(h -> h.getStatusNovo() == StatusOS.ENCERRADA || h.getStatusNovo() == StatusOS.CANCELADA)
                    .count();
            assertThat(finais).isEqualTo(1);
        }
    }

    // ---------- fluxo completo pela API ----------

    @Test
    void fluxoCompletoDaAberturaAoEncerramento() throws Exception {
        MockHttpSession atendente = login("atendente");
        MockHttpSession prestador = login("prestador");
        long id = osAberta("prestador").getId();
        long orcamentoId = gerarOrcamento(id);
        mvc.perform(post("/orcamentos/" + orcamentoId + "/decisao").session(atendente).contentType(MediaType.APPLICATION_JSON)
                .content("{\"aprovado\":true}")).andExpect(status().isOk());

        mvc.perform(post("/os/" + id + "/andamentos").session(prestador).contentType(MediaType.APPLICATION_JSON)
                .content(andamento("Desmontado, aguardando peça", null))).andExpect(status().isCreated());
        mvc.perform(post("/os/" + id + "/andamentos").session(prestador).contentType(MediaType.APPLICATION_JSON)
                .content(andamento("Peça instalada e testada", "PRONTA"))).andExpect(status().isCreated());
        mvc.perform(post("/os/" + id + "/encerramento").session(atendente).contentType(MediaType.APPLICATION_JSON)
                .content("{\"observacao\":\"Entregue ao cliente\"}")).andExpect(status().isOk());

        mvc.perform(get("/os/" + id).session(atendente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENCERRADA"))
                .andExpect(jsonPath("$.dataFinalizacao").isNotEmpty())
                .andExpect(jsonPath("$.orcamentos[0].status").value("APROVADO"))
                .andExpect(jsonPath("$.historico.length()").value(5))
                .andExpect(jsonPath("$.historico[0].statusNovo").value("AGUARDANDO_APROVACAO"))
                .andExpect(jsonPath("$.historico[1].statusNovo").value("EM_EXECUCAO"))
                .andExpect(jsonPath("$.historico[2].statusNovo").doesNotExist())
                .andExpect(jsonPath("$.historico[2].observacao").value("Desmontado, aguardando peça"))
                .andExpect(jsonPath("$.historico[3].statusAnterior").value("EM_EXECUCAO"))
                .andExpect(jsonPath("$.historico[3].statusNovo").value("PRONTA"))
                .andExpect(jsonPath("$.historico[3].usuario.nome").value("Prestador Teste"))
                .andExpect(jsonPath("$.historico[4].statusNovo").value("ENCERRADA"))
                .andExpect(jsonPath("$.historico[4].usuario.nome").value("Atendente Teste"));

        mvc.perform(get("/os").param("status", "ENCERRADA").param("numero",
                        String.valueOf(ordemServicoService.buscarPorId(id).getNumero())).session(atendente))
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ---------- apoio ----------

    private MockHttpSession login(String login) throws Exception {
        return (MockHttpSession) mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + login + "\",\"senha\":\"123\"}"))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    private OrdemServico osAberta(String loginPrestador) {
        return ordemServicoService.abrir(new AbrirOsRequest(novoCliente().getId(), "Serviço de teste",
                usuario(loginPrestador).getId()));
    }

    /** Simula o orçamento aprovado (RF003): a OS fica EM_EXECUCAO. */
    private long osEmExecucao(String loginPrestador) {
        OrdemServico os = osAberta(loginPrestador);
        ordemServicoService.mudarStatus(os, StatusOS.EM_EXECUCAO, usuario("atendente"), null);
        return os.getId();
    }

    private long osPronta(String loginPrestador) {
        long id = osEmExecucao(loginPrestador);
        ordemServicoService.mudarStatus(ordemServicoService.buscarPorId(id), StatusOS.PRONTA, usuario(loginPrestador), null);
        return id;
    }

    private long gerarOrcamento(long osId) throws Exception {
        String resposta = mvc.perform(post("/os/" + osId + "/orcamentos").session(login("prestador"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"Troca da peça\",\"valor\":150.00,\"prazoDias\":2}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }

    private StatusOS statusDa(long id) {
        return ordemServicoRepository.findById(id).orElseThrow().getStatus();
    }

    private Historico ultimoHistorico(long osId) {
        List<Historico> historico = historicoRepository.findByOsIdOrderByDataHistoricoAsc(osId);
        return historico.get(historico.size() - 1);
    }

    /** Roda a operação numa thread com o usuário logado; devolve false se a regra de negócio recusou. */
    private static Callable<Boolean> comoUsuario(Usuario usuario, Callable<?> operacao) {
        UsuarioAutenticado logado = new UsuarioAutenticado(usuario);
        return () -> {
            SecurityContextHolder.getContext().setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated(logado, null, logado.getAuthorities()));
            try {
                operacao.call();
                return true;
            } catch (RegraNegocioException recusada) {
                return false;
            } finally {
                SecurityContextHolder.clearContext();
            }
        };
    }

    private Usuario usuario(String login) {
        return usuarioRepository.findByLogin(login).orElseThrow();
    }

    private Cliente novoCliente() {
        Cliente c = new Cliente();
        c.setNomeCliente("Cliente andamento " + SEQUENCIA.get());
        c.setCpf(String.format("8%010d", SEQUENCIA.incrementAndGet()));
        c.setTelefone("17999990000");
        return clienteRepository.save(c);
    }

    private static String andamento(String observacao, String status) {
        return "{\"observacao\":%s,\"status\":%s}".formatted(
                observacao == null ? "null" : "\"" + observacao + "\"", status == null ? "null" : "\"" + status + "\"");
    }

    private static String cancelamento(String motivo) {
        return "{\"motivo\":\"" + motivo + "\"}";
    }
}
