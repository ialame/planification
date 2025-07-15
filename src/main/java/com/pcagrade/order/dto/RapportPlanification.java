package com.pcagrade.order.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * DTO pour représenter le rapport de planification
 * Utilisé par les algorithmes de planification pour retourner les résultats
 */
public class RapportPlanification {

    private boolean success;
    private String message;
    private int nombreCommandesPlanifiees;
    private int nombreCommandesNonPlanifiees;
    private int nombreEmployesUtilises;
    private double tempsExecutionMs;
    private String algorithmeUtilise;
    private LocalDateTime dateExecution;
    private List<String> detailsOperations;
    private List<PlanificationCreee> planificationsCreees;

    // ========== CONSTRUCTEURS ==========

    public RapportPlanification() {
        this.detailsOperations = new ArrayList<>();
        this.planificationsCreees = new ArrayList<>();
        this.dateExecution = LocalDateTime.now();
    }

    public RapportPlanification(boolean success, String message, int nombreCommandesPlanifiees, int nombreCommandesNonPlanifiees) {
        this();
        this.success = success;
        this.message = message;
        this.nombreCommandesPlanifiees = nombreCommandesPlanifiees;
        this.nombreCommandesNonPlanifiees = nombreCommandesNonPlanifiees;
    }

    public RapportPlanification(boolean success, String message, int nombreCommandesPlanifiees,
                                int nombreCommandesNonPlanifiees, String algorithmeUtilise) {
        this(success, message, nombreCommandesPlanifiees, nombreCommandesNonPlanifiees);
        this.algorithmeUtilise = algorithmeUtilise;
    }

    // ========== GETTERS ET SETTERS ==========

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getNombreCommandesPlanifiees() {
        return nombreCommandesPlanifiees;
    }

    public void setNombreCommandesPlanifiees(int nombreCommandesPlanifiees) {
        this.nombreCommandesPlanifiees = nombreCommandesPlanifiees;
    }

    public int getNombreCommandesNonPlanifiees() {
        return nombreCommandesNonPlanifiees;
    }

    public void setNombreCommandesNonPlanifiees(int nombreCommandesNonPlanifiees) {
        this.nombreCommandesNonPlanifiees = nombreCommandesNonPlanifiees;
    }

    public int getNombreEmployesUtilises() {
        return nombreEmployesUtilises;
    }

    public void setNombreEmployesUtilises(int nombreEmployesUtilises) {
        this.nombreEmployesUtilises = nombreEmployesUtilises;
    }

    public double getTempsExecutionMs() {
        return tempsExecutionMs;
    }

    public void setTempsExecutionMs(double tempsExecutionMs) {
        this.tempsExecutionMs = tempsExecutionMs;
    }

    public String getAlgorithmeUtilise() {
        return algorithmeUtilise;
    }

    public void setAlgorithmeUtilise(String algorithmeUtilise) {
        this.algorithmeUtilise = algorithmeUtilise;
    }

    public LocalDateTime getDateExecution() {
        return dateExecution;
    }

    public void setDateExecution(LocalDateTime dateExecution) {
        this.dateExecution = dateExecution;
    }

    public List<String> getDetailsOperations() {
        return detailsOperations;
    }

    public void setDetailsOperations(List<String> detailsOperations) {
        this.detailsOperations = detailsOperations;
    }

    public List<PlanificationCreee> getPlanificationsCreees() {
        return planificationsCreees;
    }

    public void setPlanificationsCreees(List<PlanificationCreee> planificationsCreees) {
        this.planificationsCreees = planificationsCreees;
    }

    // ========== MÉTHODES UTILITAIRES ==========

    /**
     * Ajoute un détail d'opération au rapport
     */
    public void ajouterDetail(String detail) {
        if (this.detailsOperations == null) {
            this.detailsOperations = new ArrayList<>();
        }
        this.detailsOperations.add(detail);
    }

    /**
     * Ajoute une planification créée au rapport
     */
    public void ajouterPlanification(PlanificationCreee planification) {
        if (this.planificationsCreees == null) {
            this.planificationsCreees = new ArrayList<>();
        }
        this.planificationsCreees.add(planification);
    }

    /**
     * Calcule le taux de réussite de la planification
     */
    public double getTauxReussite() {
        int total = nombreCommandesPlanifiees + nombreCommandesNonPlanifiees;
        if (total == 0) return 0.0;
        return (double) nombreCommandesPlanifiees / total * 100.0;
    }

