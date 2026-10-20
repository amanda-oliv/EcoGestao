package com.ecogestao.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Dia 25 — filtros por setor e período (e combinados) nos três módulos.
 * Cada teste roda para agua, energia e residuos (colunas do CsvSource: módulo e as
 * quantidades dos registros A, B, C e D).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FiltrosIntegracaoTest {

    private static final String MARCO = "dataInicio=2024-03-01&dataFim=2024-03-31";
    private static final String ABRIL = "dataInicio=2024-04-01&dataFim=2024-04-30";

    private static final String DADOS = """
            agua,150.00,200.00,300.00,350.00
            energia,120.50,180.75,250.00,310.25
            residuos,20.00,15.00,30.00,12.00
            """;

    @Autowired
    private MockMvc mockMvc;

    private CenarioIntegracao cenario;

    @BeforeEach
    void preparar() throws Exception {
        cenario = CenarioIntegracao.criar(mockMvc);
    }

    @ParameterizedTest(name = "{0}: setor + período isolam um único registro")
    @CsvSource(textBlock = DADOS)
    void setorEPeriodoCombinados(String modulo, double a, double b, double c, double d) throws Exception {
        esperaUnico("/api/" + modulo + "?setorId=" + cenario.setor1 + "&" + MARCO, a, cenario.setor1);
        esperaUnico("/api/" + modulo + "?setorId=" + cenario.setor1 + "&" + ABRIL, b, cenario.setor1);
        esperaUnico("/api/" + modulo + "?setorId=" + cenario.setor2 + "&" + MARCO, c, cenario.setor2);
        esperaUnico("/api/" + modulo + "?setorId=" + cenario.setor2 + "&" + ABRIL, d, cenario.setor2);
    }

    @ParameterizedTest(name = "{0}: intervalo de um único dia é inclusivo")
    @CsvSource(textBlock = DADOS)
    void intervaloDeUmDia(String modulo, double a, double b, double c, double d) throws Exception {
        esperaUnico("/api/" + modulo + "?setorId=" + cenario.setor1
                + "&dataInicio=2024-03-10&dataFim=2024-03-10", a, cenario.setor1);
    }

    @ParameterizedTest(name = "{0}: filtro só por setor retorna os dois registros do setor")
    @CsvSource(textBlock = DADOS)
    void somenteSetor(String modulo, double a, double b, double c, double d) throws Exception {
        mockMvc.perform(get("/api/" + modulo + "?setorId=" + cenario.setor1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.quantidade == " + a + ")]").exists())
                .andExpect(jsonPath("$[?(@.quantidade == " + b + ")]").exists());

        mockMvc.perform(get("/api/" + modulo + "?setorId=" + cenario.setor2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.quantidade == " + c + ")]").exists())
                .andExpect(jsonPath("$[?(@.quantidade == " + d + ")]").exists());
    }

    @ParameterizedTest(name = "{0}: filtro só por período traz os dois setores")
    @CsvSource(textBlock = DADOS)
    void somentePeriodo(String modulo, double a, double b, double c, double d) throws Exception {
        mockMvc.perform(get("/api/" + modulo + "?" + MARCO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.setor.id == " + cenario.setor1 + ")]").exists())
                .andExpect(jsonPath("$[?(@.setor.id == " + cenario.setor2 + ")]").exists())
                .andExpect(jsonPath("$[?(@.quantidade == " + a + ")]").exists())
                .andExpect(jsonPath("$[?(@.quantidade == " + c + ")]").exists())
                // registros de abril não podem aparecer no filtro de março
                .andExpect(jsonPath("$[?(@.quantidade == " + b + ")]").doesNotExist())
                .andExpect(jsonPath("$[?(@.quantidade == " + d + ")]").doesNotExist());
    }

    @ParameterizedTest(name = "{0}: setor válido sem dados no período retorna lista vazia")
    @CsvSource(textBlock = DADOS)
    void semDadosNoPeriodo(String modulo, double a, double b, double c, double d) throws Exception {
        mockMvc.perform(get("/api/" + modulo + "?setorId=" + cenario.setor1
                        + "&dataInicio=2024-11-01&dataFim=2024-11-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @ParameterizedTest(name = "{0}: setor inexistente retorna 404")
    @CsvSource(textBlock = DADOS)
    void setorInexistente(String modulo, double a, double b, double c, double d) throws Exception {
        mockMvc.perform(get("/api/" + modulo + "?setorId=999999&" + MARCO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Setor com id 999999 não encontrado"));
    }

    @ParameterizedTest(name = "{0}: período incompleto retorna 400")
    @CsvSource(textBlock = DADOS)
    void periodoIncompleto(String modulo, double a, double b, double c, double d) throws Exception {
        mockMvc.perform(get("/api/" + modulo + "?setorId=" + cenario.setor1 + "&dataInicio=2024-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem")
                        .value("Informe dataInicio e dataFim juntas para filtrar por período"));

        mockMvc.perform(get("/api/" + modulo + "?dataFim=2024-03-31"))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest(name = "{0}: intervalo invertido retorna 400")
    @CsvSource(textBlock = DADOS)
    void intervaloInvertido(String modulo, double a, double b, double c, double d) throws Exception {
        mockMvc.perform(get("/api/" + modulo + "?setorId=" + cenario.setor1
                        + "&dataInicio=2024-04-01&dataFim=2024-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("dataInicio não pode ser posterior a dataFim"));
    }

    // ---------- Auxiliar ----------

    private void esperaUnico(String url, double quantidade, Long setorId) throws Exception {
        mockMvc.perform(get(url))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].quantidade").value(quantidade))
                .andExpect(jsonPath("$[0].setor.id").value(setorId));
    }
}