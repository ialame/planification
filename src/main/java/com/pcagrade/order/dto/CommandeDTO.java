package com.pcagrade.order.dto;


import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class CommandeDTO {
    private Long id;
    private String numeroCommande;
    private Integer nombreCartes;
    private BigDecimal prixTotal;
    private String priorite;
    private String statut;
    private LocalDateTime dateCreation;
    private LocalDateTime dateLimite;
    private Integer tempsEstimeMinutes;

    // Constructeur
    public CommandeDTO(Long id, String numeroCommande, Integer nombreCartes,
                       BigDecimal prixTotal, String priorite, String statut,
                       LocalDateTime dateCreation, LocalDateTime dateLimite,
                       Integer tempsEstimeMinutes) {
        this.id = id;
        this.numeroCommande = numeroCommande;
        this.nombreCartes = nombreCartes;
        this.prixTotal = prixTotal;
        this.priorite = priorite;
        this.statut = statut;
        this.dateCreation = dateCreation;
        this.dateLimite = dateLimite;
        this.tempsEstimeMinutes = tempsEstimeMinutes;
    }

    // Getters et Setters

    // ... autres getters/setters
}