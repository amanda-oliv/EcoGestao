package com.ecogestao.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
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
 * Dia 25 — endpoints agregados do dashboard:
 * agua-mensal, energia-mensal, residuos-mensal, reciclagem e consumo-por-setor.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DashboardIntegracaoTest {

    private static final String PERIODO = "dataInicio=2024-03-01&dataFim=2024-04-30";
    private static final String MARCO = "dataInicio=2024-03-01&dataFim=2024-03-31";
    private static final String ABRIL = "dataInicio=2024-04-01&dataFim=2024-04-30";
    private static final String NOVEMBRO = "dataInicio=2024-11-01&dataFim=2024-11-30";

    // módulo do endpoint mensal + quantidades A, B, C, D
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

    // ---------- Totais mensais ----------

    @ParameterizedTest(name = "{0}-mensal por setor: um item por mês, em ordem cronológica")
    @CsvSource(textBlock = DADOS)
    void mensalPorSetor(String modulo, double a, double b, double c, double d) throws Exception {
        mockMvc.perform(get("/api/dashboard/" + modulo + "-mensal?setorId=" + cenario.setor1 + "&" + PERIODO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ano").value(2024))
                .andExpect(jsonPath("$[0].mes").value(3))
                .andExpect(jsonPath("$[0].rotulo").value("Março/2024"))
                .andExpect(jsonPath("$[0].total").value(a))
                .andExpect(jsonPath("$[1].mes").value(4))
                .andExpect(jsonPath("$[1].rotulo").value("Abril/2024"))
                .andExpect(jsonPath("$[1].total").value(b));
    }

    @ParameterizedTest(name = "{0}-mensal sem filtro de setor soma os dois setores")
    @CsvSource(textBlock = DADOS)
    void mensalSomaOsSetores(String modulo, double a, double b, double c, double d) throws Exception {
        mockMvc.perform(get("/api/dashboard/" + modulo + "-mensal?" + PERIODO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].total").value(a + c))
                .andExpect(jsonPath("$[1].total").value(b + d));
    }

    @ParameterizedTest(name = "{0}-mensal com período de um mês retorna só aquele mês")
    @CsvSource(textBlock = DADOS)
    void mensalUmMes(String modulo, double a, double b, double c, double d) throws Exception {
        mockMvc.perform(get("/api/dashboard/" + modulo + "-mensal?" + ABRIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].mes").value(4))
                .andExpect(jsonPath("$[0].total").value(b + d));
    }

    @ParameterizedTest(name = "{0}-mensal sem dados retorna lista vazia")
    @CsvSource(textBlock = DADOS)
    void mensalSemDados(String modulo, double a, double b, double c, double d) throws Exception {
        mockMvc.perform(get("/api/dashboard/" + modulo + "-mensal?setorId=" + cenario.setor1 + "&" + NOVEMBRO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @ParameterizedTest(name = "{0}-mensal: erros 404 e 400")
    @CsvSource(textBlock = DADOS)
    void mensalErros(String modulo, double a, double b, double c, double d) throws Exception {
        mockMvc.perform(get("/api/dashboard/" + modulo + "-mensal?setorId=999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensagem").value("Setor com id 999999 não encontrado"));

        mockMvc.perform(get("/api/dashboard/" + modulo + "-mensal?dataInicio=2024-03-01"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/dashboard/" + modulo + "-mensal?dataInicio=2024-04-01&dataFim=2024-03-01"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Reciclagem ----------

    @Test
    @DisplayName("reciclagem: setor 1 (tudo reciclado) e setor 2 (nada reciclado)")
    void reciclagemPorSetor() throws Exception {
        mockMvc.perform(get("/api/dashboard/reciclagem?setorId=" + cenario.setor1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGerado").value(35.0))
                .andExpect(jsonPath("$.totalReciclado").value(35.0))
                .andExpect(jsonPath("$.totalNaoReciclado").value(0.0))
                .andExpect(jsonPath("$.percentualReciclagem").value(100.0));

        mockMvc.perform(get("/api/dashboard/reciclagem?setorId=" + cenario.setor2))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGerado").value(42.0))
                .andExpect(jsonPath("$.totalReciclado").value(0.0))
                .andExpect(jsonPath("$.totalNaoReciclado").value(42.0))
                .andExpect(jsonPath("$.percentualReciclagem").value(0.0));
    }

    @Test
    @DisplayName("reciclagem por período: março = 40,00% e abril = 55,56% (arredondamento HALF_UP)")
    void reciclagemPorPeriodo() throws Exception {
        // Sem filtro de setor o resultado soma os dois setores do cenário.
        mockMvc.perform(get("/api/dashboard/reciclagem?" + MARCO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGerado").value(50.0))
                .andExpect(jsonPath("$.totalReciclado").value(20.0))
                .andExpect(jsonPath("$.percentualReciclagem").value(40.0));

        mockMvc.perform(get("/api/dashboard/reciclagem?" + ABRIL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGerado").value(27.0))
                .andExpect(jsonPath("$.totalReciclado").value(15.0))
                .andExpect(jsonPath("$.percentualReciclagem").value(55.56));
    }

    @Test
    @DisplayName("reciclagem: sem dados retorna zeros; setor inexistente e período inválido retornam erro")
    void reciclagemCasosLimite() throws Exception {
        mockMvc.perform(get("/api/dashboard/reciclagem?setorId=" + cenario.setor1 + "&" + NOVEMBRO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalGerado").value(0.0))
                .andExpect(jsonPath("$.totalReciclado").value(0.0))
                .andExpect(jsonPath("$.percentualReciclagem").value(0.0));

        mockMvc.perform(get("/api/dashboard/reciclagem?setorId=999999"))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/dashboard/reciclagem?dataInicio=2024-03-01"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Consumo por setor ----------

    @Test
    @DisplayName("consumo-por-setor: água, energia e resíduos consolidados por setor")
    void consumoPorSetor() throws Exception {
        mockMvc.perform(get("/api/dashboard/consumo-por-setor?" + PERIODO))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtro(cenario.setor1) + ".agua").value(350.0))
                .andExpect(jsonPath(filtro(cenario.setor1) + ".energia").value(301.25))
                .andExpect(jsonPath(filtro(cenario.setor1) + ".residuos").value(35.0))
                .andExpect(jsonPath(filtro(cenario.setor2) + ".agua").value(650.0))
                .andExpect(jsonPath(filtro(cenario.setor2) + ".energia").value(560.25))
                .andExpect(jsonPath(filtro(cenario.setor2) + ".residuos").value(42.0));
    }

    @Test
    @DisplayName("consumo-por-setor: filtro de período restringe os valores (março)")
    void consumoPorSetorEmMarco() throws Exception {
        mockMvc.perform(get("/api/dashboard/consumo-por-setor?" + MARCO))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtro(cenario.setor1) + ".agua").value(150.0))
                .andExpect(jsonPath(filtro(cenario.setor1) + ".energia").value(120.5))
                .andExpect(jsonPath(filtro(cenario.setor1) + ".residuos").value(20.0))
                .andExpect(jsonPath(filtro(cenario.setor2) + ".agua").value(300.0))
                .andExpect(jsonPath(filtro(cenario.setor2) + ".energia").value(250.0))
                .andExpect(jsonPath(filtro(cenario.setor2) + ".residuos").value(30.0));
    }

    @Test
    @DisplayName("consumo-por-setor: setores sem dados no período continuam listados, zerados")
    void consumoPorSetorSemDados() throws Exception {
        mockMvc.perform(get("/api/dashboard/consumo-por-setor?" + NOVEMBRO))
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtro(cenario.setor1) + ".setor").value("Setor Teste Filtros 1"))
                .andExpect(jsonPath(filtro(cenario.setor1) + ".agua").value(0.0))
                .andExpect(jsonPath(filtro(cenario.setor1) + ".energia").value(0.0))
                .andExpect(jsonPath(filtro(cenario.setor1) + ".residuos").value(0.0))
                .andExpect(jsonPath(filtro(cenario.setor2) + ".agua").value(0.0));
    }

    @Test
    @DisplayName("consumo-por-setor: intervalo invertido e período incompleto retornam 400")
    void consumoPorSetorErros() throws Exception {
        mockMvc.perform(get("/api/dashboard/consumo-por-setor?dataInicio=2024-04-01&dataFim=2024-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensagem").value("dataInicio não pode ser posterior a dataFim"));

        mockMvc.perform(get("/api/dashboard/consumo-por-setor?dataInicio=2024-03-01"))
                .andExpect(status().isBadRequest());
    }

    // ---------- Auxiliar ----------

    private static String filtro(Long setorId) {
        return "$[?(@.setorId == %d)]".formatted(setorId);
    }
}