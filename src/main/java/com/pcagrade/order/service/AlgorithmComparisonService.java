package com.pcagrade.order.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Service de comparaison entre les différents algorithmes de planification
 */
@Service
@Transactional
public class AlgorithmComparisonService {

    @Autowired
    private DynamicProgrammingPlanificationService dpService;

    @Autowired
    private PlanificationService planificationService; // Votre service glouton existant

    @Autowired
    private CommandeService commandeService;

    @Autowired
    private EmployeService employeService;

    /**
     * Compare les performances des deux algorithmes sur les mêmes données
     */
    public Map<String, Object> comparerAlgorithmes(int jour, int mois, int annee) {
        Map<String, Object> comparaison = new HashMap<>();
        long tempsDebutTotal = System.currentTimeMillis();

        try {
            System.out.println("⚖️ === COMPARAISON ALGORITHMES ===");
            System.out.printf("📅 Date d'analyse: %02d/%02d/%d%n", jour, mois, annee);

            // 1. Analyser les données d'entrée
            List<Map<String, Object>> commandes = commandeService.getCommandesAPlanifierDepuisDate(jour, mois, annee);
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();

            Map<String, Object> donneesEntree = analyserDonneesEntree(commandes, employes);
            comparaison.put("donneesEntree", donneesEntree);

            if (commandes.isEmpty() || employes.isEmpty()) {
                comparaison.put("success", false);
                comparaison.put("message", "Données insuffisantes pour la comparaison");
                return comparaison;
            }

            // 2. Nettoyer les planifications existantes pour éviter les conflits
            nettoyerPlanificationsExistantes();

            // 3. Tester l'algorithme de Programmation Dynamique
            Map<String, Object> resultatDP = testerAlgorithmeDP(jour, mois, annee);

            // 4. Nettoyer à nouveau pour le test suivant
            nettoyerPlanificationsExistantes();

            // 5. Tester l'algorithme Glouton existant
            Map<String, Object> resultatGlouton = testerAlgorithmeGlouton();

            // 6. Calculer les métriques de comparaison
            Map<String, Object> metriques = calculerMetriquesComparaison(resultatDP, resultatGlouton, commandes);

            // 7. Créer le rapport final
            long tempsTotal = System.currentTimeMillis() - tempsDebutTotal;

            comparaison.put("success", true);
            comparaison.put("tempsComparaisonTotalMs", tempsTotal);
            comparaison.put("timestamp", LocalDateTime.now());

            // Résultats des algorithmes
            comparaison.put("programmationDynamique", resultatDP);
            comparaison.put("algorithmeGlouton", resultatGlouton);

            // Métriques de comparaison
            comparaison.put("metriques", metriques);

            // Recommandation
            Map<String, Object> recommandation = genererRecommandation(resultatDP, resultatGlouton, donneesEntree);
            comparaison.put("recommandation", recommandation);

            System.out.println("✅ Comparaison terminée en " + tempsTotal + "ms");

            return comparaison;

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la comparaison: " + e.getMessage());
            e.printStackTrace();

            comparaison.put("success", false);
            comparaison.put("message", "Erreur lors de la comparaison: " + e.getMessage());
            comparaison.put("tempsComparaisonTotalMs", System.currentTimeMillis() - tempsDebutTotal);

            return comparaison;
        }
    }

    /**
     * Analyse les données d'entrée pour la comparaison
     */
    private Map<String, Object> analyserDonneesEntree(List<Map<String, Object>> commandes,
                                                      List<Map<String, Object>> employes) {
        Map<String, Object> analyse = new HashMap<>();

        analyse.put("nombreCommandes", commandes.size());
        analyse.put("nombreEmployes", employes.size());

        if (!commandes.isEmpty()) {
            // Statistiques des commandes
            double dureeMoyenne = commandes.stream()
                    .mapToInt(cmd -> (Integer) cmd.getOrDefault("tempsEstimeMinutes", 120))
                    .average()
                    .orElse(0.0);

            int dureeMax = commandes.stream()
                    .mapToInt(cmd -> (Integer) cmd.getOrDefault("tempsEstimeMinutes", 120))
                    .max()
                    .orElse(0);

            double prixMoyen = commandes.stream()
                    .mapToDouble(cmd -> cmd.get("prixTotal") != null ?
                            ((Number) cmd.get("prixTotal")).doubleValue() : 0.0)
                    .average()
                    .orElse(0.0);

            analyse.put("dureeeMoyenneMinutes", Math.round(dureeMoyenne));
            analyse.put("dureeMaximaleMinutes", dureeMax);
            analyse.put("prixMoyenEuros", Math.round(prixMoyen * 100) / 100.0);

            // Complexité estimée
            long complexiteDP = (long) commandes.size() * 30 * 8 * employes.size(); // n × jours × heures × employés
            analyse.put("complexiteEstimeeDP", complexiteDP);

            String niveauComplexite;
            if (complexiteDP < 1_000_000) {
                niveauComplexite = "FAIBLE";
            } else if (complexiteDP < 10_000_000) {
                niveauComplexite = "MOYENNE";
            } else {
                niveauComplexite = "ELEVEE";
            }
            analyse.put("niveauComplexite", niveauComplexite);
        }

        return analyse;
    }

