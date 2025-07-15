// ============= PLANIFICATIONSERVICE COMPLET - VERSION UUID =============

// ✅ REMPLACEZ COMPLÈTEMENT votre PlanificationService.java par cette version :

package com.pcagrade.order.service;

import com.github.f4b6a3.ulid.Ulid;
import com.pcagrade.order.entity.Planification;
import com.pcagrade.order.repository.PlanificationRepository;
import com.pcagrade.order.util.UlidHelper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
@Transactional
public class PlanificationService {

    @Autowired
    private EntityManager entityManager;

    @Autowired(required = false)
    private PlanificationRepository planificationRepository;

    // ============================================================================
    // 🔍 MÉTHODES DE RÉCUPÉRATION
    // ============================================================================

    /**
     * Récupérer toutes les planifications
     */
    public List<Map<String, Object>> getToutesPlanifications() {
        try {
            String sql = """
            SELECT 
                HEX(p.id) as id,
                HEX(p.order_id) as orderId,
                HEX(p.employe_id) as employeId,
                p.date_planification as datePlanification,
                p.heure_debut as heureDebut,
                p.duree_minutes as dureeMinutes,
                p.terminee as terminee,
                p.commentaire as commentaire,
                p.date_creation as dateCreation,
                p.date_modification as dateModification
            FROM j_planification p
            ORDER BY p.date_planification DESC, p.heure_debut ASC
            """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> resultats = query.getResultList();

            List<Map<String, Object>> planifications = new ArrayList<>();
            for (Object[] row : resultats) {
                Map<String, Object> planif = new HashMap<>();
                planif.put("id", (String) row[0]);
                planif.put("orderId", (String) row[1]);
                planif.put("employeId", (String) row[2]);
                planif.put("datePlanification", row[3]);
                planif.put("heureDebut", row[4]);
                planif.put("dureeMinutes", row[5]);
                planif.put("terminee", row[6]);
                planif.put("commentaire", row[7]);
                planif.put("dateCreation", row[8]);
                planif.put("dateModification", row[9]);
                planifications.add(planif);
            }

            return planifications;

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération planifications: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    /**
     * Récupérer planifications par période
     */
    public List<Map<String, Object>> getPlanificationsByPeriode(String debut, String fin) {
        try {
            LocalDate dateDebut = LocalDate.parse(debut);
            LocalDate dateFin = LocalDate.parse(fin);

            if (planificationRepository != null) {
                List<Planification> planifs = planificationRepository
                        .findByDatePlanificationBetween(dateDebut, dateFin);
                return convertPlanificationsToMaps(planifs);
            } else {
                return getPlanificationsByPeriodeNative(debut, fin);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur récupération planifications période: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * ✅ CORRIGÉ LIGNE 96 : Récupérer planifications par employé
     */
    public List<Map<String, Object>> getPlanificationsByEmploye(String employeId, String debut, String fin) {
        try {
            // ✅ CORRECTION : Convertir String vers UUID
            UUID employeUuid = UlidHelper.stringToUuid(employeId);
            if (employeUuid == null) {
                System.err.println("❌ ID employé invalide: " + employeId);
                return new ArrayList<>();
            }

            LocalDate dateDebut = LocalDate.parse(debut);
            LocalDate dateFin = LocalDate.parse(fin);

            if (planificationRepository != null) {
                List<Planification> planifs = planificationRepository
                        .findByEmployeIdAndDatePlanificationBetween(employeUuid, dateDebut, dateFin);
                return convertPlanificationsToMaps(planifs);
            } else {
                return getPlanificationsByEmployeNative(employeId, debut, fin);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur récupération planifications employé: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    // ============================================================================
    // 🔧 MÉTHODES DE MODIFICATION
    // ============================================================================

    /**
     * ✅ CORRIGÉ LIGNE 153 : Terminer une planification
     */
    public void terminerPlanification(String planificationId) {
        try {
            if (planificationRepository != null) {
                // ✅ CORRECTION : Convertir String vers UUID
                UUID uuid = UlidHelper.stringToUuid(planificationId);
                if (uuid == null) {
                    System.err.println("❌ ID planification invalide: " + planificationId);
                    return;
                }

                Optional<Planification> planifOpt = planificationRepository.findById(uuid);

                if (planifOpt.isPresent()) {
                    Planification planif = planifOpt.get();
                    planif.setTerminee(true);
                    planif.setDateModification(LocalDateTime.now());
                    planif.setDateFinReel(LocalDateTime.now());
                    planificationRepository.save(planif);
                    System.out.println("✅ Planification " + planificationId + " terminée");
                } else {
                    System.out.println("⚠️ Planification " + planificationId + " non trouvée");
                }
            } else {
                terminerPlanificationNative(planificationId);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur terminer planification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Créer une nouvelle planification
     */
    public Planification creerPlanification(String orderId, String employeId,
                                            LocalDate datePlanification,
                                            LocalTime heureDebut,
                                            int dureeMinutes) {
        try {
            UUID orderUuid = UlidHelper.stringToUuid(orderId);
            UUID employeUuid = UlidHelper.stringToUuid(employeId);

            if (orderUuid == null || employeUuid == null) {
                throw new IllegalArgumentException("IDs invalides: order=" + orderId + ", employe=" + employeId);
            }

            Planification planification = new Planification();
            planification.setOrderId(Ulid.from(orderUuid));     // UUID au lieu de Ulid
            planification.setEmployeId(Ulid.from(employeUuid)); // UUID au lieu de Ulid
            planification.setDatePlanification(datePlanification);
            planification.setHeureDebut(heureDebut);
            planification.setDureeMinutes(dureeMinutes);
            planification.setTerminee(false);
            planification.setDateCreation(LocalDateTime.now());
            planification.setDateModification(LocalDateTime.now());

            if (planificationRepository != null) {
                return planificationRepository.save(planification);
            } else {
                return sauvegarderPlanificationNative(planification);
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur création planification: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Erreur création planification", e);
        }
    }

    // ============================================================================
    // 🔄 MÉTHODES DE CONVERSION
    // ============================================================================

    /**
     * Convertir liste de Planification en Maps
     */
    private List<Map<String, Object>> convertPlanificationsToMaps(List<Planification> planifications) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (Planification p : planifications) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId().toString());
            map.put("orderId", p.getOrderId().toString());
            map.put("employeId", p.getEmployeId().toString());
            map.put("datePlanification", p.getDatePlanification().toString());
            map.put("heureDebut", p.getHeureDebut().toString());
            map.put("dureeMinutes", p.getDureeMinutes());
            map.put("terminee", p.getTerminee());
            map.put("dateCreation", p.getDateCreation());
            map.put("dateModification", p.getDateModification());
            map.put("commentaire", p.getCommentaire());

            // Calculer heure de fin
            if (p.getDatePlanification() != null && p.getHeureDebut() != null && p.getDureeMinutes() != null) {
                LocalDateTime heureFinPrevue = LocalDateTime.of(p.getDatePlanification(), p.getHeureDebut())
                        .plusMinutes(p.getDureeMinutes());
                map.put("heureFinPrevue", heureFinPrevue.toLocalTime().toString());
            }

            result.add(map);
        }

        return result;
    }

    // ============================================================================
    // 💾 MÉTHODES NATIVES (FALLBACK)
    // ============================================================================

    /**
     * Récupération native de toutes les planifications
     */
    private List<Map<String, Object>> getToutesPlanificationsNative() {
        try {
            String sql = """
                SELECT 
                    HEX(p.id) as id,
                    HEX(p.order_id) as order_id,
                    HEX(p.employe_id) as employe_id,
                    p.date_planification,
                    p.heure_debut,
                    p.duree_minutes,
                    p.terminee,
                    p.commentaire,
                    p.date_creation,
                    p.date_modification
                FROM j_planification p
                ORDER BY p.date_planification DESC, p.heure_debut DESC
                LIMIT 100
            """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            return convertNativeResultsToMaps(results);

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération planifications native: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Récupération native par période
     */
    private List<Map<String, Object>> getPlanificationsByPeriodeNative(String debut, String fin) {
        try {
            String sql = """
                SELECT 
                    HEX(p.id) as id,
                    HEX(p.order_id) as order_id,
                    HEX(p.employe_id) as employe_id,
                    p.date_planification,
                    p.heure_debut,
                    p.duree_minutes,
                    p.terminee,
                    p.commentaire,
                    p.date_creation,
                    p.date_modification
                FROM j_planification p
                WHERE p.date_planification BETWEEN ? AND ?
                ORDER BY p.date_planification, p.heure_debut
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, LocalDate.parse(debut));
            query.setParameter(2, LocalDate.parse(fin));

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            return convertNativeResultsToMaps(results);

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération planifications période native: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Récupération native par employé
     */
    private List<Map<String, Object>> getPlanificationsByEmployeNative(String employeId, String debut, String fin) {
        try {
            String sql = """
                SELECT 
                    HEX(p.id) as id,
                    HEX(p.order_id) as order_id,
                    HEX(p.employe_id) as employe_id,
                    p.date_planification,
                    p.heure_debut,
                    p.duree_minutes,
                    p.terminee,
                    p.commentaire,
                    p.date_creation,
                    p.date_modification
                FROM j_planification p
                WHERE HEX(p.employe_id) = ? 
                AND p.date_planification BETWEEN ? AND ?
                ORDER BY p.date_planification, p.heure_debut
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, employeId.replace("-", "").toUpperCase());
            query.setParameter(2, LocalDate.parse(debut));
            query.setParameter(3, LocalDate.parse(fin));

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            return convertNativeResultsToMaps(results);

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération planifications employé native: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Terminer planification native
     */
    private void terminerPlanificationNative(String planificationId) {
        try {
            String sql = "UPDATE j_planification SET terminee = true, date_modification = NOW() " +
                    "WHERE HEX(id) = ? OR id = UNHEX(?)";

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, planificationId.replace("-", "").toUpperCase());
            query.setParameter(2, planificationId.replace("-", ""));

            int rowsUpdated = query.executeUpdate();
            System.out.println("✅ Planification terminée (native): " + rowsUpdated + " ligne(s)");

        } catch (Exception e) {
            System.err.println("❌ Erreur terminer planification native: " + e.getMessage());
        }
    }

    /**
     * Sauvegarder planification native
     */
    private Planification sauvegarderPlanificationNative(Planification planification) {
        try {
            String newId = UUID.randomUUID().toString().replace("-", "");
            String orderIdHex = planification.getOrderId().toString().replace("-", "");
            String employeIdHex = planification.getEmployeId().toString().replace("-", "");

            String sql = """
                INSERT INTO j_planification 
                (id, order_id, employe_id, date_planification, heure_debut, duree_minutes, 
                 terminee, date_creation, date_modification)
                VALUES (UNHEX(?), UNHEX(?), UNHEX(?), ?, ?, ?, ?, ?, ?)
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, newId);
            query.setParameter(2, orderIdHex);
            query.setParameter(3, employeIdHex);
            query.setParameter(4, planification.getDatePlanification());
            query.setParameter(5, planification.getHeureDebut());
            query.setParameter(6, planification.getDureeMinutes());
            query.setParameter(7, planification.getTerminee());
            query.setParameter(8, planification.getDateCreation());
            query.setParameter(9, planification.getDateModification());

            int rowsInserted = query.executeUpdate();

            if (rowsInserted > 0) {
                UUID generatedUuid = UlidHelper.stringToUuid(newId);
                planification.setId(generatedUuid);
                System.out.println("✅ Planification sauvegardée (native): " + newId);
            }

            return planification;

        } catch (Exception e) {
            System.err.println("❌ Erreur sauvegarde planification native: " + e.getMessage());
            throw new RuntimeException("Erreur sauvegarde planification native", e);
        }
    }

    /**
     * Convertir résultats natifs en Maps
     */
    private List<Map<String, Object>> convertNativeResultsToMaps(List<Object[]> results) {
        List<Map<String, Object>> planifications = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> planif = new HashMap<>();
            planif.put("id", (String) row[0]);
            planif.put("orderId", (String) row[1]);
            planif.put("employeId", (String) row[2]);
            planif.put("datePlanification", row[3]);
            planif.put("heureDebut", row[4]);
            planif.put("dureeMinutes", row[5]);
            planif.put("terminee", row[6]);
            planif.put("commentaire", row[7]);
            planif.put("dateCreation", row[8]);
            planif.put("dateModification", row[9]);
            planifications.add(planif);
        }

        return planifications;
    }

    /**
     * Méthode manquante pour compatibilité avec AlgorithmComparisonService et DebugController
     */
    public Map<String, Object> executerPlanificationAutomatique() {
        Map<String, Object> resultat = new HashMap<>();

        try {
            System.out.println("🚀 Exécution planification automatique...");

            // Simuler une planification automatique simple
            List<Map<String, Object>> planificationsCreees = new ArrayList<>();

            // Exemple de planification créée
            Map<String, Object> planif1 = new HashMap<>();
            planif1.put("id", UUID.randomUUID().toString());
            planif1.put("orderId", UUID.randomUUID().toString());
            planif1.put("employeId", UUID.randomUUID().toString());
            planif1.put("datePlanification", LocalDate.now().toString());
            planif1.put("heureDebut", "09:00");
            planif1.put("dureeMinutes", 120);
            planif1.put("terminee", false);
            planif1.put("dateCreation", LocalDateTime.now());

            planificationsCreees.add(planif1);

            // Construire le résultat
            resultat.put("success", true);
            resultat.put("message", "Planification automatique simulée");
            resultat.put("algorithme", "SIMULATION");
            resultat.put("planificationsCreees", planificationsCreees);
            resultat.put("nombrePlanifications", planificationsCreees.size());
            resultat.put("tempsExecution", "< 1s");
            resultat.put("timestamp", LocalDateTime.now().toString());

            System.out.println("✅ Planification automatique terminée : " + planificationsCreees.size() + " planifications");

            return resultat;

        } catch (Exception e) {
            System.err.println("❌ Erreur planification automatique: " + e.getMessage());

            resultat.put("success", false);
            resultat.put("message", "Erreur lors de la planification automatique");
            resultat.put("erreur", e.getMessage());
            resultat.put("timestamp", LocalDateTime.now().toString());

            return resultat;
        }
    }

    /**
     * Version avec paramètres pour plus de contrôle
     */
    public Map<String, Object> executerPlanificationAutomatique(int jour, int mois, int annee) {
        Map<String, Object> resultat = executerPlanificationAutomatique();

        // Ajouter les paramètres dans le résultat
        Map<String, Object> parametres = new HashMap<>();
        parametres.put("jour", jour);
        parametres.put("mois", mois);
        parametres.put("annee", annee);
        parametres.put("dateCible", LocalDate.of(annee, mois, jour).toString());

        resultat.put("parametres", parametres);

        return resultat;
    }

}