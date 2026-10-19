package com.ecogestao.api.controller;

import com.ecogestao.api.dto.ConsumoPorSetorDTO;
import com.ecogestao.api.dto.ReciclagemDTO;
import com.ecogestao.api.dto.TotalMensalDTO;
import com.ecogestao.api.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    // ---------- Dia 20: totais mensais ----------

    @GetMapping("/agua-mensal")
    public ResponseEntity<List<TotalMensalDTO>> aguaMensal(
            @RequestParam(name = "setorId", required = false) Long setorId,
            @RequestParam(name = "dataInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(name = "dataFim", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return ResponseEntity.ok(dashboardService.aguaMensal(setorId, dataInicio, dataFim));
    }

    @GetMapping("/energia-mensal")
    public ResponseEntity<List<TotalMensalDTO>> energiaMensal(
            @RequestParam(name = "setorId", required = false) Long setorId,
            @RequestParam(name = "dataInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(name = "dataFim", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return ResponseEntity.ok(dashboardService.energiaMensal(setorId, dataInicio, dataFim));
    }

    @GetMapping("/residuos-mensal")
    public ResponseEntity<List<TotalMensalDTO>> residuosMensal(
            @RequestParam(name = "setorId", required = false) Long setorId,
            @RequestParam(name = "dataInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(name = "dataFim", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return ResponseEntity.ok(dashboardService.residuosMensal(setorId, dataInicio, dataFim));
    }

    // ---------- Dia 21: percentual de reciclagem e consumo por setor ----------

    @GetMapping("/reciclagem")
    public ResponseEntity<ReciclagemDTO> reciclagem(
            @RequestParam(name = "setorId", required = false) Long setorId,
            @RequestParam(name = "dataInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(name = "dataFim", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return ResponseEntity.ok(dashboardService.reciclagem(setorId, dataInicio, dataFim));
    }

    @GetMapping("/consumo-por-setor")
    public ResponseEntity<List<ConsumoPorSetorDTO>> consumoPorSetor(
            @RequestParam(name = "dataInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(name = "dataFim", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {
        return ResponseEntity.ok(dashboardService.consumoPorSetor(dataInicio, dataFim));
    }
}