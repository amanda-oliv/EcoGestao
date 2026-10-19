package com.ecogestao.api.dto;

import java.math.BigDecimal;

public record ConsumoPorSetorDTO(Long setorId,
                                 String setor,
                                 BigDecimal agua,
                                 BigDecimal energia,
                                 BigDecimal residuos) { }