package com.pcagrade.order.entity;


import com.github.f4b6a3.ulid.Ulid;
import com.pcagrade.order.util.AbstractUlidEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import java.util.Map;
import java.util.HashMap;

@Entity
@Table(name = "`order`")  // Échapper le mot-clé SQL
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@NoArgsConstructor
public class Commande extends AbstractUlidEntity {

    @Column(name = "num")
    private Integer num;

    @Column(name = "customer_id", columnDefinition = "BINARY(16)")
    private Ulid customerId;

    @Column(name = "date")
    private LocalDateTime date;

    @Column(name = "num_commande")
    private String numeroCommande;

    @Column(name = "status")
    private Integer status;

    @Column(name = "delai")
    private String delai;  // ← String, pas Integer !

    @Column(name = "employe_id")
    private String employeId;  // ← String selon la table

    @Column(name = "nombre_cartes")
    private Integer nombreCartes;

    @Column(name = "priorite_string")
    private String priorite;

    @Column(name = "prix_total")
    private Double prixTotal;

    @Column(name = "temps_estime_minutes")
    private Integer tempsEstimeMinutes;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @Column(name = "date_modification")
    private LocalDateTime dateModification;

    // ========== CHAMPS MANQUANTS ==========

    @Column(name = "date_limite")
    private LocalDateTime dateLimite;

    @Column(name = "date_debut_traitement")
    private LocalDateTime dateDebutTraitement;

    @Column(name = "date_fin_traitement")
    private LocalDateTime dateFinTraitement;

    @Column(name = "nb_descellements")
    private Integer nbDescellements;

    // ========== MÉTHODES ALIAS POUR COMPATIBILITÉ ==========

    /**
     * Alias pour setNumeroCommande() - compatibilité avec Test.java
     */
    public void setNumCommande(String numCommande) {
        this.numeroCommande = numCommande;
    }

    public String getNumCommande() {
        return this.numeroCommande;
    }


    // Votre relation N-N existante
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "card_certification_order",
            joinColumns = @JoinColumn(name = "order_id"),
            inverseJoinColumns = @JoinColumn(name = "card_certification_id")
    )
    @ToString.Exclude
    private Set<CardCertification> cardCertifications = new HashSet<>();

    // Vos méthodes utilitaires existantes pour les cartes...
    public int getNombreCartes() {
        return cardCertifications != null ? cardCertifications.size() : 0;
    }

    // etc...

    /**
     * Version améliorée du résumé des cartes
     */
    public Map<String, Long> getResumerCartes() {
        if (cardCertifications == null || cardCertifications.isEmpty()) {
            System.out.println("⚠️ Aucune certification pour résumé: " + this.numeroCommande);
            return new HashMap<>();
        }

        return cardCertifications.stream()
                .collect(Collectors.groupingBy(
                        certification -> {
                            Card card = certification.getCard();
                            if (card == null) {
                                return "Carte inconnue";
                            }

                            if (card.getTranslations() == null || card.getTranslations().isEmpty()) {
                                return "Carte #" + card.getNum();
                            }

                            // Même logique que getNomsCartes()
                            List<String> localesPrioritaires = Arrays.asList("fr", "en", "fr_FR", "en_US");

                            for (String locale : localesPrioritaires) {
                                Optional<String> nom = card.getTranslations().stream()
                                        .filter(t -> locale.equals(t.getLocale()))
                                        .findFirst()
                                        .map(CardTranslation::getName);

                                if (nom.isPresent() && !nom.get().trim().isEmpty()) {
                                    return nom.get();
                                }
                            }

                            // Fallback
                            return card.getTranslations().stream()
                                    .findFirst()
                                    .map(CardTranslation::getName)
                                    .orElse("Carte #" + card.getNum());
                        },
                        Collectors.counting()
                ));
    }

    // ========== MÉTHODES UTILITAIRES POUR LES CARTES ==========

    /**
     * Retourne la liste des noms de cartes (version corrigée)
     */
    public List<String> getNomsCartes() {
        if (cardCertifications == null || cardCertifications.isEmpty()) {
            System.out.println("⚠️ Aucune certification de carte pour commande: " + this.numeroCommande);
            return new ArrayList<>();
        }

        System.out.println("🃏 Traitement " + cardCertifications.size() + " certifications pour: " + this.numeroCommande);

        return cardCertifications.stream()
                .map(certification -> {
                    Card card = certification.getCard();
                    if (card == null) {
                        System.out.println("❌ Carte null pour certification: " + certification.getId());
                        return "Carte non trouvée";
                    }

                    if (card.getTranslations() == null || card.getTranslations().isEmpty()) {
                        System.out.println("⚠️ Pas de traductions pour carte: " + card.getId() + " (num: " + card.getNum() + ")");
                        return "Carte #" + card.getNum();
                    }

                    // ✅ CORRECTION - Essayer plusieurs locales dans l'ordre de préférence
                    List<String> localesPrioritaires = Arrays.asList("fr", "en", "fr_FR", "en_US");

                    for (String locale : localesPrioritaires) {
                        Optional<String> nom = card.getTranslations().stream()
                                .filter(t -> locale.equals(t.getLocale()))
                                .findFirst()
                                .map(CardTranslation::getName);

                        if (nom.isPresent() && !nom.get().trim().isEmpty()) {
                            System.out.println("✅ Carte trouvée (" + locale + "): " + nom.get());
                            return nom.get();
                        }
                    }

                    // ✅ Fallback - Prendre la première traduction disponible
                    Optional<CardTranslation> premiereTraduction = card.getTranslations().stream().findFirst();
                    if (premiereTraduction.isPresent()) {
                        String nom = premiereTraduction.get().getName();
                        String locale = premiereTraduction.get().getLocale();
                        System.out.println("🔄 Fallback (" + locale + "): " + nom);
                        return nom;
                    }

                    System.out.println("❌ Aucune traduction valide pour carte: " + card.getId());
                    return "Carte #" + card.getNum();
                })
                .collect(Collectors.toList());
    }

    public Integer getDureeEstimeeMinutes() {
        return this.tempsEstimeMinutes;
    }

}