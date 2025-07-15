package com.pcagrade.order.test;

import com.pcagrade.order.service.DynamicProgrammingPlanificationService;
import com.pcagrade.order.service.CommandeService;
import com.pcagrade.order.service.EmployeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Classe de test pour l'algorithme de planification dynamique
 * Utilise les vraies données de votre base de données
 */
@Component
public class TestDynamicProgramming implements CommandLineRunner {

    @Autowired
    private DynamicProgrammingPlanificationService dpService;

    @Autowired
    private CommandeService commandeService;

    @Autowired
    private EmployeService employeService;

    @Override
    public void run(String... args) throws Exception {
        // Ne lancer le test que si le paramètre --test-dp est passé
        if (args.length > 0 && "--test-dp".equals(args[0])) {
            System.out.println("🧪 === LANCEMENT DES TESTS PROGRAMMATION DYNAMIQUE ===");
            executerTousLesTests();
        }
    }

    /**
     * Lance tous les tests de l'algorithme DP
     */
    public void executerTousLesTests() {
        try {
            System.out.println("\n📊 === DIAGNOSTIC DONNÉES ===");
            diagnostiquerDonnees();

            System.out.println("\n🎯 === TEST PLANIFICATION DP ===");
            testerPlanificationDP();

            System.out.println("\n📈 === TEST PERFORMANCE ===");
            testerPerformance();

            System.out.println("\n✅ === TESTS TERMINÉS ===");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors des tests: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Diagnostique les données disponibles
     */
    private void diagnostiquerDonnees() {
        try {
            // Vérifier les commandes disponibles
            LocalDate aujourdHui = LocalDate.now();
            List<Map<String, Object>> commandes = commandeService.getCommandesAPlanifierDepuisDate(
                    aujourdHui.getDayOfMonth(),
                    aujourdHui.getMonthValue(),
                    aujourdHui.getYear()
            );

            System.out.println("📦 Commandes disponibles depuis aujourd'hui: " + commandes.size());

            if (!commandes.isEmpty()) {
                // Afficher quelques exemples
                System.out.println("\n📋 Échantillon des commandes:");
                for (int i = 0; i < Math.min(5, commandes.size()); i++) {
                    Map<String, Object> cmd = commandes.get(i);
                    System.out.printf("  • %s - %d min - Priorité: %s - Prix: %.2f€%n",
                            cmd.get("numeroCommande"),
                            cmd.get("tempsEstimeMinutes"),
                            cmd.get("priorite"),
                            cmd.get("prixTotal") != null ? (Double) cmd.get("prixTotal") : 0.0
                    );
                }

                // Statistiques des commandes
                analyserCommandes(commandes);
            } else {
                System.out.println("⚠️ Aucune commande trouvée depuis aujourd'hui");

                // Essayer avec une date plus ancienne
                System.out.println("🔍 Recherche avec une date plus ancienne...");
                List<Map<String, Object>> commandesAnciennes = commandeService.getCommandesAPlanifierDepuisDate(1, 1, 2024);
                System.out.println("📦 Commandes depuis 01/01/2024: " + commandesAnciennes.size());

                if (!commandesAnciennes.isEmpty()) {
                    analyserCommandes(commandesAnciennes.subList(0, Math.min(10, commandesAnciennes.size())));
                }
            }

            // Vérifier les employés
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();
            System.out.println("\n👥 Employés actifs: " + employes.size());

            if (!employes.isEmpty()) {
                System.out.println("📋 Liste des employés:");
                for (Map<String, Object> emp : employes) {
                    System.out.printf("  • %s %s (ID: %s)%n",
                            emp.get("prenom"),
                            emp.get("nom"),
                            emp.get("id")
                    );
                }
            } else {
                System.out.println("⚠️ Aucun employé actif trouvé");
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur diagnostic données: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Analyse des caractéristiques des commandes
     */
    private void analyserCommandes(List<Map<String, Object>> commandes) {
        if (commandes.isEmpty()) return;

        System.out.println("\n📊 Analyse des commandes:");

        // Durées
        double dureeMoyenne = commandes.stream()
                .mapToInt(cmd -> (Integer) cmd.getOrDefault("tempsEstimeMinutes", 120))
                .average()
                .orElse(0.0);

        int dureeMin = commandes.stream()
                .mapToInt(cmd -> (Integer) cmd.getOrDefault("tempsEstimeMinutes", 120))
                .min()
                .orElse(0);

        int dureeMax = commandes.stream()
                .mapToInt(cmd -> (Integer) cmd.getOrDefault("tempsEstimeMinutes", 120))
                .max()
                .orElse(0);

        System.out.printf("  • Durée moyenne: %.1f min%n", dureeMoyenne);
        System.out.printf("  • Durée min/max: %d/%d min%n", dureeMin, dureeMax);

        // Prix
        double prixMoyen = commandes.stream()
                .mapToDouble(cmd -> cmd.get("prixTotal") != null ? (Double) cmd.get("prixTotal") : 0.0)
                .average()
                .orElse(0.0);

        System.out.printf("  • Prix moyen: %.2f€%n", prixMoyen);

        // Répartition par priorité
        Map<String, Long> repartitionPriorite = commandes.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        cmd -> (String) cmd.getOrDefault("priorite", "NORMALE"),
                        java.util.stream.Collectors.counting()
                ));

        System.out.println("  • Répartition priorités:");
        repartitionPriorite.forEach((priorite, count) ->
                System.out.printf("    - %s: %d commandes%n", priorite, count)
        );
    }

    /**
     * Test de l'algorithme de planification DP
     */
    private void testerPlanificationDP() {
        try {
            LocalDate dateTest = LocalDate.now();
            System.out.printf("🎯 Test planification DP pour le %02d/%02d/%d%n",
                    dateTest.getDayOfMonth(), dateTest.getMonthValue(), dateTest.getYear());

            long tempsDebut = System.currentTimeMillis();

            Map<String, Object> resultat = dpService.executerPlanificationDP(
                    dateTest.getDayOfMonth(),
                    dateTest.getMonthValue(),
                    dateTest.getYear()
            );

            long tempsFinal = System.currentTimeMillis() - tempsDebut;

            System.out.println("\n📋 Résultats du test:");
            System.out.println("  • Succès: " + resultat.get("success"));
            System.out.println("  • Méthode utilisée: " + resultat.get("methode"));
            System.out.println("  • Temps total: " + tempsFinal + "ms");

            if ((Boolean) resultat.get("success")) {
                System.out.println("  • Temps calcul: " + resultat.get("tempsCalculMs") + "ms");
                System.out.println("  • Score total: " + String.format("%.2f", (Double) resultat.get("scoreTotal")));
                System.out.println("  • Planifications créées: " + resultat.get("nombrePlanifications"));

                // Afficher les statistiques
                @SuppressWarnings("unchecked")
                Map<String, Object> stats = (Map<String, Object>) resultat.get("stats");
                if (stats != null) {
                    System.out.println("\n📊 Statistiques détaillées:");
                    System.out.println("  • Total commandes: " + stats.get("totalCommandes"));
                    System.out.println("  • Commandes planifiées: " + stats.get("commandesPlanifiees"));
                    System.out.println("  • Taux de planification: " + String.format("%.1f%%", (Double) stats.get("tauxPlanification")));
                    System.out.println("  • Durée moyenne: " + stats.get("dureeeMoyenneMinutes") + " min");
                }

                // Afficher quelques planifications
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> planifications = (List<Map<String, Object>>) resultat.get("planifications");
                if (planifications != null && !planifications.isEmpty()) {
                    System.out.println("\n📅 Exemple de planifications créées:");
                    for (int i = 0; i < Math.min(3, planifications.size()); i++) {
                        Map<String, Object> p = planifications.get(i);
                        System.out.printf("  • %s → %s (%s - %s)%n",
                                p.get("numeroCommande"),
                                p.get("employeNom"),
                                p.get("dateDebut"),
                                p.get("dureeMinutes") + " min"
                        );
                    }
                }
            } else {
                System.out.println("❌ Échec: " + resultat.get("message"));
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur test planification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test de performance avec différentes tailles de données
     */
    private void testerPerformance() {
        try {
            System.out.println("📈 Tests de performance...");

            // Test avec les données réelles
            LocalDate[] datesTest = {
                    LocalDate.now(),
                    LocalDate.now().minusDays(7),
                    LocalDate.now().minusDays(30)
            };

            for (LocalDate date : datesTest) {
                System.out.printf("\n🔍 Test performance pour %02d/%02d/%d:%n",
                        date.getDayOfMonth(), date.getMonthValue(), date.getYear());

                try {
                    // Compter les commandes disponibles
                    List<Map<String, Object>> commandes = commandeService.getCommandesAPlanifierDepuisDate(
                            date.getDayOfMonth(),
                            date.getMonthValue(),
                            date.getYear()
                    );

                    System.out.printf("  • Commandes disponibles: %d%n", commandes.size());

                    if (commandes.size() > 0) {
                        long tempsDebut = System.currentTimeMillis();

                        Map<String, Object> resultat = dpService.executerPlanificationDP(
                                date.getDayOfMonth(),
                                date.getMonthValue(),
                                date.getYear()
                        );

                        long tempsFinal = System.currentTimeMillis() - tempsDebut;

                        if ((Boolean) resultat.get("success")) {
                            System.out.printf("  • Temps total: %dms%n", tempsFinal);
                            System.out.printf("  • Méthode: %s%n", resultat.get("methode"));
                            System.out.printf("  • Planifications: %d%n", resultat.get("nombrePlanifications"));

                            // Calcul du débit
                            double commandesParSeconde = commandes.size() / (tempsFinal / 1000.0);
                            System.out.printf("  • Débit: %.1f commandes/seconde%n", commandesParSeconde);
                        } else {
                            System.out.println("  • ❌ Échec: " + resultat.get("message"));
                        }
                    } else {
                        System.out.println("  • Aucune commande disponible pour cette date");
                    }

                } catch (Exception e) {
                    System.out.printf("  • ❌ Erreur: %s%n", e.getMessage());
                }
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur test performance: " + e.getMessage());
        }
    }

    /**
     * Méthode utilitaire pour lancer les tests manuellement
     */
    public static void lancerTestsDirectement() {
        System.out.println("🧪 Lancement direct des tests DP...");
        System.out.println("Note: Cette méthode nécessite un contexte Spring actif");
        System.out.println("Utilisez plutôt: mvn spring-boot:run -Dspring-boot.run.arguments=--test-dp");
    }
}