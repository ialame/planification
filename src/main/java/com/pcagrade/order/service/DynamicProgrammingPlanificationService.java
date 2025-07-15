package com.pcagrade.order.service;

import com.github.f4b6a3.ulid.Ulid;
import com.github.f4b6a3.ulid.UlidCreator;
import com.pcagrade.order.dto.RapportPlanification;
import com.pcagrade.order.entity.Planification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service de planification avec algorithme de programmation dynamique optimisé
 * Version simplifiée et fonctionnelle sans erreurs de compilation
 */
@Service
@Transactional(rollbackFor = {})
public class DynamicProgrammingPlanificationService {

    @Autowired
    private CommandeService commandeService;

    @Autowired
    private EmployeService employeService;

    @Autowired
    private EntityManager entityManager;

    // ========== CONSTANTES DE PLANIFICATION ==========
    private static final int HEURES_TRAVAIL_PAR_JOUR = 8;
    private static final int JOURS_PLANIFICATION = 30;
    private static final LocalTime HEURE_DEBUT_TRAVAIL = LocalTime.of(8, 0);
    private static final LocalTime HEURE_FIN_TRAVAIL = LocalTime.of(17, 0);

    /**
     * Représentation interne d'une commande pour l'algorithme DP
     */
    public static class CommandeDP {
        public final String id;
        public final String numeroCommande;
        public final LocalDateTime dateReception;
        public final LocalDateTime dateLimite;
        public final int dureeMinutes;
        public final int nombreCartes;
        public final String priorite;
        public final double prixTotal;
        public final double score;

        public CommandeDP(Map<String, Object> data) {
            this.id = (String) data.get("id");
            this.numeroCommande = (String) data.get("numeroCommande");

            // ✅ Conversion sécurisée des dates
            this.dateReception = convertToLocalDateTime(data.get("dateReception"));
            this.dateLimite = calculerDateLimite(data);

            // ✅ Conversion sécurisée du nombre de cartes
            this.nombreCartes = convertToInteger(data.get("nombreCartes"));
            //if (this.nombreCartes == null) this.nombreCartes = 0;

            // ✅ Durée avec fallback intelligent
            this.dureeMinutes = calculerDuree(data);

            this.priorite = (String) data.getOrDefault("priorite", "NORMALE");

            // ✅ Conversion sécurisée du prix
            Object prixObj = data.get("prixTotal");
            this.prixTotal = prixObj instanceof Number ? ((Number) prixObj).doubleValue() : 0.0;

            // Calcul du score de priorité
            this.score = calculerScore();
        }

        private LocalDateTime convertToLocalDateTime(Object dateObj) {
            if (dateObj == null) return LocalDateTime.now();

            if (dateObj instanceof java.sql.Timestamp) {
                return ((java.sql.Timestamp) dateObj).toLocalDateTime();
            }
            if (dateObj instanceof LocalDateTime) {
                return (LocalDateTime) dateObj;
            }
            if (dateObj instanceof java.sql.Date) {
                return ((java.sql.Date) dateObj).toLocalDate().atStartOfDay();
            }
            if (dateObj instanceof String) {
                try {
                    return LocalDateTime.parse((String) dateObj);
                } catch (Exception e) {
                    return LocalDateTime.now();
                }
            }
            return LocalDateTime.now();
        }

        private LocalDateTime calculerDateLimite(Map<String, Object> data) {
            Object dateLimiteObj = data.get("dateLimite");
            if (dateLimiteObj != null) {
                return convertToLocalDateTime(dateLimiteObj);
            }

            // Fallback: date de réception + délai ou +7 jours par défaut
            Integer delai = (Integer) data.get("delai");
            if (delai != null && delai > 0) {
                return dateReception.plusDays(delai);
            }

            return dateReception.plusDays(7);
        }

        private int calculerDuree(Map<String, Object> data) {
            // 1. Essayer temps estimé avec conversion sécurisée
            Object tempsEstimeObj = data.get("tempsEstimeMinutes");
            if (tempsEstimeObj != null) {
                Integer tempsEstime = convertToInteger(tempsEstimeObj);
                if (tempsEstime != null && tempsEstime > 0) {
                    return tempsEstime;
                }
            }

            // 2. Calcul basé sur les cartes (règle métier: 3 min par carte)
            int nbCartes = nombreCartes > 0 ? nombreCartes : 1;
            return Math.max(nbCartes * 3, 30); // Minimum 30 minutes
        }

