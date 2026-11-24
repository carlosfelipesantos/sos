package br.com.sos.cliente;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** RF001: os 4 endpoints de clientes, as validações e a permissão por cargo (RNF002). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClienteControllerTest {

    /** Gera CPFs/CNPJs diferentes a cada teste, para um teste não atrapalhar o outro no mesmo banco. */
    private static final AtomicLong SEQUENCIA = new AtomicLong(ThreadLocalRandom.current().nextLong(1_000_000));

    @Autowired
    MockMvc mvc;

    // ---------- cadastrar ----------

    @Test
    void cadastraClienteComCpfComMascara() throws Exception {
        String cpf = novoCpf();
        mvc.perform(post("/clientes").session(login("atendente")).contentType(MediaType.APPLICATION_JSON)
                        .content(json("  Maria da Silva  ", comMascaraCpf(cpf), "(17) 99999-0000", "maria@email.com")))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/clientes/")))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.nome").value("Maria da Silva"))
                .andExpect(jsonPath("$.cpfCnpj").value(cpf))
                .andExpect(jsonPath("$.telefone").value("(17) 99999-0000"))
                .andExpect(jsonPath("$.email").value("maria@email.com"))
                .andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void cadastraClienteComCnpjESemEmail() throws Exception {
        String cnpj = novoCnpj();
        mvc.perform(post("/clientes").session(login("atendente")).contentType(MediaType.APPLICATION_JSON)
                        .content(json("Oficina Exemplo LTDA", cnpj, "1733334444", "")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cpfCnpj").value(cnpj))
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    @Test
    void cpfComDigitoErradoDa400() throws Exception {
        mvc.perform(post("/clientes").session(login("atendente")).contentType(MediaType.APPLICATION_JSON)
                        .content(json("João", "529.982.247-26", "17999990000", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("cpfCnpj"))
                .andExpect(jsonPath("$.mensagem").value("CPF ou CNPJ inválido. Confira os números digitados."));
    }

    @Test
    void cpfRepetidoDa409MesmoComMascaraDiferente() throws Exception {
        String cpf = novoCpf();
        MockHttpSession sessao = login("atendente");
        cadastrar(sessao, "Primeiro", cpf);

        mvc.perform(post("/clientes").session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content(json("Segundo", comMascaraCpf(cpf), "17999990000", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.campo").value("cpfCnpj"))
                .andExpect(jsonPath("$.mensagem").value("Cliente já cadastrado."));
    }

    @Test
    void nomeEmBrancoDa400() throws Exception {
        mvc.perform(post("/clientes").session(login("atendente")).contentType(MediaType.APPLICATION_JSON)
                        .content(json("   ", novoCpf(), "17999990000", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("nome"))
                .andExpect(jsonPath("$.mensagem").value("Informe o nome do cliente."));
    }

    @Test
    void semTelefoneDa400() throws Exception {
        mvc.perform(post("/clientes").session(login("atendente")).contentType(MediaType.APPLICATION_JSON)
                        .content(json("Maria", novoCpf(), null, null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("telefone"));
    }

    @Test
    void semCpfCnpjDa400() throws Exception {
        mvc.perform(post("/clientes").session(login("atendente")).contentType(MediaType.APPLICATION_JSON)
                        .content(json("Maria", "", "17999990000", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("cpfCnpj"));
    }

    @Test
    void emailInvalidoDa400() throws Exception {
        mvc.perform(post("/clientes").session(login("atendente")).contentType(MediaType.APPLICATION_JSON)
                        .content(json("Maria", novoCpf(), "17999990000", "maria.email.com")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("email"));
    }

    // ---------- editar ----------

    @Test
    void editaClienteMantendoOProprioCpf() throws Exception {
        String cpf = novoCpf();
        MockHttpSession sessao = login("atendente");
        long id = cadastrar(sessao, "Nome Antigo", cpf);

        mvc.perform(put("/clientes/" + id).session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content(json("Nome Novo", comMascaraCpf(cpf), "17911112222", "novo@email.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.nome").value("Nome Novo"))
                .andExpect(jsonPath("$.telefone").value("17911112222"))
                .andExpect(jsonPath("$.email").value("novo@email.com"));

        mvc.perform(get("/clientes/" + id).session(sessao))
                .andExpect(jsonPath("$.nome").value("Nome Novo"));
    }

    @Test
    void editarParaCpfDeOutroClienteDa409() throws Exception {
        MockHttpSession sessao = login("atendente");
        String cpfDoPrimeiro = novoCpf();
        cadastrar(sessao, "Primeiro", cpfDoPrimeiro);
        long idDoSegundo = cadastrar(sessao, "Segundo", novoCpf());

        mvc.perform(put("/clientes/" + idDoSegundo).session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content(json("Segundo", cpfDoPrimeiro, "17999990000", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem").value("Cliente já cadastrado."));
    }

    @Test
    void editarComCpfInvalidoDa400() throws Exception {
        MockHttpSession sessao = login("atendente");
        long id = cadastrar(sessao, "Cliente", novoCpf());

        mvc.perform(put("/clientes/" + id).session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content(json("Cliente", "111.111.111-11", "17999990000", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("cpfCnpj"));
    }

    @Test
    void editarClienteInexistenteDa404() throws Exception {
        mvc.perform(put("/clientes/999999").session(login("atendente")).contentType(MediaType.APPLICATION_JSON)
                        .content(json("Ninguém", novoCpf(), "17999990000", null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Cliente não encontrado."));
    }

    // ---------- buscar ----------

    @Test
    void buscaPorParteDoNomeSemDiferenciarMaiusculas() throws Exception {
        MockHttpSession sessao = login("atendente");
        String marca = marcaUnica();
        long id = cadastrar(sessao, "Carlos " + marca + " Souza", novoCpf());

        mvc.perform(get("/clientes").param("busca", marca.toLowerCase()).session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    void buscaPorCpfComOuSemMascara() throws Exception {
        MockHttpSession sessao = login("atendente");
        String cpf = novoCpf();
        long id = cadastrar(sessao, "Cliente por CPF", cpf);

        mvc.perform(get("/clientes").param("busca", comMascaraCpf(cpf)).session(sessao))
                .andExpect(jsonPath("$[*].id", hasItem((int) id)));
        mvc.perform(get("/clientes").param("busca", cpf.substring(0, 9)).session(sessao))
                .andExpect(jsonPath("$[*].id", hasItem((int) id)));
    }

    @Test
    void buscaPorNomeQueNaoExisteNaoTrazNinguem() throws Exception {
        MockHttpSession sessao = login("atendente");
        cadastrar(sessao, "Alguém", novoCpf());

        mvc.perform(get("/clientes").param("busca", "nome-que-nao-existe-" + marcaUnica()).session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void semBuscaListaTodos() throws Exception {
        MockHttpSession sessao = login("atendente");
        long id = cadastrar(sessao, "Listado", novoCpf());

        mvc.perform(get("/clientes").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) id)));
    }

    @Test
    void clienteCadastradoApareceNaPesquisaNaHora() throws Exception {
        MockHttpSession sessao = login("atendente");
        String marca = marcaUnica();
        mvc.perform(get("/clientes").param("busca", marca).session(sessao))
                .andExpect(jsonPath("$.length()").value(0));

        cadastrar(sessao, "Novo " + marca, novoCpf());

        mvc.perform(get("/clientes").param("busca", marca).session(sessao))
                .andExpect(jsonPath("$.length()").value(1));
    }

    // ---------- buscar por id ----------

    @Test
    void mostraUmCliente() throws Exception {
        MockHttpSession sessao = login("atendente");
        String cpf = novoCpf();
        long id = cadastrar(sessao, "Mostrar", cpf);

        mvc.perform(get("/clientes/" + id).session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Mostrar"))
                .andExpect(jsonPath("$.cpfCnpj").value(cpf));
    }

    @Test
    void clienteInexistenteDa404() throws Exception {
        mvc.perform(get("/clientes/999999").session(login("atendente")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Cliente não encontrado."));
    }

    // ---------- permissão (RNF002) ----------

    @Test
    void prestadorNaoUsaNenhumEndpointDeClientes() throws Exception {
        long id = cadastrar(login("atendente"), "Protegido", novoCpf());
        MockHttpSession prestador = login("prestador");

        mvc.perform(post("/clientes").session(prestador).contentType(MediaType.APPLICATION_JSON)
                        .content(json("X", novoCpf(), "17999990000", null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Você não tem permissão para usar esta função."));
        mvc.perform(put("/clientes/" + id).session(prestador).contentType(MediaType.APPLICATION_JSON)
                        .content(json("X", novoCpf(), "17999990000", null)))
                .andExpect(status().isForbidden());
        mvc.perform(get("/clientes").session(prestador)).andExpect(status().isForbidden());
        mvc.perform(get("/clientes/" + id).session(prestador)).andExpect(status().isForbidden());
    }

    @Test
    void semLoginDa401() throws Exception {
        mvc.perform(get("/clientes")).andExpect(status().isUnauthorized());
        mvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content(json("X", novoCpf(), "17999990000", null)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void autonomoTambemCadastra() throws Exception {
        mvc.perform(post("/clientes").session(login("autonomo")).contentType(MediaType.APPLICATION_JSON)
                        .content(json("Cliente do Autônomo", novoCpf(), "17999990000", null)))
                .andExpect(status().isCreated());
    }

    // ---------- apoio ----------

    private MockHttpSession login(String login) throws Exception {
        return (MockHttpSession) mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + login + "\",\"senha\":\"123\"}"))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    private long cadastrar(MockHttpSession sessao, String nome, String cpfCnpj) throws Exception {
        String resposta = mvc.perform(post("/clientes").session(sessao).contentType(MediaType.APPLICATION_JSON)
                        .content(json(nome, cpfCnpj, "17999990000", null)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(resposta, "$.id")).longValue();
    }

    private static String json(String nome, String cpfCnpj, String telefone, String email) {
        return "{\"nome\":%s,\"cpfCnpj\":%s,\"telefone\":%s,\"email\":%s}"
                .formatted(texto(nome), texto(cpfCnpj), texto(telefone), texto(email));
    }

    private static String texto(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    /** Texto único só com letras (com números, a busca também procuraria no CPF e poderia achar outro cliente). */
    private static String marcaUnica() {
        StringBuilder marca = new StringBuilder("Teste");
        for (char c : UUID.randomUUID().toString().replace("-", "").substring(0, 10).toCharArray()) {
            marca.append(Character.isDigit(c) ? (char) ('g' + (c - '0')) : c);
        }
        return marca.toString();
    }

    /** CPF válido e diferente a cada chamada (só números). */
    static String novoCpf() {
        String base = String.format("%09d", 100_000_000 + SEQUENCIA.incrementAndGet());
        return comDigitos(base);
    }

    /** CNPJ válido e diferente a cada chamada (só números). */
    static String novoCnpj() {
        String base = String.format("%08d", 10_000_000 + SEQUENCIA.incrementAndGet()) + "0001";
        return comDigitos(base);
    }

    /** Acha os 2 dígitos verificadores que tornam o número válido. */
    private static String comDigitos(String base) {
        for (int dv = 0; dv < 100; dv++) {
            String numero = base + String.format("%02d", dv);
            if (CpfCnpjValidator.valido(numero)) {
                return numero;
            }
        }
        throw new IllegalStateException("Sem dígito verificador para " + base);
    }

    private static String comMascaraCpf(String cpf) {
        return cpf.substring(0, 3) + "." + cpf.substring(3, 6) + "." + cpf.substring(6, 9) + "-" + cpf.substring(9);
    }
}
