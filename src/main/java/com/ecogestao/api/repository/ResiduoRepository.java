package com.ecogestao.api.repository;

import com.ecogestao.api.model.Residuo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ResiduoRepository extends JpaRepository<Residuo, Long> {

    List<Residuo> findBySetorId(Long setorId);

    List<Residuo> findByDataBetween(LocalDate dataInicio, LocalDate dataFim);

    List<Residuo> findBySetorIdAndDataBetween(Long setorId, LocalDate dataInicio, LocalDate dataFim);

    List<Residuo> findByTipoIgnoreCase(String tipo);

    List<Residuo> findByDestinacaoIgnoreCase(String destinacao);
}