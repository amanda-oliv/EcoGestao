package com.ecogestao.api.dto;

import java.math.BigDecimal;

public record TotalMensalDTO(int ano, int mes, String rotulo, BigDecimal total) {
}