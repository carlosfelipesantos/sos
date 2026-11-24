package br.com.sos.os;

import br.com.sos.auth.Usuario;
import br.com.sos.auth.UsuarioRepository;
import br.com.sos.cliente.Cliente;
import br.com.sos.cliente.ClienteRepository;
import br.com.sos.orcamento.Orcamento;
import br.com.sos.orcamento.OrcamentoRepository;
import br.com.sos.orcamento.StatusOrcamento;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF002 (abrir OS), RF006 (consultar OS), GET /prestadores e o mudarStatus usado pelas próximas partes. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrdemServicoControllerTest {

    /** CPFs começando com 9, para não coincidir com os gerados nos testes de clientes. */
    private static final AtomicLong SEQUENCIA = new AtomicLong(ThreadLocalRandom.current().nextLong(1_000_000));

    @Autowired
    MockMvc mvc;
    @Autowired
    OrdemServicoService ordemServicoService;
    @Autowired
    OrdemServicoRepository ordemServicoRepository;
    @Autowired
    ClienteRepository clienteRepository;
    @Autowired
    UsuarioRepository usuarioRepository;
    @Autowired
    OrcamentoRepository orcamentoRepository;

    // ---------- abrir (RF002) ----------

    @Test
    void atendenteAbreOs() throws Exception {
        Cliente cliente = novoCliente("Cliente " + marcaUnica());
        Usuario prestador = usuario("prestador");

        mvc.perform(post("/os").session(login("atendente")).contentType(MediaType.APPLICATION_JSON)
                        .content(json(cliente.getId(), "  Trocar a tela do celular  ", prestador.getId())))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/os/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.numero").isNumber())
                .andExpect(jsonPath("$.descricao").value("Trocar a tela do celular"))
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(jsonPath("$.dataAbertura").isNotEmpty())
                .andExpect(jsonPath("$.dataFinalizacao").doesNotExist())
                .andExpect(jsonPath("$.cliente.id").value(cliente.getId()))
                .andExpect(jsonPath("$.cliente.nome").value(cliente.getNomeCliente()))
                .andExpect(jsonPath("$.prestador.id").value(prestador.getId()))
                .andExpect(jsonPath("$.prestador.nome").value("Prestador Teste"));
    }

    @Test
    void cadaOsRecebeUmNumeroDiferente() throws Exception {
        Cliente cliente = novoCliente("Cliente " + marcaUnica());
        MockHttpSession sessao = login("atendente");
        long primeiro = numero(abrir(sessao, cliente, "prestador", "Serviço 1"));
        long segundo = numero(abrir(sessao, cliente, "prestador", "Serviço 2"));

        assertThat(segundo).isNotEqualTo(primeiro);
    }

    @Test
    void osAbertasAoMesmoTempoNuncaRepetemNumero() throws Exception {
        Cliente cliente = novoCliente("Cliente " + marcaUnica());
        Long prestadorId = usuario("prestador").getId();
        int quantidade = 10;

        List<Long> numeros;
        try (ExecutorService pool = Executors.newFixedThreadPool(quantidade)) {
            List<Future<Long>> tarefas = new java.util.ArrayList<>();
            for (int i = 0; i < quantidade; i++) {
                int n = i;
                tarefas.add(pool.submit(() -> ordemServicoService
                        .abrir(new AbrirOsRequest(cliente.getId(), "Simultânea " + n, prestadorId)).getNumero()));
            }
            numeros = new java.util.ArrayList<>();
            for (Future<Long> tarefa : tarefas) {
                numeros.add(tarefa.get());
            }
        }
        assertThat(Set.copyOf(numeros)).hasSize(quantidade);
    }

    @Test
    void semDescricaoDa400() throws Exception {
        Cliente cliente = novoCliente("Cliente " + marcaUnica());
        mvc.perform(post("/os").session(login("atendente")).contentType(MediaType.APPLICATION_JSON)
                        .content(json(cliente.getId(), "   ", usuario("prestador").getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("descricao"))
                .andExpect(jsonPath("$.mensagem").value("A descrição do serviço é obrigatória."));
    }

    @Test
    void semClienteOuPrestadorDa400() throws Exception {
        MockHttpSession sessao = login("atendente");
        mvc.perform(post("/os").session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content(json(null, "Serviço", usuario("prestador").getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("clienteId"));
        mvc.perform(post("/os").session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content(json(novoCliente("C " + marcaUnica()).getId(), "Serviço", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("prestadorId"));
    }

    @Test
    void clienteInexistenteDa404() throws Exception {
        mvc.perform(post("/os").session(login("atendente")).contentType(MediaType.APPLICATION_JSON)
                        .content(json(999_999L, "Serviço", usuario("prestador").getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Cliente não encontrado."));
    }

    @Test
    void prestadorInexistenteDa404() throws Exception {
        Cliente cliente = novoCliente("Cliente " + marcaUnica());
        mvc.perform(post("/os").session(login("atendente")).contentType(MediaType.APPLICATION_JSON)
                        .content(json(cliente.getId(), "Serviço", 999_999L)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Prestador não encontrado."));
    }

    @Test
    void usuarioQueNaoEPrestadorNaoPodeSerResponsavel() throws Exception {
        Cliente cliente = novoCliente("Cliente " + marcaUnica());
        mvc.perform(post("/os").session(login("atendente")).contentType(MediaType.APPLICATION_JSON)
                        .content(json(cliente.getId(), "Serviço", usuario("atendente").getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Prestador não encontrado."));
    }

    @Test
    void prestadorNaoAbreOs() throws Exception {
        Cliente cliente = novoCliente("Cliente " + marcaUnica());
        mvc.perform(post("/os").session(login("prestador")).contentType(MediaType.APPLICATION_JSON)
                        .content(json(cliente.getId(), "Serviço", usuario("prestador").getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void autonomoAbreOsParaSiMesmo() throws Exception {
        Cliente cliente = novoCliente("Cliente " + marcaUnica());
        mvc.perform(post("/os").session(login("autonomo")).contentType(MediaType.APPLICATION_JSON)
                        .content(json(cliente.getId(), "Serviço", usuario("autonomo").getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.prestador.nome").value("Prestador Autônomo"));
    }

    @Test
    void semLoginDa401() throws Exception {
        mvc.perform(get("/os")).andExpect(status().isUnauthorized());
        mvc.perform(get("/os/1")).andExpect(status().isUnauthorized());
        mvc.perform(get("/prestadores")).andExpect(status().isUnauthorized());
        mvc.perform(post("/os").contentType(MediaType.APPLICATION_JSON).content(json(1L, "X", 1L)))
                .andExpect(status().isUnauthorized());
    }

    // ---------- prestadores ----------

    @Test
    void atendenteListaSoOsPrestadores() throws Exception {
        mvc.perform(get("/prestadores").session(login("atendente")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].nome", hasItem("Prestador Teste")))
                .andExpect(jsonPath("$[*].nome", hasItem("Equipe de Manutenção")))
                .andExpect(jsonPath("$[*].nome", hasItem("Prestador Autônomo")))
                .andExpect(jsonPath("$[*].nome", not(hasItem("Atendente Teste"))))
                .andExpect(jsonPath("$[?(@.nome == 'Equipe de Manutenção')].tipoUsuario", hasItem("EQUIPE")));
    }

    @Test
    void prestadorNaoListaPrestadores() throws Exception {
        mvc.perform(get("/prestadores").session(login("prestador"))).andExpect(status().isForbidden());
    }

    // ---------- listar (RF006) ----------

    @Test
    void filtraPorNumero() throws Exception {
        MockHttpSession sessao = login("atendente");
        Cliente cliente = novoCliente("Cliente " + marcaUnica());
        long numero = numero(abrir(sessao, cliente, "prestador", "Por número"));
        abrir(sessao, cliente, "prestador", "Outra");

        mvc.perform(get("/os").param("numero", String.valueOf(numero)).session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].numero").value(numero))
                .andExpect(jsonPath("$[0].descricao").value("Por número"));
    }

    @Test
    void filtraPorParteDoNomeOuDoCpfDoCliente() throws Exception {
        MockHttpSession sessao = login("atendente");
        String marca = marcaUnica();
        Cliente cliente = novoCliente("Maria " + marca + " Souza");
        abrir(sessao, cliente, "prestador", "Do cliente buscado");
        abrir(sessao, novoCliente("Outro " + marcaUnica()), "prestador", "De outro cliente");

        mvc.perform(get("/os").param("cliente", marca.toLowerCase()).session(sessao))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cliente.id").value(cliente.getId()));

        String cpf = cliente.getCpf();
        String cpfComMascara = cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
        mvc.perform(get("/os").param("cliente", cpfComMascara).session(sessao))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].cliente.id").value(cliente.getId()));
    }

    @Test
    void filtraPorStatusECombinaFiltros() throws Exception {
        MockHttpSession sessao = login("atendente");
        String marca = marcaUnica();
        Cliente cliente = novoCliente("Cliente " + marca);
        long idAberta = id(abrir(sessao, cliente, "prestador", "Continua aberta"));
        long idEmExecucao = id(abrir(sessao, cliente, "prestador", "Vai para execução"));
        ordemServicoService.mudarStatus(ordemServicoService.buscarPorId(idEmExecucao), StatusOS.EM_EXECUCAO,
                usuario("atendente"), null);

        mvc.perform(get("/os").param("cliente", marca).param("status", "EM_EXECUCAO").session(sessao))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(idEmExecucao))
                .andExpect(jsonPath("$[0].status").value("EM_EXECUCAO"));
        mvc.perform(get("/os").param("cliente", marca).param("status", "ABERTA").session(sessao))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(idAberta));
        mvc.perform(get("/os").param("cliente", marca).param("status", "PRONTA").session(sessao))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void semFiltroTrazMaisRecentesPrimeiro() throws Exception {
        MockHttpSession sessao = login("atendente");
        String marca = marcaUnica();
        Cliente cliente = novoCliente("Cliente " + marca);
        long primeira = id(abrir(sessao, cliente, "prestador", "Primeira"));
        long segunda = id(abrir(sessao, cliente, "prestador", "Segunda"));

        mvc.perform(get("/os").param("cliente", marca).session(sessao))
                .andExpect(jsonPath("$[0].id").value(segunda))
                .andExpect(jsonPath("$[1].id").value(primeira));
        mvc.perform(get("/os").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) primeira)));
    }

    @Test
    void statusInvalidoDa400() throws Exception {
        mvc.perform(get("/os").param("status", "QUALQUER").session(login("atendente")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("status"));
    }

    @Test
    void prestadorVeSoAsDeleEAtendenteVeTodas() throws Exception {
        MockHttpSession atendente = login("atendente");
        String marca = marcaUnica();
        Cliente cliente = novoCliente("Cliente " + marca);
        long doPrestador = id(abrir(atendente, cliente, "prestador", "Do prestador"));
        long daEquipe = id(abrir(atendente, cliente, "prestador2", "Da equipe"));

        mvc.perform(get("/os").param("cliente", marca).session(login("prestador")))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(doPrestador));
        mvc.perform(get("/os").param("cliente", marca).session(login("prestador2")))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(daEquipe));
        mvc.perform(get("/os").param("cliente", marca).session(atendente))
                .andExpect(jsonPath("$.length()").value(2));
        mvc.perform(get("/os").session(login("prestador")))
                .andExpect(jsonPath("$[*].id", not(hasItem((int) daEquipe))));
    }

    // ---------- detalhar (RF006) ----------

    @Test
    void detalheTrazClientePrestadorOrcamentosEHistorico() throws Exception {
        MockHttpSession atendente = login("atendente");
        Cliente cliente = novoCliente("Cliente " + marcaUnica());
        long id = id(abrir(atendente, cliente, "prestador", "Consertar notebook"));
        OrdemServico os = ordemServicoService.buscarPorId(id);
        Usuario prestador = usuario("prestador");
        Usuario usuarioAtendente = usuario("atendente");

        // Simula o fluxo das próximas partes: um orçamento recusado e outro aprovado.
        ordemServicoService.mudarStatus(os, StatusOS.AGUARDANDO_APROVACAO, prestador, "Orçamento enviado");
        orcamento(os, "Troca da placa", "800.00", StatusOrcamento.RECUSADO, usuarioAtendente);
        ordemServicoService.mudarStatus(os, StatusOS.ABERTA, usuarioAtendente, "Cliente recusou");
        ordemServicoService.mudarStatus(os, StatusOS.AGUARDANDO_APROVACAO, prestador, "Novo orçamento");
        orcamento(os, "Reparo da placa", "350.00", StatusOrcamento.APROVADO, usuarioAtendente);
        ordemServicoService.mudarStatus(os, StatusOS.EM_EXECUCAO, usuarioAtendente, null);

        mvc.perform(get("/os/" + id).session(atendente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").isNumber())
                .andExpect(jsonPath("$.descricao").value("Consertar notebook"))
                .andExpect(jsonPath("$.status").value("EM_EXECUCAO"))
                .andExpect(jsonPath("$.cliente.id").value(cliente.getId()))
                .andExpect(jsonPath("$.cliente.cpfCnpj").value(cliente.getCpf()))
                .andExpect(jsonPath("$.cliente.telefone").value("17999990000"))
                .andExpect(jsonPath("$.prestador.id").value(prestador.getId()))
                .andExpect(jsonPath("$.orcamentos.length()").value(2))
                .andExpect(jsonPath("$.orcamentos[0].status").value("RECUSADO"))
                .andExpect(jsonPath("$.orcamentos[0].valor").value(800.00))
                .andExpect(jsonPath("$.orcamentos[0].decididoPor.nome").value("Atendente Teste"))
                .andExpect(jsonPath("$.orcamentos[1].status").value("APROVADO"))
                .andExpect(jsonPath("$.orcamentos[1].prazoDias").value(3))
                .andExpect(jsonPath("$.historico.length()").value(4))
                .andExpect(jsonPath("$.historico[0].statusAnterior").value("ABERTA"))
                .andExpect(jsonPath("$.historico[0].statusNovo").value("AGUARDANDO_APROVACAO"))
                .andExpect(jsonPath("$.historico[0].observacao").value("Orçamento enviado"))
                .andExpect(jsonPath("$.historico[0].usuario.nome").value("Prestador Teste"))
                .andExpect(jsonPath("$.historico[0].dataHora").isNotEmpty())
                .andExpect(jsonPath("$.historico[1].statusNovo").value("ABERTA"))
                .andExpect(jsonPath("$.historico[2].statusNovo").value("AGUARDANDO_APROVACAO"))
                .andExpect(jsonPath("$.historico[3].statusAnterior").value("AGUARDANDO_APROVACAO"))
                .andExpect(jsonPath("$.historico[3].statusNovo").value("EM_EXECUCAO"))
                .andExpect(jsonPath("$.historico[3].observacao").doesNotExist());
    }

    @Test
    void osRecemAbertaTemListasVazias() throws Exception {
        MockHttpSession atendente = login("atendente");
        long id = id(abrir(atendente, novoCliente("Cliente " + marcaUnica()), "prestador", "Nova"));

        mvc.perform(get("/os/" + id).session(atendente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABERTA"))
                .andExpect(jsonPath("$.orcamentos.length()").value(0))
                .andExpect(jsonPath("$.historico.length()").value(0));
    }

    @Test
    void prestadorVeDetalheDaSuaOs() throws Exception {
        long id = id(abrir(login("atendente"), novoCliente("Cliente " + marcaUnica()), "prestador", "Minha"));

        mvc.perform(get("/os/" + id).session(login("prestador")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void prestadorNaoVeOsDeOutroPrestador() throws Exception {
        long id = id(abrir(login("atendente"), novoCliente("Cliente " + marcaUnica()), "prestador", "Não é sua"));

        mvc.perform(get("/os/" + id).session(login("prestador2")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Você não pode ver a OS de outro prestador."));
    }

    @Test
    void osInexistenteDa404() throws Exception {
        mvc.perform(get("/os/999999").session(login("atendente")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Ordem de serviço não encontrada."));
    }

    // ---------- mudarStatus ----------

    @Test
    void mudarStatusTrocaOStatusEGravaHistorico() throws Exception {
        long id = id(abrir(login("atendente"), novoCliente("Cliente " + marcaUnica()), "prestador", "Status"));
        Usuario prestador = usuario("prestador");
        LocalDateTime antes = LocalDateTime.now().minusSeconds(1);

        Historico h = ordemServicoService.mudarStatus(ordemServicoService.buscarPorId(id),
                StatusOS.AGUARDANDO_APROVACAO, prestador, "  Orçamento gerado  ");

        assertThat(ordemServicoRepository.findById(id).orElseThrow().getStatus()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
        assertThat(h.getId()).isNotNull();
        assertThat(h.getOs().getId()).isEqualTo(id);
        assertThat(h.getStatusAnterior()).isEqualTo(StatusOS.ABERTA);
        assertThat(h.getStatusNovo()).isEqualTo(StatusOS.AGUARDANDO_APROVACAO);
        assertThat(h.getUsuario().getId()).isEqualTo(prestador.getId());
        assertThat(h.getDescricao()).isEqualTo("Orçamento gerado");
        assertThat(h.getDataHistorico()).isAfter(antes);
    }

    // ---------- apoio ----------

    private MockHttpSession login(String login) throws Exception {
        return (MockHttpSession) mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + login + "\",\"senha\":\"123\"}"))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    private String abrir(MockHttpSession sessao, Cliente cliente, String loginPrestador, String descricao) throws Exception {
        return mvc.perform(post("/os").session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content(json(cliente.getId(), descricao, usuario(loginPrestador).getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static long id(String resposta) {
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }

    private static long numero(String resposta) {
        return ((Number) JsonPath.read(resposta, "$.numero")).longValue();
    }

    private Usuario usuario(String login) {
        return usuarioRepository.findByLogin(login).orElseThrow();
    }

    private Cliente novoCliente(String nome) {
        Cliente c = new Cliente();
        c.setNomeCliente(nome);
        c.setCpf(String.format("9%010d", SEQUENCIA.incrementAndGet()));
        c.setTelefone("17999990000");
        return clienteRepository.save(c);
    }

    private void orcamento(OrdemServico os, String descricao, String valor, StatusOrcamento status, Usuario decididoPor) {
        Orcamento o = new Orcamento();
        o.setOs(os);
        o.setDescricao(descricao);
        o.setValorOrcamento(new BigDecimal(valor));
        o.setPrazoDias(3);
        o.setStatus(status);
        o.setDecididoPor(decididoPor);
        o.setDataDecisao(LocalDateTime.now());
        orcamentoRepository.save(o);
    }

    private static String json(Long clienteId, String descricao, Long prestadorId) {
        return "{\"clienteId\":%s,\"descricao\":%s,\"prestadorId\":%s}".formatted(
                clienteId, descricao == null ? "null" : "\"" + descricao + "\"", prestadorId);
    }

    /** Texto único só com letras (com números, a busca também procuraria no CPF). */
    private static String marcaUnica() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 10).chars()
                .mapToObj(c -> String.valueOf(Character.isDigit(c) ? (char) ('g' + (c - '0')) : (char) c))
                .collect(Collectors.joining("", "Os", ""));
    }
}
