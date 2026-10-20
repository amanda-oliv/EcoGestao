package com.ecogestao.api;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Dia 26 — toda resposta de erro da API deve ter a mesma estrutura JSON:
 * timestamp, status (igual ao status HTTP), erro e mensagem.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ErroPadraoIntegracaoTest {

    @Autowired
    private MockMvc mockMvc;

    private Long setorId;

    @BeforeEach
    void criarSetor() throws Exception {
        MvcResult r = mockMvc.perform(post("/api/setores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Setor Teste Erros\",\"descricao\":\"Criado pelo teste\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
        setorId = id.longValue();
    }

    // ---------- 404: id inexistente (antes, GET/PUT/DELETE respondiam 404 sem corpo) ----------

    @ParameterizedTest(name = "{0}: GET, PUT e DELETE com id inexistente retornam 404 padronizado")
    @ValueSource(strings = {"agua", "energia", "residuos"})
    void idInexistente(String modulo) throws Exception {
        erroPadrao(mockMvc.perform(get("/api/" + modulo + "/999999")), 404)
                .andExpect(jsonPath("$.mensagem").value(containsString("999999")));

        erroPadrao(mockMvc.perform(put("/api/" + modulo + "/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(modulo, setorId, "100.00"))), 404)
                .andExpect(jsonPath("$.mensagem").value(containsString("999999")));

        erroPadrao(mockMvc.perform(delete("/api/" + modulo + "/999999")), 404)
                .andExpect(jsonPath("$.mensagem").value(containsString("999999")));
    }

    @Test
    @DisplayName("setores: GET por id inexistente retorna 404 padronizado")
    void setorNaoEncontradoPorId() throws Exception {
        erroPadrao(mockMvc.perform(get("/api/setores/999999")), 404)
                .andExpect(jsonPath("$.mensagem").value("Setor com id 999999 não encontrado"));
    }

    // ---------- 404: setor inexistente no cadastro ----------

    @ParameterizedTest(name = "{0}: POST com setor inexistente retorna 404 padronizado")
    @ValueSource(strings = {"agua", "energia", "residuos"})
    void setorInexistenteNoCadastro(String modulo) throws Exception {
        erroPadrao(mockMvc.perform(post("/api/" + modulo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(modulo, 999999L, "10.00"))), 404)
                .andExpect(jsonPath("$.mensagem").value("Setor com id 999999 não encontrado"));
    }

    // ---------- 400: validações ----------

    @ParameterizedTest(name = "{0}: POST com quantidade negativa retorna 400 padronizado")
    @ValueSource(strings = {"agua", "energia", "residuos"})
    void validacaoNoCadastro(String modulo) throws Exception {
        erroPadrao(mockMvc.perform(post("/api/" + modulo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(modulo, setorId, "-10.00"))), 400)
                .andExpect(jsonPath("$.erro").value("Erro de validação"))
                .andExpect(jsonPath("$.mensagem").value("A quantidade deve ser maior que zero"));
    }

    @Test
    @DisplayName("setores: POST sem nome retorna 400 padronizado (antes retornava 500)")
    void setorSemNome() throws Exception {
        erroPadrao(mockMvc.perform(post("/api/setores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descricao\":\"sem nome\"}")), 400)
                .andExpect(jsonPath("$.erro").value("Erro de validação"))
                .andExpect(jsonPath("$.mensagem").value("O nome do setor é obrigatório"));
    }

    @Test
    @DisplayName("resíduos: tipo fora da lista retorna 400 padronizado")
    void valorInvalidoEmResiduos() throws Exception {
        erroPadrao(mockMvc.perform(post("/api/residuos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"setor\":{\"id\":%d},\"data\":\"2024-03-10\",\"tipo\":\"Madeira\","
                                + "\"quantidade\":10.00,\"destinacao\":\"Reciclagem\"}").formatted(setorId))), 400)
                .andExpect(jsonPath("$.erro").value("Valor inválido"));
    }

    // ---------- 400: período invertido em TODOS os endpoints de listagem e de dashboard ----------

    @ParameterizedTest(name = "{0}: intervalo invertido retorna 400 padronizado")
    @ValueSource(strings = {
            "/api/agua",
            "/api/energia",
            "/api/residuos",
            "/api/dashboard/agua-mensal",
            "/api/dashboard/energia-mensal",
            "/api/dashboard/residuos-mensal",
            "/api/dashboard/reciclagem",
            "/api/dashboard/consumo-por-setor"})
    void periodoInvertido(String endpoint) throws Exception {
        erroPadrao(mockMvc.perform(get(endpoint + "?dataInicio=2024-04-01&dataFim=2024-03-01")), 400)
                .andExpect(jsonPath("$.mensagem").value("dataInicio não pode ser posterior a dataFim"));
    }

    // ---------- Auxiliares ----------

    /** Confere status HTTP, content type JSON e os quatro campos do ErroResposta. */
    private ResultActions erroPadrao(ResultActions resultado, int esperado) throws Exception {
        return resultado
                .andExpect(status().is(esperado))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.status").value(esperado))
                .andExpect(jsonPath("$.erro").isNotEmpty())
                .andExpect(jsonPath("$.mensagem").isNotEmpty());
    }

    private String corpo(String modulo, Long setor, String quantidade) {
        if ("residuos".equals(modulo)) {
            return ("{\"setor\":{\"id\":%d},\"data\":\"2024-03-10\",\"tipo\":\"Papel\","
                    + "\"quantidade\":%s,\"destinacao\":\"Reciclagem\"}").formatted(setor, quantidade);
        }
        return "{\"setor\":{\"id\":%d},\"data\":\"2024-03-10\",\"quantidade\":%s}".formatted(setor, quantidade);
    }
}