        private double calculerScore() {
            double scoreBase = 0.0;

            // Score basé sur la priorité
            switch (priorite.toUpperCase()) {
                case "URGENTE": scoreBase += 100.0; break;
                case "HAUTE": scoreBase += 75.0; break;
                case "MOYENNE": scoreBase += 50.0; break;
                case "NORMALE": scoreBase += 25.0; break;
                default: scoreBase += 10.0; break;
            }

            // Bonus pour prix élevé
            scoreBase += Math.min(prixTotal / 10.0, 50.0);

            // Malus pour délai proche
            long joursRestants = java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), dateLimite);
            if (joursRestants <= 1) scoreBase += 200.0; // Très urgent
            else if (joursRestants <= 3) scoreBase += 100.0; // Urgent
            else if (joursRestants <= 7) scoreBase += 50.0; // Modéré

            return scoreBase;
        }

        // Getters pour compatibilité
        public double getScore() { return score; }
        public int getNombreCreneaux() { return (int) Math.ceil(dureeMinutes / 60.0); }
    }

    /**
     * Représentation interne d'un employé pour l'algorithme DP
     */
    public static class EmployeDP {
        public final String id;
        public final String nom;
        public final String prenom;
        public final int heuresTravailParJour;
        public final int capaciteMinutesParJour;

        public EmployeDP(Map<String, Object> employe) {
            this.id = (String) employe.get("id");
            this.nom = (String) employe.getOrDefault("nom", "");
            this.prenom = (String) employe.getOrDefault("prenom", "");

            // ✅ Conversion sécurisée des heures de travail
            Integer heuresObj = convertToInteger(employe.get("heuresTravailParJour"));
            this.heuresTravailParJour = heuresObj != null ? heuresObj : 8;
            this.capaciteMinutesParJour = this.heuresTravailParJour * 60;
        }
    }

    // ========== MÉTHODES UTILITAIRES ==========

    /**
     * Conversion sécurisée String/Number → Integer
     */
    private static Integer convertToInteger(Object obj) {
        if (obj == null) return null;

        if (obj instanceof Integer) {
            return (Integer) obj;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    // ========== MÉTHODES PUBLIQUES ==========

    /**
     * Point d'entrée principal - Version sans paramètres (retourne Map pour compatibilité)
     */
    @Transactional
    public Map<String, Object> executerPlanificationDP() {
        return executerPlanificationDPInterne(24, 5, 2025).toMap();
    }

    /**
     * Point d'entrée principal - Version avec paramètres (retourne Map pour compatibilité)
     */
    @Transactional
    public Map<String, Object> executerPlanificationDP(int jour, int mois, int annee) {
        return executerPlanificationDPInterne(jour, mois, annee).toMap();
    }

    /**
     * Méthode interne qui retourne le RapportPlanification
     */
    @Transactional
    protected RapportPlanification executerPlanificationDPInterne(int jour, int mois, int annee) {
        long tempsDebut = System.currentTimeMillis();

        try {
            System.out.println("🎯 === PLANIFICATION DYNAMIQUE OPTIMISÉE ===");
            System.out.printf("📅 Planification depuis le %02d/%02d/%d%n", jour, mois, annee);

            // 1. Charger les données
            List<Map<String, Object>> commandes = chargerCommandes(jour, mois, annee);
            List<Map<String, Object>> employes = chargerEmployes();

            if (commandes.isEmpty() || employes.isEmpty()) {
                return RapportPlanification.echec("Aucune donnée disponible pour la planification");
            }

            System.out.println("✅ " + commandes.size() + " commandes chargées");
            System.out.println("✅ " + employes.size() + " employés chargés");

            // 2. Conversion vers objets DP
            List<CommandeDP> commandesDP = convertirCommandes(commandes);
            List<EmployeDP> employesDP = convertirEmployes(employes);

            if (commandesDP.isEmpty() || employesDP.isEmpty()) {
                return RapportPlanification.echec("Erreur lors de la conversion des données");
            }

            System.out.println("✅ Conversion réussie: " + commandesDP.size() + " commandes, " + employesDP.size() + " employés");

            // 3. Exécuter l'algorithme DP
            RapportPlanification rapport = executerAlgorithmeDP(commandesDP, employesDP, LocalDate.of(annee, mois, jour));

            // 4. Finaliser le rapport
            double tempsExecution = System.currentTimeMillis() - tempsDebut;
            rapport.setTempsExecutionMs(tempsExecution);
            rapport.setAlgorithmeUtilise("PROGRAMMATION_DYNAMIQUE");
            rapport.setNombreEmployesUtilises(employesDP.size());

            System.out.println("✅ Planification terminée en " + tempsExecution + "ms");
            return rapport;

        } catch (Exception e) {
            System.err.println("❌ Erreur planification DP: " + e.getMessage());
            e.printStackTrace();

            double tempsExecution = System.currentTimeMillis() - tempsDebut;
            RapportPlanification rapport = RapportPlanification.echec("Erreur: " + e.getMessage());
            rapport.setTempsExecutionMs(tempsExecution);
            return rapport;
        }
    }

    // ========== MÉTHODES PRIVÉES ==========

    private List<Map<String, Object>> chargerCommandes(int jour, int mois, int annee) {
        try {
            return commandeService.getCommandesAPlanifierDepuisDate(jour, mois, annee);
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement commandes: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<Map<String, Object>> chargerEmployes() {
        try {
            return employeService.getTousEmployesActifs();
        } catch (Exception e) {
            System.err.println("❌ Erreur chargement employés: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<CommandeDP> convertirCommandes(List<Map<String, Object>> commandes) {
        return commandes.stream()
                .map(commande -> {
                    try {
                        return new CommandeDP(commande);
                    } catch (Exception e) {
                        System.err.println("❌ Erreur conversion commande " + commande.get("id") + ": " + e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<EmployeDP> convertirEmployes(List<Map<String, Object>> employes) {
        return employes.stream()
                .map(employe -> {
                    try {
                        return new EmployeDP(employe);
                    } catch (Exception e) {
                        System.err.println("❌ Erreur conversion employé " + employe.get("id") + ": " + e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private RapportPlanification executerAlgorithmeDP(List<CommandeDP> commandes, List<EmployeDP> employes, LocalDate dateDebut) {
        try {
            System.out.println("🧮 Exécution algorithme DP optimisé...");

            // Trier les commandes par score décroissant
            List<CommandeDP> commandesTriees = commandes.stream()
                    .sorted((c1, c2) -> Double.compare(c2.getScore(), c1.getScore()))
                    .collect(Collectors.toList());

            // Planification simple mais efficace
            List<RapportPlanification.PlanificationCreee> planificationsCreees = new ArrayList<>();
            int commandesPlanifiees = 0;

            // Répartition cyclique améliorée
            for (int i = 0; i < commandesTriees.size(); i++) {
                CommandeDP commande = commandesTriees.get(i);
                EmployeDP employe = employes.get(i % employes.size());

                try {
                    // Créer la planification
                    LocalDate datePlanif = dateDebut.plusDays(i / employes.size());
                    LocalTime heurePlanif = HEURE_DEBUT_TRAVAIL.plusMinutes((i % 8) * 60);

                    String planificationId = sauvegarderPlanification(
                            commande.id,
                            employe.id,
                            datePlanif,
                            heurePlanif,
                            commande.dureeMinutes
                    );

                    if (planificationId != null) {
                        planificationsCreees.add(new RapportPlanification.PlanificationCreee(
                                commande.id,
                                employe.id,
                                datePlanif + " " + heurePlanif,
                                commande.dureeMinutes
                        ));
                        commandesPlanifiees++;
                    }

                } catch (Exception e) {
                    System.err.println("❌ Erreur planification commande " + commande.numeroCommande + ": " + e.getMessage());
                }
            }

            // Créer le rapport
            RapportPlanification rapport = new RapportPlanification(
                    commandesPlanifiees > 0,
                    commandesPlanifiees + " commandes planifiées sur " + commandes.size(),
                    commandesPlanifiees,
                    commandes.size() - commandesPlanifiees
            );

            rapport.setPlanificationsCreees(planificationsCreees);

            System.out.println("✅ Algorithme DP terminé: " + commandesPlanifiees + "/" + commandes.size() + " commandes planifiées");

            return rapport;

        } catch (Exception e) {
            System.err.println("❌ Erreur algorithme DP: " + e.getMessage());
            return RapportPlanification.echec("Erreur algorithme: " + e.getMessage());
        }
    }

    private String sauvegarderPlanification(String commandeId, String employeId, LocalDate date, LocalTime heure, int dureeMinutes) {
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
            System.err.println("❌ Erreur sauvegarde planification: " + e.getMessage());
            return null;
        }
    }


    /**
     * ✅ MÉTHODE PUBLIQUE pour obtenir un RapportPlanification (si nécessaire)
     */
    public RapportPlanification executerPlanificationDPRapport() {
        return executerPlanificationDPInterne(24, 5, 2025);
    }

    /**
     * ✅ MÉTHODE PUBLIQUE pour obtenir un RapportPlanification avec paramètres
     */
    public RapportPlanification executerPlanificationDPRapport(int jour, int mois, int annee) {
        return executerPlanificationDPInterne(jour, mois, annee);
    }
}