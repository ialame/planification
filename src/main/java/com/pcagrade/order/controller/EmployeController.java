// ========== AJOUT DANS EmployeController.java ==========

package com.pcagrade.order.controller;

import com.pcagrade.order.entity.Employe;
import com.pcagrade.order.service.EmployeService;
import com.pcagrade.order.service.CommandeService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@RequestMapping("/api/employes")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"}, allowCredentials = "false")
public class EmployeController {

    @Autowired
    private EmployeService employeService;

    @Autowired
    private CommandeService commandeService;

    @Autowired
    private EntityManager entityManager;

    // ... vos méthodes existantes ...

    /**
     * 🎯 NOUVEAU : Récupérer les commandes planifiées pour un employé
     */
    @GetMapping("/{employeId}/commandes")
    public ResponseEntity<Map<String, Object>> getCommandesEmploye(
            @PathVariable String employeId,
            @RequestParam(required = false) String date) {

        try {
            String dateFilter = date != null ? date : LocalDate.now().toString();
            System.out.println("🔍 Recherche commandes pour employé: " + employeId);
            System.out.println("📅 Date de filtrage: " + dateFilter);

            // Vérifier d'abord si la table j_planification existe
            String checkTableSql = "SHOW TABLES LIKE 'j_planification'";
            Query checkQuery = entityManager.createNativeQuery(checkTableSql);
            @SuppressWarnings("unchecked")
            List<Object> tables = checkQuery.getResultList();

            if (tables.isEmpty()) {
                System.out.println("⚠️ Table j_planification n'existe pas - retour de données vides");

                // Récupérer au moins les infos de l'employé
                Optional<Employe> employeOpt = employeService.getEmployeById(employeId);
                Map<String, Object> response = new HashMap<>();

                if (employeOpt.isPresent()) {
                    Employe emp = employeOpt.get();
                    response.put("success", true);
                    response.put("employeId", employeId);
                    response.put("date", dateFilter);
                    response.put("employe", Map.of(
                            "id", emp.getId().toString(),
                            "nomComplet", emp.getPrenom() + " " + emp.getNom(),
                            "heuresTravailParJour", emp.getHeuresTravailParJour()
                    ));
                    response.put("commandes", new ArrayList<>());
                    response.put("nombreCommandes", 0);
                    response.put("dureeeTotaleMinutes", 0);
                    response.put("dureeeTotaleFormatee", "0min");
                    response.put("statistiques", Map.of(
                            "dureeeTotaleMinutes", 0,
                            "nombreCommandes", 0
                    ));
                } else {
                    response.put("success", false);
                    response.put("message", "Employé non trouvé");
                }

                return ResponseEntity.ok(response);
            }

            // La table existe, faire la vraie requête
            String sql = """
            SELECT
                DISTINCT     
                HEX(o.id) as order_id,
                o.num_commande,
                o.temps_estime_minutes,
                o.priorite_string,
                o.status,
                o.date_creation,
                o.date_limite,
                p.date_planification,
                p.heure_debut,
                p.duree_minutes,
                p.terminee,
                HEX(p.id) as planification_id,
                CONCAT(e.prenom, ' ', e.nom) as employe_nom,
                (SELECT COUNT(*) FROM card_certification_order cco WHERE cco.order_id = o.id) as nombre_cartes
            FROM j_planification p
            JOIN `order` o ON p.order_id = o.id
            JOIN j_employe e ON p.employe_id = e.id
            WHERE HEX(e.id) = ?
            AND DATE(p.date_planification) = ?
            ORDER BY p.heure_debut ASC
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, employeId.replace("-", ""));
            query.setParameter(2, dateFilter);

            @SuppressWarnings("unchecked")
            List<Object[]> resultats = query.getResultList();

            List<Map<String, Object>> commandes = new ArrayList<>();
            int dureeeTotale = 0;

            for (Object[] row : resultats) {
                Map<String, Object> commande = new HashMap<>();
                commande.put("id", (String) row[0]);
                commande.put("numeroCommande", (String) row[1]);
                commande.put("tempsEstimeMinutes", row[2] != null ? (Integer) row[2] : 0);
                commande.put("priorite", (String) row[3]);
                commande.put("status", (Integer) row[4]);
                commande.put("dateCreation", row[5]);
                commande.put("dateLimite", row[6]);
                commande.put("datePlanification", row[7]);
                commande.put("heureDebut", row[8]);
                commande.put("dureeMinutes", row[9] != null ? (Integer) row[9] : 0);
                commande.put("terminee", row[10] != null ? (Boolean) row[10] : false);
                commande.put("planificationId", (String) row[11]);
                commande.put("employeNom", (String) row[12]);
                commande.put("nombreCartes", row[13] != null ? ((Number) row[13]).intValue() : 0);

                // Calculer durée avec règle: 3 × nombre de cartes
                int nombreCartes = (Integer) commande.get("nombreCartes");
                int dureeCalculee = Math.max(nombreCartes * 3, 30); // Min 30 min
                commande.put("dureeCalculee", dureeCalculee);

                dureeeTotale += dureeCalculee;
                commandes.add(commande);
            }

            // Récupérer les informations de l'employé
            Optional<Employe> employeOpt = employeService.getEmployeById(employeId);
            Map<String, Object> response = new HashMap<>();

            if (employeOpt.isPresent()) {
                Employe emp = employeOpt.get();
                response.put("success", true);
                response.put("employeId", employeId);
                response.put("date", dateFilter);
                response.put("employe", Map.of(
                        "id", emp.getId().toString(),
                        "nomComplet", emp.getPrenom() + " " + emp.getNom(),
                        "heuresTravailParJour", emp.getHeuresTravailParJour()
                ));
                response.put("commandes", commandes);
                response.put("nombreCommandes", commandes.size());
                response.put("dureeeTotaleMinutes", dureeeTotale);
                response.put("dureeeTotaleFormatee", formaterDuree(dureeeTotale));
                response.put("statistiques", Map.of(
                        "dureeeTotaleMinutes", dureeeTotale,
                        "nombreCommandes", commandes.size()
                ));
            } else {
                response.put("success", false);
                response.put("message", "Employé non trouvé");
            }

            System.out.println("✅ " + commandes.size() + " commandes trouvées pour employé " + employeId);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération commandes employé: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> erreur = new HashMap<>();
            erreur.put("success", false);
            erreur.put("message", "Erreur: " + e.getMessage());
            erreur.put("employeId", employeId);
            erreur.put("commandes", new ArrayList<>());

            return ResponseEntity.status(500).body(erreur);
        }
    }



    /**
     * 🎯 NOUVEAU : Récupérer le planning complet d'un employé avec statistiques
     */
    @GetMapping("/{employeId}/planning")
    public ResponseEntity<Map<String, Object>> getPlanningEmploye(
            @PathVariable String employeId,
            @RequestParam(required = false) String date) {

        try {
            System.out.println("📋 Récupération planning employé: " + employeId);

            // Récupérer les informations de l'employé
            Optional<Employe> employeOpt = employeService.getEmployeById(employeId);
            if (employeOpt.isEmpty()) {
                Map<String, Object> erreur = new HashMap<>();
                erreur.put("success", false);
                erreur.put("message", "Employé non trouvé");
                return ResponseEntity.notFound().build();
            }

            Employe employe = employeOpt.get();
            String dateFilter = date != null ? date : LocalDate.now().toString();

            // Récupérer les commandes de cet employé
            ResponseEntity<Map<String, Object>> commandesResponse = getCommandesEmploye(employeId, dateFilter);
            Map<String, Object> commandesData = commandesResponse.getBody();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> commandes = (List<Map<String, Object>>) commandesData.get("commandes");

            // Calculer les statistiques
            int commandesTerminees = 0;
            int commandesEnCours = 0;
            int commandesPlanifiees = 0;
            int dureeeTotale = 0;
            int cartesTotales = 0;

            for (Map<String, Object> cmd : commandes) {
                Integer status = (Integer) cmd.get("status");
                Boolean terminee = (Boolean) cmd.get("terminee");

                if (Boolean.TRUE.equals(terminee) || (status != null && status == 4)) {
                    commandesTerminees++;
                } else if (status != null && status == 3) {
                    commandesEnCours++;
                } else {
                    commandesPlanifiees++;
                }

                dureeeTotale += (Integer) cmd.get("dureeCalculee");
                cartesTotales += (Integer) cmd.get("nombreCartes");
            }

            // Préparer la réponse complète
            Map<String, Object> planning = new HashMap<>();
            planning.put("success", true);
            planning.put("employe", Map.of(
                    "id", employe.getId().toString(),
                    "nom", employe.getNom(),
                    "prenom", employe.getPrenom(),
                    "nomComplet", employe.getPrenom() + " " + employe.getNom(),
                    "heuresTravailParJour", employe.getHeuresTravailParJour()
            ));
            planning.put("date", dateFilter);
            planning.put("commandes", commandes);

            // Statistiques
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCommandes", commandes.size());
            stats.put("commandesTerminees", commandesTerminees);
            stats.put("commandesEnCours", commandesEnCours);
            stats.put("commandesPlanifiees", commandesPlanifiees);
            stats.put("cartesTotales", cartesTotales);
            stats.put("dureeeTotaleMinutes", dureeeTotale);
            stats.put("dureeeTotaleFormatee", formaterDuree(dureeeTotale));
            stats.put("moyenneCartesParCommande", commandes.size() > 0 ? (double) cartesTotales / commandes.size() : 0);
            stats.put("moyenneDureeParCommande", commandes.size() > 0 ? (double) dureeeTotale / commandes.size() : 0);

            planning.put("statistiques", stats);

            System.out.println("✅ Planning employé récupéré: " + commandes.size() + " commandes");

            return ResponseEntity.ok(planning);

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération planning employé: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> erreur = new HashMap<>();
            erreur.put("success", false);
            erreur.put("message", "Erreur: " + e.getMessage());

            return ResponseEntity.status(500).body(erreur);
        }
    }

    /**
     * 🎯 NOUVEAU : Liste tous les employés avec leurs statistiques du jour
     */
    @GetMapping("/avec-stats")
    public ResponseEntity<List<Map<String, Object>>> getEmployesAvecStats(
            @RequestParam(required = false) String date) {

        try {
            String dateFilter = date != null ? date : LocalDate.now().toString();
            System.out.println("👥 Récupération employés avec stats pour: " + dateFilter);

            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();
            List<Map<String, Object>> employesAvecStats = new ArrayList<>();

            for (Map<String, Object> emp : employes) {
                String employeId = (String) emp.get("id");

                // Récupérer les commandes de cet employé
                ResponseEntity<Map<String, Object>> commandesResponse = getCommandesEmploye(employeId, dateFilter);
                Map<String, Object> commandesData = commandesResponse.getBody();

                // Ajouter les statistiques à l'employé
                Map<String, Object> employeAvecStats = new HashMap<>(emp);
                employeAvecStats.put("nombreCommandes", commandesData.get("nombreCommandes"));
                employeAvecStats.put("dureeeTotaleMinutes", commandesData.get("dureeeTotaleMinutes"));
                employeAvecStats.put("dureeeTotaleFormatee", commandesData.get("dureeeTotaleFormatee"));

                // Calculer le statut de charge
                Integer dureeeTotale = (Integer) commandesData.get("dureeeTotaleMinutes");
                Integer heuresTravail = (Integer) emp.get("heuresTravailParJour");
                int capaciteMaxMinutes = (heuresTravail != null ? heuresTravail : 8) * 60;

                String statut;
                if (dureeeTotale > capaciteMaxMinutes) {
                    statut = "overloaded";
                } else if (dureeeTotale > capaciteMaxMinutes * 0.8) {
                    statut = "full";
                } else {
                    statut = "available";
                }

                employeAvecStats.put("statut", statut);
                employeAvecStats.put("capaciteMaxMinutes", capaciteMaxMinutes);
                employeAvecStats.put("pourcentageCharge",
                        capaciteMaxMinutes > 0 ? (double) dureeeTotale / capaciteMaxMinutes * 100 : 0);

                employesAvecStats.add(employeAvecStats);
            }

            System.out.println("✅ " + employesAvecStats.size() + " employés avec stats récupérés");

            return ResponseEntity.ok(employesAvecStats);

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération employés avec stats: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /**
     * Formatte une durée en minutes vers un format lisible
     */
    private String formaterDuree(int minutes) {
        if (minutes < 60) {
            return minutes + "min";
        }
        int heures = minutes / 60;
        int minutesRestantes = minutes % 60;
        return minutesRestantes > 0 ? heures + "h" + minutesRestantes + "min" : heures + "h";
    }
}