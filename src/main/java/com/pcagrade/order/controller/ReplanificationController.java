package com.pcagrade.order.controller;

import com.pcagrade.order.service.DynamicProgrammingPlanificationService;
import com.pcagrade.order.service.PlanificationInitService;
import com.pcagrade.order.service.CommandeService;
import com.pcagrade.order.service.EmployeService;
import com.pcagrade.order.util.DureeCalculator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur unifié pour la re-planification
 * Fournit les APIs nécessaires au frontend pour déclencher
 * différents types de planification
 */
@RestController
@RequestMapping("/api/planification")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"}, allowCredentials = "false")
public class ReplanificationController {

    @Autowired
    private DynamicProgrammingPlanificationService dpService;

    @Autowired
    private PlanificationInitService initService;

    @Autowired
    private CommandeService commandeService;

    @Autowired
    private EmployeService employeService;

    /**
     * Endpoint principal : Re-planification avec options flexibles
     *
     * @param periode Période à planifier ("dernier-mois", "semaine", "aujourd-hui", "custom")
     * @param algorithme Algorithme à utiliser ("auto", "dp", "glouton")
     * @param forcer Forcer la re-planification même si déjà planifié
     * @param dateDebut Date de début pour période custom (format: yyyy-MM-dd)
     * @param dateFin Date de fin pour période custom (format: yyyy-MM-dd)
     * @return Résultat de la planification
     */
    @PostMapping("/relancer")
    public ResponseEntity<Map<String, Object>> relancerPlanification(
            @RequestParam(defaultValue = "dernier-mois") String periode,
            @RequestParam(defaultValue = "auto") String algorithme,
            @RequestParam(defaultValue = "false") boolean forcer,
            @RequestParam(required = false) String dateDebut,
            @RequestParam(required = false) String dateFin) {

        long tempsDebut = System.currentTimeMillis();

        try {
            System.out.println("🎯 API: Demande re-planification - période: " + periode +
                    ", algorithme: " + algorithme + ", forcé: " + forcer);

            // Calculer les dates selon la période
            PeriodePlanification periodePlan = calculerPeriode(periode, dateDebut, dateFin);

            // Analyser les données avant planification
            Map<String, Object> analyse = analyserDonneesAvantPlanification(periodePlan);

            // Exécuter la planification
            Map<String, Object> resultat = executerPlanificationAvecOptions(
                    periodePlan, algorithme, forcer);

            // Enrichir le résultat avec les métadonnées
            enrichirResultat(resultat, analyse, periodePlan, algorithme, tempsDebut);

            if ((Boolean) resultat.get("success")) {
                System.out.println("✅ API: Re-planification réussie");
                return ResponseEntity.ok(resultat);
            } else {
                System.out.println("❌ API: Re-planification échouée");
                return ResponseEntity.status(500).body(resultat);
            }

        } catch (Exception e) {
            System.err.println("❌ API: Erreur re-planification: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> erreur = new HashMap<>();
            erreur.put("success", false);
            erreur.put("message", "Erreur lors de la re-planification: " + e.getMessage());
            erreur.put("timestamp", System.currentTimeMillis());
            erreur.put("dureeMs", System.currentTimeMillis() - tempsDebut);

            return ResponseEntity.status(500).body(erreur);
        }
    }

    /**
     * Re-planification rapide du dernier mois (bouton principal du frontend)
     */
    @PostMapping("/dernier-mois")
    public ResponseEntity<Map<String, Object>> replanifierDernierMois() {
        return relancerPlanification("dernier-mois", "auto", false, null, null);
    }

    /**
     * Re-planification forcée complète (pour maintenance)
     */
    @PostMapping("/force-complete")
    public ResponseEntity<Map<String, Object>> forcerReplanificationComplete() {
        try {
            System.out.println("🔄 API: Force re-planification complète");
            Map<String, Object> resultat = initService.forcerReplanificationComplete();
            return ResponseEntity.ok(resultat);
        } catch (Exception e) {
            System.err.println("❌ API: Erreur force re-planification: " + e.getMessage());
            Map<String, Object> erreur = new HashMap<>();
            erreur.put("success", false);
            erreur.put("message", e.getMessage());
            return ResponseEntity.status(500).body(erreur);
        }
    }

    /**
     * Planification des nouvelles commandes uniquement
     */
    @PostMapping("/nouvelles-commandes")
    public ResponseEntity<Map<String, Object>> planifierNouvellesCommandes() {
        try {
            System.out.println("➕ API: Planification nouvelles commandes");
            Map<String, Object> resultat = initService.planifierNouvellesCommandes();
            return ResponseEntity.ok(resultat);
        } catch (Exception e) {
            System.err.println("❌ API: Erreur planification nouvelles: " + e.getMessage());
            Map<String, Object> erreur = new HashMap<>();
            erreur.put("success", false);
            erreur.put("message", e.getMessage());
            return ResponseEntity.status(500).body(erreur);
        }
    }

    /**
     * Diagnostic des données de planification
     */
    @GetMapping("/diagnostic")
    public ResponseEntity<Map<String, Object>> diagnostic() {
        try {
            Map<String, Object> diagnostic = new HashMap<>();

            // Statistiques générales - Correction pour les types d'entités
            List<com.pcagrade.order.entity.Commande> toutesCommandesEntites = commandeService.getToutesCommandes();
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();

            diagnostic.put("totalCommandes", toutesCommandesEntites.size());
            diagnostic.put("employesActifs", employes.size());

            // Analyser les commandes récentes
            LocalDate dateDebut = LocalDate.now().minusDays(30);
            List<Map<String, Object>> commandesRecentes = commandeService.getCommandesAPlanifierDepuisDate(
                    dateDebut.getDayOfMonth(),
                    dateDebut.getMonthValue(),
                    dateDebut.getYear()
            );

            diagnostic.put("commandesRecentes30j", commandesRecentes.size());

            // Analyser la qualité des données
            Map<String, Object> qualiteDonnees = analyserQualiteDonnees(commandesRecentes);
            diagnostic.put("qualiteDonnees", qualiteDonnees);

            // État de la planification
            diagnostic.put("derniereMiseAJour", System.currentTimeMillis());
            diagnostic.put("systemeInitialise", true);

            return ResponseEntity.ok(diagnostic);

        } catch (Exception e) {
            System.err.println("❌ API: Erreur diagnostic: " + e.getMessage());
            Map<String, Object> erreur = new HashMap<>();
            erreur.put("success", false);
            erreur.put("message", e.getMessage());
            return ResponseEntity.status(500).body(erreur);
        }
    }

    /**
     * Endpoint de test pour vérifier le bon fonctionnement
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "ReplanificationController");
        status.put("status", "OK");
        status.put("timestamp", System.currentTimeMillis());
        status.put("version", "1.0.0");
        status.put("endpoints", Map.of(
                "relancer", "POST /api/planification/relancer",
                "dernierMois", "POST /api/planification/dernier-mois",
                "forceComplete", "POST /api/planification/force-complete",
                "nouvelles", "POST /api/planification/nouvelles-commandes",
                "diagnostic", "GET /api/planification/diagnostic"
        ));
        return ResponseEntity.ok(status);
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Classe pour représenter une période de planification
     */
    private static class PeriodePlanification {
        final LocalDate dateDebut;
        final LocalDate dateFin;
        final String description;

        PeriodePlanification(LocalDate dateDebut, LocalDate dateFin, String description) {
            this.dateDebut = dateDebut;
            this.dateFin = dateFin;
            this.description = description;
        }
    }

    /**
     * Calcule les dates selon la période demandée
     */
    private PeriodePlanification calculerPeriode(String periode, String dateDebutStr, String dateFinStr) {
        LocalDate aujourdHui = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        switch (periode.toLowerCase()) {
            case "dernier-mois":
                return new PeriodePlanification(
                        aujourdHui.minusDays(30),
                        aujourdHui,
                        "Dernier mois (30 jours)"
                );

            case "semaine":
                return new PeriodePlanification(
                        aujourdHui.minusDays(7),
                        aujourdHui,
                        "Dernière semaine (7 jours)"
                );

            case "aujourd-hui":
                return new PeriodePlanification(
                        aujourdHui,
                        aujourdHui,
                        "Aujourd'hui"
                );

            case "custom":
                if (dateDebutStr != null && dateFinStr != null) {
                    LocalDate debut = LocalDate.parse(dateDebutStr, formatter);
                    LocalDate fin = LocalDate.parse(dateFinStr, formatter);
                    return new PeriodePlanification(
                            debut,
                            fin,
                            "Période personnalisée: " + dateDebutStr + " → " + dateFinStr
                    );
                }
                // Fallback si dates custom invalides
                return new PeriodePlanification(
                        aujourdHui.minusDays(30),
                        aujourdHui,
                        "Dernier mois (fallback)"
                );

            default:
                return new PeriodePlanification(
                        aujourdHui.minusDays(30),
                        aujourdHui,
                        "Dernier mois (défaut)"
                );
        }
    }

    /**
     * Analyse les données avant planification
     */
    private Map<String, Object> analyserDonneesAvantPlanification(PeriodePlanification periode) {
        Map<String, Object> analyse = new HashMap<>();

        try {
            List<Map<String, Object>> commandes = commandeService.getCommandesAPlanifierDepuisDate(
                    periode.dateDebut.getDayOfMonth(),
                    periode.dateDebut.getMonthValue(),
                    periode.dateDebut.getYear()
            );

            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();

            analyse.put("nombreCommandes", commandes.size());
            analyse.put("nombreEmployes", employes.size());
            analyse.put("periode", periode.description);
            analyse.put("qualiteDonnees", analyserQualiteDonnees(commandes));

        } catch (Exception e) {
            analyse.put("erreur", e.getMessage());
        }

        return analyse;
    }

    /**
     * Analyse la qualité des données (cartes, durées)
     */
    private Map<String, Object> analyserQualiteDonnees(List<Map<String, Object>> commandes) {
        Map<String, Object> qualite = new HashMap<>();

        int commandesAvecCartes = 0;
        int commandesAvecDuree = 0;
        double dureeeTotale = 0;
        int cartesTotales = 0;

        for (Map<String, Object> commande : commandes) {
            Integer nombreCartes = (Integer) commande.get("nombreCartesReelles");
            if (nombreCartes == null) {
                nombreCartes = (Integer) commande.get("nombreCartes");
            }

            if (nombreCartes != null && nombreCartes > 0) {
                commandesAvecCartes++;
                cartesTotales += nombreCartes;
            }

            // Calculer la durée avec la nouvelle logique
            int dureeCalculee = DureeCalculator.calculerDureeDepuisCommande(commande);
            if (dureeCalculee > 0) {
                commandesAvecDuree++;
                dureeeTotale += dureeCalculee;
            }
        }

        qualite.put("commandesAvecCartes", commandesAvecCartes);
        qualite.put("commandesAvecDuree", commandesAvecDuree);
        qualite.put("cartesTotales", cartesTotales);
        qualite.put("dureeeTotaleMinutes", (int)dureeeTotale);
        qualite.put("dureeeTotaleFormatee", DureeCalculator.formaterDuree((int)dureeeTotale));

        if (commandesAvecCartes > 0) {
            qualite.put("moyenneCartesParCommande", (double)cartesTotales / commandesAvecCartes);
        }

        if (commandesAvecDuree > 0) {
            qualite.put("moyenneDureeParCommande", dureeeTotale / commandesAvecDuree);
        }

        return qualite;
    }

    /**
     * Exécute la planification avec les options spécifiées
     */
    private Map<String, Object> executerPlanificationAvecOptions(
            PeriodePlanification periode, String algorithme, boolean forcer) {

        // Pour l'instant, utilise le service DP existant
        // TODO: Implémenter la sélection d'algorithme et l'option forcer

        return dpService.executerPlanificationDP(
                periode.dateDebut.getDayOfMonth(),
                periode.dateDebut.getMonthValue(),
                periode.dateDebut.getYear()
        );
    }

    /**
     * Enrichit le résultat avec des métadonnées utiles
     */
    private void enrichirResultat(Map<String, Object> resultat, Map<String, Object> analyse,
                                  PeriodePlanification periode, String algorithme, long tempsDebut) {

        resultat.put("metadonnees", Map.of(
                "periode", periode.description,
                "algorithmeRequis", algorithme,
                "analyse", analyse,
                "dureeExecutionMs", System.currentTimeMillis() - tempsDebut,
                "timestamp", System.currentTimeMillis()
        ));
    }
}