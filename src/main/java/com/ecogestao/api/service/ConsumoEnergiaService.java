package com.ecogestao.api.service;

import com.ecogestao.api.exception.RecursoNaoEncontradoException;
import com.ecogestao.api.exception.ValorInvalidoException;
import com.ecogestao.api.model.ConsumoEnergia;
import com.ecogestao.api.model.Setor;
import com.ecogestao.api.repository.ConsumoEnergiaRepository;
import com.ecogestao.api.repository.SetorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ConsumoEnergiaService {

    @Autowired
    private ConsumoEnergiaRepository consumoEnergiaRepository;

    @Autowired
    private SetorRepository setorRepository;

    public ConsumoEnergia salvar(ConsumoEnergia consumo) {
        Setor setor = buscarSetorOuFalhar(consumo.getSetor().getId());
        consumo.setSetor(setor);
        return consumoEnergiaRepository.save(consumo);
    }

    public List<ConsumoEnergia> listarTodos() {
        return consumoEnergiaRepository.findAll();
    }

    public Optional<ConsumoEnergia> buscarPorId(Long id) {
        return consumoEnergiaRepository.findById(id);
    }

    public List<ConsumoEnergia> listarPorSetor(Long setorId) {
        return consumoEnergiaRepository.findBySetorId(setorId);
    }

    public List<ConsumoEnergia> listarPorPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        return consumoEnergiaRepository.findByDataBetween(dataInicio, dataFim);
    }

    public List<ConsumoEnergia> listarComFiltros(Long setorId, LocalDate dataInicio, LocalDate dataFim) {
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
                return consumoEnergiaRepository.findBySetorIdAndDataBetween(setorId, dataInicio, dataFim);
            }
            return consumoEnergiaRepository.findBySetorId(setorId);
        }

        if (dataInicio != null) {
            return consumoEnergiaRepository.findByDataBetween(dataInicio, dataFim);
        }
        return consumoEnergiaRepository.findAll();
    }

    public ConsumoEnergia atualizar(Long id, ConsumoEnergia dadosAtualizados) {
        Setor setor = buscarSetorOuFalhar(dadosAtualizados.getSetor().getId());
        return consumoEnergiaRepository.findById(id)
                .map(consumo -> {
                    consumo.setSetor(setor);
                    consumo.setData(dadosAtualizados.getData());
                    consumo.setQuantidade(dadosAtualizados.getQuantidade());
                    return consumoEnergiaRepository.save(consumo);
                })
                .orElse(null);
    }

    public void deletar(Long id) {
        consumoEnergiaRepository.deleteById(id);
    }

    private Setor buscarSetorOuFalhar(Long setorId) {
        return setorRepository.findById(setorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Setor com id " + setorId + " não encontrado"));
    }
}