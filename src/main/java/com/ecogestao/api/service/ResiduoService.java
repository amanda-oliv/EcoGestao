package com.ecogestao.api.service;

import com.ecogestao.api.exception.RecursoNaoEncontradoException;
import com.ecogestao.api.exception.ValorInvalidoException;
import com.ecogestao.api.model.Residuo;
import com.ecogestao.api.model.ResiduoValores;
import com.ecogestao.api.model.Setor;
import com.ecogestao.api.repository.ResiduoRepository;
import com.ecogestao.api.repository.SetorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class ResiduoService {

    @Autowired
    private ResiduoRepository residuoRepository;

    @Autowired
    private SetorRepository setorRepository;

    public Residuo salvar(Residuo residuo) {
        validarEPadronizar(residuo);
        Setor setor = buscarSetorOuFalhar(residuo.getSetor().getId());
        residuo.setSetor(setor);
        return residuoRepository.save(residuo);
    }

    public List<Residuo> listarTodos() {
        return residuoRepository.findAll();
    }

    public Optional<Residuo> buscarPorId(Long id) {
        return residuoRepository.findById(id);
    }

    public List<Residuo> listarPorSetor(Long setorId) {
        return residuoRepository.findBySetorId(setorId);
    }

    public List<Residuo> listarPorPeriodo(LocalDate dataInicio, LocalDate dataFim) {
        return residuoRepository.findByDataBetween(dataInicio, dataFim);
    }

    public List<Residuo> listarComFiltros(Long setorId, LocalDate dataInicio, LocalDate dataFim) {
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
                return residuoRepository.findBySetorIdAndDataBetween(setorId, dataInicio, dataFim);
            }
            return residuoRepository.findBySetorId(setorId);
        }

        if (dataInicio != null) {
            return residuoRepository.findByDataBetween(dataInicio, dataFim);
        }
        return residuoRepository.findAll();
    }

    public Residuo atualizar(Long id, Residuo dadosAtualizados) {
        validarEPadronizar(dadosAtualizados);
        Setor setor = buscarSetorOuFalhar(dadosAtualizados.getSetor().getId());
        return residuoRepository.findById(id)
                .map(residuo -> {
                    residuo.setSetor(setor);
                    residuo.setData(dadosAtualizados.getData());
                    residuo.setTipo(dadosAtualizados.getTipo());
                    residuo.setQuantidade(dadosAtualizados.getQuantidade());
                    residuo.setDestinacao(dadosAtualizados.getDestinacao());
                    return residuoRepository.save(residuo);
                })
                .orElse(null);
    }

    public void deletar(Long id) {
        residuoRepository.deleteById(id);
    }

    private void validarEPadronizar(Residuo residuo) {
        residuo.setTipo(ResiduoValores.padronizar(
                residuo.getTipo(), ResiduoValores.TIPOS, "tipo"));
        residuo.setDestinacao(ResiduoValores.padronizar(
                residuo.getDestinacao(), ResiduoValores.DESTINACOES, "destinação"));
    }

    private Setor buscarSetorOuFalhar(Long setorId) {
        return setorRepository.findById(setorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Setor com id " + setorId + " não encontrado"));
    }
}