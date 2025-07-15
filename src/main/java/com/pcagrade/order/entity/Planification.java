package com.pcagrade.order.entity;

import com.github.f4b6a3.ulid.Ulid;
import com.pcagrade.order.util.AbstractUlidEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "j_planification")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class Planification extends AbstractUlidEntity {

    @Column(name = "order_id", nullable = false, columnDefinition = "BINARY(16)")
    private Ulid orderId;

    @Column(name = "employe_id", nullable = false, columnDefinition = "BINARY(16)")
    private Ulid employeId;

    @Column(name = "date_planification", nullable = false)
    private LocalDate datePlanification;

    @Column(name = "heure_debut", nullable = false)
    private LocalTime heureDebut;

    @Column(name = "duree_minutes", nullable = false)
    private Integer dureeMinutes;

    @Column(name = "terminee", nullable = false)
    private Boolean terminee = false;

    @Column(name = "date_debut_reel")
    private LocalDateTime dateDebutReel;

    @Column(name = "date_fin_reel")
    private LocalDateTime dateFinReel;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_modification", nullable = false)
    private LocalDateTime dateModification;

    @Column(name = "commentaire", columnDefinition = "TEXT")
    private String commentaire;

    // Relations JPA (optionnelles pour éviter les problèmes de lazy loading)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", insertable = false, updatable = false)
    private Commande commande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employe_id", insertable = false, updatable = false)
    private Employe employe;

    // Constructeur simplifié
    public Planification(Ulid orderId, Ulid employeId, LocalDate datePlanification,
                         LocalTime heureDebut, Integer dureeMinutes) {
        this.orderId = orderId;
        this.employeId = employeId;
        this.datePlanification = datePlanification;
        this.heureDebut = heureDebut;
        this.dureeMinutes = dureeMinutes;
        this.terminee = false;
        this.dateCreation = LocalDateTime.now();
        this.dateModification = LocalDateTime.now();
    }

    // Méthodes utilitaires
    public LocalTime getHeureFin() {
        return heureDebut.plusMinutes(dureeMinutes);
    }

    public boolean isEnCours() {
        return !terminee && dateDebutReel != null && dateFinReel == null;
    }

    public void commencer() {
        this.dateDebutReel = LocalDateTime.now();
        this.dateModification = LocalDateTime.now();
    }

    public void terminer() {
        this.terminee = true;
        this.dateFinReel = LocalDateTime.now();
        this.dateModification = LocalDateTime.now();
    }

    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
        if (dateModification == null) {
            dateModification = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dateModification = LocalDateTime.now();
    }
}