    /**
     * Test de l'algorithme de Programmation Dynamique
     */
    private Map<String, Object> testerAlgorithmeDP(int jour, int mois, int annee) {
        try {
            System.out.println("🎯 Test algorithme DP...");
            long tempsDebut = System.currentTimeMillis();

            Map<String, Object> resultat = dpService.executerPlanificationDP(jour, mois, annee);

            long tempsFinal = System.currentTimeMillis() - tempsDebut;

            Map<String, Object> testDP = new HashMap<>();
            testDP.put("success", resultat.get("success"));
            testDP.put("tempsExecutionMs", tempsFinal);
            testDP.put("methodeUtilisee", resultat.get("methodeUtilisee"));

            if ((Boolean) resultat.get("success")) {
                testDP.put("scoreTotal", resultat.get("scoreTotal"));
                testDP.put("nombrePlanifications", resultat.get("nombrePlanifications"));

                System.out.println("✅ DP: " + resultat.get("nombrePlanifications") + " planifications en " + tempsFinal + "ms");
            } else {
                testDP.put("erreur", resultat.get("message"));
                System.out.println("❌ DP: " + resultat.get("message"));
            }

            return testDP;

        } catch (Exception e) {
            System.err.println("❌ Erreur test DP: " + e.getMessage());

            Map<String, Object> erreur = new HashMap<>();
            erreur.put("success", false);
            erreur.put("erreur", "Exception: " + e.getMessage());
            erreur.put("tempsExecutionMs", 0);

            return erreur;
        }
    }

