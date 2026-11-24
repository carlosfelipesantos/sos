package br.com.sos.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarioRepository;

    private MockHttpSession login(String login, String senha) throws Exception {
        MvcResult r = mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + login + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk())
                .andReturn();
        return (MockHttpSession) r.getRequest().getSession(false);
    }

    @Test
    void loginCorretoDevolveUsuarioEPapeis() throws Exception {
        MockHttpSession sessao = login("atendente", "123");
        mvc.perform(get("/auth/me").session(sessao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("atendente"))
                .andExpect(jsonPath("$.cargos[0]").value("ATENDENTE"));
    }

    @Test
    void senhaErradaDa401() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"atendente\",\"senha\":\"errada\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Login ou senha inválidos."));
    }

    @Test
    void loginSemSenhaDa400ComCampo() throws Exception {
        mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"atendente\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.campo").value("senha"));
    }

    @Test
    void semLoginDa401() throws Exception {
        mvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("Faça login para continuar."));
    }

    @Test
    void logoutEncerraSessao() throws Exception {
        MockHttpSession sessao = login("prestador", "123");
        mvc.perform(post("/auth/logout").session(sessao)).andExpect(status().isNoContent());
        mvc.perform(get("/auth/me").session(sessao)).andExpect(status().isUnauthorized());
    }

    @Test
    void senhaGravadaComoHashBcrypt() {
        String hash = usuarioRepository.findByLogin("atendente").orElseThrow().getSenhaHash();
        org.assertj.core.api.Assertions.assertThat(hash).startsWith("$2").isNotEqualTo("123");
    }
}
