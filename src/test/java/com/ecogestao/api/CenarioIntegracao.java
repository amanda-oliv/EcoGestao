package com.ecogestao.api;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Monta o mesmo cenário dos Dias 19 a 21: dois setores e quatro registros por módulo.
 *
 *   A = setor 1 / março    B = setor 1 / abril
 *   C = setor 2 / março    D = setor 2 / abril
 *
 * As datas ficam em 2024 e cada teste cria seus próprios setores. Assim, os testes
 * não dependem (nem sofrem interferência) dos dados de 2026 já existentes no banco.
 * O @Transactional das classes de teste desfaz tudo ao final.
 */
final class CenarioIntegracao {

    static final LocalDate MARCO = LocalDate.of(2024, 3, 10);
    static final LocalDate ABRIL = LocalDate.of(2024, 4, 2);

    final Long setor1;
    final Long setor2;

    private CenarioIntegracao(Long setor1, Long setor2) {
        this.setor1 = setor1;
        this.setor2 = setor2;
    }

    static CenarioIntegracao criar(MockMvc mockMvc) throws Exception {
        Long s1 = criarSetor(mockMvc, "Setor Teste Filtros 1");
        Long s2 = criarSetor(mockMvc, "Setor Teste Filtros 2");

        // Água (litros): A, B, C, D
        consumo(mockMvc, "agua", s1, MARCO, "150.00");
        consumo(mockMvc, "agua", s1, ABRIL, "200.00");
        consumo(mockMvc, "agua", s2, MARCO, "300.00");
        consumo(mockMvc, "agua", s2, ABRIL, "350.00");

        // Energia (kWh): A, B, C, D
        consumo(mockMvc, "energia", s1, MARCO, "120.50");
        consumo(mockMvc, "energia", s1, ABRIL, "180.75");
        consumo(mockMvc, "energia", s2, MARCO, "250.00");
        consumo(mockMvc, "energia", s2, ABRIL, "310.25");

        // Resíduos (kg): A, B, C, D
        residuo(mockMvc, s1, MARCO, "20.00", "Papel", "Reciclagem");
        residuo(mockMvc, s1, ABRIL, "15.00", "Plástico", "Reciclagem");
        residuo(mockMvc, s2, MARCO, "30.00", "Vidro", "Aterro");
        residuo(mockMvc, s2, ABRIL, "12.00", "Metal", "Empresa especializada");

        return new CenarioIntegracao(s1, s2);
    }

    private static Long criarSetor(MockMvc mockMvc, String nome) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/setores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"" + nome + "\",\"descricao\":\"Criado pelo teste\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        Number id = JsonPath.read(r.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

    private static void consumo(MockMvc mockMvc, String modulo, Long setorId,
                                LocalDate data, String quantidade) throws Exception {
        mockMvc.perform(post("/api/" + modulo)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"setor\":{\"id\":%d},\"data\":\"%s\",\"quantidade\":%s}"
                                .formatted(setorId, data, quantidade)))
                .andExpect(status().isCreated());
    }

    private static void residuo(MockMvc mockMvc, Long setorId, LocalDate data, String quantidade,
                                String tipo, String destinacao) throws Exception {
        mockMvc.perform(post("/api/residuos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"setor\":{\"id\":%d},\"data\":\"%s\",\"tipo\":\"%s\","
                                + "\"quantidade\":%s,\"destinacao\":\"%s\"}")
                                .formatted(setorId, data, tipo, quantidade, destinacao)))
                .andExpect(status().isCreated());
    }
}