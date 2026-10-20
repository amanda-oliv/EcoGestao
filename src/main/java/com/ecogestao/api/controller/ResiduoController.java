package com.ecogestao.api.controller;

import com.ecogestao.api.exception.RecursoNaoEncontradoException;
import com.ecogestao.api.model.Residuo;
import com.ecogestao.api.service.ResiduoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/residuos")
public class ResiduoController {

    @Autowired
    private ResiduoService residuoService;

    @PostMapping
    public ResponseEntity<Residuo> cadastrar(@Valid @RequestBody Residuo residuo) {
        Residuo salvo = residuoService.salvar(residuo);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @GetMapping
    public ResponseEntity<List<Residuo>> listar(
            @RequestParam(name = "setorId", required = false) Long setorId,
            @RequestParam(name = "dataInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(name = "dataFim", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {

        return ResponseEntity.ok(
                residuoService.listarComFiltros(setorId, dataInicio, dataFim));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Residuo> buscarPorId(@PathVariable Long id) {
        return residuoService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> naoEncontrado(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Residuo> atualizar(@PathVariable Long id,
                                             @Valid @RequestBody Residuo residuo) {
        Residuo atualizado = residuoService.atualizar(id, residuo);
        if (atualizado == null) {
            throw naoEncontrado(id);
        }
        return ResponseEntity.ok(atualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (residuoService.buscarPorId(id).isEmpty()) {
            throw naoEncontrado(id);
        }
        residuoService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    private RecursoNaoEncontradoException naoEncontrado(Long id) {
        return new RecursoNaoEncontradoException(
                "Resíduo com id " + id + " não encontrado");
    }
}