    /**
     * Test de l'algorithme Glouton existant
     */
    private Map<String, Object> testerAlgorithmeGlouton() {
        try {
            System.out.println("🎲 Test algorithme Glouton...");
            long tempsDebut = System.currentTimeMillis();

            // Utiliser votre service de planification existant
            try {
                Map<String, Object> resultatPlanification = planificationService.executerPlanificationAutomatique();

                // ✅ CORRIGÉ : Utiliser 'resultatPlanification' au lieu de 'resultat'
                if ((Boolean) resultatPlanification.get("success")) {
                    System.out.println("✅ Planification automatique réussie");
                    System.out.println("Commandes planifiées: " + resultatPlanification.get("nombreCommandesPlanifiees"));
                    System.out.println("Score: " + resultatPlanification.get("scoreOptimalite"));

                    // Retourner le résultat de la planification
                    return resultatPlanification;

                } else {
                    System.out.println("❌ Échec planification automatique");
                    System.out.println("Erreur: " + resultatPlanification.get("erreur"));

                    // Retourner le résultat d'erreur
                    return resultatPlanification;
                }

            } catch (Exception e) {
                System.err.println("❌ Erreur dans AlgorithmComparisonService: " + e.getMessage());

                Map<String, Object> erreur = new HashMap<>();
                erreur.put("success", false);
                erreur.put("erreur", "Erreur service: " + e.getMessage());
                return erreur;
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur test Glouton: " + e.getMessage());

            Map<String, Object> erreur = new HashMap<>();
            erreur.put("success", false);
            erreur.put("erreur", "Exception: " + e.getMessage());
            erreur.put("tempsExecutionMs", 0);

            return erreur;
        }
    }

    /**
     * Calcule les métriques de comparaison entre les deux algorithmes
     */
    private Map<String, Object> calculerMetriquesComparaison(Map<String, Object> resultatDP,
                                                             Map<String, Object> resultatGlouton,
                                                             List<Map<String, Object>> commandes) {
        Map<String, Object> metriques = new HashMap<>();

        // Métriques de performance temporelle
        Map<String, Object> performance = new HashMap<>();
        long tempsDP = (Long) resultatDP.getOrDefault("tempsExecutionMs", 0L);
        long tempsGlouton = (Long) resultatGlouton.getOrDefault("tempsExecutionMs", 0L);

        performance.put("tempsDPMs", tempsDP);
        performance.put("tempsGloutonMs", tempsGlouton);

        if (tempsGlouton > 0) {
            double ratioTemps = (double) tempsDP / tempsGlouton;
            performance.put("ratioTempsDP_Glouton", Math.round(ratioTemps * 100) / 100.0);
        }

        metriques.put("performance", performance);

        // Métriques de qualité
        Map<String, Object> qualite = new HashMap<>();
        int planifDP = (Integer) resultatDP.getOrDefault("nombrePlanifications", 0);
        int planifGlouton = (Integer) resultatGlouton.getOrDefault("nombrePlanifications", 0);

        qualite.put("planificationsDP", planifDP);
        qualite.put("planificationsGlouton", planifGlouton);

        if (commandes.size() > 0) {
            double tauxReussiteDP = (double) planifDP / commandes.size() * 100;
            double tauxReussiteGlouton = (double) planifGlouton / commandes.size() * 100;

            qualite.put("tauxReussiteDP", Math.round(tauxReussiteDP * 100) / 100.0);
            qualite.put("tauxReussiteGlouton", Math.round(tauxReussiteGlouton * 100) / 100.0);
        }

        metriques.put("qualite", qualite);

        return metriques;
    }

    /**
     * Génère une recommandation basée sur les résultats
     */
    private Map<String, Object> genererRecommandation(Map<String, Object> resultatDP,
                                                      Map<String, Object> resultatGlouton,
                                                      Map<String, Object> donneesEntree) {
        Map<String, Object> recommandation = new HashMap<>();

        boolean successDP = (Boolean) resultatDP.getOrDefault("success", false);
        boolean successGlouton = (Boolean) resultatGlouton.getOrDefault("success", false);

        long tempsDP = (Long) resultatDP.getOrDefault("tempsExecutionMs", 0L);
        long tempsGlouton = (Long) resultatGlouton.getOrDefault("tempsExecutionMs", 0L);

        int planifDP = (Integer) resultatDP.getOrDefault("nombrePlanifications", 0);
        int planifGlouton = (Integer) resultatGlouton.getOrDefault("nombrePlanifications", 0);

        String algorithmeLePluseRapide = tempsDP <= tempsGlouton ? "DP" : "Glouton";
        String algorithmeLesPlusEfficace = planifDP >= planifGlouton ? "DP" : "Glouton";

        recommandation.put("algorithmeLePlusRapide", algorithmeLePluseRapide);
        recommandation.put("algorithmeLesPlusEfficace", algorithmeLesPlusEfficace);

        // Recommandation contextuelle
        String niveauComplexite = (String) donneesEntree.getOrDefault("niveauComplexite", "MOYENNE");
        String conseil;

        if (!successDP && !successGlouton) {
            conseil = "Aucun algorithme n'a réussi. Vérifiez les données d'entrée.";
        } else if (successDP && successGlouton) {
            if ("FAIBLE".equals(niveauComplexite)) {
                conseil = "Les deux algorithmes fonctionnent bien. DP recommandé pour la qualité optimale.";
            } else if ("ELEVEE".equals(niveauComplexite)) {
                conseil = "Complexité élevée détectée. Algorithme glouton recommandé pour la rapidité.";
            } else {
                conseil = planifDP > planifGlouton ?
                        "DP recommandé : meilleure qualité de planification." :
                        "Glouton recommandé : performance équivalente avec rapidité supérieure.";
            }
        } else if (successDP) {
            conseil = "Seul l'algorithme DP a réussi. Utiliser DP.";
        } else {
            conseil = "Seul l'algorithme Glouton a réussi. Utiliser Glouton.";
        }

        recommandation.put("conseil", conseil);
        recommandation.put("niveauComplexite", niveauComplexite);

        return recommandation;
    }

    /**
     * Nettoie les planifications existantes pour éviter les conflits
     */
    private void nettoyerPlanificationsExistantes() {
        try {
            // Cette méthode peut être personnalisée selon vos besoins
            // Pour l'instant, on ne fait rien pour préserver les données existantes
            System.out.println("🧹 Nettoyage des planifications test (simulation)");
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lors du nettoyage: " + e.getMessage());
        }
    }

    /**
     * Méthode utilitaire pour les tests
     */
    public Map<String, Object> obtenirStatistiquesRapides() {
        Map<String, Object> stats = new HashMap<>();

        try {
            List<Map<String, Object>> commandes = commandeService.getCommandesAPlanifierDepuisDate(1, 1, 2025);
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();

            stats.put("commandesDisponibles", commandes.size());
            stats.put("employesActifs", employes.size());
            stats.put("dateDerniereMiseAJour", LocalDateTime.now());
            stats.put("statusService", "ACTIF");

        } catch (Exception e) {
            stats.put("erreur", e.getMessage());
            stats.put("statusService", "ERREUR");
        }

        return stats;
    }
}