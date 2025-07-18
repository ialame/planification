package com.pcagrade.order.controller;

import com.pcagrade.order.service.EmployeService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/test-planification")
@CrossOrigin(origins = "*")
public class TestPlanificationController {

    @Autowired
    private EmployeService employeService;

    @Autowired
    private EntityManager entityManager;

    /**
     * 🚀 PLANIFICATION SIMPLE - À ajouter dans TestPlanificationController.java
     */
    @PostMapping("/planifier-simple")
    public ResponseEntity<Map<String, Object>> planifierSimple() {
        try {
            System.out.println("🚀 === PLANIFICATION SIMPLE ===");

            Map<String, Object> resultat = new HashMap<>();

            // 1. Récupérer les employés
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();
            if (employes.isEmpty()) {
                resultat.put("success", false);
                resultat.put("message", "Aucun employé disponible");
                return ResponseEntity.ok(resultat);
            }

            // 2. Récupérer des commandes en attente
            String sqlCommandes = """
            SELECT 
                HEX(id) as commandeId,
                num_commande as numeroCommande,
                COALESCE(temps_estime_minutes, 120) as dureeMinutes,
                date
            FROM `order`
            WHERE status IN (1, 2) 
            AND date >= '2025-06-01'
            AND COALESCE(annulee, 0) = 0
            ORDER BY date ASC
            LIMIT 20
            """;

            Query query = entityManager.createNativeQuery(sqlCommandes);
            @SuppressWarnings("unchecked")
            List<Object[]> commandes = query.getResultList();

            if (commandes.isEmpty()) {
                resultat.put("success", false);
                resultat.put("message", "Aucune commande à planifier");
                return ResponseEntity.ok(resultat);
            }

            // 3. Assignation simple : distribuer les commandes aux employés
            int commandesAssignees = 0;
            int employeIndex = 0;

            for (Object[] commande : commandes) {
                String commandeId = (String) commande[0];
                String numeroCommande = (String) commande[1];
                Number dureeMinutes = (Number) commande[2];

                // Employé en rotation
                Map<String, Object> employe = employes.get(employeIndex % employes.size());
                String employeId = (String) employe.get("id");
                String employeNom = employe.get("nom") + " " + employe.get("prenom");

                // Créer l'assignation (version simplifiée - stockage en mémoire)
                System.out.println("➡️ Assignation: Commande " + numeroCommande +
                        " → Employé " + employeNom +
                        " (durée: " + dureeMinutes + " min)");

                commandesAssignees++;
                employeIndex++;
            }

            resultat.put("success", true);
            resultat.put("message", "Planification simple terminée");
            resultat.put("commandesAssignees", commandesAssignees);
            resultat.put("employesUtilises", employes.size());
            resultat.put("methodePlanification", "ROTATION_SIMPLE");

            return ResponseEntity.ok(resultat);

        } catch (Exception e) {
            System.err.println("❌ Erreur planification simple: " + e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("erreur", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 📊 DONNÉES EMPLOYÉS AVEC COMMANDES SIMULÉES
     */
    @GetMapping("/employes-avec-commandes")
    public ResponseEntity<List<Map<String, Object>>> getEmployesAvecCommandes() {
        try {
            System.out.println("📊 Génération employés avec commandes simulées");

            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();

            // Récupérer quelques commandes réelles pour simulation
            String sqlCommandes = """
            SELECT 
                HEX(id) as commandeId,
                num_commande as numeroCommande,
                COALESCE(temps_estime_minutes, 120) as dureeMinutes,
                date
            FROM `order`
            WHERE status IN (1, 2) 
            AND date >= '2025-06-01'
            ORDER BY date ASC
            LIMIT 15
            """;

            Query query = entityManager.createNativeQuery(sqlCommandes);
            @SuppressWarnings("unchecked")
            List<Object[]> commandesData = query.getResultList();

            // Distribuer les commandes aux employés
            List<Map<String, Object>> employesAvecCommandes = new ArrayList<>();

            for (int i = 0; i < employes.size(); i++) {
                Map<String, Object> employe = employes.get(i);
                Map<String, Object> employeAvecCommandes = new HashMap<>(employe);

                // Calculer les commandes pour cet employé
                List<Map<String, Object>> commandesEmploye = new ArrayList<>();
                int tempsTotal = 0;
                int totalCartes = 0;

                // Prendre quelques commandes pour cet employé
                for (int j = i; j < commandesData.size(); j += employes.size()) {
                    if (commandesEmploye.size() >= 3) break; // Max 3 commandes par employé

                    Object[] commande = commandesData.get(j);
                    Map<String, Object> commandeMap = new HashMap<>();
                    commandeMap.put("id", commande[0]);
                    commandeMap.put("numeroCommande", commande[1]);
                    commandeMap.put("dureeMinutes", ((Number) commande[2]).intValue());
                    commandeMap.put("date", commande[3]);
                    commandeMap.put("heureDebut", "09:00");
                    commandeMap.put("heureFin", "11:00");
                    commandeMap.put("nombreCartes", 15 + (j * 3)); // Simulation
                    commandeMap.put("terminee", false);

                    commandesEmploye.add(commandeMap);
                    tempsTotal += ((Number) commande[2]).intValue();
                    totalCartes += 15 + (j * 3);
                }

                employeAvecCommandes.put("commandes", commandesEmploye);
                employeAvecCommandes.put("nombreCommandes", commandesEmploye.size());
                employeAvecCommandes.put("tempsTotal", tempsTotal);
                employeAvecCommandes.put("totalCartes", totalCartes);
                employeAvecCommandes.put("pourcentageCharge", Math.min(100, (tempsTotal * 100) / (8 * 60))); // 8h de travail
                employeAvecCommandes.put("status", tempsTotal > 400 ? "CHARGE" : "DISPONIBLE");

                employesAvecCommandes.add(employeAvecCommandes);
            }

            System.out.println("✅ " + employesAvecCommandes.size() + " employés avec commandes générés");
            return ResponseEntity.ok(employesAvecCommandes);

        } catch (Exception e) {
            System.err.println("❌ Erreur génération employés: " + e.getMessage());
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }


    @GetMapping("/diagnostic")
    public ResponseEntity<Map<String, Object>> diagnostic() {
        try {
            System.out.println("🔍 Diagnostic du système de planification");

            Map<String, Object> response = new HashMap<>();

            // Vérifier employés
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();
            Map<String, Object> employesInfo = new HashMap<>();
            employesInfo.put("nombreActifs", employes.size());
            employesInfo.put("liste", employes.stream()
                    .map(e -> e.get("nom") + " " + e.get("prenom"))
                    .toList());

            // Vérifier commandes
            String sqlCommandes = """
                SELECT 
                    COUNT(*) as totalSysteme,
                    COUNT(CASE WHEN status IN (1, 2) THEN 1 END) as enAttente,
                    COUNT(CASE WHEN status IN (1, 2) AND date >= '2025-06-01' THEN 1 END) as aPlanifierDepuisMois
                FROM `order`
                WHERE COALESCE(annulee, 0) = 0
                """;

            Query query = entityManager.createNativeQuery(sqlCommandes);
            @SuppressWarnings("unchecked")
            List<Object[]> resultatCommandes = query.getResultList();

            Map<String, Object> commandesInfo = new HashMap<>();
            if (!resultatCommandes.isEmpty()) {
                Object[] row = resultatCommandes.get(0);
                commandesInfo.put("totalSysteme", ((Number) row[0]).intValue());
                commandesInfo.put("enAttente", ((Number) row[1]).intValue());
                commandesInfo.put("aPlanifierDepuisMois", ((Number) row[2]).intValue());
            }

            response.put("success", true);
            response.put("employes", employesInfo);
            response.put("commandes", commandesInfo);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("erreur", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    @GetMapping("/test-rapide")
    public ResponseEntity<Map<String, Object>> testRapide() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Test rapide OK");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }


    /**
     * 🔍 ENDPOINT DEBUG STRUCTURE - À ajouter dans TestPlanificationController.java
     */
    @GetMapping("/debug-structure-order")
    public ResponseEntity<Map<String, Object>> debugStructureOrder() {
        try {
            System.out.println("🔍 === DEBUG STRUCTURE TABLE ORDER ===");

            Map<String, Object> debug = new HashMap<>();

            // 1. Vérifier que la table existe
            String sqlTables = "SHOW TABLES LIKE 'order'";
            Query queryTables = entityManager.createNativeQuery(sqlTables);
            @SuppressWarnings("unchecked")
            List<String> tables = queryTables.getResultList();

            debug.put("table_order_existe", !tables.isEmpty());
            System.out.println("Table 'order' existe: " + !tables.isEmpty());

            if (tables.isEmpty()) {
                debug.put("erreur", "Table 'order' n'existe pas");
                return ResponseEntity.ok(debug);
            }

            // 2. Décrire la structure de la table
            String sqlDesc = "DESCRIBE `order`";
            Query queryDesc = entityManager.createNativeQuery(sqlDesc);
            @SuppressWarnings("unchecked")
            List<Object[]> colonnes = queryDesc.getResultList();

            Map<String, String> structureTable = new HashMap<>();
            List<String> nomsColonnes = new ArrayList<>();
            for (Object[] col : colonnes) {
                String nomColonne = (String) col[0];
                String typeColonne = (String) col[1];
                structureTable.put(nomColonne, typeColonne);
                nomsColonnes.add(nomColonne);
                System.out.println("  - " + nomColonne + " (" + typeColonne + ")");
            }
            debug.put("structure_table", structureTable);
            debug.put("colonnes_disponibles", nomsColonnes);

            // 3. Compter le nombre total de commandes
            String sqlCount = "SELECT COUNT(*) FROM `order`";
            Query queryCount = entityManager.createNativeQuery(sqlCount);
            Object totalResult = queryCount.getSingleResult();
            Long totalCommandes = ((Number) totalResult).longValue();

            debug.put("total_commandes", totalCommandes);
            System.out.println("Total commandes dans la table: " + totalCommandes);

            // 4. Échantillon de données (5 premières commandes)
            String sqlEchantillon = "SELECT * FROM `order` LIMIT 5";
            Query queryEchantillon = entityManager.createNativeQuery(sqlEchantillon);
            @SuppressWarnings("unchecked")
            List<Object[]> echantillon = queryEchantillon.getResultList();

            List<Map<String, Object>> echantillonData = new ArrayList<>();
            for (Object[] row : echantillon) {
                Map<String, Object> ligne = new HashMap<>();
                for (int i = 0; i < Math.min(row.length, nomsColonnes.size()); i++) {
                    ligne.put(nomsColonnes.get(i), row[i]);
                }
                echantillonData.add(ligne);
            }
            debug.put("echantillon_donnees", echantillonData);

            // 5. Statistiques des statuts
            String sqlStatuts = """
            SELECT 
                status,
                COUNT(*) as nombre
            FROM `order`
            GROUP BY status
            ORDER BY status
            """;

            Query queryStatuts = entityManager.createNativeQuery(sqlStatuts);
            @SuppressWarnings("unchecked")
            List<Object[]> statuts = queryStatuts.getResultList();

            Map<String, Integer> statutsMap = new HashMap<>();
            for (Object[] row : statuts) {
                statutsMap.put(String.valueOf(row[0]), ((Number) row[1]).intValue());
            }
            debug.put("repartition_statuts", statutsMap);

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            System.err.println("❌ Erreur debug structure: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("erreur", e.getMessage()));
        }
    }

    /**
     * 🚀 PLANIFICATION SIMPLE CORRIGÉE - Avec les vraies colonnes
     */
    @PostMapping("/planifier-simple-corrige")
    public ResponseEntity<Map<String, Object>> planifierSimpleCorrige() {
        try {
            System.out.println("🚀 === PLANIFICATION SIMPLE CORRIGÉE ===");

            Map<String, Object> resultat = new HashMap<>();

            // 1. Récupérer les employés
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();
            if (employes.isEmpty()) {
                resultat.put("success", false);
                resultat.put("message", "Aucun employé disponible");
                return ResponseEntity.ok(resultat);
            }

            // 2. Récupérer des commandes avec seulement les colonnes qui existent
            String sqlCommandes = """
            SELECT 
                HEX(id) as commandeId,
                num_commande as numeroCommande,
                date,
                status,
                COALESCE(delai, '7') as delai
            FROM `order`
            WHERE status IN (1, 2) 
            AND date >= '2025-06-01'
            ORDER BY date ASC
            LIMIT 20
            """;

            Query query = entityManager.createNativeQuery(sqlCommandes);
            @SuppressWarnings("unchecked")
            List<Object[]> commandes = query.getResultList();

            if (commandes.isEmpty()) {
                resultat.put("success", false);
                resultat.put("message", "Aucune commande à planifier");
                return ResponseEntity.ok(resultat);
            }

            // 3. Assignation simple : distribuer les commandes aux employés
            int commandesAssignees = 0;
            int employeIndex = 0;
            List<Map<String, Object>> assignations = new ArrayList<>();

            for (Object[] commande : commandes) {
                String commandeId = (String) commande[0];
                String numeroCommande = (String) commande[1];
                Object date = commande[2];
                Object status = commande[3];
                String delai = (String) commande[4];

                // Employé en rotation
                Map<String, Object> employe = employes.get(employeIndex % employes.size());
                String employeId = (String) employe.get("id");
                String employeNom = employe.get("nom") + " " + employe.get("prenom");

                // Estimation de durée simple (3 minutes par carte estimée)
                int dureeEstimee = 120; // 2h par défaut

                Map<String, Object> assignation = new HashMap<>();
                assignation.put("commandeId", commandeId);
                assignation.put("numeroCommande", numeroCommande);
                assignation.put("employeId", employeId);
                assignation.put("employeNom", employeNom);
                assignation.put("date", date);
                assignation.put("dureeEstimee", dureeEstimee);
                assignation.put("delai", delai);
                assignations.add(assignation);

                System.out.println("➡️ Assignation: Commande " + numeroCommande +
                        " → Employé " + employeNom +
                        " (durée: " + dureeEstimee + " min)");

                commandesAssignees++;
                employeIndex++;
            }

            resultat.put("success", true);
            resultat.put("message", "Planification simple corrigée terminée");
            resultat.put("commandesAssignees", commandesAssignees);
            resultat.put("employesUtilises", employes.size());
            resultat.put("assignations", assignations);
            resultat.put("methodePlanification", "ROTATION_SIMPLE_CORRIGEE");

            return ResponseEntity.ok(resultat);

        } catch (Exception e) {
            System.err.println("❌ Erreur planification simple corrigée: " + e.getMessage());
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("erreur", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * 📊 EMPLOYÉS AVEC COMMANDES CORRIGÉES
     */
    @PostMapping("/generer-employes-avec-commandes")
    public ResponseEntity<List<Map<String, Object>>> genererEmployesAvecCommandes() {
        try {
            System.out.println("📊 Génération employés avec commandes (version corrigée)");

            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();

            if (employes.isEmpty()) {
                return ResponseEntity.ok(new ArrayList<>());
            }

            // Récupérer des commandes réelles avec les bonnes colonnes
            String sqlCommandes = """
            SELECT 
                HEX(id) as commandeId,
                num_commande as numeroCommande,
                date,
                status
            FROM `order`
            WHERE status IN (1, 2) 
            AND date >= '2025-06-01'
            ORDER BY date ASC
            LIMIT 18
            """;

            Query query = entityManager.createNativeQuery(sqlCommandes);
            @SuppressWarnings("unchecked")
            List<Object[]> commandesData = query.getResultList();

            // Distribuer les commandes aux employés
            List<Map<String, Object>> employesAvecCommandes = new ArrayList<>();

            for (int i = 0; i < employes.size(); i++) {
                Map<String, Object> employe = employes.get(i);
                Map<String, Object> employeAvecCommandes = new HashMap<>(employe);

                // Calculer les commandes pour cet employé
                List<Map<String, Object>> commandesEmploye = new ArrayList<>();
                int tempsTotal = 0;
                int totalCartes = 0;

                // Prendre quelques commandes pour cet employé
                for (int j = i; j < commandesData.size(); j += employes.size()) {
                    if (commandesEmploye.size() >= 3) break; // Max 3 commandes par employé

                    Object[] commande = commandesData.get(j);
                    Map<String, Object> commandeMap = new HashMap<>();
                    commandeMap.put("id", commande[0]);
                    commandeMap.put("numeroCommande", commande[1]);

                    // Estimation de durée et cartes
                    int dureeEstimee = 90 + (j * 30); // Durée variable
                    int nombreCartes = 10 + (j * 5); // Nombre de cartes variable

                    commandeMap.put("dureeMinutes", dureeEstimee);
                    commandeMap.put("dureeCalculee", dureeEstimee);
                    commandeMap.put("date", commande[2]);
                    commandeMap.put("heureDebut", String.format("%02d:00", 9 + (commandesEmploye.size() * 2)));
                    commandeMap.put("heureFin", String.format("%02d:30", 9 + (commandesEmploye.size() * 2) + 1));
                    commandeMap.put("nombreCartes", nombreCartes);
                    commandeMap.put("terminee", false);
                    commandeMap.put("priorite", "NORMALE");
                    commandeMap.put("status", commande[3]);

                    commandesEmploye.add(commandeMap);
                    tempsTotal += dureeEstimee;
                    totalCartes += nombreCartes;
                }

                employeAvecCommandes.put("commandes", commandesEmploye);
                employeAvecCommandes.put("nombreCommandes", commandesEmploye.size());
                employeAvecCommandes.put("tempsTotal", tempsTotal);
                employeAvecCommandes.put("totalCartes", totalCartes);
                employeAvecCommandes.put("pourcentageCharge", Math.min(100, (tempsTotal * 100) / (8 * 60))); // 8h de travail
                employeAvecCommandes.put("status", tempsTotal > 400 ? "CHARGE" : "DISPONIBLE");

                employesAvecCommandes.add(employeAvecCommandes);
            }

            System.out.println("✅ " + employesAvecCommandes.size() + " employés avec commandes générés");
            return ResponseEntity.ok(employesAvecCommandes);

        } catch (Exception e) {
            System.err.println("❌ Erreur génération employés: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }
}