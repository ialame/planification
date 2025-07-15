package com.pcagrade.order.controller;

import com.pcagrade.order.service.CommandeService;
import com.pcagrade.order.service.DynamicProgrammingPlanificationService;
import com.pcagrade.order.service.EmployeService;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/planification-dp")
public class DynamicProgrammingController {

    @Autowired
    private DynamicProgrammingPlanificationService dpService;

    @Autowired
    private CommandeService commandeService;

    @Autowired
    private EmployeService employeService;

    @Autowired
    private EntityManager entityManager;

    /**
     * Exécute la planification avec l'algorithme de programmation dynamique optimisé
     *
     * @param jour Jour de début (1-31)
     * @param mois Mois de début (1-12)
     * @param annee Année de début
     * @return Résultat de la planification avec statistiques
     */
    @PostMapping("/executer")
    public ResponseEntity<Map<String, Object>> executerPlanificationDP(
            @RequestParam int jour,
            @RequestParam int mois,
            @RequestParam int annee) {

        try {
            System.out.println("🎯 API: Demande planification DP pour " + jour + "/" + mois + "/" + annee);

            // Validation des paramètres
            if (!isDateValide(jour, mois, annee)) {
                Map<String, Object> erreur = new HashMap<>();
                erreur.put("success", false);
                erreur.put("message", "Date invalide: " + jour + "/" + mois + "/" + annee);
                return ResponseEntity.badRequest().body(erreur);
            }

            // Exécution de la planification
            Map<String, Object> resultat = dpService.executerPlanificationDP(jour, mois, annee);

            if ((Boolean) resultat.get("success")) {
                System.out.println("✅ API: Planification DP réussie");
                return ResponseEntity.ok(resultat);
            } else {
                System.out.println("❌ API: Planification DP échouée");
                return ResponseEntity.status(500).body(resultat);
            }

        } catch (Exception e) {
            System.err.println("❌ API: Erreur planification DP: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> erreur = new HashMap<>();
            erreur.put("success", false);
            erreur.put("message", "Erreur interne: " + e.getMessage());
            erreur.put("type", "INTERNAL_ERROR");

            return ResponseEntity.status(500).body(erreur);
        }
    }

    /**
     * Exécute la planification pour aujourd'hui (raccourci)
     */
    @PostMapping("/executer/aujourd-hui")
    public ResponseEntity<Map<String, Object>> executerPlanificationAujourdHui() {
        LocalDate aujourdHui = LocalDate.now();
        return executerPlanificationDP(
                aujourdHui.getDayOfMonth(),
                aujourdHui.getMonthValue(),
                aujourdHui.getYear()
        );
    }

    /**
     * Compare les performances entre l'algorithme glouton et DP
     */
    @PostMapping("/comparer")
    public ResponseEntity<Map<String, Object>> comparerAlgorithmes(
            @RequestParam int jour,
            @RequestParam int mois,
            @RequestParam int annee) {

        try {
            System.out.println("📊 API: Comparaison algorithmes pour " + jour + "/" + mois + "/" + annee);

            long tempsDebut = System.currentTimeMillis();

            // 1. Exécuter l'algorithme DP
            Map<String, Object> resultatDP = dpService.executerPlanificationDP(jour, mois, annee);

            // 2. TODO: Exécuter l'algorithme glouton pour comparaison
            // Map<String, Object> resultatGlouton = gloutonService.executerPlanification(jour, mois, annee);

            long tempsFinal = System.currentTimeMillis() - tempsDebut;

            // 3. Créer la comparaison
            Map<String, Object> comparaison = new HashMap<>();
            comparaison.put("success", true);
            comparaison.put("tempsComparaisonMs", tempsFinal);
            comparaison.put("dateAnalysee", jour + "/" + mois + "/" + annee);

            // Résultats DP
            Map<String, Object> infoDP = new HashMap<>();
            infoDP.put("methode", resultatDP.get("methode"));
            infoDP.put("tempsCalculMs", resultatDP.get("tempsCalculMs"));
            infoDP.put("scoreTotal", resultatDP.get("scoreTotal"));
            infoDP.put("nombrePlanifications", resultatDP.get("nombrePlanifications"));
            infoDP.put("stats", resultatDP.get("stats"));

            comparaison.put("algorithmeDynamique", infoDP);

            // TODO: Ajouter les résultats glouton
            Map<String, Object> infoGlouton = new HashMap<>();
            infoGlouton.put("methode", "GLOUTON");
            infoGlouton.put("tempsCalculMs", 0);
            infoGlouton.put("scoreTotal", 0.0);
            infoGlouton.put("nombrePlanifications", 0);
            infoGlouton.put("note", "Non implémenté dans cette version");

            comparaison.put("algorithmeGlouton", infoGlouton);

            // Analyse comparative
            Map<String, Object> analyse = new HashMap<>();
            analyse.put("methodeRecommandee", resultatDP.get("methode"));
            analyse.put("raisonRecommandation", "Algorithme adaptatif selon la complexité");

            comparaison.put("analyse", analyse);

            System.out.println("✅ API: Comparaison terminée en " + tempsFinal + "ms");

            return ResponseEntity.ok(comparaison);

        } catch (Exception e) {
            System.err.println("❌ API: Erreur comparaison: " + e.getMessage());

            Map<String, Object> erreur = new HashMap<>();
            erreur.put("success", false);
            erreur.put("message", "Erreur lors de la comparaison: " + e.getMessage());

            return ResponseEntity.status(500).body(erreur);
        }
    }

    /**
     * Obtient les statistiques de performance de l'algorithme DP
     */
    @GetMapping("/statistiques")
    public ResponseEntity<Map<String, Object>> getStatistiquesPerformance() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // Informations sur l'algorithme
            Map<String, Object> infosAlgorithme = new HashMap<>();
            infosAlgorithme.put("nom", "Programmation Dynamique Optimisée");
            infosAlgorithme.put("complexiteTheorique", "O(n × T × m)");
            infosAlgorithme.put("creneauDureeMinutes", 60);
            infosAlgorithme.put("heuresTravailParJour", 8);
            infosAlgorithme.put("joursPlanification", 30);
            infosAlgorithme.put("seuilComplexiteMax", "10M opérations");

            stats.put("algorithme", infosAlgorithme);

            // Configuration système
            Map<String, Object> configSystme = new HashMap<>();
            configSystme.put("heureDebutTravail", "09:00");
            configSystme.put("heureFinTravail", "17:00");
            configSystme.put("discrétisation", "Créneaux d'1 heure");
            configSystme.put("optimisations", "Tri par priorité, early stopping, mémoization");

            stats.put("configuration", configSystme);

            // Métriques de qualité
            Map<String, Object> qualite = new HashMap<>();
            qualite.put("optimalite", "Optimale pour petites instances");
            qualite.put("approximation", "Glouton optimisé pour grandes instances");
            qualite.put("criteresOptimisation", "Score = Priorité + Prix + Délai");

            stats.put("qualite", qualite);

            stats.put("success", true);
            stats.put("version", "1.0.0");
            stats.put("derniereMiseAJour", LocalDate.now().toString());

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            Map<String, Object> erreur = new HashMap<>();
            erreur.put("success", false);
            erreur.put("message", "Erreur récupération statistiques: " + e.getMessage());

            return ResponseEntity.status(500).body(erreur);
        }
    }

    /**
     * Test de l'algorithme avec des données simulées
     */
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> testerAlgorithme(
            @RequestParam(defaultValue = "10") int nombreCommandes,
            @RequestParam(defaultValue = "3") int nombreEmployes) {

        try {
            System.out.println("🧪 API: Test algorithme avec " + nombreCommandes + " commandes et " + nombreEmployes + " employés");

            Map<String, Object> resultatTest = new HashMap<>();
            resultatTest.put("success", true);
            resultatTest.put("message", "Test à implémenter");
            resultatTest.put("parametres", Map.of(
                    "nombreCommandes", nombreCommandes,
                    "nombreEmployes", nombreEmployes
            ));

            // TODO: Implémenter la génération de données de test
            resultatTest.put("note", "Fonctionnalité de test en développement");

            return ResponseEntity.ok(resultatTest);

        } catch (Exception e) {
            Map<String, Object> erreur = new HashMap<>();
            erreur.put("success", false);
            erreur.put("message", "Erreur test: " + e.getMessage());

            return ResponseEntity.status(500).body(erreur);
        }
    }

    /**
     * Validation d'une date
     */
    private boolean isDateValide(int jour, int mois, int annee) {
        try {
            LocalDate.of(annee, mois, jour);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Endpoint de santé pour vérifier que le service est opérationnel
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "OK");
        health.put("service", "DynamicProgrammingPlanificationService");
        health.put("timestamp", System.currentTimeMillis());
        health.put("version", "1.0.0");

        return ResponseEntity.ok(health);
    }


    /**
     * ✅ ENDPOINT DE DEBUG - Ajouter dans DynamicProgrammingController.java
     */
    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debugServices() {
        Map<String, Object> debug = new HashMap<>();

        try {
            // Test CommandeService
            try {
                List<Map<String, Object>> commandes = commandeService.getCommandesAPlanifierDepuisDate(1, 1, 2025);
                debug.put("commandesService", "OK - " + commandes.size() + " commandes");
            } catch (Exception e) {
                debug.put("commandesService", "ERREUR: " + e.getMessage());
            }

            // Test EmployeService
            try {
                List<Map<String, Object>> employes = employeService.getTousEmployesActifs();
                debug.put("employesService", "OK - " + employes.size() + " employés");
            } catch (Exception e) {
                debug.put("employesService", "ERREUR: " + e.getMessage());
            }

            // Test DPService
            debug.put("dpService", dpService != null ? "OK" : "NULL");

            // Test EntityManager
            debug.put("entityManager", entityManager != null ? "OK" : "NULL");

            debug.put("status", "DEBUG_COMPLET");
            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            debug.put("erreurGlobale", e.getMessage());
            return ResponseEntity.status(500).body(debug);
        }
    }

}