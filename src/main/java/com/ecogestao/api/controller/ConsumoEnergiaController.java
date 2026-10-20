package com.ecogestao.api.controller;

import com.ecogestao.api.exception.RecursoNaoEncontradoException;
import com.ecogestao.api.model.ConsumoEnergia;
import com.ecogestao.api.service.ConsumoEnergiaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/energia")
public class ConsumoEnergiaController {

    @Autowired
    private ConsumoEnergiaService consumoEnergiaService;

    @PostMapping
    public ResponseEntity<ConsumoEnergia> cadastrar(@Valid @RequestBody ConsumoEnergia consumo) {
        ConsumoEnergia salvo = consumoEnergiaService.salvar(consumo);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @GetMapping
    public ResponseEntity<List<ConsumoEnergia>> listar(
            @RequestParam(name = "setorId", required = false) Long setorId,
            @RequestParam(name = "dataInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataInicio,
            @RequestParam(name = "dataFim", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataFim) {

        return ResponseEntity.ok(
                consumoEnergiaService.listarComFiltros(setorId, dataInicio, dataFim));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConsumoEnergia> buscarPorId(@PathVariable Long id) {
        return consumoEnergiaService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> naoEncontrado(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConsumoEnergia> atualizar(@PathVariable Long id,
                                                    @Valid @RequestBody ConsumoEnergia consumo) {
        ConsumoEnergia atualizado = consumoEnergiaService.atualizar(id, consumo);
        if (atualizado == null) {
            throw naoEncontrado(id);
        }
        return ResponseEntity.ok(atualizado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) {
        if (consumoEnergiaService.buscarPorId(id).isEmpty()) {
            throw naoEncontrado(id);
        }
        consumoEnergiaService.deletar(id);
        return ResponseEntity.noContent().build();
    }

    private RecursoNaoEncontradoException naoEncontrado(Long id) {
        return new RecursoNaoEncontradoException(
                "Consumo de energia com id " + id + " não encontrado");
    }
}