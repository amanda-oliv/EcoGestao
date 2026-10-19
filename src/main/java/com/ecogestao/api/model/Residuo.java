package com.ecogestao.api.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "residuo")
public class Residuo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "O setor é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "setor_id", nullable = false)
    private Setor setor;

    @NotNull(message = "A data é obrigatória")
    @PastOrPresent(message = "A data não pode ser futura")
    @Column(nullable = false)
    private LocalDate data;

    // Ex.: Papel, Plástico, Eletrônico
    @NotBlank(message = "O tipo do resíduo é obrigatório")
    @Size(max = 50, message = "O tipo deve ter no máximo 50 caracteres")
    @Column(nullable = false, length = 50)
    private String tipo;

    // kg
    @NotNull(message = "A quantidade é obrigatória")
    @Positive(message = "A quantidade deve ser maior que zero")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantidade;

    // Ex.: Reciclagem, Aterro, Empresa especializada
    @NotBlank(message = "A destinação é obrigatória")
    @Size(max = 80, message = "A destinação deve ter no máximo 80 caracteres")
    @Column(nullable = false, length = 80)
    private String destinacao;

    public Residuo() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Setor getSetor() { return setor; }
    public void setSetor(Setor setor) { this.setor = setor; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public String getDestinacao() { return destinacao; }
    public void setDestinacao(String destinacao) { this.destinacao = destinacao; }
}