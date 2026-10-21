package com.ecogestao.api;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StatusHttpIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @CsvSource({
        "DELETE, /api/dashboard/agua-mensal",
        "POST,   /api/dashboard/reciclagem",
        "PATCH,  /api/agua/1",
        "PUT,    /api/setores/1",
        "DELETE, /api/setores/1"
    })
    void metodoNaoSuportadoRetorna405(String metodo, String url) throws Exception {
        mockMvc.perform(request(org.springframework.http.HttpMethod.valueOf(metodo), url))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.erro").value("Método não permitido"))
                .andExpect(jsonPath("$.mensagem").isNotEmpty())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void rotaInexistenteRetorna404() throws Exception {
        mockMvc.perform(get("/api/inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.erro").value("Endereço não encontrado"));
    }

    @Test
    void jsonMalformadoRetorna400() throws Exception {
        mockMvc.perform(post("/api/agua")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ isto não é json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Requisição inválida"));
    }

    @Test
    void dataInvalidaNoCorpoRetorna400() throws Exception {
        mockMvc.perform(post("/api/agua")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"setor\":{\"id\":1},\"data\":\"2024-13-45\",\"quantidade\":10}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Requisição inválida"));
    }

    @ParameterizedTest
    @CsvSource({
        "/api/agua?dataInicio=abc&dataFim=2024-01-01",
        "/api/energia?setorId=abc",
        "/api/residuos/abc",
        "/api/dashboard/reciclagem?dataInicio=2024-01-01&dataFim=31-01-2024"
    })
    void parametroComFormatoInvalidoRetorna400(String url) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.erro").value("Parâmetro inválido"));
    }

    @Test
    void contentTypeNaoSuportadoRetorna415() throws Exception {
        mockMvc.perform(post("/api/agua")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("texto"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.status").value(415));
    }

    @ParameterizedTest
    @CsvSource({"/api/agua", "/api/energia", "/api/residuos", "/api/setores"})
    void getDeListagemRetorna200(String url) throws Exception {
        mockMvc.perform(get(url)).andExpect(status().isOk());
    }
}