package com.pcagrade.order.service;

import com.github.f4b6a3.ulid.Ulid;
import com.pcagrade.order.entity.Commande;
import com.pcagrade.order.repository.CommandeRepository;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.persistence.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class CommandeService {

    @Autowired
    private EntityManager entityManager;


    @Autowired
    private CommandeRepository commandeRepository;

    public List<Commande> getToutesCommandes() {
        return commandeRepository.findAll();
    }

    public Optional<Commande> getCommandeById(UUID id) {
        return commandeRepository.findById(id);
    }

    public Optional<Commande> getCommandeByNumero(String numeroCommande) {
        return commandeRepository.findByNumeroCommande(numeroCommande);
    }

    public Commande creerCommande(Commande commande) {
        // L'ID sera généré automatiquement par @PrePersist
        return commandeRepository.save(commande);
    }

    public Commande mettreAJourCommande(Commande commande) {
        // La date de modification sera mise à jour par @PreUpdate
        return commandeRepository.save(commande);
    }

    public void supprimerCommande(UUID id) {
        commandeRepository.deleteById(id);
    }

    public List<Commande> getCommandesNonAssignees() {
        return commandeRepository.findCommandesNonAssignees(1); // status = 1
    }

    public List<Commande> getOrdersATraiter() {
        // Retourner les commandes non assignées avec status = 1
        return commandeRepository.findCommandesNonAssignees(1);
    }

    public void marquerCommePlanifie(UUID commandeId) {
        Optional<Commande> commande = commandeRepository.findById(commandeId);
        if (commande.isPresent()) {
            Commande c = commande.get();
            c.setStatus(2); // ou le status approprié pour "planifié"
            commandeRepository.save(c);
        }
    }

    public long getNombreCommandesEnAttente() {
        return commandeRepository.countByStatus(1);
    }

    public long getNombreCommandesEnCours() {
        return commandeRepository.countByStatus(2);
    }

    public long getNombreCommandesTerminees() {
        return commandeRepository.countByStatus(3);
    }



    // Méthode de compatibilité (garde l'ancienne méthode)
    public List<Map<String, Object>> getToutesCommandesNative() {
        // Par défaut, charger depuis le 1er mai 2025
        return getCommandesAPlanifierDepuisDate(1, 5, 2025);
    }

    // Dans CommandeService.java, ajoutez cette méthode de test
    public void testCartesPourCommande(String numeroCommande) {
        Query query = entityManager.createQuery(
                "SELECT c FROM Commande c " +
                        "LEFT JOIN FETCH c.cardCertifications cc " +
                        "LEFT JOIN FETCH cc.card card " +
                        "LEFT JOIN FETCH card.translations " +
                        "WHERE c.numeroCommande = :numero"
        );

        query.setParameter("numero", numeroCommande);

        try {
            Commande commande = (Commande) query.getSingleResult();
            System.out.println("🔍 Commande: " + commande.getNumeroCommande());
            System.out.println("📦 Nombre de cartes: " + commande.getNombreCartes());
            System.out.println("🎴 Noms des cartes: " + commande.getNomsCartes());
        } catch (Exception e) {
            System.out.println("❌ Erreur: " + e.getMessage());
        }
    }


    // ========== MÉTHODE UTILITAIRE À AJOUTER DANS CommandeService.java ==========

    /**
     * Convertit une chaîne hexadécimale (UUID sans tirets) vers un ULID
     *
     * @param hexString ex: "0196894BD992D78614399D7C1035125B"
     * @return ULID correspondant pour utilisation avec JPA
     */
    private Ulid hexStringToUlid(String hexString) {
        if (hexString == null || hexString.length() != 32) {
            throw new IllegalArgumentException("Hex string must be 32 characters long");
        }

        try {
            // Convertir "0196894BD992D78614399D7C1035125B"
            // vers "0196894b-d992-d786-1439-9d7c1035125b"
            String formatted = hexString.toLowerCase()
                    .replaceAll("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5");

            // Créer UUID puis ULID
            UUID uuid = UUID.fromString(formatted);
            return Ulid.from(uuid);

        } catch (Exception e) {
            System.err.println("❌ Erreur conversion hex vers ULID: " + hexString + " - " + e.getMessage());
            throw new IllegalArgumentException("Invalid hex string: " + hexString, e);
        }
    }

    private Ulid stringToUlid(String hexString) {
        String formatted = hexString.toLowerCase()
                .replaceAll("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5");
        return Ulid.from(UUID.fromString(formatted));
    }

    // ============================================================================
// 📁 AJOUTS À FAIRE DANS CommandeService.java
// ============================================================================

// Ajouter cette méthode dans CommandeService.java :


    /**
     * Version avec période - NOUVELLE MÉTHODE
     */
    public List<Map<String, Object>> getCommandesPeriode(int jourDebut, int moisDebut, int anneeDebut,
                                                         int jourFin, int moisFin, int anneeFin) {
        try {
            String sql = """
            SELECT 
                HEX(o.id) as id, 
                o.num_commande as numeroCommande,
                o.date as dateReception,
                o.temps_estime_minutes as tempsEstimeMinutes,
                o.nombre_cartes as nombreCartes,
                o.priorite_string as priorite,
                o.prix_total as prixTotal,
                o.status as status,
                o.date_creation as dateCreation,
                o.date_modification as dateModification
            FROM `order` o
            WHERE o.date >= ? AND o.date <= ?
            AND o.status IN (1, 2)
            ORDER BY o.priorite_string DESC, o.prix_total DESC, o.date ASC
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, LocalDateTime.of(anneeDebut, moisDebut, jourDebut, 0, 0));
            query.setParameter(2, LocalDateTime.of(anneeFin, moisFin, jourFin, 23, 59, 59));

            List<Object[]> resultats = query.getResultList();
            List<Map<String, Object>> commandes = new ArrayList<>();

            for (Object[] row : resultats) {
                Map<String, Object> commande = new HashMap<>();
                commande.put("id", (String) row[0]);
                commande.put("numeroCommande", (String) row[1]);
                commande.put("dateReception", row[2]);
                commande.put("dateLimite", row[3]);
                commande.put("tempsEstimeMinutes", row[4] != null ? row[4] : 120);
                commande.put("nombreCartes", row[5] != null ? row[5] : 1);
                commande.put("priorite", row[6] != null ? (String) row[6] : "NORMALE");
                commande.put("prixTotal", row[7] != null ? row[7] : 0.0);
                commande.put("status", row[8]);
                commande.put("dateCreation", row[9]);
                commande.put("dateModification", row[10]);
                commandes.add(commande);
            }

            System.out.println("✅ " + commandes.size() + " commandes chargées du " +
                    jourDebut + "/" + moisDebut + "/" + anneeDebut + " au " +
                    jourFin + "/" + moisFin + "/" + anneeFin);
            return commandes;

        } catch (Exception e) {
            System.err.println("❌ Erreur chargement commandes par période: " + e.getMessage());
            throw e;
        }
    }


    /**
     * ✅ MÉTHODE MANQUANTE - Ajouter dans CommandeService.java
     */
    // ✅ CORRIGEZ dans CommandeService.java la méthode getCommandesAPlanifierDepuisDate() :

    public List<Map<String, Object>> getCommandesAPlanifierDepuisDate(int jour, int mois, int annee) {
        try {
            System.out.println("🔍 Chargement commandes depuis le " + jour + "/" + mois + "/" + annee);

            // ✅ REQUÊTE CORRIGÉE - sans les colonnes qui n'existent pas
            String sql = """
            SELECT 
                HEX(o.id) as id, 
                o.num_commande as numeroCommande,
                o.date as dateReception,
                o.temps_estime_minutes as tempsEstimeMinutes,
                o.nombre_cartes as nombreCartes,
                o.priorite_string as priorite,
                o.prix_total as prixTotal,
                o.status as status,
                o.date_creation as dateCreation,
                o.date_modification as dateModification,
                o.delai as delai
            FROM `order` o
            WHERE o.date >= ? 
            AND o.status IN (1, 2)
            ORDER BY o.priorite_string DESC, o.prix_total DESC, o.date ASC
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, LocalDateTime.of(annee, mois, jour, 0, 0));

            @SuppressWarnings("unchecked")
            List<Object[]> resultats = query.getResultList();
            List<Map<String, Object>> commandes = new ArrayList<>();

            for (Object[] row : resultats) {
                Map<String, Object> commande = new HashMap<>();
                commande.put("id", (String) row[0]);
                commande.put("numeroCommande", (String) row[1]);
                commande.put("dateReception", row[2]);
                commande.put("dateLimite", null); // ← Sera calculé par l'algorithme DP
                commande.put("tempsEstimeMinutes", row[3] != null ? row[3] : 120);
                commande.put("nombreCartes", row[4] != null ? row[4] : 1);
                commande.put("priorite", row[5] != null ? (String) row[5] : "NORMALE");
                commande.put("prixTotal", row[6] != null ? row[6] : 0.0);
                commande.put("status", row[7]);
                commande.put("dateCreation", row[8]);
                commande.put("dateModification", row[9]);
                commande.put("delai", row[10]); // Utilisé pour calculer dateLimite

                commandes.add(commande);
            }

            System.out.println("✅ " + commandes.size() + " commandes chargées depuis le " +
                    jour + "/" + mois + "/" + annee);
            return commandes;

        } catch (Exception e) {
            System.err.println("❌ Erreur chargement commandes: " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }


}
