package com.ecogestao.api.service;

import com.ecogestao.api.exception.RecursoNaoEncontradoException;
import com.ecogestao.api.exception.ValorInvalidoException;
import com.ecogestao.api.model.ConsumoAgua;
import com.ecogestao.api.model.Setor;
import com.ecogestao.api.repository.ConsumoAguaRepository;
import com.ecogestao.api.repository.SetorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ConsumoAguaService {

    @Autowired
    private ConsumoAguaRepository consumoAguaRepository;

    @Autowired
    private SetorRepository setorRepository;

    public ConsumoAgua salvar(ConsumoAgua consumo) {
        Setor setor = buscarSetorOuFalhar(consumo.getSetor().getId());
        consumo.setSetor(setor);
        return consumoAguaRepository.save(consumo);
    }

    public List<ConsumoAgua> listarTodos() {
        return consumoAguaRepository.findAll();
    }

    public Optional<ConsumoAgua> buscarPorId(Long id) {
        return consumoAguaRepository.findById(id);
    }

    public List<ConsumoAgua> listarPorSetor(Long setorId) {
        return consumoAguaRepository.findBySetorId(setorId);
    }

    public List<ConsumoAgua> listarPorPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        return consumoAguaRepository.findByDataBetween(dataInicio, dataFim);
    }

    public List<ConsumoAgua> listarComFiltros(Long setorId, LocalDate dataInicio, LocalDate dataFim) {
        if ((dataInicio == null) != (dataFim == null)) {
            throw new ValorInvalidoException(
                    "Informe dataInicio e dataFim juntas para filtrar por período");
        }
        if (dataInicio != null && dataInicio.isAfter(dataFim)) {
            throw new ValorInvalidoException(
                    "dataInicio não pode ser posterior a dataFim");
        }

        if (setorId != null) {
            buscarSetorOuFalhar(setorId); // 404 se o setor não existir
            if (dataInicio != null) {
                return consumoAguaRepository.findBySetorIdAndDataBetween(setorId, dataInicio, dataFim);
            }
            return consumoAguaRepository.findBySetorId(setorId);
        }

        if (dataInicio != null) {
            return consumoAguaRepository.findByDataBetween(dataInicio, dataFim);
        }
        return consumoAguaRepository.findAll();
    }

    public void deletar(Long id) {
        consumoAguaRepository.deleteById(id);
    }

    public ConsumoAgua atualizar(Long id, ConsumoAgua dadosAtualizados) {
        Setor setor = buscarSetorOuFalhar(dadosAtualizados.getSetor().getId());
        return consumoAguaRepository.findById(id)
                .map(consumo -> {
                    consumo.setSetor(setor);
                    consumo.setData(dadosAtualizados.getData());
                    consumo.setQuantidade(dadosAtualizados.getQuantidade());
                    return consumoAguaRepository.save(consumo);
                })
                .orElse(null);
    }

    private Setor buscarSetorOuFalhar(Long setorId) {
        return setorRepository.findById(setorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Setor com id " + setorId + " não encontrado"));
    }
}