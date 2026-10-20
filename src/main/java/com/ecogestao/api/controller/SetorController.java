package com.ecogestao.api.controller;

import com.ecogestao.api.exception.RecursoNaoEncontradoException;
import com.ecogestao.api.model.Setor;
import com.ecogestao.api.service.SetorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/setores")
public class SetorController {

    @Autowired
    private SetorService setorService;

    @PostMapping
    public ResponseEntity<Setor> cadastrar(@Valid @RequestBody Setor setor) {
        Setor salvo = setorService.salvar(setor);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    @GetMapping
    public ResponseEntity<List<Setor>> listar() {
        return ResponseEntity.ok(setorService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Setor> buscarPorId(@PathVariable Long id) {
        return setorService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Setor com id " + id + " não encontrado"));
    }
}