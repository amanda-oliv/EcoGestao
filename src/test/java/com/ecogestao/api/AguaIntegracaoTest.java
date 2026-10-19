package com.ecogestao.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional // desfaz no banco tudo o que cada teste gravar
class AguaIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    // ---------- Fluxo completo ----------

    @Test
    @DisplayName("Fluxo completo: cadastro -> consulta -> alteração -> exclusão")
    void fluxoCompleto() throws Exception {
        Long setorId = criarSetor();
        LocalDate hoje = LocalDate.now();

        // 1. Cadastro
        MvcResult criado = mockMvc.perform(post("/api/agua")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(setorId, hoje, "150.00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.quantidade").value(150.0))
                .andReturn();
        Long id = idDe(criado);

        // 2. Consulta (lista e por id)
        mockMvc.perform(get("/api/agua"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(id)).exists());

        mockMvc.perform(get("/api/agua/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.quantidade").value(150.0));

        // 3. Alteração
        mockMvc.perform(put("/api/agua/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(setorId, hoje.minusDays(1), "175.50")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(175.5));

        mockMvc.perform(get("/api/agua/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(175.5))
                .andExpect(jsonPath("$.data").value(hoje.minusDays(1).toString()));

        // 4. Exclusão
        mockMvc.perform(delete("/api/agua/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/agua/" + id))
                .andExpect(status().isNotFound());
    }

    // ---------- Validações ----------

    @Test
    @DisplayName("POST inválido: quantidade negativa, data futura, quantidade ausente")
    void validacoesNoCadastro() throws Exception {
        Long setorId = criarSetor();

        mockMvc.perform(post("/api/agua")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(setorId, LocalDate.now(), "-10.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A quantidade deve ser maior que zero"));

        mockMvc.perform(post("/api/agua")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(setorId, LocalDate.now().plusDays(1), "100.00")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A data não pode ser futura"));

        mockMvc.perform(post("/api/agua")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"setor\":{\"id\":" + setorId + "},\"data\":\"" + LocalDate.now() + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A quantidade é obrigatória"));
    }

    @Test
    @DisplayName("Setor inexistente no POST e no PUT retorna 404")
    void setorInexistente() throws Exception {
        mockMvc.perform(post("/api/agua")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(999999L, LocalDate.now(), "100.00")))
                .andExpect(status().isNotFound());

        Long setorId = criarSetor();
        MvcResult criado = mockMvc.perform(post("/api/agua")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(setorId, LocalDate.now(), "100.00")))
                .andExpect(status().isCreated())
                .andReturn();

        mockMvc.perform(put("/api/agua/" + idDe(criado))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(999999L, LocalDate.now(), "100.00")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Id inexistente em GET, PUT e DELETE retorna 404")
    void idInexistente() throws Exception {
        Long setorId = criarSetor();

        mockMvc.perform(get("/api/agua/999999"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/agua/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(setorId, LocalDate.now(), "100.00")))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/agua/999999"))
                .andExpect(status().isNotFound());
    }

    // ---------- Métodos auxiliares ----------

    private Long criarSetor() throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/setores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Setor Teste IT\",\"descricao\":\"Setor criado pelo teste\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return idDe(resultado);
    }

    private Long idDe(MvcResult resultado) throws Exception {
        Number id = JsonPath.read(resultado.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private String corpo(Long setorId, LocalDate data, String quantidade) {
        return """
                {"setor":{"id":%d},"data":"%s","quantidade":%s}
                """.formatted(setorId, data, quantidade);
    }
}