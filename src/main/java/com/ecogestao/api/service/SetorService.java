package com.ecogestao.api.service;

import com.ecogestao.api.model.Setor;
import com.ecogestao.api.repository.SetorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SetorService {

    @Autowired
    private SetorRepository setorRepository;

    public Setor salvar(Setor setor) {
        return setorRepository.save(setor);
    }

    public List<Setor> listarTodos() {
        return setorRepository.findAll();
    }

    public Optional<Setor> buscarPorId(Long id) {
        return setorRepository.findById(id);
    }

    public void deletar(Long id) {
        setorRepository.deleteById(id);
    }
}