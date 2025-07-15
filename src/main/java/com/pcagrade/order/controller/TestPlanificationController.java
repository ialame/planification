package com.pcagrade.order.controller;// 🧪 CRÉER UN ENDPOINT DE TEST pour valider l'interface

import com.pcagrade.order.entity.Employe;
import com.pcagrade.order.service.EmployeService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/test")
@CrossOrigin(origins = "http://localhost:5173")
public class TestPlanificationController {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EmployeService employeService;

    /**
     * 🧪 Créer quelques planifications de test pour valider l'interface
     */
    @PostMapping("/creer-planifications-test")
    public ResponseEntity<Map<String, Object>> creerPlanificationsTest() {
        Map<String, Object> resultat = new HashMap<>();

        try {
            System.out.println("🧪 === CRÉATION DE PLANIFICATIONS TEST ===");

            // 1. Récupérer les employés existants
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();
            if (employes.isEmpty()) {
                resultat.put("success", false);
                resultat.put("message", "Aucun employé trouvé");
                return ResponseEntity.ok(resultat);
            }

            // 2. Récupérer quelques commandes
            String sqlCommandes = """
                SELECT HEX(id) as id, num_commande, nombre_cartes
                FROM `order` 
                WHERE status IN (1, 2)
                ORDER BY date DESC
                LIMIT 5
                """;

            Query queryCommandes = entityManager.createNativeQuery(sqlCommandes);
            @SuppressWarnings("unchecked")
            List<Object[]> commandesData = queryCommandes.getResultList();

            if (commandesData.isEmpty()) {
                resultat.put("success", false);
                resultat.put("message", "Aucune commande trouvée");
                return ResponseEntity.ok(resultat);
            }

            System.out.println("📦 " + commandesData.size() + " commandes trouvées");
            System.out.println("👥 " + employes.size() + " employés trouvés");

            // 3. Créer des planifications de test
            List<Map<String, Object>> planificationsCreees = new ArrayList<>();
            LocalDate aujourdHui = LocalDate.now();

            for (int i = 0; i < Math.min(commandesData.size(), 3); i++) {
                Object[] commande = commandesData.get(i);
                Map<String, Object> employe = employes.get(i % employes.size());

                String commandeId = (String) commande[0];
                String numeroCommande = (String) commande[1];
                Object nombreCartesObj = commande[2];
                int nombreCartes = nombreCartesObj != null ? ((Number) nombreCartesObj).intValue() : 10;

                String employeId = (String) employe.get("id");
                String employeNom = employe.get("prenom") + " " + employe.get("nom");

                // Créer la planification
                String planificationId = creerPlanificationTest(
                        commandeId,
                        employeId,
                        aujourdHui,
                        LocalTime.of(9 + i, 0),
                        Math.max(nombreCartes * 3, 30) // 3 min par carte, min 30 min
                );

                if (planificationId != null) {
                    Map<String, Object> planif = new HashMap<>();
                    planif.put("id", planificationId);
                    planif.put("commandeId", commandeId);
                    planif.put("numeroCommande", numeroCommande);
                    planif.put("employeId", employeId);
                    planif.put("employeNom", employeNom);
                    planif.put("date", aujourdHui.toString());
                    planif.put("heure", String.format("%02d:00", 9 + i));
                    planif.put("nombreCartes", nombreCartes);

                    planificationsCreees.add(planif);

                    System.out.println("✅ Planification créée: " + numeroCommande + " → " + employeNom);
                }
            }

            resultat.put("success", true);
            resultat.put("message", planificationsCreees.size() + " planifications de test créées");
            resultat.put("planifications", planificationsCreees);
            resultat.put("date", aujourdHui.toString());

            System.out.println("🎉 " + planificationsCreees.size() + " planifications test créées avec succès");

            return ResponseEntity.ok(resultat);

        } catch (Exception e) {
            System.err.println("❌ Erreur création planifications test: " + e.getMessage());
            e.printStackTrace();

            resultat.put("success", false);
            resultat.put("message", "Erreur: " + e.getMessage());
            return ResponseEntity.status(500).body(resultat);
        }
    }

