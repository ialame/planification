package com.pcagrade.order.controller;

import com.pcagrade.order.service.CommandeService;
import com.pcagrade.order.service.DynamicProgrammingPlanificationService;
import com.pcagrade.order.service.EmployeService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Contrôleur pour tester la planification avec les vraies données
 * Exposé via API REST pour tests faciles
 */
@RestController
//@RequestMapping("/api/test-planification")
@CrossOrigin(origins = "*")
public class FrontendController {

    @Autowired
    private EmployeService employeService;

    @Autowired
    private EntityManager entityManager;
    /**
     * 📅 ENDPOINT FRONTEND - COMMANDES PÉRIODE SPÉCIFIQUE (22 MAI - 22 JUIN 2025)
     *
     * Combine nos données exactes avec la période de planification définie
     */
    @GetMapping("/api/frontend/commandes-periode-planification")
    public ResponseEntity<Map<String, Object>> getCommandesPeriodePlanification() {
        try {
            System.out.println("📅 === COMMANDES PÉRIODE PLANIFICATION (22 MAI - 22 JUIN 2025) ===");

            Map<String, Object> response = new HashMap<>();

            // 1. Période de planification définie
            String dateDebut = "2025-05-22";
            String dateFin = "2025-06-22";

            response.put("periode", Map.of(
                    "debut", dateDebut,
                    "fin", dateFin,
                    "description", "Période de planification automatique"
            ));

            // 2. Requête optimisée avec nos données exactes pour cette période
            String sqlCommandesPeriode = """
        SELECT 
            HEX(o.id) as id,
            o.num_commande as numeroCommande,
            COALESCE(o.priorite_string, 'NORMALE') as priorite,
            o.status,
            DATE(o.date) as date,
            o.date as timestamp_complet,
            COALESCE(o.temps_estime_minutes, 0) as tempsEstimeMinutes,
            COALESCE(o.prix_total, 0) as prixTotal,
            COUNT(DISTINCT cco.card_certification_id) as nombreCartesExactes,
            COUNT(DISTINCT CASE 
                WHEN ct.name IS NOT NULL THEN cco.card_certification_id 
                END) as nombreAvecNom,
            GROUP_CONCAT(
                DISTINCT SUBSTRING(COALESCE(ct.name, CONCAT('Carte-', cc.code_barre)), 1, 40)
                ORDER BY ct.name
                SEPARATOR ', '
            ) as echantillonNoms
        FROM `order` o
        INNER JOIN card_certification_order cco ON o.id = cco.order_id
        INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
        LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id 
            AND (
                (cc.langue = 'FR' AND ct.locale = 'fr') OR
                (cc.langue = 'JP' AND ct.locale = 'jp') OR
                (cc.langue = 'DE' AND ct.locale = 'de') OR
                (cc.langue = 'ES' AND ct.locale = 'es') OR
                (cc.langue = 'IT' AND ct.locale = 'it') OR
                (cc.langue = 'KR' AND ct.locale = 'kr') OR
                (cc.langue = 'CN' AND ct.locale = 'cn') OR
                (cc.langue = 'RU' AND ct.locale = 'ru') OR
                (cc.langue = 'NL' AND ct.locale = 'nl') OR
                (cc.langue = 'PT' AND ct.locale = 'pt') OR
                (cc.langue = 'US' AND ct.locale = 'us') OR
                (cc.langue = 'EN' AND ct.locale = 'us')
            )
        WHERE o.date >= ? AND o.date <= ?
            AND o.status IN (1, 2)  -- Commandes planifiables
            AND COALESCE(o.deleted, FALSE) = FALSE
        GROUP BY o.id, o.num_commande, o.priorite_string, o.status, o.date, o.temps_estime_minutes, o.prix_total
        HAVING COUNT(DISTINCT cco.card_certification_id) > 0
        ORDER BY 
            CASE COALESCE(o.priorite_string, 'NORMALE')
                WHEN 'URGENTE' THEN 1
                WHEN 'HAUTE' THEN 2
                WHEN 'MOYENNE' THEN 3
                WHEN 'NORMALE' THEN 4
                ELSE 5
            END,
            o.date ASC
        """;

            Query query = entityManager.createNativeQuery(sqlCommandesPeriode);
            query.setParameter(1, dateDebut);
            query.setParameter(2, dateFin);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> commandes = new ArrayList<>();
            int totalCartes = 0;
            int totalAvecNom = 0;
            int tempsTotal = 0;

            for (Object[] row : results) {
                Map<String, Object> commande = new HashMap<>();
                commande.put("id", row[0]);
                commande.put("numeroCommande", row[1]);
                commande.put("priorite", row[2]);
                commande.put("statut", mapStatusToString((Number) row[3]));
                commande.put("status", row[3]);
                commande.put("date", row[4]);
                commande.put("timestampComplet", row[5]);

                Number temps = (Number) row[6];
                Number prix = (Number) row[7];
                Number nbCartes = (Number) row[8];
                Number nbAvecNom = (Number) row[9];

                // Si pas de temps estimé, calculer 3min par carte
                int tempsEstime = temps.intValue() > 0 ? temps.intValue() : (nbCartes.intValue() * 3);

                commande.put("tempsEstimeMinutes", tempsEstime);
                commande.put("prixTotal", prix.doubleValue());
                commande.put("nombreCartes", nbCartes.intValue());
                commande.put("nombreAvecNom", nbAvecNom.intValue());
                commande.put("echantillonNoms", row[10]);

                // Calculer la qualité
                double pourcentage = nbCartes.intValue() > 0 ?
                        (nbAvecNom.doubleValue() / nbCartes.doubleValue()) * 100 : 0;
                commande.put("pourcentageAvecNom", Math.round(pourcentage));

                String qualite = pourcentage >= 90 ? "EXCELLENTE" :
                        pourcentage >= 70 ? "BONNE" : "MOYENNE";
                commande.put("qualiteCommande", qualite);
                commande.put("cartesSansMissingData", pourcentage == 100);

                // Estimation de complexité pour la planification
                double complexite = nbCartes.intValue();
                if ("URGENTE".equals(row[2])) complexite *= 1.5;
                else if ("HAUTE".equals(row[2])) complexite *= 1.2;
                commande.put("complexitePlanification", Math.round(complexite));

                commandes.add(commande);

                totalCartes += nbCartes.intValue();
                totalAvecNom += nbAvecNom.intValue();
                tempsTotal += tempsEstime;
            }

            // 3. Statistiques de la période
            double pourcentageGlobal = totalCartes > 0 ? (totalAvecNom * 100.0 / totalCartes) : 0;

            response.put("statistiques", Map.of(
                    "nombreCommandes", commandes.size(),
                    "totalCartes", totalCartes,
                    "totalAvecNom", totalAvecNom,
                    "pourcentageGlobal", Math.round(pourcentageGlobal),
                    "tempsTotal", tempsTotal,
                    "tempsTotalHeures", Math.round(tempsTotal / 60.0 * 100) / 100.0,
                    "moyenneCartesParCommande", commandes.size() > 0 ? Math.round(totalCartes / (double) commandes.size()) : 0,
                    "moyenneTempsParCommande", commandes.size() > 0 ? Math.round(tempsTotal / (double) commandes.size()) : 0
            ));

            // 4. Répartition par priorité
            Map<String, Long> repartitionPriorite = commandes.stream()
                    .collect(Collectors.groupingBy(
                            cmd -> (String) cmd.get("priorite"),
                            Collectors.counting()
                    ));

            response.put("repartitionPriorite", repartitionPriorite);

            // 5. Répartition par qualité
            Map<String, Long> repartitionQualite = commandes.stream()
                    .collect(Collectors.groupingBy(
                            cmd -> (String) cmd.get("qualiteCommande"),
                            Collectors.counting()
                    ));

            response.put("repartitionQualite", repartitionQualite);

            // 6. Les commandes elles-mêmes
            response.put("commandes", commandes);

            // 7. Recommandations pour la planification
            List<String> recommandations = new ArrayList<>();

            if (commandes.isEmpty()) {
                recommandations.add("❌ Aucune commande trouvée pour cette période");
                recommandations.add("💡 Vérifiez les dates et statuts des commandes");
            } else {
                recommandations.add("✅ " + commandes.size() + " commandes prêtes pour la planification");
                recommandations.add("📊 " + totalCartes + " cartes à traiter (" + Math.round(pourcentageGlobal) + "% avec nom)");
                recommandations.add("⏱️ " + Math.round(tempsTotal / 60.0 * 100) / 100.0 + "h de travail estimé");

                if (pourcentageGlobal >= 95) {
                    recommandations.add("🎯 Excellente qualité des données - Planification optimale possible");
                } else if (pourcentageGlobal >= 80) {
                    recommandations.add("⚠️ Bonne qualité des données - Quelques cartes sans nom");
                } else {
                    recommandations.add("🔧 Qualité des données à améliorer - Vérifier les traductions");
                }

                // Recommandation sur l'algorithme à utiliser
                if (commandes.size() <= 20) {
                    recommandations.add("🚀 Recommandation: Utiliser l'algorithme DP (solution optimale)");
                } else {
                    recommandations.add("⚡ Recommandation: Utiliser l'algorithme glouton (solution rapide)");
                }
            }

            response.put("recommandations", recommandations);
            response.put("pretPourPlanification", !commandes.isEmpty());
            response.put("status", "OK");

            System.out.println("✅ Période planification: " + commandes.size() + " commandes, "
                    + totalCartes + " cartes (" + Math.round(pourcentageGlobal) + "% avec nom)");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur période planification: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 🎯 ENDPOINT FRONTEND - DIAGNOSTIC PÉRIODE DE PLANIFICATION
     */
    @GetMapping("/api/frontend/diagnostic-periode")
    public ResponseEntity<Map<String, Object>> diagnosticPeriodePlanification() {
        try {
            System.out.println("🎯 === DIAGNOSTIC PÉRIODE PLANIFICATION ===");

            Map<String, Object> diagnostic = new HashMap<>();

            // 1. Vérifier les commandes disponibles par statut
            String sqlStatuts = """
        SELECT 
            status,
            COUNT(*) as nombre,
            COUNT(CASE WHEN date >= '2025-05-22' AND date <= '2025-06-22' THEN 1 END) as dans_periode
        FROM `order`
        WHERE date IS NOT NULL
        GROUP BY status
        ORDER BY status
        """;

            Query queryStatuts = entityManager.createNativeQuery(sqlStatuts);
            @SuppressWarnings("unchecked")
            List<Object[]> statutsResults = queryStatuts.getResultList();

            List<Map<String, Object>> statutsAnalyse = new ArrayList<>();
            for (Object[] row : statutsResults) {
                Map<String, Object> statut = new HashMap<>();
                statut.put("status", row[0]);
                statut.put("nombre_total", row[1]);
                statut.put("dans_periode", row[2]);
                statut.put("statut_libelle", mapStatusToString((Number) row[0]));
                statutsAnalyse.add(statut);
            }
            diagnostic.put("analyse_statuts", statutsAnalyse);

            // 2. Vérifier la disponibilité des cartes
            String sqlCartes = """
        SELECT 
            COUNT(DISTINCT o.id) as commandes_avec_cartes,
            COUNT(DISTINCT cco.card_certification_id) as total_certifications,
            COUNT(DISTINCT CASE WHEN ct.name IS NOT NULL THEN cco.card_certification_id END) as avec_nom
        FROM `order` o
        INNER JOIN card_certification_order cco ON o.id = cco.order_id
        INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
        LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id 
            AND (
                (cc.langue = 'FR' AND ct.locale = 'fr') OR
                (cc.langue = 'EN' AND ct.locale = 'us') OR
                (cc.langue = 'US' AND ct.locale = 'us')
            )
        WHERE o.date >= '2025-05-22' AND o.date <= '2025-06-22'
            AND o.status IN (1, 2)
        """;

            try {
                Query queryCartes = entityManager.createNativeQuery(sqlCartes);
                Object[] cartesResult = (Object[]) queryCartes.getSingleResult();

                Number commandesAvecCartes = (Number) cartesResult[0];
                Number totalCertifications = (Number) cartesResult[1];
                Number avecNom = (Number) cartesResult[2];

                double pourcentageNom = totalCertifications.intValue() > 0 ?
                        (avecNom.doubleValue() / totalCertifications.doubleValue()) * 100 : 0;

                diagnostic.put("analyse_cartes", Map.of(
                        "commandes_avec_cartes", commandesAvecCartes.intValue(),
                        "total_certifications", totalCertifications.intValue(),
                        "avec_nom", avecNom.intValue(),
                        "pourcentage_avec_nom", Math.round(pourcentageNom)
                ));

            } catch (Exception e) {
                diagnostic.put("erreur_analyse_cartes", e.getMessage());
            }

            // 3. Vérifier les employés disponibles
            try {
                List<Map<String, Object>> employes = employeService.getTousEmployesActifs();
                diagnostic.put("employes_disponibles", employes.size());
            } catch (Exception e) {
                diagnostic.put("erreur_employes", e.getMessage());
            }

            // 4. Dates disponibles dans la base
            String sqlDates = """
        SELECT 
            MIN(date) as date_min,
            MAX(date) as date_max,
            COUNT(*) as total_commandes
        FROM `order`
        WHERE date IS NOT NULL
        """;

            try {
                Query queryDates = entityManager.createNativeQuery(sqlDates);
                Object[] datesResult = (Object[]) queryDates.getSingleResult();

                diagnostic.put("donnees_disponibles", Map.of(
                        "date_min", datesResult[0],
                        "date_max", datesResult[1],
                        "total_commandes", datesResult[2]
                ));

            } catch (Exception e) {
                diagnostic.put("erreur_dates", e.getMessage());
            }

            // 5. Verdict et recommandations
            List<String> recommandations = new ArrayList<>();
            boolean systemePret = true;

            // Vérifier les prérequis
            if (!diagnostic.containsKey("employes_disponibles") ||
                    (Integer) diagnostic.getOrDefault("employes_disponibles", 0) == 0) {
                recommandations.add("❌ Aucun employé disponible");
                systemePret = false;
            }

            if (!diagnostic.containsKey("analyse_cartes")) {
                recommandations.add("❌ Problème avec l'analyse des cartes");
                systemePret = false;
            }

            if (systemePret) {
                recommandations.add("✅ Système prêt pour la planification");
                recommandations.add("📅 Période: 22 mai 2025 - 22 juin 2025");
                recommandations.add("🚀 Vous pouvez lancer la planification automatique");
            }

            diagnostic.put("recommandations", recommandations);
            diagnostic.put("systeme_pret", systemePret);
            diagnostic.put("periode_cible", "2025-05-22 à 2025-06-22");
            diagnostic.put("status", "OK");

            return ResponseEntity.ok(diagnostic);

        } catch (Exception e) {
            System.err.println("❌ Erreur diagnostic période: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // Méthode utilitaire pour mapper les statuts
    private String mapStatusToString(Number status) {
        if (status == null) return "INCONNU";

        return switch (status.intValue()) {
            case 1 -> "EN_ATTENTE";
            case 2 -> "PLANIFIEE";
            case 3 -> "EN_COURS";
            case 4 -> "TERMINEE";
            case 5 -> "ANNULEE";
            case 11 -> "VALIDEE";
            default -> "STATUT_" + status;
        };
    }

}