    /**
     * Retourne un résumé textuel du rapport
     */
    public String getResume() {
        return String.format(
                "Planification %s: %d commandes planifiées / %d total (%.1f%%) en %.2fms avec %s",
                success ? "réussie" : "échouée",
                nombreCommandesPlanifiees,
                nombreCommandesPlanifiees + nombreCommandesNonPlanifiees,
                getTauxReussite(),
                tempsExecutionMs,
                algorithmeUtilise != null ? algorithmeUtilise : "algorithme inconnu"
        );
    }

    @Override
    public String toString() {
        return String.format(
                "RapportPlanification{success=%s, message='%s', planifiees=%d, non_planifiees=%d, employes=%d, temps=%.2fms, algorithme='%s'}",
                success, message, nombreCommandesPlanifiees, nombreCommandesNonPlanifiees,
                nombreEmployesUtilises, tempsExecutionMs, algorithmeUtilise
        );
    }

    /**
     * ✅ NOUVELLE MÉTHODE: Convertit le rapport en Map pour compatibilité avec le code existant
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("success", success);
        map.put("message", message);
        map.put("nombreCommandesPlanifiees", nombreCommandesPlanifiees);
        map.put("nombreCommandesNonPlanifiees", nombreCommandesNonPlanifiees);
        map.put("nombreEmployesUtilises", nombreEmployesUtilises);
        map.put("tempsExecutionMs", tempsExecutionMs);
        map.put("algorithmeUtilise", algorithmeUtilise);
        map.put("dateExecution", dateExecution);
        map.put("detailsOperations", detailsOperations);
        map.put("planificationsCreees", planificationsCreees);
        map.put("tauxReussite", getTauxReussite());
        map.put("resume", getResume());

        // Aliases pour compatibilité avec le frontend
        map.put("planifications_creees", nombreCommandesPlanifiees);
        map.put("planifications_sauvees", nombreCommandesPlanifiees);
        map.put("commandes_planifiees", nombreCommandesPlanifiees);
        map.put("timestamp", dateExecution != null ? dateExecution.toString() : LocalDateTime.now().toString());

        return map;
    }

    // ========== MÉTHODES STATIQUES UTILITAIRES ==========

    /**
     * Méthode statique pour créer rapidement un rapport d'échec
     */
    public static RapportPlanification echec(String message) {
        return new RapportPlanification(false, message, 0, 0);
    }

    /**
     * Méthode statique pour créer rapidement un rapport de succès
     */
    public static RapportPlanification succes(String message, int nombrePlanifiees) {
        return new RapportPlanification(true, message, nombrePlanifiees, 0);
    }

    /**
     * Méthode statique pour créer un rapport avec détails complets
     */
    public static RapportPlanification complet(boolean success, String message, int planifiees,
                                               int nonPlanifiees, String algorithme, double tempsMs) {
        RapportPlanification rapport = new RapportPlanification(success, message, planifiees, nonPlanifiees, algorithme);
        rapport.setTempsExecutionMs(tempsMs);
        return rapport;
    }

    // ========== CLASSE INTERNE POUR LES PLANIFICATIONS CRÉÉES ==========

    public static class PlanificationCreee {
        private String commandeId;
        private String employeId;
        private String dateHeureDebut;
        private int dureeMinutes;
        private String statut;

        public PlanificationCreee() {}

        public PlanificationCreee(String commandeId, String employeId, String dateHeureDebut, int dureeMinutes) {
            this.commandeId = commandeId;
            this.employeId = employeId;
            this.dateHeureDebut = dateHeureDebut;
            this.dureeMinutes = dureeMinutes;
            this.statut = "PLANIFIEE";
        }

        // Getters et setters
        public String getCommandeId() {
            return commandeId;
        }

        public void setCommandeId(String commandeId) {
            this.commandeId = commandeId;
        }

        public String getEmployeId() {
            return employeId;
        }

        public void setEmployeId(String employeId) {
            this.employeId = employeId;
        }

        public String getDateHeureDebut() {
            return dateHeureDebut;
        }

        public void setDateHeureDebut(String dateHeureDebut) {
            this.dateHeureDebut = dateHeureDebut;
        }

        public int getDureeMinutes() {
            return dureeMinutes;
        }

        public void setDureeMinutes(int dureeMinutes) {
            this.dureeMinutes = dureeMinutes;
        }

        public String getStatut() {
            return statut;
        }

        public void setStatut(String statut) {
            this.statut = statut;
        }
    }
}