    /**
     * Créer une planification individuelle
     */
    private String creerPlanificationTest(String commandeId, String employeId, LocalDate date, LocalTime heure, int dureeMinutes) {
        try {
            String planificationId = UUID.randomUUID().toString().replace("-", "");

            String sql = """
                INSERT INTO j_planification 
                (id, order_id, employe_id, date_planification, heure_debut, duree_minutes, terminee, date_creation, date_modification)
                VALUES (UNHEX(?), UNHEX(?), UNHEX(?), ?, ?, ?, false, NOW(), NOW())
                """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, planificationId);
            query.setParameter(2, commandeId.replace("-", ""));
            query.setParameter(3, employeId.replace("-", ""));
            query.setParameter(4, date);
            query.setParameter(5, heure);
            query.setParameter(6, dureeMinutes);

            int result = query.executeUpdate();
            return result > 0 ? planificationId : null;

        } catch (Exception e) {
            System.err.println("❌ Erreur création planification: " + e.getMessage());
            return null;
        }
    }

    /**
     * 🧪 Vider toutes les planifications de test
     */
    @PostMapping("/vider-planifications-test")
    public ResponseEntity<Map<String, Object>> viderPlanificationsTest() {
        Map<String, Object> resultat = new HashMap<>();

        try {
            String sql = "DELETE FROM j_planification WHERE date_planification = CURDATE()";
            Query query = entityManager.createNativeQuery(sql);
            int supprimees = query.executeUpdate();

            resultat.put("success", true);
            resultat.put("message", supprimees + " planifications supprimées");
            resultat.put("planifications_supprimees", supprimees);

            System.out.println("🧹 " + supprimees + " planifications test supprimées");

            return ResponseEntity.ok(resultat);

        } catch (Exception e) {
            System.err.println("❌ Erreur suppression planifications: " + e.getMessage());

            resultat.put("success", false);
            resultat.put("message", "Erreur: " + e.getMessage());
            return ResponseEntity.status(500).body(resultat);
        }
    }

    /**
     * 🧪 Tester la récupération des données employé
     */
    @GetMapping("/debug-employe/{employeId}")
    public ResponseEntity<Map<String, Object>> debugEmploye(@PathVariable String employeId) {
        Map<String, Object> debug = new HashMap<>();

        try {
            System.out.println("🔍 Debug employé: " + employeId);

            // 1. Vérifier l'employé
            Optional<Employe> employeOpt = employeService.getEmployeById(employeId);
            debug.put("employe_existe", employeOpt.isPresent());

            if (employeOpt.isPresent()) {
                Employe emp = employeOpt.get();
                debug.put("employe_details", Map.of(
                        "id", emp.getId().toString(),
                        "nom", emp.getNom(),
                        "prenom", emp.getPrenom(),
                        "actif", emp.getActif(),
                        "heures", emp.getHeuresTravailParJour()
                ));
            }

            // 2. Vérifier les planifications
            String sqlPlanif = """
                SELECT COUNT(*) FROM j_planification 
                WHERE HEX(employe_id) = ? 
                AND date_planification = CURDATE()
                """;

            Query queryPlanif = entityManager.createNativeQuery(sqlPlanif);
            queryPlanif.setParameter(1, employeId.replace("-", ""));
            Number countPlanif = (Number) queryPlanif.getSingleResult();

            debug.put("planifications_aujourd_hui", countPlanif.intValue());

            // 3. Test de la requête complète
            String sqlComplete = """
                SELECT 
                    HEX(o.id) as order_id,
                    o.num_commande,
                    p.heure_debut,
                    p.duree_minutes,
                    (SELECT COUNT(*) FROM card_certification_order cco WHERE cco.order_id = o.id) as nombre_cartes
                FROM j_planification p
                JOIN `order` o ON p.order_id = o.id
                WHERE HEX(p.employe_id) = ?
                AND DATE(p.date_planification) = CURDATE()
                """;

            Query queryComplete = entityManager.createNativeQuery(sqlComplete);
            queryComplete.setParameter(1, employeId.replace("-", ""));
            @SuppressWarnings("unchecked")
            List<Object[]> resultats = queryComplete.getResultList();

            debug.put("commandes_trouvees", resultats.size());
            debug.put("details_commandes", resultats.stream().limit(3).map(row -> Map.of(
                    "commande", row[1],
                    "heure", row[2],
                    "duree", row[3],
                    "cartes", row[4]
            )).toList());

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            debug.put("erreur", e.getMessage());
            return ResponseEntity.ok(debug);
        }
    }

    /**
     * 🃏 SOLUTION COMPLÈTE POUR LES VRAIS NOMS DE CARTES
     *
     * ✅ À AJOUTER dans TestController.java
     *
     * Cette méthode remplace les "Carte 1", "Carte 2" par les vrais noms
     */

