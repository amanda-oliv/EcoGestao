package com.ecogestao.api.repository;

import com.ecogestao.api.model.ConsumoEnergia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface ConsumoEnergiaRepository extends JpaRepository<ConsumoEnergia, Long> {

    List<ConsumoEnergia> findBySetorId(Long setorId);

    List<ConsumoEnergia> findByDataBetween(LocalDate dataInicio, LocalDate dataFim);

    List<ConsumoEnergia> findBySetorIdAndDataBetween(Long setorId, LocalDate dataInicio, LocalDate dataFim);
}