package com.ecogestao.api.controller;

import com.ecogestao.api.exception.RecursoNaoEncontradoException;
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

    @PostMapping
    public ResponseEntity<ConsumoAgua> cadastrar(@Valid @RequestBody ConsumoAgua consumo) {
        ConsumoAgua salvo = consumoAguaService.salvar(consumo);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

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

    @GetMapping("/{id}")
    public ResponseEntity<ConsumoAgua> buscarPorId(@PathVariable Long id) {
        return consumoAguaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> naoEncontrado(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConsumoAgua> atualizar(@PathVariable Long id,
                                                 @Valid @RequestBody ConsumoAgua consumo) {
        ConsumoAgua atualizado = consumoAguaService.atualizar(id, consumo);
        if (atualizado == null) {
            throw naoEncontrado(id);
        }
        return ResponseEntity.ok(atualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (consumoAguaService.buscarPorId(id).isEmpty()) {
            throw naoEncontrado(id);
        }
        consumoAguaService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    private RecursoNaoEncontradoException naoEncontrado(Long id) {
        return new RecursoNaoEncontradoException(
                "Consumo de água com id " + id + " não encontrado");
    }
}