// 1. ✅ ENDPOINT PRINCIPAL - CARTES DÉTAILLÉES AVEC VRAIS NOMS
    @GetMapping("/commandes/{commandeId}/cartes-details")
    public ResponseEntity<Map<String, Object>> getCartesDetailsCommande(@PathVariable String commandeId) {
        try {
            System.out.println("🃏 === RÉCUPÉRATION VRAIS NOMS - COMMANDE " + commandeId + " ===");

            String orderIdClean = commandeId.replace("-", "");
            Map<String, Object> response = new HashMap<>();

            // ✅ VÉRIFIER QUE LA COMMANDE EXISTE
            String sqlCommande = """
            SELECT 
                HEX(o.id) as id,
                o.num_commande,
                COALESCE(o.priorite_string, 'NORMALE') as priorite,
                DATE(o.date) as date
            FROM `order` o 
            WHERE HEX(o.id) = ?
            """;

            try {
                Query queryCommande = entityManager.createNativeQuery(sqlCommande);
                queryCommande.setParameter(1, orderIdClean);
                Object[] commandeInfo = (Object[]) queryCommande.getSingleResult();

                response.put("commande_info", Map.of(
                        "id", commandeInfo[0],
                        "num_commande", commandeInfo[1] != null ? commandeInfo[1] : "N/A",
                        "priorite", commandeInfo[2],
                        "date", commandeInfo[3] != null ? commandeInfo[3] : "N/A"
                ));

            } catch (NoResultException e) {
                System.err.println("❌ Commande non trouvée: " + commandeId);
                return ResponseEntity.status(404).body(Map.of("error", "Commande non trouvée"));
            }

            // ✅ RÉCUPÉRER LES VRAIES CARTES AVEC STRATÉGIE MULTI-ÉTAPES
            String sqlVraisNoms = """
            SELECT 
                HEX(cc.id) as cert_id,
                HEX(cc.card_id) as card_id,
                cc.code_barre,
                cc.langue as cert_langue,
                cc.edition,
                
                -- Stratégie 1: Correspondance exacte langue/locale
                COALESCE(
                    (SELECT ct1.name FROM card_translation ct1 
                     WHERE ct1.translatable_id = cc.card_id 
                     AND ct1.locale = cc.langue 
                     AND ct1.name IS NOT NULL AND ct1.name != ''
                     LIMIT 1),
                    ''
                ) as nom_exact,
                
                -- Stratégie 2: Français en priorité
                COALESCE(
                    (SELECT ct2.name FROM card_translation ct2 
                     WHERE ct2.translatable_id = cc.card_id 
                     AND ct2.locale IN ('fr', 'fr_FR')
                     AND ct2.name IS NOT NULL AND ct2.name != ''
                     ORDER BY CASE ct2.locale WHEN 'fr' THEN 1 ELSE 2 END
                     LIMIT 1),
                    ''
                ) as nom_francais,
                
                -- Stratégie 3: Anglais si pas de français
                COALESCE(
                    (SELECT ct3.name FROM card_translation ct3 
                     WHERE ct3.translatable_id = cc.card_id 
                     AND ct3.locale IN ('en', 'en_US', 'us')
                     AND ct3.name IS NOT NULL AND ct3.name != ''
                     ORDER BY CASE ct3.locale WHEN 'en' THEN 1 WHEN 'us' THEN 2 ELSE 3 END
                     LIMIT 1),
                    ''
                ) as nom_anglais,
                
                -- Stratégie 4: N'importe quelle traduction
                COALESCE(
                    (SELECT ct4.name FROM card_translation ct4 
                     WHERE ct4.translatable_id = cc.card_id 
                     AND ct4.name IS NOT NULL AND ct4.name != ''
                     ORDER BY ct4.id
                     LIMIT 1),
                    ''
                ) as nom_quelconque,
                
                -- Infos supplémentaires
                (SELECT ct_info.label_name FROM card_translation ct_info 
                 WHERE ct_info.translatable_id = cc.card_id 
                 AND ct_info.label_name IS NOT NULL 
                 ORDER BY CASE ct_info.locale WHEN 'fr' THEN 1 WHEN 'en' THEN 2 ELSE 3 END
                 LIMIT 1) as label_name
                
            FROM card_certification_order cco
            INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
            WHERE HEX(cco.order_id) = ?
            ORDER BY cc.code_barre
            """;

            Query queryCartes = entityManager.createNativeQuery(sqlVraisNoms);
            queryCartes.setParameter(1, orderIdClean);

            @SuppressWarnings("unchecked")
            List<Object[]> cartesResults = queryCartes.getResultList();

            // ✅ CONSTRUIRE LA LISTE AVEC ALGORITHME DE SÉLECTION DU MEILLEUR NOM
            List<Map<String, Object>> cartes = new ArrayList<>();
            int strategieExacte = 0;
            int strategieFrancais = 0;
            int strategieAnglais = 0;
            int strategieAutre = 0;
            int strategieFallback = 0;

            for (Object[] row : cartesResults) {
                Map<String, Object> carte = new HashMap<>();

                // IDs et infos de base
                carte.put("cert_id", row[0]);
                carte.put("card_id", row[1]);
                carte.put("code_barre", row[2]);
                carte.put("cert_langue", row[3]);
                carte.put("edition", row[4] != null ? ((Number) row[4]).intValue() : null);

                // ✅ ALGORITHME DE SÉLECTION DU MEILLEUR NOM
                String nomExact = (String) row[5];
                String nomFrancais = (String) row[6];
                String nomAnglais = (String) row[7];
                String nomQuelconque = (String) row[8];
                String labelName = (String) row[9];

                String nomFinal;
                String strategieUtilisee;

                if (nomExact != null && !nomExact.trim().isEmpty()) {
                    // PRIORITÉ 1: Correspondance exacte
                    nomFinal = nomExact.trim();
                    strategieUtilisee = "CORRESPONDANCE_EXACTE";
                    strategieExacte++;

                } else if (nomFrancais != null && !nomFrancais.trim().isEmpty()) {
                    // PRIORITÉ 2: Français
                    nomFinal = nomFrancais.trim();
                    strategieUtilisee = "FRANCAIS_PRIORITAIRE";
                    strategieFrancais++;

                } else if (nomAnglais != null && !nomAnglais.trim().isEmpty()) {
                    // PRIORITÉ 3: Anglais
                    nomFinal = nomAnglais.trim();
                    strategieUtilisee = "ANGLAIS_FALLBACK";
                    strategieAnglais++;

                } else if (nomQuelconque != null && !nomQuelconque.trim().isEmpty()) {
                    // PRIORITÉ 4: N'importe quelle traduction
                    nomFinal = nomQuelconque.trim();
                    strategieUtilisee = "AUTRE_LANGUE";
                    strategieAutre++;

                } else {
                    // PRIORITÉ 5: Fallback avec code barre
                    nomFinal = "Carte-" + (row[2] != null ? row[2] : "INCONNUE");
                    strategieUtilisee = "FALLBACK_CODE_BARRE";
                    strategieFallback++;
                }

                // Remplir les données de la carte
                carte.put("nom", nomFinal);
                carte.put("name", nomFinal);
                carte.put("label_name", labelName);
                carte.put("strategie_nom", strategieUtilisee);

                // Indicateur de qualité
                boolean aVraiNom = !strategieUtilisee.equals("FALLBACK_CODE_BARRE");
                carte.put("qualite_nom", aVraiNom ? "AVEC_NOM" : "SANS_NOM");
                carte.put("a_vrai_nom", aVraiNom);

                cartes.add(carte);
            }

            // ✅ STATISTIQUES ET RÉPONSE
            int totalCartes = cartes.size();
            int avecVraiNom = strategieExacte + strategieFrancais + strategieAnglais + strategieAutre;
            double pourcentageVraiNom = totalCartes > 0 ? (avecVraiNom * 100.0) / totalCartes : 0;

            response.put("cartes_details_uniques", cartes);
            response.put("statistiques", Map.of(
                    "total_cartes", totalCartes,
                    "avec_vrai_nom", avecVraiNom,
                    "sans_nom", strategieFallback,
                    "pourcentage_avec_nom", Math.round(pourcentageVraiNom),
                    "strategies", Map.of(
                            "correspondance_exacte", strategieExacte,
                            "francais_prioritaire", strategieFrancais,
                            "anglais_fallback", strategieAnglais,
                            "autre_langue", strategieAutre,
                            "fallback_code_barre", strategieFallback
                    )
            ));

            String qualiteGlobale = pourcentageVraiNom == 100 ? "PARFAITE" :
                    pourcentageVraiNom >= 90 ? "EXCELLENTE" :
                            pourcentageVraiNom >= 70 ? "BONNE" : "MOYENNE";

            response.put("qualite_globale", qualiteGlobale);
            response.put("frontend_pret", true);

            // ✅ LOGS DÉTAILLÉS
            System.out.println("✅ VRAIS NOMS RÉCUPÉRÉS - Commande " + commandeId + ":");
            System.out.println("   📊 " + totalCartes + " cartes total");
            System.out.println("   ✅ " + avecVraiNom + " avec vrai nom (" + Math.round(pourcentageVraiNom) + "%)");
            System.out.println("   🎯 Exacte=" + strategieExacte + ", FR=" + strategieFrancais +
                    ", EN=" + strategieAnglais + ", Autre=" + strategieAutre +
                    ", Fallback=" + strategieFallback);
            System.out.println("   📈 Qualité globale: " + qualiteGlobale);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération vrais noms: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", e.getMessage(),
                    "cartes_details_uniques", new ArrayList<>(),
                    "statistiques", Map.of("total_cartes", 0, "avec_vrai_nom", 0)
            ));
        }
    }

    // 2. ✅ ENDPOINT DE TEST POUR VÉRIFIER UNE COMMANDE SPÉCIFIQUE
    @GetMapping("/test-vrais-noms-commande/{commandeId}")
    public ResponseEntity<Map<String, Object>> testVraisNomsCommande(@PathVariable String commandeId) {
        try {
            System.out.println("🧪 === TEST VRAIS NOMS POUR COMMANDE " + commandeId + " ===");

            // Appeler notre endpoint principal
            ResponseEntity<Map<String, Object>> response = getCartesDetailsCommande(commandeId);
            Map<String, Object> data = response.getBody();

            if (data != null && data.containsKey("cartes_details_uniques")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> cartes = (List<Map<String, Object>>) data.get("cartes_details_uniques");

                Map<String, Object> testResult = new HashMap<>();
                testResult.put("commande_testee", commandeId);
                testResult.put("endpoint_fonctionne", true);
                testResult.put("nb_cartes_recuperees", cartes.size());

                // Analyser les stratégies utilisées
                Map<String, Integer> strategies = new HashMap<>();
                for (Map<String, Object> carte : cartes) {
                    String strategie = (String) carte.get("strategie_nom");
                    strategies.merge(strategie, 1, Integer::sum);
                }
                testResult.put("strategies_utilisees", strategies);

                // Exemples de noms
                List<String> exemplesNoms = cartes.stream()
                        .limit(5)
                        .map(carte -> (String) carte.get("nom"))
                        .collect(java.util.stream.Collectors.toList());
                testResult.put("exemples_noms", exemplesNoms);

                testResult.put("donnees_completes", data);

                return ResponseEntity.ok(testResult);
            } else {
                return ResponseEntity.status(500).body(Map.of(
                        "commande_testee", commandeId,
                        "endpoint_fonctionne", false,
                        "erreur", "Pas de cartes dans la réponse"
                ));
            }

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "commande_testee", commandeId,
                    "endpoint_fonctionne", false,
                    "erreur", e.getMessage()
            ));
        }
    }

    // 3. ✅ ENDPOINT POUR LISTER LES COMMANDES DISPONIBLES POUR TEST
    @GetMapping("/commandes-disponibles-test")
    public ResponseEntity<List<Map<String, Object>>> getCommandesDisponiblesPourTest() {
        try {
            System.out.println("📋 === COMMANDES DISPONIBLES POUR TEST VRAIS NOMS ===");

            String sqlCommandes = """
            SELECT 
                HEX(o.id) as order_id,
                o.num_commande,
                DATE(o.date) as date,
                COUNT(DISTINCT cco.card_certification_id) as nb_cartes
            FROM `order` o
            INNER JOIN card_certification_order cco ON o.id = cco.order_id
            WHERE o.date >= '2025-06-01'
            GROUP BY o.id, o.num_commande, o.date
            HAVING COUNT(DISTINCT cco.card_certification_id) > 0
            ORDER BY o.date DESC
            LIMIT 10
            """;

            Query query = entityManager.createNativeQuery(sqlCommandes);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> commandesDisponibles = new ArrayList<>();
            for (Object[] row : results) {
                Map<String, Object> commande = new HashMap<>();
                commande.put("order_id", row[0]);
                commande.put("num_commande", row[1]);
                commande.put("date", row[2]);
                commande.put("nb_cartes", row[3]);
                commande.put("url_test", "/api/test/test-vrais-noms-commande/" + row[0]);
                commandesDisponibles.add(commande);
            }

            System.out.println("✅ " + commandesDisponibles.size() + " commandes disponibles pour test");

            return ResponseEntity.ok(commandesDisponibles);

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération commandes test: " + e.getMessage());
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
}
