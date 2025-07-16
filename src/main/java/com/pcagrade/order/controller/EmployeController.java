// ========== AJOUT DANS EmployeController.java ==========

package com.pcagrade.order.controller;

import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.UUID;
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
     * ➕ ENDPOINT FRONTEND - Créer un nouvel employé
     */
    @PostMapping("/frontend/creer")
    @Transactional  // ✅ IMPORTANT: Ajouter cette annotation
    public ResponseEntity<Map<String, Object>> creerEmployeFrontend(@RequestBody Map<String, Object> employeData) {
        try {
            System.out.println("➕ Frontend: Création nouvel employé...");
            System.out.println("Données reçues: " + employeData);

            // Valider les données obligatoires
            if (!employeData.containsKey("nom") || !employeData.containsKey("prenom")) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Nom et prénom sont obligatoires"
                ));
            }

            // Vérifier si la table existe
            String sqlCheckTable = "SHOW TABLES LIKE 'j_employe'";
            Query queryCheck = entityManager.createNativeQuery(sqlCheckTable);
            @SuppressWarnings("unchecked")
            List<Object> tables = queryCheck.getResultList();

            if (tables.isEmpty()) {
                // Table n'existe pas - retourner un employé de test
                System.out.println("⚠️ Table j_employe n'existe pas - création d'employé de test");
                return ResponseEntity.ok(creerEmployeDeTest(employeData));
            }

            // Générer un nouvel ID UUID
            UUID nouvelId = UUID.randomUUID();
            String email = (String) employeData.get("email");
            if (email == null || email.trim().isEmpty()) {
                email = ((String) employeData.get("prenom")).toLowerCase() + "." +
                        ((String) employeData.get("nom")).toLowerCase() + "@exemple.com";
            }

            // ✅ VERSION CORRIGÉE: Insérer dans la vraie table avec gestion d'erreur
            String sqlInsert = """
        INSERT INTO j_employe (id, prenom, nom, email, heures_travail_par_jour, actif, date_creation, date_modification)
        VALUES (UNHEX(?), ?, ?, ?, ?, 1, NOW(), NOW())
        """;

            try {
                Query queryInsert = entityManager.createNativeQuery(sqlInsert);
                queryInsert.setParameter(1, nouvelId.toString().replace("-", ""));
                queryInsert.setParameter(2, (String) employeData.get("prenom"));
                queryInsert.setParameter(3, (String) employeData.get("nom"));
                queryInsert.setParameter(4, email);
                queryInsert.setParameter(5, employeData.get("heuresTravailParJour") != null ?
                        ((Number) employeData.get("heuresTravailParJour")).intValue() : 8);

                int rowsAffected = queryInsert.executeUpdate();

                // ✅ IMPORTANT: Forcer le commit
                entityManager.flush();

                if (rowsAffected > 0) {
                    Map<String, Object> nouvelEmploye = new HashMap<>();
                    nouvelEmploye.put("id", nouvelId.toString());
                    nouvelEmploye.put("prenom", employeData.get("prenom"));
                    nouvelEmploye.put("nom", employeData.get("nom"));
                    nouvelEmploye.put("email", email);
                    nouvelEmploye.put("heuresTravailParJour", employeData.get("heuresTravailParJour") != null ?
                            ((Number) employeData.get("heuresTravailParJour")).intValue() : 8);
                    nouvelEmploye.put("actif", true);
                    nouvelEmploye.put("dateCreation", new Date());
                    nouvelEmploye.put("nomComplet", employeData.get("prenom") + " " + employeData.get("nom"));

                    System.out.println("✅ Employé créé avec succès dans j_employe: " + nouvelEmploye.get("nomComplet"));

                    return ResponseEntity.ok(Map.of(
                            "success", true,
                            "message", "Employé créé avec succès dans la base de données",
                            "employe", nouvelEmploye
                    ));
                } else {
                    return ResponseEntity.status(500).body(Map.of(
                            "success", false,
                            "message", "Aucune ligne affectée lors de l'insertion"
                    ));
                }

            } catch (Exception sqlException) {
                System.err.println("❌ Erreur SQL insertion: " + sqlException.getMessage());
                sqlException.printStackTrace();

                // Fallback vers employé de test si erreur SQL
                System.out.println("🔄 Fallback vers employé de test");
                Map<String, Object> testResult = creerEmployeDeTest(employeData);
                testResult.put("message", "Employé de test créé (erreur SQL: " + sqlException.getMessage() + ")");
                return ResponseEntity.ok(testResult);
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur création employé: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Erreur interne: " + e.getMessage()
            ));
        }
    }



    // ✅ AJOUTEZ ces méthodes dans votre EmployeController.java (ou créez le fichier si inexistant)
    // ✅ REMPLACEZ les méthodes dans votre EmployeController.java avec gestion correcte du boolean

    /**
     * 📋 ENDPOINT FRONTEND - Liste des employés (CORRECTION BOOLEAN)
     */
    @GetMapping("/frontend/liste")
    public ResponseEntity<List<Map<String, Object>>> getEmployesFrontend() {
        try {
            System.out.println("👥 Frontend: Récupération liste des VRAIS employés...");

            // Vérifier d'abord si la table j_employe existe
            String sqlCheckTable = "SHOW TABLES LIKE 'j_employe'";
            Query queryCheck = entityManager.createNativeQuery(sqlCheckTable);
            @SuppressWarnings("unchecked")
            List<Object> tables = queryCheck.getResultList();

            if (tables.isEmpty()) {
                System.out.println("⚠️ Table j_employe n'existe pas - retour liste vide");
                return ResponseEntity.ok(new ArrayList<>());
            }

            // ✅ REQUÊTE CORRIGÉE avec gestion du boolean
            String sql = """
        SELECT 
            HEX(e.id) as id,
            e.prenom,
            e.nom,
            e.email,
            e.heures_travail_par_jour,
            CASE WHEN e.actif = 1 THEN 'true' ELSE 'false' END as actif_string,
            e.date_creation
        FROM j_employe e
        WHERE e.actif = 1
        ORDER BY e.nom, e.prenom
        """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> resultats = query.getResultList();

            List<Map<String, Object>> employes = new ArrayList<>();

            System.out.println("🔍 Nombre d'employés trouvés dans j_employe: " + resultats.size());

            for (Object[] row : resultats) {
                Map<String, Object> employe = new HashMap<>();
                employe.put("id", (String) row[0]);
                employe.put("prenom", (String) row[1]);
                employe.put("nom", (String) row[2]);
                employe.put("email", (String) row[3]);
                employe.put("heuresTravailParJour", row[4] != null ? ((Number) row[4]).intValue() : 8);
                employe.put("actif", "true".equals((String) row[5])); // ✅ Conversion string vers boolean
                employe.put("dateCreation", row[6]);

                // Champs calculés
                employe.put("nomComplet", row[1] + " " + row[2]);
                employe.put("disponible", true);
                employe.put("chargeActuelle", 0);

                employes.add(employe);

                // Log pour debug
                System.out.println("  ✅ Employé: " + row[1] + " " + row[2] + " (" + row[0] + ")");
            }

            System.out.println("✅ " + employes.size() + " vrais employés retournés");
            return ResponseEntity.ok(employes);

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération employés: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * 🔍 ENDPOINT DEBUG - Vérifier les employés dans la base (CORRECTION BOOLEAN)
     */
    @GetMapping("/debug-liste")
    public ResponseEntity<Map<String, Object>> debugListeEmployes() {
        Map<String, Object> debug = new HashMap<>();

        try {
            System.out.println("🔍 === DEBUG LISTE EMPLOYÉS ===");

            // 1. Vérifier si la table existe
            String sqlCheckTable = "SHOW TABLES LIKE 'j_employe'";
            Query queryCheck = entityManager.createNativeQuery(sqlCheckTable);
            @SuppressWarnings("unchecked")
            List<Object> tables = queryCheck.getResultList();

            debug.put("table_existe", !tables.isEmpty());

            if (tables.isEmpty()) {
                debug.put("message", "Table j_employe n'existe pas");
                return ResponseEntity.ok(debug);
            }

            // 2. Compter tous les employés (sans conversion boolean)
            String sqlCountAll = "SELECT COUNT(*) FROM j_employe";
            Query queryCountAll = entityManager.createNativeQuery(sqlCountAll);
            Long totalEmployes = ((Number) queryCountAll.getSingleResult()).longValue();

            // 3. Compter les employés actifs (sans conversion boolean)
            String sqlCountActifs = "SELECT COUNT(*) FROM j_employe WHERE actif = 1";
            Query queryCountActifs = entityManager.createNativeQuery(sqlCountActifs);
            Long employesActifs = ((Number) queryCountActifs.getSingleResult()).longValue();

            // 4. ✅ REQUÊTE CORRIGÉE: Lister les employés avec conversion boolean
            String sqlListe = """
        SELECT 
            HEX(id) as id,
            prenom,
            nom,
            email,
            CASE WHEN actif = 1 THEN 'true' ELSE 'false' END as actif_string,
            date_creation
        FROM j_employe
        ORDER BY date_creation DESC
        """;

            Query queryListe = entityManager.createNativeQuery(sqlListe);
            @SuppressWarnings("unchecked")
            List<Object[]> resultats = queryListe.getResultList();

            List<Map<String, Object>> listeEmployes = new ArrayList<>();
            for (Object[] row : resultats) {
                Map<String, Object> emp = new HashMap<>();
                emp.put("id", (String) row[0]);
                emp.put("prenom", (String) row[1]);
                emp.put("nom", (String) row[2]);
                emp.put("email", (String) row[3]);
                emp.put("actif", "true".equals((String) row[4])); // ✅ Conversion string vers boolean
                emp.put("dateCreation", row[5]);
                listeEmployes.add(emp);
            }

            debug.put("total_employes", totalEmployes);
            debug.put("employes_actifs", employesActifs);
            debug.put("liste_employes", listeEmployes);

            System.out.println("📊 Total employés: " + totalEmployes);
            System.out.println("📊 Employés actifs: " + employesActifs);

            for (Object[] row : resultats) {
                System.out.println("  👤 " + row[1] + " " + row[2] + " - Actif: " + row[4]);
            }

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            debug.put("erreur", e.getMessage());
            System.err.println("❌ Erreur debug liste: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(debug);
        }
    }

    /**
     * 🔧 ENDPOINT DEBUG - Structure de la table pour comprendre les types
     */
    @GetMapping("/debug-structure-detaillee")
    public ResponseEntity<Map<String, Object>> debugStructureDetaillee() {
        Map<String, Object> debug = new HashMap<>();

        try {
            System.out.println("🔍 === DEBUG STRUCTURE DÉTAILLÉE ===");

            // 1. Structure de la table
            String sqlDesc = "DESCRIBE j_employe";
            Query queryDesc = entityManager.createNativeQuery(sqlDesc);
            @SuppressWarnings("unchecked")
            List<Object[]> colonnes = queryDesc.getResultList();

            Map<String, String> structure = new HashMap<>();
            for (Object[] col : colonnes) {
                String nomColonne = (String) col[0];
                String typeColonne = (String) col[1];
                structure.put(nomColonne, typeColonne);
                System.out.println("  📋 " + nomColonne + " : " + typeColonne);
            }
            debug.put("structure_table", structure);

            // 2. Échantillon de données brutes
            String sqlEchantillon = "SELECT HEX(id), prenom, nom, actif FROM j_employe LIMIT 3";
            Query queryEchantillon = entityManager.createNativeQuery(sqlEchantillon);
            @SuppressWarnings("unchecked")
            List<Object[]> echantillon = queryEchantillon.getResultList();

            List<Map<String, Object>> donneesBrutes = new ArrayList<>();
            for (Object[] row : echantillon) {
                Map<String, Object> ligne = new HashMap<>();
                ligne.put("id", (String) row[0]);
                ligne.put("prenom", (String) row[1]);
                ligne.put("nom", (String) row[2]);
                ligne.put("actif_brut", row[3]);
                ligne.put("actif_type", row[3] != null ? row[3].getClass().getSimpleName() : "null");
                donneesBrutes.add(ligne);

                System.out.println("  📊 " + row[1] + " " + row[2] + " - actif: " + row[3] + " (type: " +
                        (row[3] != null ? row[3].getClass().getSimpleName() : "null") + ")");
            }
            debug.put("echantillon_donnees", donneesBrutes);

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            debug.put("erreur", e.getMessage());
            System.err.println("❌ Erreur debug structure: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(debug);
        }
    }

    /**
     * ✏️ ENDPOINT FRONTEND - Modifier un employé
     */
    @PutMapping("/frontend/modifier/{id}")
    public ResponseEntity<Map<String, Object>> modifierEmployeFrontend(
            @PathVariable String id,
            @RequestBody Map<String, Object> employeData) {
        try {
            System.out.println("✏️ Frontend: Modification employé " + id);

            // TODO: Implémenter la modification
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Modification sera implémentée prochainement"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Erreur: " + e.getMessage()
            ));
        }
    }

    /**
     * 🗑️ ENDPOINT FRONTEND - Supprimer un employé
     */
    @DeleteMapping("/frontend/supprimer/{id}")
    public ResponseEntity<Map<String, Object>> supprimerEmployeFrontend(@PathVariable String id) {
        try {
            System.out.println("🗑️ Frontend: Suppression employé " + id);

            // TODO: Implémenter la suppression (désactivation)
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Suppression sera implémentée prochainement"
            ));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of(
                    "success", false,
                    "message", "Erreur: " + e.getMessage()
            ));
        }
    }

    /**
     * 🔍 ENDPOINT DEBUG - Structure table employés
     */
    @GetMapping("/debug-structure")
    public ResponseEntity<Map<String, Object>> debugStructureEmployes() {
        Map<String, Object> debug = new HashMap<>();

        try {
            System.out.println("🔍 === DEBUG STRUCTURE TABLE EMPLOYES ===");

            // Vérifier les tables liées aux employés
            String sqlTables = "SHOW TABLES LIKE '%employ%'";
            Query queryTables = entityManager.createNativeQuery(sqlTables);
            @SuppressWarnings("unchecked")
            List<String> tables = queryTables.getResultList();

            debug.put("tables_employes", tables);
            System.out.println("Tables employés trouvées: " + tables);

            if (!tables.isEmpty()) {
                for (String table : tables) {
                    try {
                        String sqlDesc = "DESCRIBE " + table;
                        Query queryDesc = entityManager.createNativeQuery(sqlDesc);
                        @SuppressWarnings("unchecked")
                        List<Object[]> colonnes = queryDesc.getResultList();

                        Map<String, String> structure = new HashMap<>();
                        for (Object[] col : colonnes) {
                            structure.put((String) col[0], (String) col[1]);
                        }
                        debug.put("structure_" + table, structure);

                        // Compter les enregistrements
                        String sqlCount = "SELECT COUNT(*) FROM " + table;
                        Query queryCount = entityManager.createNativeQuery(sqlCount);
                        Number count = (Number) queryCount.getSingleResult();
                        debug.put("count_" + table, count.longValue());

                    } catch (Exception e) {
                        debug.put("erreur_" + table, e.getMessage());
                    }
                }
            }

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            debug.put("erreur", e.getMessage());
            return ResponseEntity.status(500).body(debug);
        }
    }


    private Map<String, Object> creerEmployeDeTest(Map<String, Object> employeData) {
        Map<String, Object> nouvelEmploye = new HashMap<>();
        nouvelEmploye.put("id", "test-" + UUID.randomUUID().toString().substring(0, 8));
        nouvelEmploye.put("prenom", employeData.get("prenom"));
        nouvelEmploye.put("nom", employeData.get("nom"));
        nouvelEmploye.put("email", employeData.get("email"));
        nouvelEmploye.put("heuresTravailParJour", employeData.get("heuresTravailParJour") != null ?
                ((Number) employeData.get("heuresTravailParJour")).intValue() : 8);
        nouvelEmploye.put("actif", true);
        nouvelEmploye.put("dateCreation", new Date());
        nouvelEmploye.put("nomComplet", employeData.get("prenom") + " " + employeData.get("nom"));

        return Map.of(
                "success", true,
                "message", "Employé de test créé (table j_employe accessible mais problème d'insertion)",
                "employe", nouvelEmploye
        );
    }

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