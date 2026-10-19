package com.ecogestao.api.controller;

import com.ecogestao.api.model.Setor;
import com.ecogestao.api.service.SetorService;
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

    // POST /api/setores
    @PostMapping
    public ResponseEntity<Setor> cadastrar(@RequestBody Setor setor) {
        Setor salvo = setorService.salvar(setor);
        return ResponseEntity.status(HttpStatus.CREATED).body(salvo);
    }

    // GET /api/setores  (lista)
    @GetMapping
    public ResponseEntity<List<Setor>> listar() {
        return ResponseEntity.ok(setorService.listarTodos());
    }

    // GET /api/setores/{id}
    @GetMapping("/{id}")
    public ResponseEntity<Setor> buscarPorId(@PathVariable Long id) {
        return setorService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}