package com.ecogestao.api.controller;

import com.ecogestao.api.model.ConsumoAgua;
import com.ecogestao.api.service.ConsumoAguaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/agua")
public class ConsumoAguaController {

    @Autowired
    private ConsumoAguaService consumoAguaService;

    // POST /api/agua
    @PostMapping
    public ResponseEntity<ConsumoAgua> cadastrar(@Valid @RequestBody ConsumoAgua consumo) {
        ConsumoAgua salvo = consumoAguaService.salvar(consumo);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    // GET /api/agua  (lista, com filtros opcionais: setorId, dataInicio, dataFim)
    @GetMapping
    public ResponseEntity<List<ConsumoAgua>> listar(
            @RequestParam(name = "setorId", required = false) Long setorId,
            @RequestParam(name = "dataInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(name = "dataFim", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {

        return ResponseEntity.ok(
                consumoAguaService.listarComFiltros(setorId, dataInicio, dataFim));
    }

    // GET /api/agua/{id}  (por id)
    @GetMapping("/{id}")
    public ResponseEntity<ConsumoAgua> buscarPorId(@PathVariable Long id) {
        return consumoAguaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // PUT /api/agua/{id}
    @PutMapping("/{id}")
    public ResponseEntity<ConsumoAgua> atualizar(@PathVariable Long id, @Valid @RequestBody ConsumoAgua consumo) {
        ConsumoAgua atualizado = consumoAguaService.atualizar(id, consumo);
        if (atualizado == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(atualizado);
    }

    // DELETE /api/agua/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (consumoAguaService.buscarPorId(id).isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        consumoAguaService.deletar(id);
        return ResponseEntity.noContent().build();
    }
}