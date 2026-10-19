package com.ecogestao.api.repository;

import com.ecogestao.api.model.ConsumoAgua;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface ConsumoAguaRepository extends JpaRepository<ConsumoAgua, Long> {

    List<ConsumoAgua> findBySetorId(Long setorId);

    List<ConsumoAgua> findByDataBetween(LocalDate dataInicio, LocalDate dataFim);

    List<ConsumoAgua> findBySetorIdAndDataBetween(Long setorId, LocalDate dataInicio, LocalDate dataFim);
}