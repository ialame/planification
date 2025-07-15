package com.pcagrade.order.service;

import com.pcagrade.order.util.DureeCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service d'initialisation automatique de la planification
 * Se déclenche au lancement de l'application pour planifier
 * toutes les commandes du dernier mois
 */
@Service
@Order(100) // S'exécute après l'initialisation des autres services
public class PlanificationInitService implements ApplicationRunner {
    private static final boolean PLANIFICATION_AUTO_ENABLED = false; // ✅ DÉSACTIVER ICI
    @Autowired
    private DynamicProgrammingPlanificationService dpService;

    @Autowired
    private CommandeService commandeService;

    @Autowired
    private EmployeService employeService;

    /**
     * Configuration du service
     */
    //private static final boolean PLANIFICATION_AUTO_ENABLED = true;
    private static final int JOURS_HISTORIQUE = 30; // Dernier mois
    private static final boolean FORCE_REPLANIFICATION = false; // Ne replanifie que les non-planifiées

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!PLANIFICATION_AUTO_ENABLED) {
            System.out.println("⏸️ Planification automatique désactivée");
            return;
        }

        // Vérifier si le paramètre --skip-auto-planning est présent
        if (args.containsOption("skip-auto-planning")) {
            System.out.println("⏸️ Planification automatique ignorée (--skip-auto-planning)");
            return;
        }

        System.out.println("\n🚀 === INITIALISATION PLANIFICATION AUTOMATIQUE ===");

        try {
            executerPlanificationInitiale();
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la planification initiale: " + e.getMessage());
            e.printStackTrace();
            // Ne pas faire planter l'application, continuer le démarrage
        }
    }

    /**
     * Exécute la planification initiale pour le dernier mois
     */
    @Transactional
    public void executerPlanificationInitiale() {
        long tempsDebut = System.currentTimeMillis();

        // Calculer la période de planification
        LocalDate aujourdHui = LocalDate.now();
        LocalDate dateDebut = aujourdHui.minusDays(JOURS_HISTORIQUE);

        System.out.println("📅 Période de planification: " +
                dateDebut.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                " → " + aujourdHui.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        // Analyser les données disponibles
        analyserDonneesDisponibles();

        // Planifier les commandes
        Map<String, Object> resultat = planifierCommandesPeriode(dateDebut, aujourdHui);

        // Afficher le résumé
        afficherResumePlanification(resultat, tempsDebut);
    }

    /**
     * Analyse les données disponibles avant planification
     */
    private void analyserDonneesDisponibles() {
        try {
            System.out.println("\n📊 === ANALYSE DES DONNÉES ===");

            // Compter les commandes totales - Correction type
            List<com.pcagrade.order.entity.Commande> toutesCommandesEntites = commandeService.getToutesCommandes();
            System.out.println("📦 Total commandes en base: " + toutesCommandesEntites.size());

            // Compter les employés actifs
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();
            System.out.println("👥 Employés actifs: " + employes.size());

            // Analyser les commandes récentes
            LocalDate dateDebut = LocalDate.now().minusDays(JOURS_HISTORIQUE);
            List<Map<String, Object>> commandesRecentes = commandeService.getCommandesAPlanifierDepuisDate(
                    dateDebut.getDayOfMonth(),
                    dateDebut.getMonthValue(),
                    dateDebut.getYear()
            );
            System.out.println("📦 Commandes récentes (" + JOURS_HISTORIQUE + " jours): " + commandesRecentes.size());

            // Analyser les durées et cartes
            analyserQualiteDonnees(commandesRecentes);

        } catch (Exception e) {
            System.err.println("❌ Erreur analyse données: " + e.getMessage());
        }
    }

    /**
     * Analyse la qualité des données (cartes, durées)
     */
    private void analyserQualiteDonnees(List<Map<String, Object>> commandes) {
        int commandesAvecCartes = 0;
        int commandesAvecDuree = 0;
        int commandesIncompletes = 0;
        double dureeeTotale = 0;
        int cartesTotales = 0;

        for (Map<String, Object> commande : commandes) {
            Integer nombreCartes = (Integer) commande.get("nombreCartesReelles");
            if (nombreCartes == null) {
                nombreCartes = (Integer) commande.get("nombreCartes");
            }

            Integer dureeMinutes = (Integer) commande.get("dureeMinutes");

            if (nombreCartes != null && nombreCartes > 0) {
                commandesAvecCartes++;
                cartesTotales += nombreCartes;
            }

            if (dureeMinutes != null && dureeMinutes > 0) {
                commandesAvecDuree++;
                dureeeTotale += dureeMinutes;
            }

            if ((nombreCartes == null || nombreCartes <= 0) &&
                    (dureeMinutes == null || dureeMinutes <= 0)) {
                commandesIncompletes++;
            }
        }

        System.out.println("📊 Qualité des données:");
        System.out.println("   - Commandes avec cartes: " + commandesAvecCartes + "/" + commandes.size());
        System.out.println("   - Commandes avec durée: " + commandesAvecDuree + "/" + commandes.size());
        System.out.println("   - Commandes incomplètes: " + commandesIncompletes + "/" + commandes.size());

        if (commandesAvecCartes > 0) {
            System.out.println("🃏 Total cartes: " + cartesTotales +
                    " (moyenne: " + String.format("%.1f", (double)cartesTotales / commandesAvecCartes) + " cartes/commande)");
        }

        if (commandesAvecDuree > 0) {
            System.out.println("⏱️ Durée totale: " + DureeCalculator.formaterDuree((int)dureeeTotale) +
                    " (moyenne: " + DureeCalculator.formaterDuree((int)(dureeeTotale / commandesAvecDuree)) + "/commande)");
        }
    }

    /**
     * Planifie les commandes pour une période donnée
     */
    private Map<String, Object> planifierCommandesPeriode(LocalDate dateDebut, LocalDate dateFin) {
        System.out.println("\n🎯 === EXÉCUTION PLANIFICATION ===");

        try {
            // Utiliser le service DP existant
            Map<String, Object> resultat = dpService.executerPlanificationDP(
                    dateDebut.getDayOfMonth(),
                    dateDebut.getMonthValue(),
                    dateDebut.getYear()
            );

            return resultat;

        } catch (Exception e) {
            System.err.println("❌ Erreur planification période: " + e.getMessage());

            // Retourner un résultat d'erreur
            Map<String, Object> erreur = new HashMap<>();
            erreur.put("success", false);
            erreur.put("message", "Erreur planification: " + e.getMessage());
            erreur.put("nombreCommandesPlanifiees", 0);
            return erreur;
        }
    }

    /**
     * Affiche le résumé de la planification initiale
     */
    private void afficherResumePlanification(Map<String, Object> resultat, long tempsDebut) {
        long tempsFin = System.currentTimeMillis();
        long dureeMs = tempsFin - tempsDebut;

        System.out.println("\n📋 === RÉSUMÉ PLANIFICATION INITIALE ===");

        Boolean success = (Boolean) resultat.get("success");
        if (Boolean.TRUE.equals(success)) {
            System.out.println("✅ Planification initiale réussie");

            Integer commandesPlanifiees = (Integer) resultat.get("nombreCommandesPlanifiees");
            if (commandesPlanifiees != null) {
                System.out.println("📦 Commandes planifiées: " + commandesPlanifiees);
            }

            String algorithme = (String) resultat.get("algorithmeUtilise");
            if (algorithme != null) {
                System.out.println("🧮 Algorithme utilisé: " + algorithme);
            }

        } else {
            System.out.println("❌ Planification initiale échouée");
            String message = (String) resultat.get("message");
            if (message != null) {
                System.out.println("   Raison: " + message);
            }
        }

        System.out.println("⏱️ Durée d'exécution: " + dureeMs + "ms");
        System.out.println("🏁 Application prête avec planification à jour");
    }

    /**
     * Force une re-planification complète (méthode publique pour usage manuel)
     */
    @Transactional
    public Map<String, Object> forcerReplanificationComplete() {
        System.out.println("\n🔄 === FORCE RE-PLANIFICATION COMPLÈTE ===");

        LocalDate aujourdHui = LocalDate.now();
        LocalDate dateDebut = aujourdHui.minusDays(JOURS_HISTORIQUE);

        return planifierCommandesPeriode(dateDebut, aujourdHui);
    }

    /**
     * Planifie uniquement les nouvelles commandes depuis la dernière planification
     */
    @Transactional
    public Map<String, Object> planifierNouvellesCommandes() {
        System.out.println("\n➕ === PLANIFICATION NOUVELLES COMMANDES ===");

        // TODO: Implémenter la logique pour identifier les nouvelles commandes
        // Pour l'instant, utilise la même logique que la planification complète

        LocalDate aujourdHui = LocalDate.now();
        return planifierCommandesPeriode(aujourdHui, aujourdHui);
    }
}