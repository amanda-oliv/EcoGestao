package com.ecogestao.api.dto;

import java.math.BigDecimal;

public record ReciclagemDTO(BigDecimal totalGerado,
                            BigDecimal totalReciclado,
                            BigDecimal totalNaoReciclado,
                            BigDecimal percentualReciclagem) { }