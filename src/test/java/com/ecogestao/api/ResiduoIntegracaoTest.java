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
class ResiduoIntegracaoTest {

    private static final String MSG_TIPO_INVALIDO =
            "Valor inválido para tipo: 'Madeira'. Valores aceitos: "
                    + "Papel, Plástico, Vidro, Metal, Orgânico, Eletrônico, Outros";

    private static final String MSG_DESTINACAO_INVALIDA =
            "Valor inválido para destinação: 'Lixão'. Valores aceitos: "
                    + "Reciclagem, Compostagem, Aterro, Empresa especializada";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Fluxo completo: cadastro -> consulta -> alteração -> exclusão")
    void fluxoCompleto() throws Exception {
        Long setorId = criarSetor();
        LocalDate hoje = LocalDate.now();

        // 1. Cadastro
        MvcResult criado = mockMvc.perform(post("/api/residuos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(setorId, hoje, "Papel", "45.50", "Reciclagem")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.tipo").value("Papel"))
                .andExpect(jsonPath("$.quantidade").value(45.5))
                .andExpect(jsonPath("$.destinacao").value("Reciclagem"))
                .andReturn();
        Long id = idDe(criado);

        // 2. Consulta (lista e por id)
        mockMvc.perform(get("/api/residuos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %d)]".formatted(id)).exists());

        mockMvc.perform(get("/api/residuos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.tipo").value("Papel"));

        // 3. Alteração
        mockMvc.perform(put("/api/residuos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(setorId, hoje.minusDays(1), "Vidro", "30.00", "Aterro")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Vidro"))
                .andExpect(jsonPath("$.quantidade").value(30.0))
                .andExpect(jsonPath("$.destinacao").value("Aterro"));

        mockMvc.perform(get("/api/residuos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Vidro"))
                .andExpect(jsonPath("$.destinacao").value("Aterro"))
                .andExpect(jsonPath("$.data").value(hoje.minusDays(1).toString()));

        // 4. Exclusão
        mockMvc.perform(delete("/api/residuos/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/residuos/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST inválido (Bean Validation): quantidade, data, tipo e destinação")
    void validacoesNoCadastro() throws Exception {
        Long setorId = criarSetor();
        LocalDate hoje = LocalDate.now();

        // quantidade negativa
        mockMvc.perform(post("/api/residuos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(setorId, hoje, "Papel", "-10.00", "Reciclagem")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A quantidade deve ser maior que zero"));

        // quantidade zero
        mockMvc.perform(post("/api/residuos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(setorId, hoje, "Papel", "0", "Reciclagem")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A quantidade deve ser maior que zero"));

        // data futura
        mockMvc.perform(post("/api/residuos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(setorId, hoje.plusDays(1), "Papel", "10.00", "Reciclagem")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A data não pode ser futura"));

        // quantidade ausente
        mockMvc.perform(post("/api/residuos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"setor\":{\"id\":" + setorId + "},\"data\":\"" + hoje
                                + "\",\"tipo\":\"Papel\",\"destinacao\":\"Reciclagem\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A quantidade é obrigatória"));

        // tipo vazio
        mockMvc.perform(post("/api/residuos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(setorId, hoje, "", "10.00", "Reciclagem")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("O tipo do resíduo é obrigatório"));

        // destinação ausente
        mockMvc.perform(post("/api/residuos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"setor\":{\"id\":" + setorId + "},\"data\":\"" + hoje
                                + "\",\"tipo\":\"Papel\",\"quantidade\":10.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("A destinação é obrigatória"));
    }

    @Test
    @DisplayName("Valores fora da lista de tipo e destinação retornam 400 (POST e PUT)")
    void valoresForaDaLista() throws Exception {
        Long setorId = criarSetor();
        LocalDate hoje = LocalDate.now();

        // POST com tipo inválido
        mockMvc.perform(post("/api/residuos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(setorId, hoje, "Madeira", "10.00", "Reciclagem")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Valor inválido"))
                .andExpect(jsonPath("$.mensagem").value(MSG_TIPO_INVALIDO));

        // POST com destinação inválida
        mockMvc.perform(post("/api/residuos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(setorId, hoje, "Papel", "10.00", "Lixão")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("Valor inválido"))
                .andExpect(jsonPath("$.mensagem").value(MSG_DESTINACAO_INVALIDA));

        // PUT com tipo e destinação inválidos
        MvcResult criado = mockMvc.perform(post("/api/residuos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(setorId, hoje, "Papel", "10.00", "Reciclagem")))
                .andExpect(status().isCreated())
                .andReturn();
        Long id = idDe(criado);

        mockMvc.perform(put("/api/residuos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(setorId, hoje, "Madeira", "10.00", "Reciclagem")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(MSG_TIPO_INVALIDO));

        mockMvc.perform(put("/api/residuos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(setorId, hoje, "Papel", "10.00", "Lixão")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value(MSG_DESTINACAO_INVALIDA));

        // o registro original permanece intacto após os PUTs rejeitados
        mockMvc.perform(get("/api/residuos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Papel"))
                .andExpect(jsonPath("$.destinacao").value("Reciclagem"));
    }

    @Test
    @DisplayName("Padronização da grafia: maiúsculas, minúsculas e espaços (POST e PUT)")
    void padronizacaoDaGrafia() throws Exception {
        Long setorId = criarSetor();
        LocalDate hoje = LocalDate.now();

        // POST com grafia diferente -> gravado na grafia oficial
        MvcResult criado = mockMvc.perform(post("/api/residuos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(setorId, hoje, "plástico", "12.00", "RECICLAGEM")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("Plástico"))
                .andExpect(jsonPath("$.destinacao").value("Reciclagem"))
                .andReturn();
        Long id = idDe(criado);

        // POST com espaços nas extremidades
        mockMvc.perform(post("/api/residuos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(setorId, hoje, "  Papel  ", "5.00", "  Compostagem ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("Papel"))
                .andExpect(jsonPath("$.destinacao").value("Compostagem"));

        // PUT com grafia diferente
        mockMvc.perform(put("/api/residuos/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(setorId, hoje, "eletrônico", "12.00", "aterro")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Eletrônico"))
                .andExpect(jsonPath("$.destinacao").value("Aterro"));

        // a consulta devolve a grafia oficial
        mockMvc.perform(get("/api/residuos/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Eletrônico"))
                .andExpect(jsonPath("$.destinacao").value("Aterro"));
    }

    @Test
    @DisplayName("Setor inexistente no POST e no PUT retorna 404")
    void setorInexistente() throws Exception {
        mockMvc.perform(post("/api/residuos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(999999L, LocalDate.now(), "Papel", "10.00", "Reciclagem")))
                .andExpect(status().isNotFound());

        Long setorId = criarSetor();
        MvcResult criado = mockMvc.perform(post("/api/residuos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(setorId, LocalDate.now(), "Papel", "10.00", "Reciclagem")))
                .andExpect(status().isCreated())
                .andReturn();

        mockMvc.perform(put("/api/residuos/" + idDe(criado))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(999999L, LocalDate.now(), "Papel", "10.00", "Reciclagem")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Id inexistente em GET, PUT e DELETE retorna 404")
    void idInexistente() throws Exception {
        Long setorId = criarSetor();

        mockMvc.perform(get("/api/residuos/999999"))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/residuos/999999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo(setorId, LocalDate.now(), "Papel", "10.00", "Reciclagem")))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/residuos/999999"))
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

    private String corpo(Long setorId, LocalDate data, String tipo, String quantidade, String destinacao) {
        return """
                {"setor":{"id":%d},"data":"%s","tipo":"%s","quantidade":%s,"destinacao":"%s"}
                """.formatted(setorId, data, tipo, quantidade, destinacao);
    }
}