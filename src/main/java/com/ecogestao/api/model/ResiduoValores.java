package com.ecogestao.api.model;

import com.ecogestao.api.exception.ValorInvalidoException;

import java.util.List;

public final class ResiduoValores {

    public static final List<String> TIPOS = List.of(
            "Papel", "Plástico", "Vidro", "Metal",
            "Orgânico", "Eletrônico", "Outros");

    public static final List<String> DESTINACOES = List.of(
            "Reciclagem", "Compostagem", "Aterro", "Empresa especializada");

    private ResiduoValores() {}

    /** Retorna o valor na grafia oficial ou lança ValorInvalidoException. */
    public static String padronizar(String valor, List<String> aceitos, String campo) {
        String informado = valor == null ? "" : valor.trim();
        return aceitos.stream()
                .filter(a -> a.equalsIgnoreCase(informado))
                .findFirst()
                .orElseThrow(() -> new ValorInvalidoException(
                        "Valor inválido para " + campo + ": '" + informado
                        + "'. Valores aceitos: " + String.join(", ", aceitos)));
    }
}