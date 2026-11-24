package br.com.sos.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Garante que @PreAuthorize recusa no servidor quem não tem o cargo (RNF002). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PermissaoPorCargoTest.RotasDeTeste.class)
class PermissaoPorCargoTest {

    @TestConfiguration
    @RestController
    static class RotasDeTeste {
        @GetMapping("/teste/atendente")
        @PreAuthorize("hasRole('ATENDENTE')")
        String atendente() {
            return "ok";
        }

        @GetMapping("/teste/prestador")
        @PreAuthorize("hasRole('PRESTADOR')")
        String prestador() {
            return "ok";
        }
    }

    @Autowired
    MockMvc mvc;

    private MockHttpSession login(String login) throws Exception {
        return (MockHttpSession) mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"login\":\"" + login + "\",\"senha\":\"123\"}"))
                .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
    }

    @Test
    void prestadorNaoAcessaFuncaoDeAtendente() throws Exception {
        mvc.perform(get("/teste/atendente").session(login("prestador")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensagem").value("Você não tem permissão para usar esta função."));
    }

    @Test
    void atendenteNaoAcessaFuncaoDePrestador() throws Exception {
        mvc.perform(get("/teste/prestador").session(login("atendente")))
                .andExpect(status().isForbidden());
    }

    @Test
    void cadaCargoAcessaSuaFuncao() throws Exception {
        mvc.perform(get("/teste/atendente").session(login("atendente"))).andExpect(status().isOk());
        mvc.perform(get("/teste/prestador").session(login("prestador"))).andExpect(status().isOk());
    }

    @Test
    void autonomoAcessaAsDuas() throws Exception {
        MockHttpSession s = login("autonomo");
        mvc.perform(get("/teste/atendente").session(s)).andExpect(status().isOk());
        mvc.perform(get("/teste/prestador").session(s)).andExpect(status().isOk());
    }
}
