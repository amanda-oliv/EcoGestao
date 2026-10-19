package com.ecogestao.api.service;

import com.ecogestao.api.dto.ConsumoPorSetorDTO;
import com.ecogestao.api.dto.ReciclagemDTO;
import com.ecogestao.api.dto.TotalMensalDTO;
import com.ecogestao.api.model.Residuo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;


@Service
public class DashboardService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2);

    @Autowired
    private ConsumoAguaService consumoAguaService;

    @Autowired
    private ConsumoEnergiaService consumoEnergiaService;

    @Autowired
    private ResiduoService residuoService;

    @Autowired
    private SetorService setorService;

    // ---------- Dia 20: totais mensais ----------

    public List<TotalMensalDTO> aguaMensal(Long setorId, LocalDate dataInicio, LocalDate dataFim) {
        return agruparPorMes(
                consumoAguaService.listarComFiltros(setorId, dataInicio, dataFim),
                c -> c.getData(), c -> c.getQuantidade());
    }

    public List<TotalMensalDTO> energiaMensal(Long setorId, LocalDate dataInicio, LocalDate dataFim) {
        return agruparPorMes(
                consumoEnergiaService.listarComFiltros(setorId, dataInicio, dataFim),
                c -> c.getData(), c -> c.getQuantidade());
    }

    public List<TotalMensalDTO> residuosMensal(Long setorId, LocalDate dataInicio, LocalDate dataFim) {
        return agruparPorMes(
                residuoService.listarComFiltros(setorId, dataInicio, dataFim),
                r -> r.getData(), r -> r.getQuantidade());
    }

    // ---------- Dia 21: percentual de reciclagem e consumo por setor ----------

    public ReciclagemDTO reciclagem(Long setorId, LocalDate dataInicio, LocalDate dataFim) {
        List<Residuo> residuos = residuoService.listarComFiltros(setorId, dataInicio, dataFim);

        BigDecimal total = ZERO;
        BigDecimal reciclado = ZERO;
        for (Residuo r : residuos) {
            total = total.add(r.getQuantidade());
            if ("Reciclagem".equals(r.getDestinacao())) {
                reciclado = reciclado.add(r.getQuantidade());
            }
        }

        BigDecimal percentual = total.signum() == 0
                ? ZERO
                : reciclado.multiply(BigDecimal.valueOf(100))
                           .divide(total, 2, RoundingMode.HALF_UP);

        return new ReciclagemDTO(total, reciclado, total.subtract(reciclado), percentual);
    }

    public List<ConsumoPorSetorDTO> consumoPorSetor(LocalDate dataInicio, LocalDate dataFim) {
        Map<Long, BigDecimal> agua = somarPorSetor(
                consumoAguaService.listarComFiltros(null, dataInicio, dataFim),
                c -> c.getSetor().getId(), c -> c.getQuantidade());
        Map<Long, BigDecimal> energia = somarPorSetor(
                consumoEnergiaService.listarComFiltros(null, dataInicio, dataFim),
                c -> c.getSetor().getId(), c -> c.getQuantidade());
        Map<Long, BigDecimal> residuos = somarPorSetor(
                residuoService.listarComFiltros(null, dataInicio, dataFim),
                r -> r.getSetor().getId(), r -> r.getQuantidade());

        return setorService.listarTodos().stream()
                .map(s -> new ConsumoPorSetorDTO(
                        s.getId(),
                        s.getNome(),
                        agua.getOrDefault(s.getId(), ZERO),
                        energia.getOrDefault(s.getId(), ZERO),
                        residuos.getOrDefault(s.getId(), ZERO)))
                .toList();
    }

    // ---------- Métodos auxiliares ----------

    private <T> List<TotalMensalDTO> agruparPorMes(List<T> registros,
                                                   Function<T, LocalDate> data,
                                                   Function<T, BigDecimal> quantidade) {
        // TreeMap mantém os meses em ordem cronológica
        Map<YearMonth, BigDecimal> totais = new TreeMap<>();
        for (T registro : registros) {
            totais.merge(YearMonth.from(data.apply(registro)),
                    quantidade.apply(registro), BigDecimal::add);
        }

        return totais.entrySet().stream()
                .map(e -> new TotalMensalDTO(
                        e.getKey().getYear(),
                        e.getKey().getMonthValue(),
                        rotulo(e.getKey()),
                        e.getValue()))
                .toList();
    }

    private <T> Map<Long, BigDecimal> somarPorSetor(List<T> registros,
                                                    Function<T, Long> setorId,
                                                    Function<T, BigDecimal> quantidade) {
        Map<Long, BigDecimal> totais = new HashMap<>();
        for (T registro : registros) {
            totais.merge(setorId.apply(registro), quantidade.apply(registro), BigDecimal::add);
        }
        return totais;
    }

    private String rotulo(YearMonth ym) {
        String nome = ym.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
        return Character.toUpperCase(nome.charAt(0)) + nome.substring(1) + "/" + ym.getYear();
    }
}