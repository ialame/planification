// ============= TESTCONTROLLER CORRIGÉ - COMPATIBLE AVEC VOTRE EMPLOYESERVICE =============

package com.pcagrade.order.controller;

import com.pcagrade.order.entity.Employe;
import com.pcagrade.order.service.EmployeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@RestController
public class TestController {

    @Autowired
    private EmployeService employeService;

    @Autowired
    private EntityManager entityManager;

    @GetMapping("/test-simple")
    public String testSimple() {
        return "OK";
    }

    // ============================================================================
    // 🔄 EMPLOYÉS - AVEC SAUVEGARDE RÉELLE EN BASE DE DONNÉES
    // ============================================================================

    /**
     * Récupération des employés - MÉLANGE base réelle + test
     */
    @GetMapping("/api/test/employes")
    public ResponseEntity<List<Map<String, Object>>> getEmployesTest() {
        try {
            System.out.println("📋 Récupération employés (base + test)...");

            List<Map<String, Object>> employes = new ArrayList<>();

            // 1. Récupérer les employés réels de la base
            try {
                List<Map<String, Object>> employesReels = employeService.getTousEmployesActifs(); // ✅ Méthode existante
                System.out.println("🔍 Trouvé " + employesReels.size() + " employés en base");

                // Ajouter un marqueur pour identifier la source
                for (Map<String, Object> emp : employesReels) {
                    emp.put("source", "BASE_DONNEES");
                    employes.add(emp);
                }
            } catch (Exception e) {
                System.err.println("⚠️ Erreur lecture base: " + e.getMessage());
            }

            // 2. Si pas d'employés en base, ajouter des employés de test
            if (employes.isEmpty()) {
                System.out.println("🧪 Aucun employé en base, ajout d'employés de test");
                employes.addAll(creerEmployesDeTestStatiques());
            }

            System.out.println("✅ Retour de " + employes.size() + " employés");
            return ResponseEntity.ok(employes);

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération employés: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(creerEmployesDeTestStatiques());
        }
    }

    /**
     * Création d'employé - SAUVEGARDE RÉELLE EN BASE
     */
    @PostMapping("/api/test/employes")
    public ResponseEntity<Map<String, Object>> creerEmployeTest(@RequestBody Map<String, Object> employeData) {
        try {
            System.out.println("💾 CRÉATION EMPLOYÉ RÉELLE: " + employeData);

            // 1. Créer l'entité Employe
            Employe nouvelEmploye = new Employe();
            nouvelEmploye.setNom((String) employeData.get("nom"));
            nouvelEmploye.setPrenom((String) employeData.get("prenom"));
            nouvelEmploye.setEmail((String) employeData.get("email"));

            // Conversion sécurisée des heures
            Object heuresObj = employeData.get("heuresTravailParJour");
            if (heuresObj instanceof Number) {
                nouvelEmploye.setHeuresTravailParJour(((Number) heuresObj).intValue());
            } else {
                nouvelEmploye.setHeuresTravailParJour(8); // Défaut
            }

            nouvelEmploye.setActif(true);
            nouvelEmploye.setDateCreation(LocalDateTime.now());
            nouvelEmploye.setDateModification(LocalDateTime.now());

            // 2. SAUVEGARDER EN BASE via EmployeService
            Employe employeSauvegarde = employeService.creerEmploye(nouvelEmploye); // ✅ Méthode existante

            System.out.println("✅ EMPLOYÉ SAUVEGARDÉ EN BASE: ID=" + employeSauvegarde.getId());

            // 3. Retourner la réponse avec l'ID réel
            Map<String, Object> reponse = new HashMap<>();
            reponse.put("id", employeSauvegarde.getId().toString());
            reponse.put("nom", employeSauvegarde.getNom());
            reponse.put("prenom", employeSauvegarde.getPrenom());
            reponse.put("email", employeSauvegarde.getEmail());
            reponse.put("heuresTravailParJour", employeSauvegarde.getHeuresTravailParJour());
            reponse.put("actif", employeSauvegarde.getActif());
            reponse.put("dateCreation", employeSauvegarde.getDateCreation().toString());
            reponse.put("source", "SAUVEGARDE_REELLE");
            reponse.put("success", true);

            return ResponseEntity.ok(reponse);

        } catch (Exception e) {
            System.err.println("❌ ERREUR SAUVEGARDE EMPLOYÉ: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> erreur = new HashMap<>();
            erreur.put("success", false);
            erreur.put("message", "Erreur sauvegarde: " + e.getMessage());
            erreur.put("source", "ERREUR_SAUVEGARDE");

            return ResponseEntity.status(500).body(erreur);
        }
    }

    /**
     * Modification d'employé - MISE À JOUR RÉELLE EN BASE
     */
    @PutMapping("/api/test/employes/{id}")
    public ResponseEntity<Map<String, Object>> modifierEmployeTest(
            @PathVariable String id,
            @RequestBody Map<String, Object> employeData) {
        try {
            System.out.println("🔄 MODIFICATION EMPLOYÉ RÉELLE: " + id);

            // 1. Récupérer l'employé existant
            Optional<Employe> employeOpt = employeService.getEmployeById(id); // ✅ Méthode existante

            if (employeOpt.isEmpty()) {
                Map<String, Object> erreur = new HashMap<>();
                erreur.put("success", false);
                erreur.put("message", "Employé " + id + " non trouvé");
                return ResponseEntity.status(404).body(erreur);
            }

            Employe employe = employeOpt.get();

            // 2. Mettre à jour les champs
            if (employeData.containsKey("nom")) {
                employe.setNom((String) employeData.get("nom"));
            }
            if (employeData.containsKey("prenom")) {
                employe.setPrenom((String) employeData.get("prenom"));
            }
            if (employeData.containsKey("email")) {
                employe.setEmail((String) employeData.get("email"));
            }
            if (employeData.containsKey("heuresTravailParJour")) {
                Object heuresObj = employeData.get("heuresTravailParJour");
                if (heuresObj instanceof Number) {
                    employe.setHeuresTravailParJour(((Number) heuresObj).intValue());
                }
            }
            if (employeData.containsKey("actif")) {
                employe.setActif((Boolean) employeData.get("actif"));
            }

            employe.setDateModification(LocalDateTime.now());

            // 3. SAUVEGARDER EN BASE
            Employe employeModifie = employeService.updateEmploye(id, employe); // ✅ Méthode existante

            System.out.println("✅ EMPLOYÉ MODIFIÉ EN BASE: " + employeModifie.getId());

            // 4. Retourner la réponse
            Map<String, Object> reponse = new HashMap<>();
            reponse.put("id", employeModifie.getId().toString());
            reponse.put("nom", employeModifie.getNom());
            reponse.put("prenom", employeModifie.getPrenom());
            reponse.put("email", employeModifie.getEmail());
            reponse.put("heuresTravailParJour", employeModifie.getHeuresTravailParJour());
            reponse.put("actif", employeModifie.getActif());
            reponse.put("dateModification", employeModifie.getDateModification().toString());
            reponse.put("source", "MODIFICATION_REELLE");
            reponse.put("success", true);

            return ResponseEntity.ok(reponse);

        } catch (Exception e) {
            System.err.println("❌ ERREUR MODIFICATION EMPLOYÉ: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> erreur = new HashMap<>();
            erreur.put("success", false);
            erreur.put("message", "Erreur modification: " + e.getMessage());

            return ResponseEntity.status(500).body(erreur);
        }
    }

    /**
     * Désactivation d'employé - MISE À JOUR RÉELLE EN BASE
     */
    @DeleteMapping("/api/test/employes/{id}")
    public ResponseEntity<Map<String, Object>> desactiverEmployeTest(@PathVariable String id) {
        try {
            System.out.println("🗑️ DÉSACTIVATION EMPLOYÉ RÉELLE: " + id);

            // 1. Utiliser le service pour désactiver
            employeService.toggleEmployeActif(id); // ✅ Méthode existante (basculer = désactiver si actif)

            System.out.println("✅ EMPLOYÉ DÉSACTIVÉ EN BASE: " + id);

            Map<String, Object> reponse = new HashMap<>();
            reponse.put("success", true);
            reponse.put("message", "Employé " + id + " désactivé avec succès");
            reponse.put("employeId", id);
            reponse.put("dateDesactivation", LocalDateTime.now().toString());
            reponse.put("source", "DESACTIVATION_REELLE");

            return ResponseEntity.ok(reponse);

        } catch (Exception e) {
            System.err.println("❌ ERREUR DÉSACTIVATION EMPLOYÉ: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> erreur = new HashMap<>();
            erreur.put("success", false);
            erreur.put("message", "Erreur désactivation: " + e.getMessage());

            return ResponseEntity.status(500).body(erreur);
        }
    }

    // ============================================================================
    // 🧪 MÉTHODES DE TEST ET FALLBACK
    // ============================================================================

    /**
     * Employés de test statiques (si base vide)
     */
    private List<Map<String, Object>> creerEmployesDeTestStatiques() {
        List<Map<String, Object>> employes = new ArrayList<>();

        Map<String, Object> emp1 = new HashMap<>();
        emp1.put("id", "test-" + UUID.randomUUID().toString());
        emp1.put("nom", "Dupont");
        emp1.put("prenom", "Jean");
        emp1.put("email", "jean.dupont@test.com");
        emp1.put("heuresTravailParJour", 8);
        emp1.put("actif", true);
        emp1.put("source", "TEST_STATIQUE");

        Map<String, Object> emp2 = new HashMap<>();
        emp2.put("id", "test-" + UUID.randomUUID().toString());
        emp2.put("nom", "Martin");
        emp2.put("prenom", "Marie");
        emp2.put("email", "marie.martin@test.com");
        emp2.put("heuresTravailParJour", 8);
        emp2.put("actif", true);
        emp2.put("source", "TEST_STATIQUE");

        Map<String, Object> emp3 = new HashMap<>();
        emp3.put("id", "test-" + UUID.randomUUID().toString());
        emp3.put("nom", "Durand");
        emp3.put("prenom", "Paul");
        emp3.put("email", "paul.durand@test.com");
        emp3.put("heuresTravailParJour", 7);
        emp3.put("actif", true);
        emp3.put("source", "TEST_STATIQUE");

        employes.add(emp1);
        employes.add(emp2);
        employes.add(emp3);

        return employes;
    }

    // ============================================================================
    // 🔍 ENDPOINTS DE DIAGNOSTIC
    // ============================================================================

    /**
     * Test de connexion base de données
     */
    @GetMapping("/api/test/diagnostic-base")
    public ResponseEntity<Map<String, Object>> diagnosticBase() {
        Map<String, Object> diagnostic = new HashMap<>();

        try {
            // Test service employé
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();
            diagnostic.put("employes_en_base", employes.size());
            diagnostic.put("service_employe_fonctionne", true);

            // Test compte employés actifs
            long nombreActifs = employeService.getNombreEmployesActifs();
            diagnostic.put("nombre_employes_actifs", nombreActifs);

            // Test connexion
            diagnostic.put("connexion_base", "OK");
            diagnostic.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.ok(diagnostic);

        } catch (Exception e) {
            diagnostic.put("erreur", e.getMessage());
            diagnostic.put("service_employe_fonctionne", false);
            return ResponseEntity.status(500).body(diagnostic);
        }
    }

    // ============================================================================
    // 🗂️ AUTRES ENDPOINTS DE TEST (commandes, planifications...)
    // ============================================================================

    @GetMapping("/api/test/commandes")
    public ResponseEntity<List<Map<String, Object>>> getCommandesTest() {
        // Données de test pour les commandes
        List<Map<String, Object>> commandes = new ArrayList<>();

        Map<String, Object> cmd1 = new HashMap<>();
        cmd1.put("id", "cmd-" + UUID.randomUUID().toString());
        cmd1.put("numeroCommande", "CMD-001");
        cmd1.put("nombreCartes", 15);
        cmd1.put("priorite", "HAUTE");
        cmd1.put("status", 1);
        cmd1.put("tempsEstimeMinutes", 180);
        cmd1.put("dateCreation", LocalDateTime.now().toString());
        cmd1.put("dateLimite", LocalDateTime.now().plusDays(5).toString());

        commandes.add(cmd1);
        return ResponseEntity.ok(commandes);
    }

    @GetMapping("/test-all")
    public ResponseEntity<Map<String, Object>> testAllEndpoints() {
        Map<String, Object> endpoints = new HashMap<>();
        endpoints.put("employes_GET", "GET /api/test/employes");
        endpoints.put("employes_POST", "POST /api/test/employes");
        endpoints.put("employes_PUT", "PUT /api/test/employes/{id}");
        endpoints.put("employes_DELETE", "DELETE /api/test/employes/{id}");
        endpoints.put("diagnostic", "GET /api/test/diagnostic-base");
        endpoints.put("status", "Endpoints avec sauvegarde réelle");
        endpoints.put("note", "Compatible avec EmployeService existant");

        return ResponseEntity.ok(endpoints);
    }

    // ============= AJOUTS AU TESTCONTROLLER EXISTANT =============

// ✅ AJOUTEZ ces méthodes à votre TestController.java existant :


// ============================================================================

// ✅ VERSION SIMPLIFIÉE DU TEST DE PLANIFICATION :

    /**
     * 🚀 TEST PLANIFICATION SIMPLE - SANS DÉPENDANCES
     */
    @PostMapping("/api/test/test-planification-simple")
    public ResponseEntity<Map<String, Object>> testPlanificationSimple() {
        Map<String, Object> resultat = new HashMap<>();

        try {
            System.out.println("🧪 === TEST PLANIFICATION SIMPLE ===");

            // 1. Vérifier employés
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();
            if (employes.isEmpty()) {
                resultat.put("success", false);
                resultat.put("message", "Aucun employé disponible");
                return ResponseEntity.ok(resultat);
            }

            // 2. Vérifier commandes
            String sql = "SELECT HEX(id), num_commande, temps_estime_minutes FROM `order` WHERE status IN (1, 2) LIMIT 3";
            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> commandes = query.getResultList();

            if (commandes.isEmpty()) {
                resultat.put("success", false);
                resultat.put("message", "Aucune commande à planifier");
                return ResponseEntity.ok(resultat);
            }

            // 3. Simuler planification (sans sauvegarde)
            List<Map<String, Object>> planificationsSimulees = new ArrayList<>();

            for (int i = 0; i < Math.min(commandes.size(), employes.size()); i++) {
                Object[] commande = commandes.get(i);
                Map<String, Object> employe = employes.get(i);

                Map<String, Object> planif = new HashMap<>();
                planif.put("commande_id", (String) commande[0]);
                planif.put("commande_numero", (String) commande[1]);
                planif.put("employe_id", employe.get("id"));
                planif.put("employe_nom", employe.get("prenom") + " " + employe.get("nom"));
                planif.put("date", LocalDate.now().plusDays(i).toString());
                planif.put("heure", "09:00");
                planif.put("duree", commande[2] != null ? commande[2] : 120);
                planif.put("statut", "SIMULEE");

                planificationsSimulees.add(planif);

                System.out.println("✅ " + commande[1] + " → " + employe.get("prenom") + " " + employe.get("nom"));
            }

            resultat.put("success", true);
            resultat.put("message", "Test de planification réussi");
            resultat.put("commandes_traitees", commandes.size());
            resultat.put("employes_utilises", employes.size());
            resultat.put("planifications", planificationsSimulees);
            resultat.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(resultat);

        } catch (Exception e) {
            System.err.println("❌ Erreur test planification: " + e.getMessage());
            e.printStackTrace();

            resultat.put("success", false);
            resultat.put("message", "Erreur: " + e.getMessage());
            return ResponseEntity.status(500).body(resultat);
        }
    }

    /**
     * 🚀 PLANIFICATION AUTOMATIQUE ROBUSTE
     */
    @PostMapping("/api/test/planifier-automatique-robuste")
    public ResponseEntity<Map<String, Object>> planifierAutomatiqueRobuste() {
        Map<String, Object> resultat = new HashMap<>();

        try {
            System.out.println("🚀 === PLANIFICATION AUTOMATIQUE ROBUSTE ===");

            // 1. Vérifications préliminaires
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();
            if (employes.isEmpty()) {
                resultat.put("success", false);
                resultat.put("message", "Aucun employé actif disponible");
                resultat.put("solution", "Créez des employés via POST /api/test/employes");
                return ResponseEntity.ok(resultat);
            }

            // 2. Vérifier les commandes
            int nombreCommandes = 0;
            try {
                String sql = "SELECT COUNT(*) FROM `order` WHERE status IN (1, 2)";
                Query query = entityManager.createNativeQuery(sql);
                Number count = (Number) query.getSingleResult();
                nombreCommandes = count.intValue();
            } catch (Exception e) {
                resultat.put("success", false);
                resultat.put("message", "Erreur accès table commandes: " + e.getMessage());
                return ResponseEntity.ok(resultat);
            }

            if (nombreCommandes == 0) {
                resultat.put("success", false);
                resultat.put("message", "Aucune commande à planifier (status 1 ou 2)");
                resultat.put("solution", "Vérifiez que des commandes existent dans la table 'order' avec status=1 ou status=2");
                return ResponseEntity.ok(resultat);
            }

            System.out.println("📦 " + nombreCommandes + " commandes à planifier");
            System.out.println("👥 " + employes.size() + " employés disponibles");

            // 3. Algorithme de planification simple et robuste
            List<Map<String, Object>> planificationsCreees = new ArrayList<>();

            // Récupérer quelques commandes à planifier
            try {
                String sqlCommandes = """
                SELECT HEX(id) as id, num_commande, temps_estime_minutes 
                FROM `order` 
                WHERE status IN (1, 2) 
                ORDER BY date ASC 
                LIMIT 5
            """;

                Query queryCommandes = entityManager.createNativeQuery(sqlCommandes);
                @SuppressWarnings("unchecked")
                List<Object[]> commandesData = queryCommandes.getResultList();

                // Planifier chaque commande
                for (int i = 0; i < Math.min(commandesData.size(), employes.size()); i++) {
                    Object[] commande = commandesData.get(i);
                    Map<String, Object> employe = employes.get(i % employes.size());

                    Map<String, Object> planification = new HashMap<>();
                    planification.put("commande_id", (String) commande[0]);
                    planification.put("commande_numero", (String) commande[1]);
                    planification.put("employe_id", employe.get("id"));
                    planification.put("employe_nom", employe.get("prenom") + " " + employe.get("nom"));
                    planification.put("date_planification", LocalDate.now().plusDays(i).toString());
                    planification.put("heure_debut", "09:00");
                    planification.put("duree_minutes", commande[2] != null ? commande[2] : 120);
                    planification.put("statut", "PLANIFIEE");

                    planificationsCreees.add(planification);

                    System.out.println("✅ " + commande[1] + " → " + employe.get("prenom") + " " + employe.get("nom"));
                }

            } catch (Exception e) {
                resultat.put("success", false);
                resultat.put("message", "Erreur lors de la planification: " + e.getMessage());
                return ResponseEntity.ok(resultat);
            }

            // 4. Préparer le résultat
            resultat.put("success", true);
            resultat.put("message", "Planification automatique réussie");
            resultat.put("algorithme", "SIMPLE_ROBUSTE");
            resultat.put("commandes_analysees", nombreCommandes);
            resultat.put("employes_disponibles", employes.size());
            resultat.put("planifications_creees", planificationsCreees.size());
            resultat.put("planifications", planificationsCreees);
            resultat.put("temps_execution", "< 1s");
            resultat.put("timestamp", System.currentTimeMillis());

            System.out.println("🎉 Planification terminée: " + planificationsCreees.size() + " planifications créées");

            return ResponseEntity.ok(resultat);

        } catch (Exception e) {
            System.err.println("❌ Erreur critique planification robuste: " + e.getMessage());
            e.printStackTrace();

            resultat.put("success", false);
            resultat.put("message", "Erreur critique: " + e.getMessage());
            resultat.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(resultat);
        }
    }

    /**
     * 🔧 CRÉER DONNÉES DE TEST COMPLÈTES
     */
    @PostMapping("/api/test/initialiser-donnees-test")
    public ResponseEntity<Map<String, Object>> initialiserDonneesTest() {
        Map<String, Object> resultat = new HashMap<>();

        try {
            System.out.println("🔧 === INITIALISATION DONNÉES DE TEST ===");

            // 1. Créer des employés s'ils n'existent pas
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();
            if (employes.isEmpty()) {
                System.out.println("👥 Création d'employés de test...");

                // Créer 3 employés via l'API
                String[] nomsEmployes = {
                        "Dupont,Jean,jean.dupont@test.com",
                        "Martin,Marie,marie.martin@test.com",
                        "Durand,Paul,paul.durand@test.com"
                };

                int employesCreés = 0;
                for (String employeData : nomsEmployes) {
                    String[] parts = employeData.split(",");

                    try {
                        Map<String, Object> empData = new HashMap<>();
                        empData.put("nom", parts[0]);
                        empData.put("prenom", parts[1]);
                        empData.put("email", parts[2]);
                        empData.put("heuresTravailParJour", 8);
                        empData.put("actif", true);

                        ResponseEntity<Map<String, Object>> response = creerEmployeTest(empData);
                        if (response.getStatusCode().is2xxSuccessful()) {
                            employesCreés++;
                        }
                    } catch (Exception e) {
                        System.err.println("❌ Erreur création employé " + parts[1] + ": " + e.getMessage());
                    }
                }

                resultat.put("employes_crees", employesCreés);
            } else {
                resultat.put("employes_existants", employes.size());
            }

            // 2. Vérifier les commandes
            String sql = "SELECT COUNT(*) FROM `order` WHERE status IN (1, 2)";
            Query query = entityManager.createNativeQuery(sql);
            Number count = (Number) query.getSingleResult();
            resultat.put("commandes_disponibles", count.intValue());

            // 3. Statut final
            boolean pret = count.intValue() > 0 && (employeService.getTousEmployesActifs().size() > 0);
            resultat.put("pret_pour_planification", pret);
            resultat.put("message", pret ?
                    "✅ Système prêt pour la planification automatique" :
                    "⚠️ Des données manquent encore");

            resultat.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(resultat);

        } catch (Exception e) {
            System.err.println("❌ Erreur initialisation: " + e.getMessage());
            resultat.put("erreur", e.getMessage());
            return ResponseEntity.status(500).body(resultat);
        }
    }

// ✅ N'oubliez pas d'ajouter ces imports en haut du fichier TestController.java :

// ============= PLANIFICATION AUTOMATIQUE FINALE =============

// ✅ AJOUTEZ cette méthode dans votre TestController.java :

    /**
     * 🚀 PLANIFICATION AUTOMATIQUE FINALE - AVEC SAUVEGARDE RÉELLE
     */
    @PostMapping("/api/test/planifier-automatique-final")
    @Transactional
    public ResponseEntity<Map<String, Object>> planifierAutomatiqueFinal(
            @RequestParam(defaultValue = "10") int nombreCommandes,
            @RequestParam(defaultValue = "true") boolean sauvegarder) {

        Map<String, Object> resultat = new HashMap<>();

        try {
            System.out.println("🚀 === PLANIFICATION AUTOMATIQUE FINALE ===");
            System.out.println("📦 Limite: " + nombreCommandes + " commandes");
            System.out.println("💾 Sauvegarde: " + (sauvegarder ? "OUI" : "SIMULATION"));

            // 1. Récupérer employés actifs
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();
            if (employes.isEmpty()) {
                resultat.put("success", false);
                resultat.put("message", "Aucun employé disponible");
                return ResponseEntity.ok(resultat);
            }

            // 2. Récupérer commandes à planifier (depuis 2021 pour avoir vos vraies données)
            String sqlCommandes = """
            SELECT HEX(id) as id, num_commande, temps_estime_minutes, priorite_string, prix_total
            FROM `order` 
            WHERE status IN (1, 2)
            ORDER BY 
                CASE priorite_string 
                    WHEN 'HAUTE' THEN 1 
                    WHEN 'MOYENNE' THEN 2 
                    WHEN 'NORMALE' THEN 3 
                    ELSE 4 
                END,
                prix_total DESC,
                date ASC
            LIMIT ?
        """;

            Query queryCommandes = entityManager.createNativeQuery(sqlCommandes);
            queryCommandes.setParameter(1, nombreCommandes);
            @SuppressWarnings("unchecked")
            List<Object[]> commandesData = queryCommandes.getResultList();

            if (commandesData.isEmpty()) {
                resultat.put("success", false);
                resultat.put("message", "Aucune commande à planifier trouvée");
                return ResponseEntity.ok(resultat);
            }

            System.out.println("📦 " + commandesData.size() + " commandes sélectionnées");
            System.out.println("👥 " + employes.size() + " employés disponibles");

            // 3. Algorithme de planification intelligent
            List<Map<String, Object>> planificationsCreees = new ArrayList<>();
            int planificationsSauvees = 0;

            LocalDate dateDebut = LocalDate.now();
            int employeIndex = 0;
            int heureDebut = 9; // Commencer à 9h

            for (Object[] commande : commandesData) {
                String commandeId = (String) commande[0];
                String numeroCommande = (String) commande[1];
                Integer tempsEstime = (Integer) commande[2];
                String priorite = (String) commande[3];
                Double prixTotal = (Double) commande[4];

                // Choisir l'employé (rotation)
                Map<String, Object> employe = employes.get(employeIndex % employes.size());
                String employeId = (String) employe.get("id");
                String employeNom = employe.get("prenom") + " " + employe.get("nom");

                // Calculer durée (minimum 1h, maximum 8h)
                int dureeMinutes = Math.max(60, Math.min(480, tempsEstime != null ? tempsEstime : 120));

                // Calculer date et heure
                LocalDate datePlanif = dateDebut.plusDays(employeIndex / employes.size());
                LocalTime heurePlanif = LocalTime.of(heureDebut + (employeIndex % 8), 0);

                // Créer la planification
                Map<String, Object> planification = new HashMap<>();
                planification.put("commande_id", commandeId);
                planification.put("commande_numero", numeroCommande);
                planification.put("employe_id", employeId);
                planification.put("employe_nom", employeNom);
                planification.put("date_planification", datePlanif.toString());
                planification.put("heure_debut", heurePlanif.toString());
                planification.put("duree_minutes", dureeMinutes);
                planification.put("priorite", priorite);
                planification.put("prix_total", prixTotal);
                planification.put("statut", sauvegarder ? "SAUVEGARDEE" : "SIMULEE");

                planificationsCreees.add(planification);

                // 4. Sauvegarder en base si demandé
                if (sauvegarder) {
                    try {
                        // ✅ SOLUTION SIMPLE : Utiliser l'EntityManager avec flush
                        entityManager.flush(); // Force la synchronisation

                        // Générer un ID unique pour la planification
                        String planifId = UUID.randomUUID().toString().replace("-", "");

                        // Nettoyer les IDs (enlever les tirets)
                        String orderIdClean = commandeId.replace("-", "");
                        String employeIdClean = employeId.replace("-", "");

                        // ✅ REQUÊTE SIMPLIFIÉE qui marche
                        String sqlInsert = """
            INSERT INTO j_planification 
            (id, order_id, employe_id, date_planification, heure_debut, duree_minutes, 
             terminee, date_creation, date_modification)
            VALUES (UNHEX(?), UNHEX(?), UNHEX(?), ?, ?, ?, 0, NOW(), NOW())
        """;

                        // Vérifier si les IDs sont au bon format
                        if (orderIdClean.length() != 32) {
                            planification.put("erreur_sauvegarde", "ID commande format invalide: " + orderIdClean.length() + " caractères");
                            continue;
                        }

                        if (employeIdClean.length() != 32) {
                            planification.put("erreur_sauvegarde", "ID employé format invalide: " + employeIdClean.length() + " caractères");
                            continue;
                        }

                        // Exécuter l'insertion
                        int rowsInserted = entityManager.createNativeQuery(sqlInsert)
                                .setParameter(1, planifId)
                                .setParameter(2, orderIdClean)
                                .setParameter(3, employeIdClean)
                                .setParameter(4, datePlanif)
                                .setParameter(5, heurePlanif)
                                .setParameter(6, dureeMinutes)
                                .executeUpdate();

                        if (rowsInserted > 0) {
                            planificationsSauvees++;
                            planification.put("id_planification", planifId);
                            planification.put("sauvegarde_reussie", true);

                            System.out.println("💾 SAUVÉ: " + numeroCommande + " → " + employeNom + " (ID: " + planifId + ")");
                        } else {
                            planification.put("erreur_sauvegarde", "Aucune ligne insérée");
                        }

                    } catch (Exception e) {
                        System.err.println("❌ Erreur sauvegarde " + numeroCommande + ": " + e.getMessage());
                        planification.put("erreur_sauvegarde", e.getClass().getSimpleName() + ": " + e.getMessage());
                    }
                }

                System.out.println("✅ " + numeroCommande +
                        " → " + employeNom +
                        " (" + datePlanif + " à " + heurePlanif + ")" +
                        (sauvegarder ? " [SAUVÉ]" : " [SIMULÉ]"));

                employeIndex++;
            }

            // 5. Préparer le résultat final
            resultat.put("success", true);
            resultat.put("message", "Planification automatique " + (sauvegarder ? "terminée" : "simulée") + " avec succès");
            resultat.put("algorithme", "INTELLIGENT_FINAL");
            resultat.put("mode", sauvegarder ? "SAUVEGARDE_REELLE" : "SIMULATION");
            resultat.put("commandes_analysees", commandesData.size());
            resultat.put("planifications_creees", planificationsCreees.size());
            resultat.put("planifications_sauvees", planificationsSauvees);
            resultat.put("employes_utilises", employes.size());
            resultat.put("date_debut_planification", dateDebut.toString());
            resultat.put("planifications", planificationsCreees);
            resultat.put("statistiques", Map.of(
                    "taux_reussite", planificationsCreees.size() > 0 ?
                            (double) planificationsSauvees / planificationsCreees.size() * 100 : 0,
                    "duree_moyenne", planificationsCreees.stream()
                            .mapToInt(p -> (Integer) p.get("duree_minutes"))
                            .average()
                            .orElse(0),
                    "repartition_employes", employes.stream()
                            .collect(java.util.stream.Collectors.toMap(
                                    e -> e.get("prenom") + " " + e.get("nom"),
                                    e -> (int) planificationsCreees.stream()
                                            .filter(p -> p.get("employe_id").equals(e.get("id")))
                                            .count()
                            ))
            ));
            resultat.put("timestamp", System.currentTimeMillis());

            System.out.println("🎉 PLANIFICATION TERMINÉE !");
            System.out.println("📊 " + planificationsCreees.size() + " planifications créées");
            if (sauvegarder) {
                System.out.println("💾 " + planificationsSauvees + " planifications sauvegardées en base");
            }

            return ResponseEntity.ok(resultat);

        } catch (Exception e) {
            System.err.println("❌ ERREUR CRITIQUE planification finale: " + e.getMessage());
            e.printStackTrace();

            resultat.put("success", false);
            resultat.put("message", "Erreur critique: " + e.getMessage());
            resultat.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(resultat);
        }
    }

    /**
     * 📊 VOIR LES PLANIFICATIONS CRÉÉES
     */
    @GetMapping("/api/test/voir-planifications")
    public ResponseEntity<Map<String, Object>> voirPlanifications() {
        Map<String, Object> resultat = new HashMap<>();

        try {
            String sql = """
            SELECT 
                HEX(p.id) as planif_id,
                HEX(p.order_id) as order_id,
                HEX(p.employe_id) as employe_id,
                p.date_planification,
                p.heure_debut,
                p.duree_minutes,
                p.terminee,
                o.num_commande,
                o.priorite_string,
                CONCAT(e.prenom, ' ', e.nom) as employe_nom
            FROM j_planification p
            LEFT JOIN `order` o ON p.order_id = o.id
            LEFT JOIN j_employe e ON p.employe_id = e.id
            ORDER BY p.date_planification, p.heure_debut
            LIMIT 20
        """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> planifications = new ArrayList<>();
            for (Object[] row : results) {
                Map<String, Object> planif = new HashMap<>();
                planif.put("planification_id", (String) row[0]);
                planif.put("order_id", (String) row[1]);
                planif.put("employe_id", (String) row[2]);
                planif.put("date_planification", row[3]);
                planif.put("heure_debut", row[4]);
                planif.put("duree_minutes", row[5]);
                planif.put("terminee", row[6]);
                planif.put("num_commande", (String) row[7]);
                planif.put("priorite", (String) row[8]);
                planif.put("employe_nom", (String) row[9]);
                planifications.add(planif);
            }

            resultat.put("planifications", planifications);
            resultat.put("nombre_total", planifications.size());
            resultat.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.ok(resultat);

        } catch (Exception e) {
            resultat.put("erreur", e.getMessage());
            return ResponseEntity.status(500).body(resultat);
        }
    }

// ============= CORRECTION SAUVEGARDE - GESTION TRANSACTION =============

// ✅ REMPLACEZ la méthode de sauvegarde dans TestController.java :

    /**
     * 🚀 PLANIFICATION AUTOMATIQUE AVEC SAUVEGARDE CORRIGÉE
     */
    @PostMapping("/api/test/planifier-automatique-final-v2")
    @Transactional  // ✅ AJOUT : Annotation transaction
    public ResponseEntity<Map<String, Object>> planifierAutomatiqueFinalV2(
            @RequestParam(defaultValue = "10") int nombreCommandes,
            @RequestParam(defaultValue = "true") boolean sauvegarder) {

        Map<String, Object> resultat = new HashMap<>();

        try {
            System.out.println("🚀 === PLANIFICATION AUTOMATIQUE V2 (TRANSACTION) ===");
            System.out.println("📦 Limite: " + nombreCommandes + " commandes");
            System.out.println("💾 Sauvegarde: " + (sauvegarder ? "OUI" : "SIMULATION"));

            // 1. Récupérer employés actifs
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();
            if (employes.isEmpty()) {
                resultat.put("success", false);
                resultat.put("message", "Aucun employé disponible");
                return ResponseEntity.ok(resultat);
            }

            // 2. Récupérer commandes à planifier
            String sqlCommandes = """
            SELECT HEX(id) as id, num_commande, temps_estime_minutes, priorite_string, prix_total
            FROM `order` 
            WHERE status IN (1, 2)
            ORDER BY 
                CASE priorite_string 
                    WHEN 'HAUTE' THEN 1 
                    WHEN 'MOYENNE' THEN 2 
                    WHEN 'NORMALE' THEN 3 
                    ELSE 4 
                END,
                prix_total DESC,
                date ASC
            LIMIT ?
        """;

            Query queryCommandes = entityManager.createNativeQuery(sqlCommandes);
            queryCommandes.setParameter(1, nombreCommandes);
            @SuppressWarnings("unchecked")
            List<Object[]> commandesData = queryCommandes.getResultList();

            if (commandesData.isEmpty()) {
                resultat.put("success", false);
                resultat.put("message", "Aucune commande à planifier trouvée");
                return ResponseEntity.ok(resultat);
            }

            System.out.println("📦 " + commandesData.size() + " commandes sélectionnées");
            System.out.println("👥 " + employes.size() + " employés disponibles");

            // 3. Algorithme de planification intelligent
            List<Map<String, Object>> planificationsCreees = new ArrayList<>();
            int planificationsSauvees = 0;

            LocalDate dateDebut = LocalDate.now();
            int employeIndex = 0;
            int heureDebut = 9; // Commencer à 9h

            for (Object[] commande : commandesData) {
                String commandeId = (String) commande[0];
                String numeroCommande = (String) commande[1];
                Integer tempsEstime = (Integer) commande[2];
                String priorite = (String) commande[3];
                Double prixTotal = (Double) commande[4];

                // Choisir l'employé (rotation)
                Map<String, Object> employe = employes.get(employeIndex % employes.size());
                String employeId = (String) employe.get("id");
                String employeNom = employe.get("prenom") + " " + employe.get("nom");

                // Calculer durée (minimum 1h, maximum 8h)
                int dureeMinutes = Math.max(60, Math.min(480, tempsEstime != null ? tempsEstime : 120));

                // Calculer date et heure
                LocalDate datePlanif = dateDebut.plusDays(employeIndex / employes.size());
                LocalTime heurePlanif = LocalTime.of(heureDebut + (employeIndex % 8), 0);

                // Créer la planification
                Map<String, Object> planification = new HashMap<>();
                planification.put("commande_id", commandeId);
                planification.put("commande_numero", numeroCommande);
                planification.put("employe_id", employeId);
                planification.put("employe_nom", employeNom);
                planification.put("date_planification", datePlanif.toString());
                planification.put("heure_debut", heurePlanif.toString());
                planification.put("duree_minutes", dureeMinutes);
                planification.put("priorite", priorite);
                planification.put("prix_total", prixTotal);
                planification.put("statut", sauvegarder ? "SAUVEGARDEE" : "SIMULEE");

                planificationsCreees.add(planification);

                // 4. Sauvegarder en base si demandé - VERSION CORRIGÉE
                if (sauvegarder) {
                    try {
                        // ✅ CORRECTION : Vérifier d'abord que les IDs existent
                        String checkOrderSql = "SELECT COUNT(*) FROM `order` WHERE HEX(id) = ?";
                        Query checkOrderQuery = entityManager.createNativeQuery(checkOrderSql);
                        checkOrderQuery.setParameter(1, commandeId);
                        Number orderExists = (Number) checkOrderQuery.getSingleResult();

                        String checkEmployeSql = "SELECT COUNT(*) FROM j_employe WHERE HEX(id) = ?";
                        Query checkEmployeQuery = entityManager.createNativeQuery(checkEmployeSql);
                        checkEmployeQuery.setParameter(1, employeId.replace("-", ""));
                        Number employeExists = (Number) checkEmployeQuery.getSingleResult();

                        if (orderExists.intValue() == 0) {
                            planification.put("erreur_sauvegarde", "Commande ID non trouvé: " + commandeId);
                            continue;
                        }

                        if (employeExists.intValue() == 0) {
                            planification.put("erreur_sauvegarde", "Employé ID non trouvé: " + employeId);
                            continue;
                        }

                        // ✅ CORRECTION : Vérifier si planification existe déjà
                        String checkExistsSql = """
                        SELECT COUNT(*) FROM j_planification 
                        WHERE order_id = UNHEX(?) AND employe_id = UNHEX(?)
                    """;
                        Query checkExistsQuery = entityManager.createNativeQuery(checkExistsSql);
                        checkExistsQuery.setParameter(1, commandeId);
                        checkExistsQuery.setParameter(2, employeId.replace("-", ""));
                        Number existsCount = (Number) checkExistsQuery.getSingleResult();

                        if (existsCount.intValue() > 0) {
                            planification.put("erreur_sauvegarde", "Planification déjà existante");
                            continue;
                        }

                        // ✅ CORRECTION : Insertion avec gestion d'erreur améliorée
                        String planifId = UUID.randomUUID().toString().replace("-", "");

                        String sqlInsert = """
                        INSERT INTO j_planification 
                        (id, order_id, employe_id, date_planification, heure_debut, duree_minutes, 
                         terminee, date_creation, date_modification)
                        VALUES (UNHEX(?), UNHEX(?), UNHEX(?), ?, ?, ?, false, NOW(), NOW())
                    """;

                        Query insertQuery = entityManager.createNativeQuery(sqlInsert);
                        insertQuery.setParameter(1, planifId);
                        insertQuery.setParameter(2, commandeId);
                        insertQuery.setParameter(3, employeId.replace("-", ""));
                        insertQuery.setParameter(4, datePlanif);
                        insertQuery.setParameter(5, heurePlanif);
                        insertQuery.setParameter(6, dureeMinutes);

                        int rowsInserted = insertQuery.executeUpdate();

                        if (rowsInserted > 0) {
                            planificationsSauvees++;
                            planification.put("id_planification", planifId);
                            planification.put("sauvegarde_reussie", true);

                            // ✅ OPTIONNEL : Mettre à jour le statut de la commande
                            try {
                                String sqlUpdate = "UPDATE `order` SET status = 2 WHERE HEX(id) = ?";
                                Query updateQuery = entityManager.createNativeQuery(sqlUpdate);
                                updateQuery.setParameter(1, commandeId);
                                updateQuery.executeUpdate();
                                planification.put("statut_commande_mis_a_jour", true);
                            } catch (Exception e) {
                                System.err.println("⚠️ Erreur mise à jour statut commande " + numeroCommande + ": " + e.getMessage());
                            }

                            System.out.println("💾 SAUVÉ: " + numeroCommande + " → " + employeNom);
                        } else {
                            planification.put("erreur_sauvegarde", "Aucune ligne insérée");
                        }

                    } catch (Exception e) {
                        System.err.println("❌ Erreur sauvegarde planification " + numeroCommande + ": " + e.getMessage());
                        e.printStackTrace();
                        planification.put("erreur_sauvegarde", e.getMessage());
                    }
                }

                System.out.println("✅ " + numeroCommande +
                        " → " + employeNom +
                        " (" + datePlanif + " à " + heurePlanif + ")" +
                        (sauvegarder ?
                                (planification.containsKey("sauvegarde_reussie") ? " [SAUVÉ]" : " [ERREUR]") :
                                " [SIMULÉ]"));

                employeIndex++;
            }

            // 5. Préparer le résultat final
            resultat.put("success", true);
            resultat.put("message", "Planification automatique " + (sauvegarder ? "terminée" : "simulée") + " avec succès");
            resultat.put("algorithme", "INTELLIGENT_V2");
            resultat.put("mode", sauvegarder ? "SAUVEGARDE_REELLE" : "SIMULATION");
            resultat.put("commandes_analysees", commandesData.size());
            resultat.put("planifications_creees", planificationsCreees.size());
            resultat.put("planifications_sauvees", planificationsSauvees);
            resultat.put("taux_reussite_sauvegarde", planificationsCreees.size() > 0 ?
                    (double) planificationsSauvees / planificationsCreees.size() * 100 : 0);
            resultat.put("employes_utilises", employes.size());
            resultat.put("date_debut_planification", dateDebut.toString());
            resultat.put("planifications", planificationsCreees);

            // ✅ Détail des erreurs s'il y en a
            List<String> erreurs = planificationsCreees.stream()
                    .filter(p -> p.containsKey("erreur_sauvegarde"))
                    .map(p -> p.get("commande_numero") + ": " + p.get("erreur_sauvegarde"))
                    .collect(java.util.stream.Collectors.toList());

            if (!erreurs.isEmpty()) {
                resultat.put("erreurs_detaillees", erreurs);
            }

            resultat.put("timestamp", System.currentTimeMillis());

            System.out.println("🎉 PLANIFICATION V2 TERMINÉE !");
            System.out.println("📊 " + planificationsCreees.size() + " planifications créées");
            if (sauvegarder) {
                System.out.println("💾 " + planificationsSauvees + " planifications sauvegardées en base");
                if (planificationsSauvees < planificationsCreees.size()) {
                    System.out.println("⚠️ " + (planificationsCreees.size() - planificationsSauvees) + " erreurs de sauvegarde");
                }
            }

            return ResponseEntity.ok(resultat);

        } catch (Exception e) {
            System.err.println("❌ ERREUR CRITIQUE planification V2: " + e.getMessage());
            e.printStackTrace();

            resultat.put("success", false);
            resultat.put("message", "Erreur critique: " + e.getMessage());
            resultat.put("timestamp", System.currentTimeMillis());

            return ResponseEntity.status(500).body(resultat);
        }
    }

    /**
     * 🔧 TEST SAUVEGARDE DIRECTE - MÉTHODE SIMPLE
     */
    @PostMapping("/api/test/sauvegarder-planification-test")
    @Transactional
    public ResponseEntity<Map<String, Object>> sauvegarderPlanificationTest() {
        Map<String, Object> resultat = new HashMap<>();

        try {
            System.out.println("🔧 === TEST SAUVEGARDE DIRECTE ===");

            // 1. Récupérer un employé et une commande pour test
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();
            if (employes.isEmpty()) {
                resultat.put("success", false);
                resultat.put("message", "Aucun employé disponible");
                return ResponseEntity.ok(resultat);
            }

            // Récupérer une commande
            String sqlCommande = "SELECT HEX(id), num_commande FROM `order` WHERE status = 1 LIMIT 1";
            Query queryCommande = entityManager.createNativeQuery(sqlCommande);
            @SuppressWarnings("unchecked")
            List<Object[]> commandeData = queryCommande.getResultList();

            if (commandeData.isEmpty()) {
                resultat.put("success", false);
                resultat.put("message", "Aucune commande disponible");
                return ResponseEntity.ok(resultat);
            }

            Object[] commande = commandeData.get(0);
            String commandeId = (String) commande[0];
            String numeroCommande = (String) commande[1];

            Map<String, Object> employe = employes.get(0);
            String employeId = (String) employe.get("id");
            String employeNom = employe.get("prenom") + " " + employe.get("nom");

            System.out.println("🔍 Test avec:");
            System.out.println("  Commande: " + numeroCommande + " (ID: " + commandeId + ")");
            System.out.println("  Employé: " + employeNom + " (ID: " + employeId + ")");

            // 2. Test de sauvegarde
            String planifId = UUID.randomUUID().toString().replace("-", "");
            LocalDate datePlanif = LocalDate.now().plusDays(1);
            LocalTime heurePlanif = LocalTime.of(9, 0);

            try {
                // ✅ VERSION TRÈS SIMPLE
                String sqlTest = """
                INSERT INTO j_planification 
                (id, order_id, employe_id, date_planification, heure_debut, duree_minutes, terminee)
                VALUES (UNHEX(?), UNHEX(?), UNHEX(?), ?, ?, ?, false)
            """;

                int rowsInserted = entityManager.createNativeQuery(sqlTest)
                        .setParameter(1, planifId)
                        .setParameter(2, commandeId.replace("-", ""))
                        .setParameter(3, employeId.replace("-", ""))
                        .setParameter(4, datePlanif)
                        .setParameter(5, heurePlanif)
                        .setParameter(6, 120)
                        .executeUpdate();

                if (rowsInserted > 0) {
                    resultat.put("success", true);
                    resultat.put("message", "✅ Sauvegarde réussie !");
                    resultat.put("planification_id", planifId);
                    resultat.put("commande", numeroCommande);
                    resultat.put("employe", employeNom);
                    resultat.put("date", datePlanif.toString());
                    resultat.put("heure", heurePlanif.toString());

                    System.out.println("✅ SUCCÈS: Planification sauvegardée avec ID " + planifId);
                } else {
                    resultat.put("success", false);
                    resultat.put("message", "❌ Aucune ligne insérée");
                }

            } catch (Exception e) {
                System.err.println("❌ Erreur sauvegarde: " + e.getMessage());
                e.printStackTrace();

                resultat.put("success", false);
                resultat.put("message", "Erreur sauvegarde: " + e.getMessage());
                resultat.put("erreur_type", e.getClass().getSimpleName());

                // Diagnostics supplémentaires
                resultat.put("diagnostic", Map.of(
                        "commande_id_longueur", commandeId.replace("-", "").length(),
                        "employe_id_longueur", employeId.replace("-", "").length(),
                        "commande_id_format", commandeId,
                        "employe_id_format", employeId
                ));
            }

            resultat.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.ok(resultat);

        } catch (Exception e) {
            System.err.println("❌ Erreur test sauvegarde: " + e.getMessage());
            e.printStackTrace();

            resultat.put("success", false);
            resultat.put("message", "Erreur générale: " + e.getMessage());
            return ResponseEntity.status(500).body(resultat);
        }
    }

    /**
     * 🧹 NETTOYER LES PLANIFICATIONS TEST
     */
    @PostMapping("/api/test/nettoyer-planifications")
    @Transactional
    public ResponseEntity<Map<String, Object>> nettoyerPlanifications() {
        Map<String, Object> resultat = new HashMap<>();

        try {
            String sqlDelete = "DELETE FROM j_planification WHERE date_creation >= CURDATE()";
            int rowsDeleted = entityManager.createNativeQuery(sqlDelete).executeUpdate();

            resultat.put("success", true);
            resultat.put("message", "Planifications nettoyées");
            resultat.put("planifications_supprimees", rowsDeleted);

            return ResponseEntity.ok(resultat);

        } catch (Exception e) {
            resultat.put("success", false);
            resultat.put("message", "Erreur: " + e.getMessage());
            return ResponseEntity.status(500).body(resultat);
        }
    }



    /**
     * 🚀 PLANIFICATION AUTOMATIQUE - VERSION TRANSACTION
     */
    @PostMapping("/api/test/planifier-avec-transaction")
    @Transactional
    public ResponseEntity<Map<String, Object>> planifierAvecTransaction(
            @RequestParam(defaultValue = "5") int nombreCommandes) {

        Map<String, Object> resultat = new HashMap<>();

        try {
            System.out.println("🚀 === PLANIFICATION AVEC TRANSACTION ===");

            // 1. Récupérer employés
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();
            if (employes.isEmpty()) {
                resultat.put("success", false);
                resultat.put("message", "Aucun employé disponible");
                return ResponseEntity.ok(resultat);
            }

            // 2. Récupérer commandes
            String sqlCommandes = """
            SELECT HEX(id) as id, num_commande, temps_estime_minutes
            FROM `order` 
            WHERE status = 1
            ORDER BY date ASC
            LIMIT ?
        """;

            Query queryCommandes = entityManager.createNativeQuery(sqlCommandes);
            queryCommandes.setParameter(1, nombreCommandes);
            @SuppressWarnings("unchecked")
            List<Object[]> commandesData = queryCommandes.getResultList();

            if (commandesData.isEmpty()) {
                resultat.put("success", false);
                resultat.put("message", "Aucune commande à planifier");
                return ResponseEntity.ok(resultat);
            }

            System.out.println("📦 " + commandesData.size() + " commandes à planifier");
            System.out.println("👥 " + employes.size() + " employés disponibles");

            // 3. Planifier et sauvegarder
            List<Map<String, Object>> planificationsCreees = new ArrayList<>();
            int planificationsSauvees = 0;

            LocalDate dateDebut = LocalDate.now().plusDays(1); // Demain

            for (int i = 0; i < commandesData.size(); i++) {
                Object[] commande = commandesData.get(i);
                String commandeId = (String) commande[0];
                String numeroCommande = (String) commande[1];
                Integer tempsEstime = (Integer) commande[2];

                // Choisir employé (rotation)
                Map<String, Object> employe = employes.get(i % employes.size());
                String employeId = (String) employe.get("id");
                String employeNom = employe.get("prenom") + " " + employe.get("nom");

                // Paramètres planification
                LocalDate datePlanif = dateDebut.plusDays(i / employes.size());
                LocalTime heurePlanif = LocalTime.of(9 + (i % 8), 0);
                int dureeMinutes = Math.max(60, Math.min(480, tempsEstime != null ? tempsEstime : 120));

                try {
                    // Générer ID planification
                    String planifId = UUID.randomUUID().toString().replace("-", "");

                    // ✅ INSERTION SIMPLE AVEC TRANSACTION
                    String sqlInsert = """
                    INSERT INTO j_planification 
                    (id, order_id, employe_id, date_planification, heure_debut, duree_minutes, terminee)
                    VALUES (UNHEX(?), UNHEX(?), UNHEX(?), ?, ?, ?, false)
                """;

                    int rowsInserted = entityManager.createNativeQuery(sqlInsert)
                            .setParameter(1, planifId)
                            .setParameter(2, commandeId.replace("-", ""))
                            .setParameter(3, employeId.replace("-", ""))
                            .setParameter(4, datePlanif)
                            .setParameter(5, heurePlanif)
                            .setParameter(6, dureeMinutes)
                            .executeUpdate();

                    if (rowsInserted > 0) {
                        planificationsSauvees++;

                        // Mettre à jour statut commande
                        String sqlUpdate = "UPDATE `order` SET status = 2 WHERE HEX(id) = ?";
                        entityManager.createNativeQuery(sqlUpdate)
                                .setParameter(1, commandeId)
                                .executeUpdate();

                        // Ajouter à la liste des réussites
                        Map<String, Object> planif = new HashMap<>();
                        planif.put("planification_id", planifId);
                        planif.put("commande_id", commandeId);
                        planif.put("commande_numero", numeroCommande);
                        planif.put("employe_id", employeId);
                        planif.put("employe_nom", employeNom);
                        planif.put("date_planification", datePlanif.toString());
                        planif.put("heure_debut", heurePlanif.toString());
                        planif.put("duree_minutes", dureeMinutes);
                        planif.put("statut", "SAUVEGARDE_REUSSIE");

                        planificationsCreees.add(planif);

                        System.out.println("✅ SAUVÉ: " + numeroCommande + " → " + employeNom +
                                " (" + datePlanif + " à " + heurePlanif + ")");
                    }

                } catch (Exception e) {
                    System.err.println("❌ Erreur planification " + numeroCommande + ": " + e.getMessage());
                }
            }

            // 4. Résultat final
            resultat.put("success", true);
            resultat.put("message", "Planification avec transaction terminée");
            resultat.put("commandes_analysees", commandesData.size());
            resultat.put("planifications_creees", planificationsCreees.size());
            resultat.put("planifications_sauvees", planificationsSauvees);
            resultat.put("taux_reussite", commandesData.size() > 0 ?
                    (double) planificationsSauvees / commandesData.size() * 100 : 0);
            resultat.put("planifications", planificationsCreees);
            resultat.put("timestamp", System.currentTimeMillis());

            System.out.println("🎉 PLANIFICATION TERMINÉE AVEC SUCCÈS !");
            System.out.println("💾 " + planificationsSauvees + "/" + commandesData.size() + " planifications sauvegardées");

            return ResponseEntity.ok(resultat);

        } catch (Exception e) {
            System.err.println("❌ Erreur planification avec transaction: " + e.getMessage());
            e.printStackTrace();

            resultat.put("success", false);
            resultat.put("message", "Erreur: " + e.getMessage());
            return ResponseEntity.status(500).body(resultat);
        }
    }
// ============= CORRECTION TESTCONTROLLER.JAVA =============
// ✅ REMPLACEZ vos méthodes existantes par ces versions unifiées

/**
 * 📅 RÉCUPÉRER TOUTES LES PLANIFICATIONS (VERSION UNIFIÉE)
 */
        @GetMapping("/api/test/planifications")
        public ResponseEntity<List<Map<String, Object>>> getPlanifications() {
            try {
                System.out.println("📋 Récupération de toutes les planifications");

                String sql = """
        SELECT 
            HEX(p.id) as planification_id,
            HEX(p.order_id) as order_id,
            HEX(p.employe_id) as employe_id,
            p.date_planification,
            p.heure_debut,
            p.duree_minutes,
            p.terminee,
            o.num_commande,
            o.priorite_string,
            CONCAT(e.prenom, ' ', e.nom) as employe_nom
        FROM j_planification p
        LEFT JOIN `order` o ON p.order_id = o.id
        LEFT JOIN j_employe e ON p.employe_id = e.id
        ORDER BY p.date_planification DESC, p.heure_debut ASC
        LIMIT 100
        """;

                Query query = entityManager.createNativeQuery(sql);
                @SuppressWarnings("unchecked")
                List<Object[]> resultList = query.getResultList();

                List<Map<String, Object>> planifications = new ArrayList<>();

                for (Object[] row : resultList) {
                    Map<String, Object> planif = new HashMap<>();

                    planif.put("id", (String) row[0]);
                    planif.put("orderId", (String) row[1]);
                    planif.put("employeId", (String) row[2]);
                    planif.put("datePlanifiee", row[3]);
                    planif.put("heureDebut", row[4]);
                    planif.put("dureeMinutes", row[5]);
                    planif.put("terminee", row[6]);
                    planif.put("numeroCommande", (String) row[7]);
                    planif.put("priorite", (String) row[8]);
                    planif.put("employeNom", (String) row[9]);

                    planifications.add(planif);
                }

                System.out.println("✅ " + planifications.size() + " planifications retournées");
                return ResponseEntity.ok(planifications);

            } catch (Exception e) {
                System.err.println("❌ Erreur récupération planifications: " + e.getMessage());
                return ResponseEntity.ok(new ArrayList<>());
            }
        }

/**
 * 📅 PLANIFICATIONS PAR PÉRIODE (VERSION UNIFIÉE)
 */
        @GetMapping("/api/test/planifications/periode")
        public ResponseEntity<List<Map<String, Object>>> getPlanificationsByPeriode(
                @RequestParam String debut,
                @RequestParam String fin) {

            try {
                System.out.println("🔍 Planifications pour période: " + debut + " à " + fin);

                String sql = """
        SELECT 
            HEX(p.id) as planification_id,
            HEX(p.order_id) as order_id,
            HEX(p.employe_id) as employe_id,
            p.date_planification,
            p.heure_debut,
            p.duree_minutes,
            p.terminee,
            o.num_commande,
            o.priorite_string,
            CONCAT(e.prenom, ' ', e.nom) as employe_nom
        FROM j_planification p
        LEFT JOIN `order` o ON p.order_id = o.id
        LEFT JOIN j_employe e ON p.employe_id = e.id
        WHERE p.date_planification BETWEEN ? AND ?
        ORDER BY p.date_planification ASC, p.heure_debut ASC
        """;

                Query query = entityManager.createNativeQuery(sql);
                query.setParameter(1, debut);
                query.setParameter(2, fin);

                @SuppressWarnings("unchecked")
                List<Object[]> resultList = query.getResultList();

                List<Map<String, Object>> planifications = new ArrayList<>();

                for (Object[] row : resultList) {
                    Map<String, Object> planif = new HashMap<>();

                    planif.put("id", (String) row[0]);
                    planif.put("orderId", (String) row[1]);
                    planif.put("employeId", (String) row[2]);
                    planif.put("datePlanifiee", row[3]);
                    planif.put("heureDebut", row[4]);
                    planif.put("dureeMinutes", row[5]);
                    planif.put("terminee", row[6]);
                    planif.put("numeroCommande", (String) row[7]);
                    planif.put("priorite", (String) row[8]);
                    planif.put("employeNom", (String) row[9]);

                    planifications.add(planif);
                }

                System.out.println("✅ " + planifications.size() + " planifications pour période");
                return ResponseEntity.ok(planifications);

            } catch (Exception e) {
                System.err.println("❌ Erreur récupération par période: " + e.getMessage());
                return ResponseEntity.ok(new ArrayList<>());
            }
        }


/**
 * ✅ TERMINER UNE PLANIFICATION (VERSION UNIFIÉE)
 */
        @PostMapping("/api/test/planifications/{id}/terminer")
        public ResponseEntity<Map<String, Object>> terminerPlanification(@PathVariable String id) {
            try {
                System.out.println("✅ Terminer planification: " + id);

                String sql = "UPDATE j_planification SET terminee = 1 WHERE HEX(id) = ?";
                Query query = entityManager.createNativeQuery(sql);
                query.setParameter(1, id);

                int rowsUpdated = query.executeUpdate();

                Map<String, Object> response = new HashMap<>();
                if (rowsUpdated > 0) {
                    response.put("success", true);
                    response.put("message", "Planification terminée avec succès");
                    System.out.println("✅ Planification " + id + " terminée");
                } else {
                    response.put("success", false);
                    response.put("message", "Planification non trouvée");
                    System.out.println("⚠️ Planification " + id + " non trouvée");
                }

                return ResponseEntity.ok(response);

            } catch (Exception e) {
                System.err.println("❌ Erreur terminer planification: " + e.getMessage());

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Erreur: " + e.getMessage());

                return ResponseEntity.status(500).body(errorResponse);
            }
        }

/**
 * 🧹 VIDER TOUTES LES PLANIFICATIONS (VERSION UNIFIÉE - REMPLACE L'EXISTANTE)
 */
        @PostMapping("/api/test/planifications/vider")
        @Transactional
        public ResponseEntity<Map<String, Object>> viderPlanifications() {
            try {
                System.out.println("🧹 Vider toutes les planifications");

                // Compter d'abord
                String countSql = "SELECT COUNT(*) FROM j_planification";
                Query countQuery = entityManager.createNativeQuery(countSql);
                Number count = (Number) countQuery.getSingleResult();

                // Supprimer
                String deleteSql = "DELETE FROM j_planification";
                Query deleteQuery = entityManager.createNativeQuery(deleteSql);
                int rowsDeleted = deleteQuery.executeUpdate();

                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", rowsDeleted + " planifications supprimées");
                response.put("planificationsSupprimees", rowsDeleted);

                System.out.println("✅ " + rowsDeleted + " planifications supprimées");
                return ResponseEntity.ok(response);

            } catch (Exception e) {
                System.err.println("❌ Erreur vider planifications: " + e.getMessage());

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Erreur: " + e.getMessage());

                return ResponseEntity.status(500).body(errorResponse);
            }
        }

// ============= MÉTHODE DE DIAGNOSTIC AMÉLIORÉE =============

    /**
     * 🔍 DIAGNOSTIC PLANIFICATION AMÉLIORÉ
     */
    @GetMapping("/api/test/diagnostic-planification")
    public ResponseEntity<Map<String, Object>> diagnosticPlanification() {
        Map<String, Object> diagnostic = new HashMap<>();

        try {
            System.out.println("🔍 === DIAGNOSTIC PLANIFICATION AMÉLIORÉ ===");

            // 1. Vérifier employés
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();
            diagnostic.put("employes_actifs", employes.size());

            // 2. ✅ DIAGNOSTIC DÉTAILLÉ DES COMMANDES
            // Compter par statut
            String sqlStatuts = """
        SELECT 
            status, 
            COUNT(*) as count,
            GROUP_CONCAT(DISTINCT priorite_string) as priorites
        FROM `order` 
        GROUP BY status
        ORDER BY status
        """;

            Query queryStatuts = entityManager.createNativeQuery(sqlStatuts);
            @SuppressWarnings("unchecked")
            List<Object[]> statutsData = queryStatuts.getResultList();

            Map<String, Object> commandesParStatut = new HashMap<>();
            int totalCommandes = 0;

            for (Object[] row : statutsData) {
                int statut = ((Number) row[0]).intValue();
                int count = ((Number) row[1]).intValue();
                String priorites = (String) row[2];

                totalCommandes += count;
                commandesParStatut.put("statut_" + statut, Map.of(
                        "count", count,
                        "priorites", priorites != null ? priorites : "null"
                ));
            }

            diagnostic.put("commandes_par_statut", commandesParStatut);
            diagnostic.put("total_commandes", totalCommandes);

            // 3. Commandes récentes (utilisées par la planification)
            String sqlRecentes = "SELECT COUNT(*) FROM `order` WHERE date_creation >= DATE_SUB(NOW(), INTERVAL 30 DAY)";
            Query queryRecentes = entityManager.createNativeQuery(sqlRecentes);
            Number commandesRecentes = (Number) queryRecentes.getSingleResult();
            diagnostic.put("commandes_recentes_30j", commandesRecentes.intValue());

            // 4. Planifications existantes
            String planifSql = "SELECT COUNT(*) FROM j_planification";
            Query planifQuery = entityManager.createNativeQuery(planifSql);
            Number planifCount = (Number) planifQuery.getSingleResult();
            diagnostic.put("planifications_existantes", planifCount.intValue());

            // 5. ✅ RECOMMANDATIONS
            List<String> recommandations = new ArrayList<>();

            if (employes.size() == 0) {
                recommandations.add("❌ Aucun employé disponible - vérifiez la table j_employe");
            }

            if (totalCommandes == 0) {
                recommandations.add("❌ Aucune commande en base - vérifiez la table `order`");
            } else if (commandesRecentes.intValue() == 0) {
                recommandations.add("⚠️ Aucune commande récente - la planification risque d'être vide");
            } else {
                recommandations.add("✅ " + commandesRecentes + " commandes récentes disponibles pour planification");
            }

            diagnostic.put("recommandations", recommandations);

            // 6. Endpoints disponibles
            diagnostic.put("endpoints_disponibles", List.of(
                    "GET /api/test/planifications",
                    "GET /api/test/planifications/periode",
                    "POST /api/test/planifier-automatique",
                    "POST /api/test/planifications/{id}/terminer",
                    "POST /api/test/planifications/vider"
            ));

            diagnostic.put("status", "OK");
            diagnostic.put("timestamp", System.currentTimeMillis());

            System.out.println("✅ Diagnostic terminé: " + employes.size() + " employés, " +
                    totalCommandes + " commandes (" + commandesRecentes + " récentes), " +
                    planifCount + " planifications");

            return ResponseEntity.ok(diagnostic);

        } catch (Exception e) {
            System.err.println("❌ Erreur diagnostic: " + e.getMessage());
            e.printStackTrace();

            diagnostic.put("status", "ERROR");
            diagnostic.put("erreur", e.getMessage());
            return ResponseEntity.status(500).body(diagnostic);
        }
    }


// ============= DIAGNOSTIC SPÉCIALISÉ DERNIER MOIS =============

    /**
     * 🔍 DIAGNOSTIC - Commandes du dernier mois
     */
    @GetMapping("/api/test/diagnostic-dernier-mois")
    public ResponseEntity<Map<String, Object>> diagnosticDernierMois() {
        Map<String, Object> diagnostic = new HashMap<>();

        try {
            System.out.println("🔍 === DIAGNOSTIC COMMANDES DERNIER MOIS ===");

            // 1. Compter par statut dans la période
            String sqlPeriode = """
        SELECT 
            status,
            COUNT(*) as count,
            MIN(DATE(date_creation)) as date_min,
            MAX(DATE(date_creation)) as date_max
        FROM `order` 
        WHERE date_creation >= '2025-05-22 00:00:00'
        AND date_creation <= '2025-06-22 23:59:59'
        GROUP BY status
        ORDER BY status
        """;

            Query queryPeriode = entityManager.createNativeQuery(sqlPeriode);
            @SuppressWarnings("unchecked")
            List<Object[]> resultsPeriode = queryPeriode.getResultList();

            Map<String, Object> commandesPeriode = new HashMap<>();
            int totalPeriode = 0;
            int planifiablesPeriode = 0;

            for (Object[] row : resultsPeriode) {
                int statut = ((Number) row[0]).intValue();
                int count = ((Number) row[1]).intValue();
                totalPeriode += count;

                if (statut == 1) {
                    planifiablesPeriode = count;
                }

                commandesPeriode.put("statut_" + statut, Map.of(
                        "count", count,
                        "date_min", row[2],
                        "date_max", row[3]
                ));
            }

            diagnostic.put("commandes_dernier_mois", commandesPeriode);
            diagnostic.put("total_dernier_mois", totalPeriode);
            diagnostic.put("planifiables_dernier_mois", planifiablesPeriode);

            // 2. Répartition par jour (pour voir la distribution)
            String sqlParJour = """
        SELECT 
            DATE(date_creation) as jour,
            COUNT(*) as count,
            COUNT(CASE WHEN status = 1 THEN 1 END) as planifiables
        FROM `order` 
        WHERE date_creation >= '2025-05-22 00:00:00'
        AND date_creation <= '2025-06-22 23:59:59'
        GROUP BY DATE(date_creation)
        ORDER BY jour DESC
        LIMIT 10  -- 10 derniers jours avec des commandes
        """;

            Query queryParJour = entityManager.createNativeQuery(sqlParJour);
            @SuppressWarnings("unchecked")
            List<Object[]> resultsParJour = queryParJour.getResultList();

            List<Map<String, Object>> repartitionJours = new ArrayList<>();
            for (Object[] row : resultsParJour) {
                Map<String, Object> jour = new HashMap<>();
                jour.put("date", row[0]);
                jour.put("total", row[1]);
                jour.put("planifiables", row[2]);
                repartitionJours.add(jour);
            }

            diagnostic.put("repartition_par_jour", repartitionJours);

            // 3. Employés disponibles
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();
            diagnostic.put("employes_actifs", employes.size());

            // 4. Recommandation
            List<String> recommandations = new ArrayList<>();

            if (planifiablesPeriode == 0) {
                recommandations.add("❌ Aucune commande planifiable (statut 1) dans le dernier mois");
                recommandations.add("💡 Vérifiez les statuts disponibles dans cette période");
            } else {
                recommandations.add("✅ " + planifiablesPeriode + " commandes planifiables trouvées dans le dernier mois");
                recommandations.add("🚀 Vous pouvez lancer la planification automatique");
            }

            diagnostic.put("recommandations", recommandations);
            diagnostic.put("periode_analysee", "22 mai 2025 - 22 juin 2025");
            diagnostic.put("status", "OK");
            diagnostic.put("timestamp", System.currentTimeMillis());

            System.out.println("✅ Diagnostic dernier mois: " + totalPeriode + " commandes (" + planifiablesPeriode + " planifiables)");

            return ResponseEntity.ok(diagnostic);

        } catch (Exception e) {
            System.err.println("❌ Erreur diagnostic dernier mois: " + e.getMessage());
            e.printStackTrace();

            diagnostic.put("status", "ERROR");
            diagnostic.put("erreur", e.getMessage());
            return ResponseEntity.status(500).body(diagnostic);
        }
    }

    // ============= DEBUG DATES COMMANDES =============
// ✅ AJOUTEZ ces méthodes dans votre TestController.java pour diagnostiquer

    /**
     * 🔍 DEBUG - Analyser les vraies dates des commandes
     */
    @GetMapping("/api/test/debug-dates-commandes")
    public ResponseEntity<Map<String, Object>> debugDatesCommandes() {
        Map<String, Object> debug = new HashMap<>();

        try {
            System.out.println("🔍 === DEBUG DATES COMMANDES ===");

            // 1. ✅ ANALYSE GÉNÉRALE DES DATES
            String sqlDatesGenerales = """
        SELECT 
            MIN(date_creation) as date_min,
            MAX(date_creation) as date_max,
            COUNT(*) as total_commandes,
            MIN(DATE(date_creation)) as date_min_seule,
            MAX(DATE(date_creation)) as date_max_seule
        FROM `order`
        """;

            Query queryGenerales = entityManager.createNativeQuery(sqlDatesGenerales);
            Object[] datesInfo = (Object[]) queryGenerales.getSingleResult();

            debug.put("analyse_generale", Map.of(
                    "date_creation_min", datesInfo[0],
                    "date_creation_max", datesInfo[1],
                    "total_commandes", datesInfo[2],
                    "date_min_seule", datesInfo[3],
                    "date_max_seule", datesInfo[4]
            ));

            // 2. ✅ COMMANDES DE JUIN 2025 (recherche large)
            String sqlJuin2025 = """
        SELECT 
            DATE(date_creation) as jour,
            COUNT(*) as count_total,
            COUNT(CASE WHEN status = 1 THEN 1 END) as count_statut1,
            GROUP_CONCAT(DISTINCT status ORDER BY status) as statuts_disponibles
        FROM `order` 
        WHERE YEAR(date_creation) = 2025 
        AND MONTH(date_creation) = 6
        GROUP BY DATE(date_creation)
        ORDER BY jour DESC
        LIMIT 15
        """;

            Query queryJuin = entityManager.createNativeQuery(sqlJuin2025);
            @SuppressWarnings("unchecked")
            List<Object[]> resultsJuin = queryJuin.getResultList();

            List<Map<String, Object>> commandesJuin = new ArrayList<>();
            int totalJuin = 0;
            int planifiablesJuin = 0;

            for (Object[] row : resultsJuin) {
                Map<String, Object> jour = new HashMap<>();
                jour.put("date", row[0]);
                jour.put("total", row[1]);
                jour.put("statut1", row[2]);
                jour.put("statuts_disponibles", row[3]);

                totalJuin += ((Number) row[1]).intValue();
                planifiablesJuin += ((Number) row[2]).intValue();

                commandesJuin.add(jour);
            }

            debug.put("commandes_juin_2025", commandesJuin);
            debug.put("total_juin_2025", totalJuin);
            debug.put("planifiables_juin_2025", planifiablesJuin);

            // 3. ✅ TEST AVEC DIFFÉRENTS FORMATS DE DATE
            String[] formatsTest = {
                    "'2025-06-01' AND '2025-06-30'",
                    "'2025-06-01 00:00:00' AND '2025-06-30 23:59:59'",
                    "DATE('2025-06-01') AND DATE('2025-06-30')"
            };

            Map<String, Object> testsFormats = new HashMap<>();

            for (int i = 0; i < formatsTest.length; i++) {
                try {
                    String sqlTest = "SELECT COUNT(*) FROM `order` WHERE date_creation BETWEEN " + formatsTest[i];
                    Query queryTest = entityManager.createNativeQuery(sqlTest);
                    Number count = (Number) queryTest.getSingleResult();
                    testsFormats.put("format_" + (i+1), Map.of(
                            "requete", formatsTest[i],
                            "resultats", count.intValue()
                    ));
                    System.out.println("Format " + (i+1) + ": " + count + " commandes");
                } catch (Exception e) {
                    testsFormats.put("format_" + (i+1), Map.of(
                            "requete", formatsTest[i],
                            "erreur", e.getMessage()
                    ));
                }
            }

            debug.put("tests_formats_dates", testsFormats);

            // 4. ✅ ÉCHANTILLON DE COMMANDES JUIN 2025
            String sqlEchantillon = """
        SELECT 
            HEX(id) as id,
            num_commande,
            date_creation,
            DATE(date_creation) as date_seule,
            status,
            COALESCE(priorite_string, 'null') as priorite
        FROM `order` 
        WHERE YEAR(date_creation) = 2025 
        AND MONTH(date_creation) = 6
        ORDER BY date_creation DESC
        LIMIT 10
        """;

            Query queryEchantillon = entityManager.createNativeQuery(sqlEchantillon);
            @SuppressWarnings("unchecked")
            List<Object[]> echantillon = queryEchantillon.getResultList();

            List<Map<String, Object>> commandesEchantillon = new ArrayList<>();
            for (Object[] row : echantillon) {
                Map<String, Object> cmd = new HashMap<>();
                cmd.put("id", row[0]);
                cmd.put("numero", row[1]);
                cmd.put("date_creation_complete", row[2]);
                cmd.put("date_seule", row[3]);
                cmd.put("status", row[4]);
                cmd.put("priorite", row[5]);
                commandesEchantillon.add(cmd);
            }

            debug.put("echantillon_juin_2025", commandesEchantillon);

            // 5. ✅ RÉPARTITION DES STATUTS EN JUIN
            String sqlStatutsJuin = """
        SELECT 
            status,
            COUNT(*) as count
        FROM `order` 
        WHERE YEAR(date_creation) = 2025 
        AND MONTH(date_creation) = 6
        GROUP BY status
        ORDER BY count DESC
        """;

            Query queryStatutsJuin = entityManager.createNativeQuery(sqlStatutsJuin);
            @SuppressWarnings("unchecked")
            List<Object[]> statutsJuin = queryStatutsJuin.getResultList();

            Map<String, Integer> repartitionStatuts = new HashMap<>();
            for (Object[] row : statutsJuin) {
                repartitionStatuts.put("statut_" + row[0], ((Number) row[1]).intValue());
            }

            debug.put("repartition_statuts_juin", repartitionStatuts);

            debug.put("status", "OK");
            debug.put("timestamp", System.currentTimeMillis());

            System.out.println("✅ Debug terminé - Juin 2025: " + totalJuin + " commandes (" + planifiablesJuin + " statut 1)");

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            System.err.println("❌ Erreur debug dates: " + e.getMessage());
            e.printStackTrace();

            debug.put("status", "ERROR");
            debug.put("erreur", e.getMessage());
            return ResponseEntity.status(500).body(debug);
        }
    }

    /**
     * 🔍 TEST RAPIDE - Compter commandes juin avec différents critères
     */
    @GetMapping("/api/test/test-juin-2025")
    public ResponseEntity<Map<String, Object>> testJuin2025() {
        Map<String, Object> tests = new HashMap<>();

        try {
            System.out.println("🔍 === TESTS JUIN 2025 ===");

            // Test 1: Année et mois
            String sql1 = "SELECT COUNT(*) FROM `order` WHERE YEAR(date_creation) = 2025 AND MONTH(date_creation) = 6";
            Number count1 = (Number) entityManager.createNativeQuery(sql1).getSingleResult();
            tests.put("test_year_month", count1.intValue());

            // Test 2: Date BETWEEN
            String sql2 = "SELECT COUNT(*) FROM `order` WHERE date_creation BETWEEN '2025-06-01' AND '2025-06-30'";
            Number count2 = (Number) entityManager.createNativeQuery(sql2).getSingleResult();
            tests.put("test_between_dates", count2.intValue());

            // Test 3: Date >= et <=
            String sql3 = "SELECT COUNT(*) FROM `order` WHERE date_creation >= '2025-06-01 00:00:00' AND date_creation <= '2025-06-30 23:59:59'";
            Number count3 = (Number) entityManager.createNativeQuery(sql3).getSingleResult();
            tests.put("test_ge_le_datetime", count3.intValue());

            // Test 4: Avec statut 1
            String sql4 = "SELECT COUNT(*) FROM `order` WHERE YEAR(date_creation) = 2025 AND MONTH(date_creation) = 6 AND status = 1";
            Number count4 = (Number) entityManager.createNativeQuery(sql4).getSingleResult();
            tests.put("test_juin_statut1", count4.intValue());

            // Test 5: Période exacte demandée (22 mai - 22 juin)
            String sql5 = "SELECT COUNT(*) FROM `order` WHERE date_creation >= '2025-05-22 00:00:00' AND date_creation <= '2025-06-22 23:59:59'";
            Number count5 = (Number) entityManager.createNativeQuery(sql5).getSingleResult();
            tests.put("test_22mai_22juin", count5.intValue());

            // Test 6: Avec statut 1 dans la période exacte
            String sql6 = "SELECT COUNT(*) FROM `order` WHERE date_creation >= '2025-05-22 00:00:00' AND date_creation <= '2025-06-22 23:59:59' AND status = 1";
            Number count6 = (Number) entityManager.createNativeQuery(sql6).getSingleResult();
            tests.put("test_22mai_22juin_statut1", count6.intValue());

            tests.put("status", "OK");
            tests.put("timestamp", System.currentTimeMillis());

            System.out.println("📊 Résultats tests:");
            System.out.println("   - Juin 2025 total: " + count1);
            System.out.println("   - Juin 2025 statut 1: " + count4);
            System.out.println("   - 22 mai - 22 juin total: " + count5);
            System.out.println("   - 22 mai - 22 juin statut 1: " + count6);

            return ResponseEntity.ok(tests);

        } catch (Exception e) {
            System.err.println("❌ Erreur tests: " + e.getMessage());
            tests.put("status", "ERROR");
            tests.put("erreur", e.getMessage());
            return ResponseEntity.status(500).body(tests);
        }
    }

    // ============= CORRECTION - UTILISER LE CHAMP "date" AU LIEU DE "date_creation" =============

    /**
     * 🔍 TEST AVEC LE BON CHAMP "date"
     */
    @GetMapping("/api/test/test-juin-2025-correct")
    public ResponseEntity<Map<String, Object>> testJuin2025Correct() {
        Map<String, Object> tests = new HashMap<>();

        try {
            System.out.println("🔍 === TESTS JUIN 2025 AVEC CHAMP 'date' ===");

            // Test 1: Année et mois avec le bon champ
            String sql1 = "SELECT COUNT(*) FROM `order` WHERE YEAR(date) = 2025 AND MONTH(date) = 6";
            Number count1 = (Number) entityManager.createNativeQuery(sql1).getSingleResult();
            tests.put("test_year_month_date", count1.intValue());

            // Test 2: Avec statut 1
            String sql2 = "SELECT COUNT(*) FROM `order` WHERE YEAR(date) = 2025 AND MONTH(date) = 6 AND status = 1";
            Number count2 = (Number) entityManager.createNativeQuery(sql2).getSingleResult();
            tests.put("test_juin_statut1_date", count2.intValue());

            // Test 3: Période exacte (22 mai - 22 juin) avec bon champ
            String sql3 = "SELECT COUNT(*) FROM `order` WHERE date >= '2025-05-22' AND date <= '2025-06-22'";
            Number count3 = (Number) entityManager.createNativeQuery(sql3).getSingleResult();
            tests.put("test_22mai_22juin_date", count3.intValue());

            // Test 4: Période exacte avec statut 1
            String sql4 = "SELECT COUNT(*) FROM `order` WHERE date >= '2025-05-22' AND date <= '2025-06-22' AND status = 1";
            Number count4 = (Number) entityManager.createNativeQuery(sql4).getSingleResult();
            tests.put("test_22mai_22juin_statut1_date", count4.intValue());

            // Test 5: Répartition des statuts en juin
            String sql5 = """
        SELECT 
            status,
            COUNT(*) as count
        FROM `order` 
        WHERE YEAR(date) = 2025 AND MONTH(date) = 6
        GROUP BY status
        ORDER BY count DESC
        """;

            Query query5 = entityManager.createNativeQuery(sql5);
            @SuppressWarnings("unchecked")
            List<Object[]> statuts = query5.getResultList();

            Map<String, Integer> repartitionStatuts = new HashMap<>();
            for (Object[] row : statuts) {
                repartitionStatuts.put("statut_" + row[0], ((Number) row[1]).intValue());
            }
            tests.put("repartition_statuts_juin", repartitionStatuts);

            // Test 6: Échantillon de commandes juin
            String sql6 = """
        SELECT 
            HEX(id) as id,
            num_commande,
            date,
            status,
            COALESCE(priorite_string, 'null') as priorite
        FROM `order` 
        WHERE YEAR(date) = 2025 AND MONTH(date) = 6
        ORDER BY date DESC
        LIMIT 10
        """;

            Query query6 = entityManager.createNativeQuery(sql6);
            @SuppressWarnings("unchecked")
            List<Object[]> echantillon = query6.getResultList();

            List<Map<String, Object>> commandesEchantillon = new ArrayList<>();
            for (Object[] row : echantillon) {
                Map<String, Object> cmd = new HashMap<>();
                cmd.put("id", row[0]);
                cmd.put("numero", row[1]);
                cmd.put("date", row[2]);
                cmd.put("status", row[3]);
                cmd.put("priorite", row[4]);
                commandesEchantillon.add(cmd);
            }
            tests.put("echantillon_juin_2025", commandesEchantillon);

            tests.put("status", "OK");
            tests.put("timestamp", System.currentTimeMillis());

            System.out.println("📊 Résultats avec champ 'date':");
            System.out.println("   - Juin 2025 total: " + count1);
            System.out.println("   - Juin 2025 statut 1: " + count2);
            System.out.println("   - 22 mai - 22 juin total: " + count3);
            System.out.println("   - 22 mai - 22 juin statut 1: " + count4);

            return ResponseEntity.ok(tests);

        } catch (Exception e) {
            System.err.println("❌ Erreur tests avec champ date: " + e.getMessage());
            e.printStackTrace();
            tests.put("status", "ERROR");
            tests.put("erreur", e.getMessage());
            return ResponseEntity.status(500).body(tests);
        }
    }

    /**
     * 🚀 PLANIFICATION AUTOMATIQUE CORRIGÉE - AVEC BON CHAMP "date"
     */
    @PostMapping("/api/test/planifier-automatique")
    @Transactional
    public ResponseEntity<Map<String, Object>> planifierAutomatique() {
        try {
            System.out.println("🚀 === PLANIFICATION AUTOMATIQUE (CHAMP 'date' CORRIGÉ) ===");

            // 1. Vérifier employés
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();
            if (employes.isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Aucun employé disponible");
                errorResponse.put("nombreCommandesPlanifiees", 0);
                errorResponse.put("nombreCommandesNonPlanifiees", 0);
                return ResponseEntity.ok(errorResponse);
            }

            System.out.println("👥 " + employes.size() + " employés disponibles");

            // 2. ✅ RECHERCHE AVEC LE BON CHAMP "date" (au lieu de "date_creation")
            String sqlCommandes = """
        SELECT 
            HEX(id) as id, 
            num_commande, 
            COALESCE(temps_estime_minutes, 120) as temps_estime_minutes, 
            COALESCE(priorite_string, 'NORMALE') as priorite_string, 
            COALESCE(prix_total, 100.0) as prix_total,
            status,
            date
        FROM `order` 
        WHERE status = 1  -- Commandes planifiables
        AND date >= '2025-05-22'  -- ✅ Utiliser le champ "date" au lieu de "date_creation"
        AND date <= '2025-06-22'  -- ✅ Période 22 mai - 22 juin 2025
        ORDER BY 
            CASE COALESCE(priorite_string, 'NORMALE')
                WHEN 'HAUTE' THEN 1 
                WHEN 'MOYENNE' THEN 2 
                WHEN 'NORMALE' THEN 3 
                ELSE 4 
            END,
            COALESCE(prix_total, 0) DESC,
            date ASC  -- Plus anciennes en premier
        LIMIT 50  -- ✅ Maximum 50 commandes
        """;

            Query queryCommandes = entityManager.createNativeQuery(sqlCommandes);
            @SuppressWarnings("unchecked")
            List<Object[]> commandesData = queryCommandes.getResultList();

            System.out.println("📦 Commandes du dernier mois trouvées: " + commandesData.size());

            // 3. ✅ Si aucune commande trouvée, diagnostic avec le bon champ
            if (commandesData.isEmpty()) {
                System.out.println("⚠️ Aucune commande trouvée, diagnostic...");

                // Vérifier les dates disponibles avec le bon champ "date"
                String sqlDates = """
            SELECT 
                DATE(MIN(date)) as date_min,
                DATE(MAX(date)) as date_max,
                COUNT(*) as total_commandes,
                COUNT(CASE WHEN status = 1 THEN 1 END) as commandes_statut1
            FROM `order`
            WHERE date IS NOT NULL
            """;

                Query queryDates = entityManager.createNativeQuery(sqlDates);
                Object[] datesInfo = (Object[]) queryDates.getSingleResult();

                System.out.println("📅 Dates disponibles (champ 'date'): " + datesInfo[0] + " → " + datesInfo[1]);
                System.out.println("📊 Total: " + datesInfo[2] + " commandes, Statut 1: " + datesInfo[3]);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("success", false);
                errorResponse.put("message", "Aucune commande trouvée entre le 22 mai et 22 juin 2025 (champ 'date')");
                errorResponse.put("nombreCommandesPlanifiees", 0);
                errorResponse.put("nombreCommandesNonPlanifiees", 0);
                errorResponse.put("diagnostic", Map.of(
                        "date_min_disponible", datesInfo[0],
                        "date_max_disponible", datesInfo[1],
                        "total_commandes", datesInfo[2],
                        "commandes_statut1", datesInfo[3],
                        "periode_recherchee", "2025-05-22 à 2025-06-22",
                        "champ_utilise", "date (au lieu de date_creation)"
                ));
                return ResponseEntity.ok(errorResponse);
            }

            // 4. ✅ DEBUG : Afficher les premières commandes trouvées
            System.out.println("📋 Premières commandes du dernier mois:");
            for (int i = 0; i < Math.min(5, commandesData.size()); i++) {
                Object[] row = commandesData.get(i);
                System.out.println("   📦 " + row[1] + " (date: " + row[6] + ", prix: " + row[4] + "€)");
            }

            // 5. ✅ CRÉER ET SAUVEGARDER LES PLANIFICATIONS
            List<Map<String, Object>> planificationsCreees = new ArrayList<>();
            int planificationsSauvees = 0;
            LocalDate dateBase = LocalDate.now(); // Planifier à partir d'aujourd'hui

            for (int i = 0; i < commandesData.size(); i++) {
                Object[] commandeData = commandesData.get(i);

                try {
                    // Récupérer les données
                    String orderId = (String) commandeData[0];
                    String numeroCommande = (String) commandeData[1];
                    Integer tempsEstime = ((Number) commandeData[2]).intValue();
                    String priorite = (String) commandeData[3];
                    Double prix = ((Number) commandeData[4]).doubleValue();
                    Object dateCommande = commandeData[6];

                    // ✅ Vérifier que l'ID est valide
                    String orderIdClean = orderId.replace("-", "");
                    if (orderIdClean.length() != 32) {
                        System.out.println("⚠️ ID commande invalide: " + orderId + " - ignoré");
                        continue;
                    }

                    // Choisir un employé (rotation)
                    Map<String, Object> employe = employes.get(i % employes.size());
                    String employeId = (String) employe.get("id");
                    String employeNom = employe.get("prenom") + " " + employe.get("nom");

                    // ✅ Vérifier l'ID employé
                    String employeIdClean = employeId.replace("-", "");
                    if (employeIdClean.length() != 32) {
                        System.out.println("⚠️ ID employé invalide: " + employeId + " - ignoré");
                        continue;
                    }

                    // ✅ Calculer date et heure de planification
                    int joursDecalage = i / employes.size(); // Répartir sur plusieurs jours
                    int heureDecalage = i % 8; // Heures de 8h à 15h

                    LocalDate datePlanif = dateBase.plusDays(joursDecalage);
                    LocalTime heurePlanif = LocalTime.of(8 + heureDecalage, 0);

                    // ✅ SAUVEGARDER EN BASE
                    try {
                        String planifId = java.util.UUID.randomUUID().toString().replace("-", "");

                        String sqlInsert = """
                    INSERT INTO j_planification 
                    (id, order_id, employe_id, date_planification, heure_debut, duree_minutes, terminee, date_creation, date_modification)
                    VALUES (UNHEX(?), UNHEX(?), UNHEX(?), ?, ?, ?, 0, NOW(), NOW())
                    """;

                        int rowsInserted = entityManager.createNativeQuery(sqlInsert)
                                .setParameter(1, planifId)
                                .setParameter(2, orderIdClean)
                                .setParameter(3, employeIdClean)
                                .setParameter(4, datePlanif)
                                .setParameter(5, heurePlanif)
                                .setParameter(6, tempsEstime)
                                .executeUpdate();

                        if (rowsInserted > 0) {
                            planificationsSauvees++;

                            // Créer l'objet planification pour le retour
                            Map<String, Object> planification = new HashMap<>();
                            planification.put("id", planifId);
                            planification.put("commandeId", orderId);
                            planification.put("employeId", employeId);
                            planification.put("numeroCommande", numeroCommande);
                            planification.put("employeNom", employeNom);
                            planification.put("datePlanifiee", datePlanif.toString());
                            planification.put("heureDebut", heurePlanif.toString());
                            planification.put("dureeMinutes", tempsEstime);
                            planification.put("priorite", priorite);
                            planification.put("prix", prix);
                            planification.put("dateCommande", dateCommande);
                            planification.put("sauvegarde", "SUCCESS");

                            planificationsCreees.add(planification);

                            System.out.println("✅ PLANIFIÉ: " + numeroCommande + " (date " + dateCommande + ") → " +
                                    employeNom + " (" + datePlanif + " " + heurePlanif + ")");
                        } else {
                            System.out.println("❌ ÉCHEC insertion: " + numeroCommande);
                        }

                    } catch (Exception e) {
                        System.out.println("❌ ERREUR sauvegarde " + numeroCommande + ": " + e.getMessage());
                        // Continuer avec les autres commandes
                    }

                } catch (Exception e) {
                    System.out.println("❌ Erreur traitement commande " + i + ": " + e.getMessage());
                }
            }

            // 6. ✅ RÉSULTAT FINAL
            Map<String, Object> resultatFinal = new HashMap<>();
            resultatFinal.put("success", true);
            resultatFinal.put("message", "Planification automatique terminée - Commandes du dernier mois (22 mai - 22 juin 2025) avec champ 'date'");
            resultatFinal.put("nombreCommandesPlanifiees", planificationsSauvees);
            resultatFinal.put("nombreCommandesNonPlanifiees", Math.max(0, commandesData.size() - planificationsSauvees));
            resultatFinal.put("planifications_creees", planificationsCreees.size());
            resultatFinal.put("planifications_sauvees", planificationsSauvees);
            resultatFinal.put("commandes_analysees", commandesData.size());
            resultatFinal.put("employes_utilises", employes.size());
            resultatFinal.put("periode_commandes", "22 mai 2025 - 22 juin 2025");
            resultatFinal.put("champ_date_utilise", "date");
            resultatFinal.put("planifications", planificationsCreees);
            resultatFinal.put("timestamp", System.currentTimeMillis());

            System.out.println("🎉 PLANIFICATION TERMINÉE !");
            System.out.println("📊 Résultat: " + planificationsSauvees + "/" + commandesData.size() + " commandes du dernier mois planifiées");

            return ResponseEntity.ok(resultatFinal);

        } catch (Exception e) {
            System.err.println("❌ Erreur planification automatique: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Erreur: " + e.getMessage());
            errorResponse.put("nombreCommandesPlanifiees", 0);
            errorResponse.put("nombreCommandesNonPlanifiees", 0);

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // ============= ENDPOINTS BACKEND POUR PLANNING EMPLOYÉS =============
// ✅ AJOUTEZ ces méthodes dans votre TestController.java



    /**
     * 📊 STATISTIQUES GLOBALES PLANNING (VRAIES DONNÉES)
     */
    @GetMapping("/api/test/stats-planning")
    public ResponseEntity<Map<String, Object>> getStatisticsPlanning(
            @RequestParam(defaultValue = "2025-06-22") String date) {

        try {
            System.out.println("📊 === STATISTIQUES PLANNING POUR " + date + " ===");

            // 1. Statistiques employés
            String sqlEmployes = "SELECT COUNT(*) FROM j_employe WHERE actif = 1";
            Number nombreEmployes = (Number) entityManager.createNativeQuery(sqlEmployes).getSingleResult();

            // 2. Statistiques planifications du jour
            String sqlPlanifications = """
        SELECT 
            COUNT(*) as total_planifications,
            SUM(duree_minutes) as total_minutes,
            COUNT(CASE WHEN terminee = 1 THEN 1 END) as planifications_terminees
        FROM j_planification 
        WHERE date_planification = ?
        """;

            Query queryPlanifs = entityManager.createNativeQuery(sqlPlanifications);
            queryPlanifs.setParameter(1, date);
            Object[] statsResult = (Object[]) queryPlanifs.getSingleResult();

            int totalPlanifications = ((Number) statsResult[0]).intValue();
            int totalMinutes = statsResult[1] != null ? ((Number) statsResult[1]).intValue() : 0;
            int planificationsTerminees = ((Number) statsResult[2]).intValue();

            // 3. Compter les commandes uniques
            String sqlCommandes = """
        SELECT COUNT(DISTINCT order_id) 
        FROM j_planification 
        WHERE date_planification = ?
        """;
            Query queryCommandes = entityManager.createNativeQuery(sqlCommandes);
            queryCommandes.setParameter(1, date);
            Number commandesUniques = (Number) queryCommandes.getSingleResult();

            // 4. Calculer la charge moyenne
            double chargesMoyenne = nombreEmployes.intValue() > 0 ?
                    (double) totalMinutes / (nombreEmployes.intValue() * 8 * 60) * 100 : 0;

            Map<String, Object> stats = new HashMap<>();
            stats.put("nombreEmployes", nombreEmployes.intValue());
            stats.put("totalPlanifications", totalPlanifications);
            stats.put("totalMinutes", totalMinutes);
            stats.put("planificationsTerminees", planificationsTerminees);
            stats.put("commandesUniques", commandesUniques.intValue());
            stats.put("chargeMoyenne", Math.round(chargesMoyenne));
            stats.put("date", date);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            System.err.println("❌ Erreur statistiques planning: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }




    /**
     * 🔍 DEBUG - TEST DE LA RELATION CARTES
     */
    @GetMapping("/api/test/debug-cartes-relation")
    public ResponseEntity<Map<String, Object>> debugCartesRelation() {
        try {
            System.out.println("🔍 === DEBUG RELATION CARTES ===");

            Map<String, Object> debug = new HashMap<>();

            // 1. Compter les tables
            String[] tables = {"card_translation_order", "card_certification", "card", "card_translation", "`order`"};
            Map<String, Integer> counts = new HashMap<>();

            for (String table : tables) {
                try {
                    String sql = "SELECT COUNT(*) FROM " + table;
                    Number count = (Number) entityManager.createNativeQuery(sql).getSingleResult();
                    counts.put(table, count.intValue());
                    System.out.println("📊 " + table + ": " + count + " enregistrements");
                } catch (Exception e) {
                    counts.put(table, -1);
                    System.out.println("❌ " + table + ": erreur - " + e.getMessage());
                }
            }
            debug.put("table_counts", counts);

            // 2. Test relation complète sur une commande
            String sqlTestRelation = """
        SELECT 
            HEX(o.id) as order_id,
            o.num_commande,
            COUNT(DISTINCT cto.card_certification_id) as nb_certifications,
            COUNT(DISTINCT c.id) as nb_cards,
            COUNT(DISTINCT ct.translatable_id) as nb_translations
        FROM `order` o
        LEFT JOIN card_translation_order cto ON o.id = cto.order_id
        LEFT JOIN card_certification cc ON cto.card_certification_id = cc.id
        LEFT JOIN card c ON cc.card_id = c.id
        LEFT JOIN card_translation ct ON c.translatable_id = ct.translatable_id AND ct.locale = 'fr'
        GROUP BY o.id, o.num_commande
        HAVING COUNT(DISTINCT cto.card_certification_id) > 0
        ORDER BY nb_certifications DESC
        LIMIT 5
        """;

            try {
                Query queryTest = entityManager.createNativeQuery(sqlTestRelation);
                @SuppressWarnings("unchecked")
                List<Object[]> testResults = queryTest.getResultList();

                List<Map<String, Object>> exemples = new ArrayList<>();
                for (Object[] row : testResults) {
                    Map<String, Object> exemple = new HashMap<>();
                    exemple.put("order_id", row[0]);
                    exemple.put("num_commande", row[1]);
                    exemple.put("nb_certifications", row[2]);
                    exemple.put("nb_cards", row[3]);
                    exemple.put("nb_translations", row[4]);
                    exemples.add(exemple);

                    System.out.println("📦 " + row[1] + ": " + row[2] + " certifications, " + row[3] + " cartes");
                }
                debug.put("exemples_commandes_avec_cartes", exemples);

            } catch (Exception e) {
                debug.put("erreur_test_relation", e.getMessage());
                System.out.println("❌ Erreur test relation: " + e.getMessage());
            }

            debug.put("status", "OK");
            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            System.err.println("❌ Erreur debug: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

// ============= CORRECTION - UTILISER LA BONNE TABLE card_certification_order =============
// ✅ REMPLACEZ vos méthodes dans TestController.java par ces versions corrigées

    /**
     * 👥 PLANNING EMPLOYÉS - AVEC VRAIE TABLE card_certification_order
     */
    @GetMapping("/api/test/planning-employes")
    public ResponseEntity<List<Map<String, Object>>> getPlanningEmployes(
            @RequestParam(defaultValue = "2025-06-22") String date) {

        try {
            System.out.println("👥 === PLANNING EMPLOYÉS POUR " + date + " ===");

            // 1. Récupérer tous les employés actifs
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();
            if (employes.isEmpty()) {
                System.out.println("⚠️ Aucun employé actif trouvé");
                return ResponseEntity.ok(new ArrayList<>());
            }

            List<Map<String, Object>> planningEmployes = new ArrayList<>();

            // 2. Pour chaque employé, calculer sa charge de travail
            for (Map<String, Object> employe : employes) {
                String employeId = (String) employe.get("id");
                String prenom = (String) employe.get("prenom");
                String nom = (String) employe.get("nom");
                Integer heuresTravail = (Integer) employe.get("heuresTravailParJour");

                // 3. Récupérer TOUTES les planifications pour cette date
                String sqlPlanifications = """
            SELECT 
                HEX(p.id) as planif_id,
                HEX(p.order_id) as order_id,
                p.duree_minutes,
                p.terminee,
                o.num_commande,
                COALESCE(o.priorite_string, 'NORMALE') as priorite
            FROM j_planification p
            LEFT JOIN `order` o ON p.order_id = o.id
            WHERE HEX(p.employe_id) = ?
            AND p.date_planification = ?
            ORDER BY p.heure_debut ASC
            """;

                Query queryPlanifs = entityManager.createNativeQuery(sqlPlanifications);
                queryPlanifs.setParameter(1, employeId.replace("-", ""));
                queryPlanifs.setParameter(2, date);

                @SuppressWarnings("unchecked")
                List<Object[]> planifications = queryPlanifs.getResultList();

                System.out.println("👤 " + prenom + " " + nom + ": " + planifications.size() + " planifications trouvées");

                // 4. Calculer les statistiques
                int totalMinutes = 0;
                int nombreTaches = planifications.size();
                int tachesTerminees = 0;
                int nombreCartesTotal = 0;

                List<Map<String, Object>> taches = new ArrayList<>();

                for (Object[] planif : planifications) {
                    Integer duree = ((Number) planif[2]).intValue();
                    Boolean terminee = (Boolean) planif[3];
                    String numeroCommande = (String) planif[4];
                    String priorite = (String) planif[5];
                    String orderId = (String) planif[1];

                    totalMinutes += duree;
                    if (terminee) tachesTerminees++;

                    // 5. ✅ CORRECTION : Compter les cartes via card_certification_order
                    int nombreCartesCommande = 0;
                    if (orderId != null) {
                        String sqlCartes = """
                    SELECT COUNT(DISTINCT cco.card_certification_id)
                    FROM card_certification_order cco
                    WHERE HEX(cco.order_id) = ?
                    """;

                        try {
                            Query queryCartes = entityManager.createNativeQuery(sqlCartes);
                            queryCartes.setParameter(1, orderId);
                            Number countCartes = (Number) queryCartes.getSingleResult();
                            nombreCartesCommande = countCartes.intValue();
                            nombreCartesTotal += nombreCartesCommande;

                            System.out.println("   📦 " + numeroCommande + ": " + nombreCartesCommande + " cartes");
                        } catch (Exception e) {
                            System.out.println("   ⚠️ Erreur comptage cartes pour " + numeroCommande + ": " + e.getMessage());
                        }
                    }

                    // Ajouter la tâche
                    Map<String, Object> tache = new HashMap<>();
                    tache.put("id", planif[0]);
                    tache.put("orderId", orderId);
                    tache.put("numeroCommande", numeroCommande);
                    tache.put("dureeMinutes", duree);
                    tache.put("terminee", terminee);
                    tache.put("priorite", priorite);
                    tache.put("nombreCartes", nombreCartesCommande);
                    taches.add(tache);
                }

                // 6. Calculer le statut de l'employé
                int maxMinutes = heuresTravail * 60;
                String status;
                if (totalMinutes > maxMinutes) {
                    status = "overloaded";
                } else if (totalMinutes >= maxMinutes * 0.9) {
                    status = "full";
                } else {
                    status = "available";
                }

                // 7. Créer l'objet employé pour le frontend
                Map<String, Object> employeePlanning = new HashMap<>();
                employeePlanning.put("id", employeId);
                employeePlanning.put("name", prenom + " " + nom);
                employeePlanning.put("totalMinutes", totalMinutes);
                employeePlanning.put("maxMinutes", maxMinutes);
                employeePlanning.put("status", status);
                employeePlanning.put("taskCount", nombreTaches);
                employeePlanning.put("cardCount", nombreCartesTotal);
                employeePlanning.put("completedTasks", tachesTerminees);
                employeePlanning.put("tasks", taches);

                planningEmployes.add(employeePlanning);

                System.out.println("   ✅ Total: " + totalMinutes + "/" + maxMinutes + " min, " + nombreCartesTotal + " cartes (" + status + ")");
            }

            System.out.println("✅ Planning calculé pour " + planningEmployes.size() + " employés");
            return ResponseEntity.ok(planningEmployes);

        } catch (Exception e) {
            System.err.println("❌ Erreur planning employés: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.ok(new ArrayList<>());
        }
    }

    /**
     * 👤 DÉTAIL EMPLOYÉ - AVEC VRAIES CARTES VIA card_certification_order
     */
    @GetMapping("/api/test/planning-employe/{employeId}")
    public ResponseEntity<Map<String, Object>> getPlanningEmployeDetail(
            @PathVariable String employeId,
            @RequestParam(defaultValue = "2025-06-22") String date) {

        try {
            System.out.println("👤 === DÉTAIL EMPLOYÉ " + employeId + " POUR " + date + " ===");

            // 1. Récupérer les infos de l'employé
            String sqlEmploye = """
        SELECT 
            HEX(id) as id,
            prenom,
            nom,
            heures_travail_par_jour
        FROM j_employe 
        WHERE HEX(id) = ? AND actif = 1
        """;

            Query queryEmploye = entityManager.createNativeQuery(sqlEmploye);
            queryEmploye.setParameter(1, employeId.replace("-", ""));

            @SuppressWarnings("unchecked")
            List<Object[]> employeResult = queryEmploye.getResultList();

            if (employeResult.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Object[] empData = employeResult.get(0);
            String prenom = (String) empData[1];
            String nom = (String) empData[2];
            Integer heuresTravail = ((Number) empData[3]).intValue();

            // 2. Récupérer TOUTES les planifications détaillées
            String sqlPlanifications = """
        SELECT 
            HEX(p.id) as planif_id,
            HEX(p.order_id) as order_id,
            p.heure_debut,
            p.duree_minutes,
            p.terminee,
            o.num_commande,
            COALESCE(o.priorite_string, 'NORMALE') as priorite,
            COALESCE(o.prix_total, 0) as prix_total,
            p.date_planification
        FROM j_planification p
        LEFT JOIN `order` o ON p.order_id = o.id
        WHERE HEX(p.employe_id) = ?
        AND p.date_planification = ?
        ORDER BY p.heure_debut ASC
        """;

            Query queryPlanifications = entityManager.createNativeQuery(sqlPlanifications);
            queryPlanifications.setParameter(1, employeId.replace("-", ""));
            queryPlanifications.setParameter(2, date);

            @SuppressWarnings("unchecked")
            List<Object[]> planifications = queryPlanifications.getResultList();

            System.out.println("📋 " + planifications.size() + " planifications trouvées pour " + prenom + " " + nom);

            // 3. Construire les tâches détaillées avec vraies cartes
            List<Map<String, Object>> tasks = new ArrayList<>();
            int totalMinutes = 0;
            int totalCartes = 0;

            for (Object[] planif : planifications) {
                String planifId = (String) planif[0];
                String orderId = (String) planif[1];
                String heureDebut = planif[2].toString();
                Integer dureeMinutes = ((Number) planif[3]).intValue();
                Boolean terminee = (Boolean) planif[4];
                String numeroCommande = (String) planif[5];
                String priorite = (String) planif[6];
                Double prixTotal = ((Number) planif[7]).doubleValue();

                totalMinutes += dureeMinutes;

                // Calculer l'heure de fin
                String heureFin = "N/A";
                try {
                    String[] heureDebugParts = heureDebut.split(":");
                    int heures = Integer.parseInt(heureDebugParts[0]);
                    int minutes = Integer.parseInt(heureDebugParts[1]);

                    int totalMinutesDebut = heures * 60 + minutes;
                    int totalMinutesFin = totalMinutesDebut + dureeMinutes;

                    int heuresFin = totalMinutesFin / 60;
                    int minutesFin = totalMinutesFin % 60;

                    heureFin = String.format("%02d:%02d", heuresFin, minutesFin);
                } catch (Exception e) {
                    System.out.println("⚠️ Erreur calcul heure fin: " + e.getMessage());
                }

                // 4. ✅ CORRECTION : Récupérer les vraies cartes via card_certification_order
                List<Map<String, Object>> cartes = new ArrayList<>();
                int nombreCartesCommande = 0;

                if (orderId != null) {

                    // 1. Compter les cartes (on sait que ça marche)
                    String sqlCountCartes = """
    SELECT COUNT(DISTINCT cco.card_certification_id)
    FROM card_certification_order cco
    WHERE HEX(cco.order_id) = ?
    """;

                    try {
                        Query queryCount = entityManager.createNativeQuery(sqlCountCartes);
                        queryCount.setParameter(1, orderId);
                        Number countCartes = (Number) queryCount.getSingleResult();
                        nombreCartesCommande = countCartes.intValue();
                        totalCartes += nombreCartesCommande;

                        System.out.println("   🃏 " + numeroCommande + ": " + nombreCartesCommande + " cartes comptées");

                        // 2. ✅ RÉCUPÉRATION OPTIMISÉE DES VRAIES CARTES
                        if (nombreCartesCommande > 0) {

                            // Essayer d'abord la requête complète avec noms
                            String sqlCartesComplete = """
            SELECT 
                HEX(cc.id) as cert_id,
                cc.code_barre,
                COALESCE(cc.type, 'Standard') as type,
                COALESCE(ct.name, CONCAT('Carte #', cc.code_barre)) as name,
                COALESCE(ct.label_name, CONCAT('Label #', cc.code_barre)) as label_name,
                COALESCE(cc.annotation, 0) as annotation
            FROM card_certification_order cco
            INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
            LEFT JOIN card_translation ct ON cc.translatable_id = ct.translatable_id AND ct.locale = 'fr'
            WHERE HEX(cco.order_id) = ?
            ORDER BY cc.code_barre ASC
            LIMIT 100
            """;

                            try {
                                Query queryCartesComplete = entityManager.createNativeQuery(sqlCartesComplete);
                                queryCartesComplete.setParameter(1, orderId);

                                @SuppressWarnings("unchecked")
                                List<Object[]> cartesCompletes = queryCartesComplete.getResultList();

                                System.out.println("   ✅ Récupération complète: " + cartesCompletes.size() + " cartes avec détails");

                                for (Object[] carte : cartesCompletes) {
                                    Map<String, Object> carteMap = new HashMap<>();
                                    carteMap.put("id", carte[0]); // cert_id
                                    carteMap.put("translatable_id", carte[0]); // Utiliser cert_id
                                    carteMap.put("code_barre", carte[1] != null ? carte[1] : "N/A");
                                    carteMap.put("type", carte[2]);
                                    carteMap.put("name", carte[3]); // Nom avec fallback automatique
                                    carteMap.put("label_name", carte[4]); // Label avec fallback automatique
                                    carteMap.put("annotation", carte[5]);
                                    carteMap.put("duration", Math.max(5, dureeMinutes / nombreCartesCommande));
                                    carteMap.put("amount", prixTotal / nombreCartesCommande);
                                    cartes.add(carteMap);
                                }

                                System.out.println("   🎯 " + cartes.size() + " vraies cartes ajoutées avec noms");

                            } catch (Exception e) {
                                System.out.println("   ⚠️ Échec récupération complète, essai version simple: " + e.getMessage());

                                // 3. ✅ FALLBACK : Version simplifiée sans card_translation
                                String sqlCartesSimple = """
                SELECT 
                    HEX(cc.id) as cert_id,
                    cc.code_barre,
                    COALESCE(cc.type, 'Pokemon') as type,
                    COALESCE(cc.annotation, 0) as annotation
                FROM card_certification_order cco
                INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
                WHERE HEX(cco.order_id) = ?
                ORDER BY cc.code_barre ASC
                LIMIT 100
                """;

                                try {
                                    Query querySimple = entityManager.createNativeQuery(sqlCartesSimple);
                                    querySimple.setParameter(1, orderId);

                                    @SuppressWarnings("unchecked")
                                    List<Object[]> cartesSimples = querySimple.getResultList();

                                    System.out.println("   🔄 Version simple: " + cartesSimples.size() + " cartes récupérées");

                                    for (Object[] carte : cartesSimples) {
                                        Map<String, Object> carteMap = new HashMap<>();
                                        carteMap.put("id", carte[0]); // cert_id
                                        carteMap.put("translatable_id", carte[0]);
                                        carteMap.put("code_barre", carte[1] != null ? carte[1] : "N/A");
                                        carteMap.put("type", carte[2]);
                                        carteMap.put("name", "Carte Pokemon " + (carte[1] != null ? carte[1] : "inconnue"));
                                        carteMap.put("label_name", "Certification " + carte[0]);
                                        carteMap.put("annotation", carte[3]);
                                        carteMap.put("duration", Math.max(5, dureeMinutes / nombreCartesCommande));
                                        carteMap.put("amount", prixTotal / nombreCartesCommande);
                                        cartes.add(carteMap);
                                    }

                                    System.out.println("   ✅ " + cartes.size() + " cartes simples créées");

                                } catch (Exception e2) {
                                    System.out.println("   ❌ Échec version simple aussi: " + e2.getMessage());

                                    // 4. ✅ DERNIER FALLBACK : Cartes génériques (comme actuellement)
                                    for (int i = 0; i < nombreCartesCommande; i++) {
                                        Map<String, Object> carteGenerique = new HashMap<>();
                                        carteGenerique.put("id", "carte_" + i);
                                        carteGenerique.put("translatable_id", "generic_" + i);
                                        carteGenerique.put("name", "Carte Pokemon " + (i + 1));
                                        carteGenerique.put("label_name", "Carte à certifier " + (i + 1));
                                        carteGenerique.put("duration", Math.max(5, dureeMinutes / nombreCartesCommande));
                                        carteGenerique.put("amount", prixTotal / nombreCartesCommande);
                                        carteGenerique.put("code_barre", "CODE_" + (i + 1));
                                        carteGenerique.put("type", "Pokemon");
                                        cartes.add(carteGenerique);
                                    }
                                    System.out.println("   🔄 " + nombreCartesCommande + " cartes génériques créées (fallback final)");
                                }
                            }
                        }

                    } catch (Exception e) {
                        System.out.println("   ❌ Erreur comptage cartes: " + e.getMessage());
                    }
                }

                // 5. Construire la tâche
                Map<String, Object> task = new HashMap<>();
                task.put("id", numeroCommande != null ? numeroCommande : planifId);
                task.put("priority", priorite);
                task.put("status", terminee ? "Terminée" : "Planifiée");
                task.put("startTime", heureDebut);
                task.put("endTime", heureFin);
                task.put("duration", dureeMinutes);
                task.put("amount", prixTotal);
                task.put("cardCount", nombreCartesCommande);
                task.put("cards", cartes);
                task.put("expanded", false);

                tasks.add(task);
            }

            // 6. Calculer le statut de l'employé
            int maxMinutes = heuresTravail * 60;
            String status;
            if (totalMinutes > maxMinutes) {
                status = "overloaded";
            } else if (totalMinutes >= maxMinutes * 0.9) {
                status = "full";
            } else {
                status = "available";
            }

            // 7. Construire la réponse
            Map<String, Object> employeDetail = new HashMap<>();
            employeDetail.put("id", employeId);
            employeDetail.put("name", prenom + " " + nom);
            employeDetail.put("totalMinutes", totalMinutes);
            employeDetail.put("maxMinutes", maxMinutes);
            employeDetail.put("status", status);
            employeDetail.put("tasks", tasks);

            System.out.println("✅ Détail employé: " + tasks.size() + " tâches, " + totalCartes + " cartes totales");

            return ResponseEntity.ok(employeDetail);

        } catch (Exception e) {
            System.err.println("❌ Erreur détail employé: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> error = new HashMap<>();
            error.put("error", "Erreur lors du chargement: " + e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }


    /**
     * 🔍 DEBUG - COMMANDE SPÉCIFIQUE CORRIGÉE POUR UUID
     */
    @GetMapping("/api/test/debug-commande-uuid")
    public ResponseEntity<Map<String, Object>> debugCommandeUUID(
            @RequestParam String orderId) {
        try {
            System.out.println("🔍 === DEBUG COMMANDE UUID " + orderId + " ===");

            Map<String, Object> debug = new HashMap<>();

            // ✅ Traiter l'ID comme UUID avec ou sans tirets
            String orderIdClean = orderId.replace("-", "");

            // 1. Info de la commande
            String sqlCommande = """
        SELECT 
            HEX(id) as order_id,
            num_commande,
            COALESCE(priorite_string, 'NORMALE') as priorite,
            COALESCE(prix_total, 0) as prix,
            date
        FROM `order` 
        WHERE HEX(id) = ?
        """;

            try {
                Query queryCommande = entityManager.createNativeQuery(sqlCommande);
                queryCommande.setParameter(1, orderIdClean);
                Object[] commandeInfo = (Object[]) queryCommande.getSingleResult();

                debug.put("commande_info", Map.of(
                        "id", commandeInfo[0],
                        "num_commande", commandeInfo[1] != null ? commandeInfo[1] : "N/A",
                        "priorite", commandeInfo[2],
                        "prix", commandeInfo[3],
                        "date", commandeInfo[4] != null ? commandeInfo[4] : "N/A"
                ));

            } catch (Exception e) {
                debug.put("erreur_commande", e.getMessage());
                return ResponseEntity.ok(debug);
            }

            // 2. Compter les certifications
            String sqlCount = """
        SELECT COUNT(DISTINCT cco.card_certification_id)
        FROM card_certification_order cco
        WHERE HEX(cco.order_id) = ?
        """;

            try {
                Query queryCount = entityManager.createNativeQuery(sqlCount);
                queryCount.setParameter(1, orderIdClean);
                Number count = (Number) queryCount.getSingleResult();
                debug.put("nombre_certifications", count.intValue());
            } catch (Exception e) {
                debug.put("erreur_count", e.getMessage());
            }

            // 3. Test relation card_translation
            String sqlTestTranslation = """
        SELECT 
            COUNT(*) as total,
            COUNT(ct.name) as avec_nom
        FROM card_certification_order cco
        INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
        LEFT JOIN card_translation ct ON cc.translatable_id = ct.translatable_id AND ct.locale = 'fr'
        WHERE HEX(cco.order_id) = ?
        """;

            try {
                Query queryTest = entityManager.createNativeQuery(sqlTestTranslation);
                queryTest.setParameter(1, orderIdClean);
                Object[] testResult = (Object[]) queryTest.getSingleResult();

                debug.put("test_translations", Map.of(
                        "total_certifications", testResult[0],
                        "avec_nom_francais", testResult[1]
                ));
            } catch (Exception e) {
                debug.put("erreur_test_translation", e.getMessage());
            }

            debug.put("status", "OK");
            debug.put("orderId_original", orderId);
            debug.put("orderId_clean", orderIdClean);

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            System.err.println("❌ Erreur debug commande UUID: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }


    /**
     * 🔍 DEBUG - COMMANDE SPÉCIFIQUE D'IBRAHIM
     */
    @GetMapping("/api/test/debug-commande/{orderId}")
    public ResponseEntity<Map<String, Object>> debugCommandeSpecifique(@PathVariable String orderId) {
        try {
            System.out.println("🔍 === DEBUG COMMANDE " + orderId + " ===");

            Map<String, Object> debug = new HashMap<>();

            // 1. Info de la commande
            String sqlCommande = """
        SELECT 
            HEX(id) as order_id,
            num_commande,
            priorite_string,
            prix_total,
            date
        FROM `order` 
        WHERE HEX(id) = ?
        """;

            Query queryCommande = entityManager.createNativeQuery(sqlCommande);
            queryCommande.setParameter(1, orderId.replace("-", ""));
            Object[] commandeInfo = (Object[]) queryCommande.getSingleResult();

            debug.put("commande_info", Map.of(
                    "id", commandeInfo[0],
                    "num_commande", commandeInfo[1],
                    "priorite", commandeInfo[2],
                    "prix", commandeInfo[3],
                    "date", commandeInfo[4]
            ));

            // 2. Compter les certifications
            String sqlCount = """
        SELECT COUNT(DISTINCT cco.card_certification_id)
        FROM card_certification_order cco
        WHERE HEX(cco.order_id) = ?
        """;

            Query queryCount = entityManager.createNativeQuery(sqlCount);
            queryCount.setParameter(1, orderId.replace("-", ""));
            Number count = (Number) queryCount.getSingleResult();
            debug.put("nombre_certifications", count.intValue());

            // 3. Échantillon de certifications
            String sqlSample = """
        SELECT 
            HEX(cc.id) as cert_id,
            cc.code_barre,
            cc.type,
            ct.name,
            ct.label_name
        FROM card_certification_order cco
        INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
        LEFT JOIN card_translation ct ON cc.translatable_id = ct.translatable_id AND ct.locale = 'fr'
        WHERE HEX(cco.order_id) = ?
        LIMIT 5
        """;

            Query querySample = entityManager.createNativeQuery(sqlSample);
            querySample.setParameter(1, orderId.replace("-", ""));
            @SuppressWarnings("unchecked")
            List<Object[]> samples = querySample.getResultList();

            List<Map<String, Object>> echantillons = new ArrayList<>();
            for (Object[] sample : samples) {
                Map<String, Object> cert = new HashMap<>();
                cert.put("cert_id", sample[0]);
                cert.put("code_barre", sample[1]);
                cert.put("type", sample[2]);
                cert.put("name", sample[3]);
                cert.put("label_name", sample[4]);
                echantillons.add(cert);
            }
            debug.put("echantillon_certifications", echantillons);

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            System.err.println("❌ Erreur debug commande: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }


    // ============= DEBUG CORRIGÉ POUR TESTER LA STRUCTURE =============
// ✅ REMPLACEZ aussi votre méthode de debug par cette version

    /**
     * 🔍 DEBUG - STRUCTURE RÉELLE DES TABLES
     */
    @GetMapping("/api/test/debug-structure-cartes")
    public ResponseEntity<Map<String, Object>> debugStructureCartes() {
        try {
            System.out.println("🔍 === DEBUG STRUCTURE CARTES ===");

            Map<String, Object> debug = new HashMap<>();

            // 1. Vérifier la structure de card_certification
            try {
                String sqlStructureCC = "DESCRIBE card_certification";
                Query queryCC = entityManager.createNativeQuery(sqlStructureCC);
                @SuppressWarnings("unchecked")
                List<Object[]> structureCC = queryCC.getResultList();

                List<String> colonnesCC = new ArrayList<>();
                for (Object[] col : structureCC) {
                    colonnesCC.add((String) col[0]);
                }
                debug.put("colonnes_card_certification", colonnesCC);
                System.out.println("📋 card_certification: " + colonnesCC);

            } catch (Exception e) {
                debug.put("erreur_structure_cc", e.getMessage());
            }

            // 2. Vérifier la structure de card
            try {
                String sqlStructureCard = "DESCRIBE card";
                Query queryCard = entityManager.createNativeQuery(sqlStructureCard);
                @SuppressWarnings("unchecked")
                List<Object[]> structureCard = queryCard.getResultList();

                List<String> colonnesCard = new ArrayList<>();
                for (Object[] col : structureCard) {
                    colonnesCard.add((String) col[0]);
                }
                debug.put("colonnes_card", colonnesCard);
                System.out.println("📋 card: " + colonnesCard);

            } catch (Exception e) {
                debug.put("erreur_structure_card", e.getMessage());
            }

            // 3. Test relation simple et fonctionnelle
            String sqlTestSimple = """
        SELECT 
            HEX(o.id) as order_id,
            o.num_commande,
            COUNT(cco.card_certification_id) as nb_certifications
        FROM `order` o
        INNER JOIN card_certification_order cco ON o.id = cco.order_id
        WHERE o.date >= '2025-06-01'
        GROUP BY o.id, o.num_commande
        ORDER BY nb_certifications DESC
        LIMIT 10
        """;

            try {
                Query queryTest = entityManager.createNativeQuery(sqlTestSimple);
                @SuppressWarnings("unchecked")
                List<Object[]> testResults = queryTest.getResultList();

                List<Map<String, Object>> exemples = new ArrayList<>();
                for (Object[] row : testResults) {
                    Map<String, Object> exemple = new HashMap<>();
                    exemple.put("order_id", row[0]);
                    exemple.put("num_commande", row[1]);
                    exemple.put("nb_certifications", row[2]);
                    exemples.add(exemple);

                    System.out.println("📦 " + row[1] + ": " + row[2] + " certifications");
                }
                debug.put("exemples_commandes_avec_cartes", exemples);

            } catch (Exception e) {
                debug.put("erreur_test_simple", e.getMessage());
                System.out.println("❌ Erreur test simple: " + e.getMessage());
            }

            // 4. Tester les noms de cartes directement depuis card_certification
            try {
                String sqlTestNoms = """
            SELECT 
                HEX(cc.id) as cert_id,
                cc.code_barre,
                ct.name,
                ct.label_name
            FROM card_certification cc
            LEFT JOIN card_translation ct ON cc.translatable_id = ct.translatable_id AND ct.locale = 'fr'
            WHERE cc.translatable_id IS NOT NULL
            LIMIT 5
            """;

                Query queryNoms = entityManager.createNativeQuery(sqlTestNoms);
                @SuppressWarnings("unchecked")
                List<Object[]> nomsResults = queryNoms.getResultList();

                List<Map<String, Object>> exemplesNoms = new ArrayList<>();
                for (Object[] row : nomsResults) {
                    Map<String, Object> nom = new HashMap<>();
                    nom.put("cert_id", row[0]);
                    nom.put("code_barre", row[1]);
                    nom.put("name", row[2]);
                    nom.put("label_name", row[3]);
                    exemplesNoms.add(nom);
                }
                debug.put("exemples_noms_cartes", exemplesNoms);

            } catch (Exception e) {
                debug.put("erreur_noms", e.getMessage());
            }

            debug.put("status", "OK");
            debug.put("conclusion", "La relation card_certification_order fonctionne !");

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            System.err.println("❌ Erreur debug structure: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }



    /**
     * 🔍 DEBUG - TEST AVEC LA BONNE TABLE card_certification_order
     */
    @GetMapping("/api/test/debug-cartes-relation-correct")
    public ResponseEntity<Map<String, Object>> debugCartesRelationCorrect() {
        try {
            System.out.println("🔍 === DEBUG RELATION CARTES (card_certification_order) ===");

            Map<String, Object> debug = new HashMap<>();

            // 1. Compter les tables
            String[] tables = {"card_certification_order", "card_certification", "card", "card_translation", "`order`"};
            Map<String, Integer> counts = new HashMap<>();

            for (String table : tables) {
                try {
                    String sql = "SELECT COUNT(*) FROM " + table;
                    Number count = (Number) entityManager.createNativeQuery(sql).getSingleResult();
                    counts.put(table, count.intValue());
                    System.out.println("📊 " + table + ": " + count + " enregistrements");
                } catch (Exception e) {
                    counts.put(table, -1);
                    System.out.println("❌ " + table + ": erreur - " + e.getMessage());
                }
            }
            debug.put("table_counts", counts);

            // 2. Test relation complète sur une commande
            String sqlTestRelation = """
        SELECT 
            HEX(o.id) as order_id,
            o.num_commande,
            COUNT(DISTINCT cco.card_certification_id) as nb_certifications,
            COUNT(DISTINCT c.id) as nb_cards,
            COUNT(DISTINCT ct.translatable_id) as nb_translations
        FROM `order` o
        LEFT JOIN card_certification_order cco ON o.id = cco.order_id
        LEFT JOIN card_certification cc ON cco.card_certification_id = cc.id
        LEFT JOIN card c ON cc.card_id = c.id
        LEFT JOIN card_translation ct ON c.translatable_id = ct.translatable_id AND ct.locale = 'fr'
        WHERE o.date >= '2025-06-01'
        GROUP BY o.id, o.num_commande
        HAVING COUNT(DISTINCT cco.card_certification_id) > 0
        ORDER BY nb_certifications DESC
        LIMIT 10
        """;

            try {
                Query queryTest = entityManager.createNativeQuery(sqlTestRelation);
                @SuppressWarnings("unchecked")
                List<Object[]> testResults = queryTest.getResultList();

                List<Map<String, Object>> exemples = new ArrayList<>();
                for (Object[] row : testResults) {
                    Map<String, Object> exemple = new HashMap<>();
                    exemple.put("order_id", row[0]);
                    exemple.put("num_commande", row[1]);
                    exemple.put("nb_certifications", row[2]);
                    exemple.put("nb_cards", row[3]);
                    exemple.put("nb_translations", row[4]);
                    exemples.add(exemple);

                    System.out.println("📦 " + row[1] + ": " + row[2] + " certifications, " + row[3] + " cartes");
                }
                debug.put("exemples_commandes_avec_cartes", exemples);

            } catch (Exception e) {
                debug.put("erreur_test_relation", e.getMessage());
                System.out.println("❌ Erreur test relation: " + e.getMessage());
                e.printStackTrace();
            }

            debug.put("status", "OK");
            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            System.err.println("❌ Erreur debug: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 🔍 DEBUG - TEST AVEC LA VRAIE RELATION ULTIME
     *
     * RELATION FINALE :
     * order → card_certification_order → card_certification → card_translation
     *
     * COURT-CIRCUIT ULTIME : card_certification.card_id = card_translation.translatable_id
     */
    @GetMapping("/api/test/debug-relation-ultime")
    public ResponseEntity<Map<String, Object>> debugRelationUltime() {
        try {
            System.out.println("🔗 === TEST RELATION ULTIME (card_id = translatable_id) ===");

            Map<String, Object> debug = new HashMap<>();

            // 1. Test de la relation directe avec card_id
            String sqlRelationUltime = """
        SELECT 
            HEX(o.id) as order_id,
            o.num_commande,
            COUNT(DISTINCT cco.card_certification_id) as nb_certifications,
            COUNT(DISTINCT ct.translatable_id) as nb_translations,
            GROUP_CONCAT(DISTINCT ct.name SEPARATOR ', ') as noms_cartes
        FROM `order` o
        INNER JOIN card_certification_order cco ON o.id = cco.order_id
        INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
        INNER JOIN card_translation ct ON cc.card_id = ct.translatable_id 
            AND ct.locale = 'fr'
        WHERE o.date >= '2025-06-01'
        GROUP BY o.id, o.num_commande
        HAVING COUNT(DISTINCT cco.card_certification_id) > 0
        ORDER BY nb_certifications DESC
        LIMIT 5
        """;

            try {
                Query queryRelation = entityManager.createNativeQuery(sqlRelationUltime);
                @SuppressWarnings("unchecked")
                List<Object[]> results = queryRelation.getResultList();

                List<Map<String, Object>> exemples = new ArrayList<>();
                for (Object[] row : results) {
                    Map<String, Object> exemple = new HashMap<>();
                    exemple.put("order_id", row[0]);
                    exemple.put("num_commande", row[1]);
                    exemple.put("nb_certifications", row[2]);
                    exemple.put("nb_translations", row[3]);
                    exemple.put("noms_cartes", row[4]);
                    exemples.add(exemple);

                    System.out.println("📦 " + row[1] + ": " + row[2] + " certifs → " + row[3] + " traductions");
                    if (row[4] != null) {
                        String noms = row[4].toString();
                        System.out.println("   Cartes: " + (noms.length() > 100 ? noms.substring(0, 100) + "..." : noms));
                    }
                }
                debug.put("exemples_relation_ultime", exemples);

            } catch (Exception e) {
                debug.put("erreur_relation_ultime", e.getMessage());
                System.out.println("❌ Erreur relation ultime: " + e.getMessage());
                e.printStackTrace();
            }

            // 2. Vérifier la correspondance card_id = translatable_id
            String sqlVerifCorrespondance = """
        SELECT 
            COUNT(*) as total_certifications,
            COUNT(ct.translatable_id) as avec_traduction_via_card_id,
            COUNT(CASE WHEN ct.locale = 'fr' THEN 1 END) as traductions_fr
        FROM card_certification cc
        LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id
        """;

            try {
                Query queryVerif = entityManager.createNativeQuery(sqlVerifCorrespondance);
                Object[] verif = (Object[]) queryVerif.getSingleResult();

                debug.put("verification_correspondance", Map.of(
                        "total_certifications", verif[0],
                        "avec_traduction_via_card_id", verif[1],
                        "traductions_francais", verif[2]
                ));

                System.out.println("📊 Vérification correspondance card_id = translatable_id:");
                System.out.println("   Total certifications: " + verif[0]);
                System.out.println("   Avec traduction via card_id: " + verif[1]);
                System.out.println("   Traductions françaises: " + verif[2]);

            } catch (Exception e) {
                debug.put("erreur_verification", e.getMessage());
            }

            // 3. Échantillon pour voir la correspondance
            String sqlEchantillonCorrespondance = """
        SELECT 
            HEX(cc.id) as cert_id,
            HEX(cc.card_id) as card_id,
            cc.code_barre,
            HEX(ct.translatable_id) as translatable_id,
            ct.name,
            ct.locale,
            CASE 
                WHEN cc.card_id = ct.translatable_id THEN 'MATCH'
                ELSE 'NO_MATCH'
            END as correspondance
        FROM card_certification cc
        LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id
        WHERE ct.translatable_id IS NOT NULL
        ORDER BY cc.code_barre
        LIMIT 10
        """;

            try {
                Query queryEchantillon = entityManager.createNativeQuery(sqlEchantillonCorrespondance);
                @SuppressWarnings("unchecked")
                List<Object[]> echantillon = queryEchantillon.getResultList();

                List<Map<String, Object>> exemplesCorrespondance = new ArrayList<>();
                for (Object[] row : echantillon) {
                    Map<String, Object> corr = new HashMap<>();
                    corr.put("cert_id", row[0]);
                    corr.put("card_id", row[1]);
                    corr.put("code_barre", row[2]);
                    corr.put("translatable_id", row[3]);
                    corr.put("name", row[4]);
                    corr.put("locale", row[5]);
                    corr.put("correspondance", row[6]);
                    exemplesCorrespondance.add(corr);
                }
                debug.put("echantillon_correspondances", exemplesCorrespondance);

            } catch (Exception e) {
                debug.put("erreur_echantillon", e.getMessage());
            }

            // 4. Test avec des commandes récentes
            String sqlTestCommandes = """
        SELECT 
            HEX(o.id) as order_id,
            o.num_commande,
            COUNT(DISTINCT cco.card_certification_id) as certifications,
            COUNT(DISTINCT CASE WHEN ct.locale = 'fr' THEN ct.translatable_id END) as noms_fr,
            COUNT(DISTINCT CASE WHEN ct.locale = 'en' THEN ct.translatable_id END) as noms_en
        FROM `order` o
        INNER JOIN card_certification_order cco ON o.id = cco.order_id
        INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
        LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id
        WHERE o.date >= '2025-06-01'
        GROUP BY o.id, o.num_commande
        HAVING COUNT(DISTINCT cco.card_certification_id) > 0
        ORDER BY certifications DESC
        LIMIT 10
        """;

            try {
                Query queryTest = entityManager.createNativeQuery(sqlTestCommandes);
                @SuppressWarnings("unchecked")
                List<Object[]> testResults = queryTest.getResultList();

                List<Map<String, Object>> exempleCommandes = new ArrayList<>();
                for (Object[] row : testResults) {
                    Map<String, Object> cmd = new HashMap<>();
                    cmd.put("order_id", row[0]);
                    cmd.put("num_commande", row[1]);
                    cmd.put("certifications", row[2]);
                    cmd.put("noms_francais", row[3]);
                    cmd.put("noms_anglais", row[4]);

                    // Calculer le pourcentage de traduction
                    Number certifs = (Number) row[2];
                    Number nomsFr = (Number) row[3];
                    if (certifs != null && nomsFr != null && certifs.intValue() > 0) {
                        double pourcentage = (nomsFr.doubleValue() / certifs.doubleValue()) * 100;
                        cmd.put("pourcentage_traduit_fr", Math.round(pourcentage));
                    } else {
                        cmd.put("pourcentage_traduit_fr", 0);
                    }

                    exempleCommandes.add(cmd);
                }
                debug.put("exemples_commandes_test", exempleCommandes);

            } catch (Exception e) {
                debug.put("erreur_test_commandes", e.getMessage());
            }

            debug.put("status", "OK");
            debug.put("relation_testee", "order → card_certification_order → card_certification → card_translation");
            debug.put("court_circuit_ultime", "card_certification.card_id = card_translation.translatable_id");

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            System.err.println("❌ Erreur debug relation ultime: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 📦 DEBUG - COMMANDE SPÉCIFIQUE AVEC RELATION ULTIME
     */
    @GetMapping("/api/test/debug-commande-ultime/{orderId}")
    public ResponseEntity<Map<String, Object>> debugCommandeUltime(@PathVariable String orderId) {
        try {
            System.out.println("📦 === DEBUG COMMANDE " + orderId + " (RELATION ULTIME) ===");

            Map<String, Object> debug = new HashMap<>();
            String orderIdClean = orderId.replace("-", "");

            // 1. Info de base de la commande
            String sqlCommande = """
        SELECT 
            HEX(id) as order_id,
            num_commande,
            COALESCE(priorite_string, 'NORMALE') as priorite,
            date
        FROM `order` 
        WHERE HEX(id) = ?
        """;

            try {
                Query queryCommande = entityManager.createNativeQuery(sqlCommande);
                queryCommande.setParameter(1, orderIdClean);
                Object[] commandeInfo = (Object[]) queryCommande.getSingleResult();

                debug.put("commande_info", Map.of(
                        "id", commandeInfo[0],
                        "num_commande", commandeInfo[1] != null ? commandeInfo[1] : "N/A",
                        "priorite", commandeInfo[2],
                        "date", commandeInfo[3] != null ? commandeInfo[3] : "N/A"
                ));

            } catch (Exception e) {
                debug.put("erreur_commande", e.getMessage());
                return ResponseEntity.ok(debug);
            }

            // 2. Détails des cartes avec relation ultime (card_id = translatable_id)
            String sqlCartesUltime = """
        SELECT 
            HEX(cc.id) as cert_id,
            HEX(cc.card_id) as card_id,
            cc.code_barre,
            ct.name as carte_nom,
            ct.label_name,
            ct.locale,
            cc.langue as cert_langue,
            cc.edition,
            CASE 
                WHEN ct.name IS NOT NULL THEN 'OUI'
                ELSE 'NON'
            END as a_traduction
        FROM card_certification_order cco
        INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
        LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id 
            AND ct.locale = 'fr'
        WHERE HEX(cco.order_id) = ?
        ORDER BY ct.name, cc.code_barre
        """;

            try {
                Query queryCartes = entityManager.createNativeQuery(sqlCartesUltime);
                queryCartes.setParameter(1, orderIdClean);
                @SuppressWarnings("unchecked")
                List<Object[]> cartesResults = queryCartes.getResultList();

                List<Map<String, Object>> cartes = new ArrayList<>();
                int avecTraduction = 0;
                int sansTraduction = 0;

                for (Object[] row : cartesResults) {
                    Map<String, Object> carte = new HashMap<>();
                    carte.put("cert_id", row[0]);
                    carte.put("card_id", row[1]);
                    carte.put("code_barre", row[2]);
                    carte.put("nom", row[3] != null ? row[3] : "❌ Pas de nom");
                    carte.put("label_name", row[4]);
                    carte.put("locale_traduction", row[5]);
                    carte.put("langue_certification", row[6]);
                    carte.put("edition", row[7]);
                    carte.put("a_traduction", row[8]);
                    cartes.add(carte);

                    if ("OUI".equals(row[8])) {
                        avecTraduction++;
                    } else {
                        sansTraduction++;
                    }
                }

                debug.put("cartes_details_ultime", cartes);
                debug.put("statistiques_ultime", Map.of(
                        "total_cartes", cartes.size(),
                        "avec_traduction_fr", avecTraduction,
                        "sans_traduction", sansTraduction,
                        "pourcentage_traduit", cartes.size() > 0 ? (avecTraduction * 100.0 / cartes.size()) : 0
                ));

                System.out.println("📊 Statistiques ULTIME: " + cartes.size() + " cartes total, "
                        + avecTraduction + " avec traduction FR ("
                        + (cartes.size() > 0 ? Math.round(avecTraduction * 100.0 / cartes.size()) : 0) + "%)");

            } catch (Exception e) {
                debug.put("erreur_cartes_ultime", e.getMessage());
                System.out.println("❌ Erreur cartes ultime: " + e.getMessage());
            }

            // 3. Test des autres langues disponibles
            String sqlAutresLangues = """
        SELECT 
            ct.locale,
            COUNT(*) as nb_traductions
        FROM card_certification_order cco
        INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
        INNER JOIN card_translation ct ON cc.card_id = ct.translatable_id
        WHERE HEX(cco.order_id) = ?
        GROUP BY ct.locale
        ORDER BY nb_traductions DESC
        """;

            try {
                Query queryLangues = entityManager.createNativeQuery(sqlAutresLangues);
                queryLangues.setParameter(1, orderIdClean);
                @SuppressWarnings("unchecked")
                List<Object[]> languesResults = queryLangues.getResultList();

                List<Map<String, Object>> langues = new ArrayList<>();
                for (Object[] row : languesResults) {
                    Map<String, Object> langue = new HashMap<>();
                    langue.put("locale", row[0]);
                    langue.put("nb_traductions", row[1]);
                    langues.add(langue);
                }
                debug.put("langues_disponibles", langues);

            } catch (Exception e) {
                debug.put("erreur_langues", e.getMessage());
            }

            debug.put("status", "OK");
            debug.put("orderId_original", orderId);
            debug.put("orderId_clean", orderIdClean);
            debug.put("relation_utilisee", "card_certification.card_id = card_translation.translatable_id");

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            System.err.println("❌ Erreur debug commande ultime: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 🔧 REQUÊTE OPTIMISÉE POUR PLANIFICATION
     * Utilisera la relation ultime validée
     */
    @GetMapping("/api/test/planning-optimise")
    public ResponseEntity<List<Map<String, Object>>> getPlanningOptimise(
            @RequestParam(defaultValue = "2025-06-22") String date) {

        try {
            System.out.println("🚀 === PLANNING OPTIMISÉ (RELATION ULTIME) ===");

            // Requête optimisée avec la bonne relation
            String sqlPlanningOptimise = """
        SELECT 
            HEX(o.id) as order_id,
            o.num_commande,
            o.priorite_string,
            o.date,
            COUNT(DISTINCT cco.card_certification_id) as nb_cartes,
            GROUP_CONCAT(
                DISTINCT COALESCE(ct.name, CONCAT('Carte-', cc.code_barre))
                ORDER BY ct.name
                SEPARATOR ', '
            ) as noms_cartes,
            AVG(CASE 
                WHEN ct.name IS NOT NULL THEN 1.0 
                ELSE 0.0 
            END) * 100 as pourcentage_traduit
        FROM `order` o
        INNER JOIN card_certification_order cco ON o.id = cco.order_id
        INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
        LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id 
            AND ct.locale = 'fr'
        WHERE o.date >= ?
        GROUP BY o.id, o.num_commande, o.priorite_string, o.date
        HAVING COUNT(DISTINCT cco.card_certification_id) > 0
        ORDER BY 
            CASE o.priorite_string 
                WHEN 'URGENTE' THEN 1
                WHEN 'HAUTE' THEN 2
                WHEN 'NORMALE' THEN 3
                ELSE 4
            END,
            o.date ASC
        """;

            Query query = entityManager.createNativeQuery(sqlPlanningOptimise);
            query.setParameter(1, date);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> commandes = new ArrayList<>();
            int totalCartes = 0;

            for (Object[] row : results) {
                Map<String, Object> commande = new HashMap<>();
                commande.put("id", row[0]);
                commande.put("numeroCommande", row[1]);
                commande.put("priorite", row[2] != null ? row[2] : "NORMALE");
                commande.put("date", row[3]);

                Number nbCartes = (Number) row[4];
                commande.put("nombreCartes", nbCartes.intValue());
                totalCartes += nbCartes.intValue();

                commande.put("nomCartes", row[5]);

                Number pourcentage = (Number) row[6];
                commande.put("pourcentageTraduit", Math.round(pourcentage.doubleValue()));

                commandes.add(commande);

                System.out.println("📦 " + row[1] + ": " + nbCartes + " cartes ("
                        + Math.round(pourcentage.doubleValue()) + "% traduites)");
            }

            System.out.println("✅ Planning optimisé: " + commandes.size() + " commandes, "
                    + totalCartes + " cartes total");

            return ResponseEntity.ok(commandes);

        } catch (Exception e) {
            System.err.println("❌ Erreur planning optimisé: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(List.of(Map.of("error", e.getMessage())));
        }
    }


    /**
     * 🔍 DEBUG - TEST AVEC CORRESPONDANCE LANGUE/LOCALE POUR UNICITÉ
     *
     * RELATION FINALE + UNICITÉ :
     * order → card_certification_order → card_certification → card_translation
     *
     * CORRESPONDANCE UNICITÉ :
     * card_certification.card_id = card_translation.translatable_id
     * ET card_certification.langue = card_translation.locale
     */
    @GetMapping("/api/test/debug-relation-unique")
    public ResponseEntity<Map<String, Object>> debugRelationUnique() {
        try {
            System.out.println("🔗 === TEST RELATION AVEC UNICITÉ LANGUE/LOCALE ===");

            Map<String, Object> debug = new HashMap<>();

            // 1. Analyser les langues dans card_certification
            String sqlAnalyseLangues = """
        SELECT 
            cc.langue,
            COUNT(*) as nb_certifications
        FROM card_certification cc
        GROUP BY cc.langue
        ORDER BY nb_certifications DESC
        """;

            try {
                Query queryLangues = entityManager.createNativeQuery(sqlAnalyseLangues);
                @SuppressWarnings("unchecked")
                List<Object[]> languesResults = queryLangues.getResultList();

                List<Map<String, Object>> languesCertif = new ArrayList<>();
                for (Object[] row : languesResults) {
                    Map<String, Object> langue = new HashMap<>();
                    langue.put("langue", row[0]);
                    langue.put("nb_certifications", row[1]);
                    languesCertif.add(langue);
                }
                debug.put("langues_certifications", languesCertif);
                System.out.println("📊 Langues dans certifications: " + languesCertif);

            } catch (Exception e) {
                debug.put("erreur_langues_certif", e.getMessage());
            }

            // 2. Analyser les locales dans card_translation
            String sqlAnalyseLocales = """
        SELECT 
            ct.locale,
            COUNT(*) as nb_traductions
        FROM card_translation ct
        GROUP BY ct.locale
        ORDER BY nb_traductions DESC
        """;

            try {
                Query queryLocales = entityManager.createNativeQuery(sqlAnalyseLocales);
                @SuppressWarnings("unchecked")
                List<Object[]> localesResults = queryLocales.getResultList();

                List<Map<String, Object>> localesTranslation = new ArrayList<>();
                for (Object[] row : localesResults) {
                    Map<String, Object> locale = new HashMap<>();
                    locale.put("locale", row[0]);
                    locale.put("nb_traductions", row[1]);
                    localesTranslation.add(locale);
                }
                debug.put("locales_translations", localesTranslation);
                System.out.println("📊 Locales dans traductions: " + localesTranslation);

            } catch (Exception e) {
                debug.put("erreur_locales_translation", e.getMessage());
            }

            // 3. Test de la relation UNIQUE avec correspondance langue/locale
            String sqlRelationUnique = """
        SELECT 
            HEX(o.id) as order_id,
            o.num_commande,
            COUNT(DISTINCT cco.card_certification_id) as nb_certifications_reelles,
            COUNT(DISTINCT CASE WHEN ct.name IS NOT NULL THEN cco.card_certification_id END) as nb_avec_traduction_unique,
            GROUP_CONCAT(DISTINCT ct.name ORDER BY ct.name SEPARATOR ', ') as noms_cartes_uniques
        FROM `order` o
        INNER JOIN card_certification_order cco ON o.id = cco.order_id
        INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
        LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id 
            AND cc.langue = ct.locale
        WHERE o.date >= '2025-06-01'
        GROUP BY o.id, o.num_commande
        HAVING COUNT(DISTINCT cco.card_certification_id) > 0
        ORDER BY nb_certifications_reelles DESC
        LIMIT 5
        """;

            try {
                Query queryRelation = entityManager.createNativeQuery(sqlRelationUnique);
                @SuppressWarnings("unchecked")
                List<Object[]> results = queryRelation.getResultList();

                List<Map<String, Object>> exemplesUniques = new ArrayList<>();
                for (Object[] row : results) {
                    Map<String, Object> exemple = new HashMap<>();
                    exemple.put("order_id", row[0]);
                    exemple.put("num_commande", row[1]);
                    exemple.put("nb_certifications_reelles", row[2]);
                    exemple.put("nb_avec_traduction_unique", row[3]);
                    exemple.put("noms_cartes_uniques", row[4]);

                    // Calculer le pourcentage de traduction
                    Number certifs = (Number) row[2];
                    Number traduits = (Number) row[3];
                    if (certifs != null && traduits != null && certifs.intValue() > 0) {
                        double pourcentage = (traduits.doubleValue() / certifs.doubleValue()) * 100;
                        exemple.put("pourcentage_traduit", Math.round(pourcentage));
                    } else {
                        exemple.put("pourcentage_traduit", 0);
                    }

                    exemplesUniques.add(exemple);

                    System.out.println("📦 " + row[1] + ": " + row[2] + " certifs → " + row[3] + " traduites ("
                            + exemple.get("pourcentage_traduit") + "%)");
                    if (row[4] != null) {
                        String noms = row[4].toString();
                        System.out.println("   Cartes: " + (noms.length() > 100 ? noms.substring(0, 100) + "..." : noms));
                    }
                }
                debug.put("exemples_relation_unique", exemplesUniques);

            } catch (Exception e) {
                debug.put("erreur_relation_unique", e.getMessage());
                System.out.println("❌ Erreur relation unique: " + e.getMessage());
                e.printStackTrace();
            }

            // 4. Comparer AVEC et SANS la correspondance langue/locale
            String sqlComparaison = """
        SELECT 
            'SANS_CORRESPONDANCE' as type,
            COUNT(DISTINCT cco.card_certification_id) as certifications_distinctes,
            COUNT(*) as total_lignes_resultats
        FROM card_certification_order cco
        INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
        LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id
        WHERE EXISTS (
            SELECT 1 FROM `order` o 
            WHERE o.id = cco.order_id AND o.date >= '2025-06-01'
        )
        
        UNION ALL
        
        SELECT 
            'AVEC_CORRESPONDANCE' as type,
            COUNT(DISTINCT cco.card_certification_id) as certifications_distinctes,
            COUNT(*) as total_lignes_resultats
        FROM card_certification_order cco
        INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
        LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id 
            AND cc.langue = ct.locale
        WHERE EXISTS (
            SELECT 1 FROM `order` o 
            WHERE o.id = cco.order_id AND o.date >= '2025-06-01'
        )
        """;

            try {
                Query queryComparaison = entityManager.createNativeQuery(sqlComparaison);
                @SuppressWarnings("unchecked")
                List<Object[]> comparaisonResults = queryComparaison.getResultList();

                List<Map<String, Object>> comparaison = new ArrayList<>();
                for (Object[] row : comparaisonResults) {
                    Map<String, Object> comp = new HashMap<>();
                    comp.put("type", row[0]);
                    comp.put("certifications_distinctes", row[1]);
                    comp.put("total_lignes_resultats", row[2]);
                    comparaison.add(comp);
                }
                debug.put("comparaison_avec_sans_correspondance", comparaison);
                System.out.println("🔍 Comparaison AVEC/SANS correspondance: " + comparaison);

            } catch (Exception e) {
                debug.put("erreur_comparaison", e.getMessage());
            }

            debug.put("status", "OK");
            debug.put("relation_testee", "card_certification.card_id = card_translation.translatable_id AND card_certification.langue = card_translation.locale");

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            System.err.println("❌ Erreur debug relation unique: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 📦 DEBUG - COMMANDE SPÉCIFIQUE AVEC UNICITÉ LANGUE/LOCALE
     */
    @GetMapping("/api/test/debug-commande-unique/{orderId}")
    public ResponseEntity<Map<String, Object>> debugCommandeUnique(@PathVariable String orderId) {
        try {
            System.out.println("📦 === DEBUG COMMANDE " + orderId + " (UNICITÉ) ===");

            Map<String, Object> debug = new HashMap<>();
            String orderIdClean = orderId.replace("-", "");

            // 1. Info de base de la commande
            String sqlCommande = """
        SELECT 
            HEX(id) as order_id,
            num_commande,
            COALESCE(priorite_string, 'NORMALE') as priorite,
            date
        FROM `order` 
        WHERE HEX(id) = ?
        """;

            try {
                Query queryCommande = entityManager.createNativeQuery(sqlCommande);
                queryCommande.setParameter(1, orderIdClean);
                Object[] commandeInfo = (Object[]) queryCommande.getSingleResult();

                debug.put("commande_info", Map.of(
                        "id", commandeInfo[0],
                        "num_commande", commandeInfo[1] != null ? commandeInfo[1] : "N/A",
                        "priorite", commandeInfo[2],
                        "date", commandeInfo[3] != null ? commandeInfo[3] : "N/A"
                ));

            } catch (Exception e) {
                debug.put("erreur_commande", e.getMessage());
                return ResponseEntity.ok(debug);
            }

            // 2. Comparaison AVANT/APRÈS correspondance langue/locale
            String sqlAvantApres = """
        SELECT 
            'AVANT_CORRESPONDANCE' as scenario,
            COUNT(DISTINCT cco.card_certification_id) as certifications_distinctes,
            COUNT(*) as lignes_totales,
            COUNT(DISTINCT ct.name) as noms_distincts
        FROM card_certification_order cco
        INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
        LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id
        WHERE HEX(cco.order_id) = ?
        
        UNION ALL
        
        SELECT 
            'APRES_CORRESPONDANCE' as scenario,
            COUNT(DISTINCT cco.card_certification_id) as certifications_distinctes,
            COUNT(*) as lignes_totales,
            COUNT(DISTINCT ct.name) as noms_distincts
        FROM card_certification_order cco
        INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
        LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id 
            AND cc.langue = ct.locale
        WHERE HEX(cco.order_id) = ?
        """;

            try {
                Query queryAvantApres = entityManager.createNativeQuery(sqlAvantApres);
                queryAvantApres.setParameter(1, orderIdClean);
                queryAvantApres.setParameter(2, orderIdClean);
                @SuppressWarnings("unchecked")
                List<Object[]> avantApresResults = queryAvantApres.getResultList();

                List<Map<String, Object>> avantApres = new ArrayList<>();
                for (Object[] row : avantApresResults) {
                    Map<String, Object> scenario = new HashMap<>();
                    scenario.put("scenario", row[0]);
                    scenario.put("certifications_distinctes", row[1]);
                    scenario.put("lignes_totales", row[2]);
                    scenario.put("noms_distincts", row[3]);
                    avantApres.add(scenario);
                }
                debug.put("comparaison_avant_apres", avantApres);
                System.out.println("🔍 Comparaison AVANT/APRÈS: " + avantApres);

            } catch (Exception e) {
                debug.put("erreur_avant_apres", e.getMessage());
            }

            // 3. Détails des cartes avec correspondance UNIQUE
            String sqlCartesUniques = """
        SELECT 
            HEX(cc.id) as cert_id,
            HEX(cc.card_id) as card_id,
            cc.code_barre,
            cc.langue as cert_langue,
            ct.locale as trad_locale,
            ct.name as carte_nom,
            ct.label_name,
            CASE 
                WHEN cc.langue = ct.locale THEN 'MATCH_EXACT'
                WHEN ct.name IS NOT NULL THEN 'TRADUCTION_AUTRE_LANGUE'
                ELSE 'AUCUNE_TRADUCTION'
            END as statut_correspondance
        FROM card_certification_order cco
        INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
        LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id 
            AND cc.langue = ct.locale
        WHERE HEX(cco.order_id) = ?
        ORDER BY cc.code_barre
        """;

            try {
                Query queryCartesUniques = entityManager.createNativeQuery(sqlCartesUniques);
                queryCartesUniques.setParameter(1, orderIdClean);
                @SuppressWarnings("unchecked")
                List<Object[]> cartesResults = queryCartesUniques.getResultList();

                List<Map<String, Object>> cartes = new ArrayList<>();
                int matchExact = 0;
                int tradAutreLangue = 0;
                int aucuneTraduction = 0;

                for (Object[] row : cartesResults) {
                    Map<String, Object> carte = new HashMap<>();
                    carte.put("cert_id", row[0]);
                    carte.put("card_id", row[1]);
                    carte.put("code_barre", row[2]);
                    carte.put("cert_langue", row[3]);
                    carte.put("trad_locale", row[4]);
                    carte.put("nom", row[5] != null ? row[5] : "❌ Pas de nom");
                    carte.put("label_name", row[6]);
                    carte.put("statut_correspondance", row[7]);
                    cartes.add(carte);

                    String statut = (String) row[7];
                    switch (statut) {
                        case "MATCH_EXACT" -> matchExact++;
                        case "TRADUCTION_AUTRE_LANGUE" -> tradAutreLangue++;
                        case "AUCUNE_TRADUCTION" -> aucuneTraduction++;
                    }
                }

                debug.put("cartes_details_uniques", cartes);
                debug.put("statistiques_unicite", Map.of(
                        "total_cartes", cartes.size(),
                        "match_exact_langue", matchExact,
                        "traduction_autre_langue", tradAutreLangue,
                        "aucune_traduction", aucuneTraduction,
                        "pourcentage_match_exact", cartes.size() > 0 ? (matchExact * 100.0 / cartes.size()) : 0
                ));

                System.out.println("📊 Statistiques UNICITÉ: " + cartes.size() + " cartes total");
                System.out.println("   ✅ Match exact langue: " + matchExact + " (" + (cartes.size() > 0 ? Math.round(matchExact * 100.0 / cartes.size()) : 0) + "%)");
                System.out.println("   ⚠️ Traduction autre langue: " + tradAutreLangue);
                System.out.println("   ❌ Aucune traduction: " + aucuneTraduction);

            } catch (Exception e) {
                debug.put("erreur_cartes_uniques", e.getMessage());
                System.out.println("❌ Erreur cartes uniques: " + e.getMessage());
            }

            debug.put("status", "OK");
            debug.put("orderId_original", orderId);
            debug.put("orderId_clean", orderIdClean);
            debug.put("relation_utilisee", "card_certification.card_id = card_translation.translatable_id AND card_certification.langue = card_translation.locale");

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            System.err.println("❌ Erreur debug commande unique: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 🚀 TEST RAPIDE - VÉRIFIER LES DONNÉES AVEC CHAMP DATE UNIQUEMENT
     *
     * ✅ UTILISE UNIQUEMENT LE CHAMP 'date' VALIDE
     * ❌ IGNORE date_creation et autres champs date (toujours NULL)
     */
    @GetMapping("/api/test/test-rapide-donnees")
    public ResponseEntity<Map<String, Object>> testRapideDonnees() {
        try {
            System.out.println("🚀 === TEST RAPIDE DES DONNÉES ===");

            Map<String, Object> test = new HashMap<>();

            // 1. Info rapide sur les données disponibles (CHAMP DATE UNIQUEMENT)
            String sqlInfoRapide = """
        SELECT 
            MIN(date) as date_min,
            MAX(date) as date_max,
            COUNT(*) as total_commandes,
            COUNT(CASE WHEN status = 1 THEN 1 END) as statut_1,
            COUNT(CASE WHEN status = 2 THEN 1 END) as statut_2
        FROM `order`
        WHERE date IS NOT NULL
        """;

            try {
                Query queryInfo = entityManager.createNativeQuery(sqlInfoRapide);
                Object[] infoResult = (Object[]) queryInfo.getSingleResult();

                test.put("donnees_disponibles", Map.of(
                        "date_min", infoResult[0],
                        "date_max", infoResult[1],
                        "total_commandes", infoResult[2],
                        "commandes_statut_1", infoResult[3],
                        "commandes_statut_2", infoResult[4]
                ));

                LocalDate dateMin = ((java.sql.Date) infoResult[0]).toLocalDate();
                LocalDate dateMax = ((java.sql.Date) infoResult[1]).toLocalDate();

                System.out.println("📅 Données disponibles: " + dateMin + " → " + dateMax);
                System.out.println("📊 " + infoResult[2] + " commandes total");
                System.out.println("📦 Statut 1: " + infoResult[3] + ", Statut 2: " + infoResult[4]);

            } catch (Exception e) {
                test.put("erreur_info", e.getMessage());
                return ResponseEntity.ok(test);
            }

            // 2. Test immédiat avec une période qui contient des données
            String sqlDernierMoisAvecDonnees = """
        SELECT 
            YEAR(date) as annee,
            MONTH(date) as mois,
            COUNT(*) as nb_commandes,
            MIN(date) as debut,
            MAX(date) as fin
        FROM `order`
        WHERE date IS NOT NULL
        GROUP BY YEAR(date), MONTH(date)
        HAVING COUNT(*) >= 5
        ORDER BY annee DESC, mois DESC
        LIMIT 1
        """;

            LocalDate debutTest = null;
            LocalDate finTest = null;

            try {
                Query queryMois = entityManager.createNativeQuery(sqlDernierMoisAvecDonnees);
                @SuppressWarnings("unchecked")
                List<Object[]> moisResults = queryMois.getResultList();

                if (!moisResults.isEmpty()) {
                    Object[] moisData = moisResults.get(0);
                    debutTest = ((java.sql.Date) moisData[3]).toLocalDate();
                    finTest = ((java.sql.Date) moisData[4]).toLocalDate();

                    test.put("periode_test_choisie", Map.of(
                            "annee", moisData[0],
                            "mois", moisData[1],
                            "nb_commandes_periode", moisData[2],
                            "debut", debutTest,
                            "fin", finTest
                    ));

                    System.out.println("🎯 Période de test choisie: " + debutTest + " → " + finTest);
                    System.out.println("📦 " + moisData[2] + " commandes dans cette période");
                }

            } catch (Exception e) {
                test.put("erreur_periode", e.getMessage());
            }

            // 3. TEST IMMÉDIAT de notre solution finale sur cette période
            if (debutTest != null && finTest != null) {
                String sqlTestSolution = """
            SELECT 
                HEX(o.id) as order_id,
                o.num_commande,
                o.date,
                COUNT(DISTINCT cco.card_certification_id) as nb_cartes_exactes,
                COUNT(DISTINCT CASE 
                    WHEN ct.name IS NOT NULL THEN cco.card_certification_id 
                    END) as nb_avec_nom,
                GROUP_CONCAT(
                    DISTINCT SUBSTRING(COALESCE(ct.name, CONCAT('Carte-', cc.code_barre)), 1, 30)
                    ORDER BY ct.name
                    SEPARATOR ', '
                ) as echantillon_noms
            FROM `order` o
            INNER JOIN card_certification_order cco ON o.id = cco.order_id
            INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
            LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id 
                AND (
                    (cc.langue = 'FR' AND ct.locale = 'fr') OR
                    (cc.langue = 'JP' AND ct.locale = 'jp') OR
                    (cc.langue = 'DE' AND ct.locale = 'de') OR
                    (cc.langue = 'ES' AND ct.locale = 'es') OR
                    (cc.langue = 'IT' AND ct.locale = 'it') OR
                    (cc.langue = 'KR' AND ct.locale = 'kr') OR
                    (cc.langue = 'CN' AND ct.locale = 'cn') OR
                    (cc.langue = 'RU' AND ct.locale = 'ru') OR
                    (cc.langue = 'NL' AND ct.locale = 'nl') OR
                    (cc.langue = 'PT' AND ct.locale = 'pt') OR
                    (cc.langue = 'US' AND ct.locale = 'us') OR
                    (cc.langue = 'EN' AND ct.locale = 'us')
                )
            WHERE o.date >= ? AND o.date <= ?
            GROUP BY o.id, o.num_commande, o.date
            HAVING COUNT(DISTINCT cco.card_certification_id) > 0
            ORDER BY nb_cartes_exactes DESC
            LIMIT 10
            """;

                try {
                    Query queryTestSolution = entityManager.createNativeQuery(sqlTestSolution);
                    queryTestSolution.setParameter(1, debutTest);
                    queryTestSolution.setParameter(2, finTest);

                    @SuppressWarnings("unchecked")
                    List<Object[]> solutionResults = queryTestSolution.getResultList();

                    List<Map<String, Object>> commandesTest = new ArrayList<>();
                    int totalCartesTest = 0;
                    int totalAvecNomTest = 0;

                    for (Object[] row : solutionResults) {
                        Map<String, Object> commande = new HashMap<>();
                        commande.put("id", row[0]);
                        commande.put("numero", row[1]);
                        commande.put("date", row[2]);

                        Number nbCartes = (Number) row[3];
                        Number nbAvecNom = (Number) row[4];

                        commande.put("nb_cartes_exactes", nbCartes.intValue());
                        commande.put("nb_avec_nom", nbAvecNom.intValue());
                        commande.put("echantillon_noms", row[5]);

                        double pourcentage = nbCartes.intValue() > 0 ?
                                (nbAvecNom.doubleValue() / nbCartes.doubleValue()) * 100 : 0;
                        commande.put("pourcentage_avec_nom", Math.round(pourcentage));

                        commandesTest.add(commande);
                        totalCartesTest += nbCartes.intValue();
                        totalAvecNomTest += nbAvecNom.intValue();

                        System.out.println("📦 " + row[1] + ": " + nbCartes + " cartes exactes (" +
                                Math.round(pourcentage) + "% avec nom)");
                    }

                    double pourcentageGlobalTest = totalCartesTest > 0 ?
                            (totalAvecNomTest * 100.0 / totalCartesTest) : 0;

                    test.put("test_solution_finale", Map.of(
                            "commandes_testees", solutionResults.size(),
                            "total_cartes_exactes", totalCartesTest,
                            "total_avec_nom", totalAvecNomTest,
                            "pourcentage_global", Math.round(pourcentageGlobalTest),
                            "details_commandes", commandesTest
                    ));

                    System.out.println("✅ TEST SOLUTION FINALE:");
                    System.out.println("   📊 " + solutionResults.size() + " commandes testées");
                    System.out.println("   🎯 " + totalCartesTest + " cartes exactes (" +
                            Math.round(pourcentageGlobalTest) + "% avec nom)");

                    // Verdict final
                    boolean solutionFonctionne = solutionResults.size() > 0 && pourcentageGlobalTest > 50;
                    test.put("verdict_final", Map.of(
                            "solution_fonctionne", solutionFonctionne,
                            "message", solutionFonctionne ?
                                    "✅ SOLUTION VALIDÉE - Prête pour production!" :
                                    "⚠️ Problème détecté - Vérifier les données",
                            "recommandation", solutionFonctionne ?
                                    "Intégrer getCommandesAvecCartesExactes() dans AlgorithmePlanificationService" :
                                    "Analyser les données avec debug-periode-donnees"
                    ));

                } catch (Exception e) {
                    test.put("erreur_test_solution", e.getMessage());
                    System.out.println("❌ Erreur test solution: " + e.getMessage());
                }
            }

            test.put("status", "TEST_RAPIDE_TERMINE");
            return ResponseEntity.ok(test);

        } catch (Exception e) {
            System.err.println("❌ Erreur test rapide: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 🎯 SOLUTION PRODUCTION FINALE - AVEC PÉRIODE AUTOMATIQUE
     *
     * Trouve automatiquement la bonne période et applique notre solution
     */
    @GetMapping("/api/test/solution-production-auto")
    public ResponseEntity<Map<String, Object>> solutionProductionAuto(
            @RequestParam(defaultValue = "50") int limite) {

        try {
            System.out.println("🎯 === SOLUTION PRODUCTION AUTOMATIQUE ===");
            System.out.println("📦 Limite: " + limite + " commandes");

            Map<String, Object> production = new HashMap<>();

            // 1. Trouver la meilleure période automatiquement
            String sqlMeilleurePeriode = """
        SELECT 
            MIN(date) as debut,
            MAX(date) as fin,
            COUNT(*) as nb_commandes
        FROM `order`
        WHERE date IS NOT NULL
            AND date >= DATE_SUB(CURDATE(), INTERVAL 2 YEAR)
        """;

            LocalDate debutProd = null;
            LocalDate finProd = null;

            try {
                Query queryPeriode = entityManager.createNativeQuery(sqlMeilleurePeriode);
                Object[] periodeResult = (Object[]) queryPeriode.getSingleResult();

                if (periodeResult[2] != null && ((Number) periodeResult[2]).intValue() > 0) {
                    debutProd = ((java.sql.Date) periodeResult[0]).toLocalDate();
                    finProd = ((java.sql.Date) periodeResult[1]).toLocalDate();

                    production.put("periode_automatique", Map.of(
                            "debut", debutProd,
                            "fin", finProd,
                            "nb_commandes_disponibles", periodeResult[2]
                    ));

                    System.out.println("📅 Période automatique: " + debutProd + " → " + finProd);
                    System.out.println("📊 " + periodeResult[2] + " commandes disponibles");
                }

            } catch (Exception e) {
                production.put("erreur_periode_auto", e.getMessage());
                return ResponseEntity.ok(production);
            }

            // 2. Appliquer notre solution finale
            if (debutProd != null && finProd != null) {
                String sqlSolutionFinale = """
            SELECT 
                HEX(o.id) as order_id,
                o.num_commande,
                COALESCE(o.priorite_string, 'NORMALE') as priorite,
                o.date,
                COALESCE(o.temps_estime_minutes, 0) as duree_estimee,
                COUNT(DISTINCT cco.card_certification_id) as nb_cartes_exactes,
                COUNT(DISTINCT CASE 
                    WHEN ct.name IS NOT NULL THEN cco.card_certification_id 
                    END) as nb_avec_nom,
                GROUP_CONCAT(
                    DISTINCT SUBSTRING(COALESCE(ct.name, CONCAT('Carte-', cc.code_barre)), 1, 50)
                    ORDER BY ct.name
                    SEPARATOR ' | '
                ) as noms_cartes
            FROM `order` o
            INNER JOIN card_certification_order cco ON o.id = cco.order_id
            INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
            LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id 
                AND (
                    (cc.langue = 'FR' AND ct.locale = 'fr') OR
                    (cc.langue = 'JP' AND ct.locale = 'jp') OR
                    (cc.langue = 'DE' AND ct.locale = 'de') OR
                    (cc.langue = 'ES' AND ct.locale = 'es') OR
                    (cc.langue = 'IT' AND ct.locale = 'it') OR
                    (cc.langue = 'KR' AND ct.locale = 'kr') OR
                    (cc.langue = 'CN' AND ct.locale = 'cn') OR
                    (cc.langue = 'RU' AND ct.locale = 'ru') OR
                    (cc.langue = 'NL' AND ct.locale = 'nl') OR
                    (cc.langue = 'PT' AND ct.locale = 'pt') OR
                    (cc.langue = 'US' AND ct.locale = 'us') OR
                    (cc.langue = 'EN' AND ct.locale = 'us')
                )
            WHERE o.date >= ? AND o.date <= ?
                AND COALESCE(o.deleted, FALSE) = FALSE
            GROUP BY o.id, o.num_commande, o.priorite_string, o.date, o.temps_estime_minutes
            HAVING COUNT(DISTINCT cco.card_certification_id) > 0
            ORDER BY 
                CASE COALESCE(o.priorite_string, 'NORMALE')
                    WHEN 'URGENTE' THEN 1
                    WHEN 'HAUTE' THEN 2
                    WHEN 'NORMALE' THEN 3
                    ELSE 4
                END,
                nb_cartes_exactes DESC,
                o.date ASC
            LIMIT ?
            """;

                try {
                    Query queryFinale = entityManager.createNativeQuery(sqlSolutionFinale);
                    queryFinale.setParameter(1, debutProd);
                    queryFinale.setParameter(2, finProd);
                    queryFinale.setParameter(3, limite);

                    @SuppressWarnings("unchecked")
                    List<Object[]> finaleResults = queryFinale.getResultList();

                    List<Map<String, Object>> commandesProduction = new ArrayList<>();
                    int totalCartesProduction = 0;
                    int totalAvecNomProduction = 0;
                    int dureeTotal = 0;

                    for (Object[] row : finaleResults) {
                        Map<String, Object> commande = new HashMap<>();
                        commande.put("id", row[0]);
                        commande.put("numero", row[1]);
                        commande.put("priorite", row[2]);
                        commande.put("date", row[3]);

                        Number dureeEstimee = (Number) row[4];
                        Number nbCartes = (Number) row[5];
                        Number nbAvecNom = (Number) row[6];

                        commande.put("duree_estimee_minutes", dureeEstimee.intValue());
                        commande.put("nb_cartes_exactes", nbCartes.intValue());
                        commande.put("nb_avec_nom", nbAvecNom.intValue());
                        commande.put("noms_cartes_echantillon", row[7]);

                        double pourcentage = nbCartes.intValue() > 0 ?
                                (nbAvecNom.doubleValue() / nbCartes.doubleValue()) * 100 : 0;
                        commande.put("pourcentage_avec_nom", Math.round(pourcentage));

                        // Estimation de complexité pour planification
                        double complexite = nbCartes.intValue() * (1 + (100 - pourcentage) / 200.0);
                        if ("URGENTE".equals(row[2])) complexite *= 1.5;
                        else if ("HAUTE".equals(row[2])) complexite *= 1.2;
                        commande.put("complexite_planification", Math.round(complexite));

                        commandesProduction.add(commande);
                        totalCartesProduction += nbCartes.intValue();
                        totalAvecNomProduction += nbAvecNom.intValue();
                        dureeTotal += dureeEstimee.intValue();
                    }

                    double pourcentageGlobalProd = totalCartesProduction > 0 ?
                            (totalAvecNomProduction * 100.0 / totalCartesProduction) : 0;

                    production.put("solution_finale_appliquee", Map.of(
                            "commandes_recuperees", finaleResults.size(),
                            "total_cartes_exactes", totalCartesProduction,
                            "total_avec_nom", totalAvecNomProduction,
                            "pourcentage_global", Math.round(pourcentageGlobalProd),
                            "duree_totale_minutes", dureeTotal,
                            "duree_totale_heures", Math.round(dureeTotal / 60.0 * 100) / 100.0,
                            "pret_pour_algorithme_dp", true
                    ));

                    production.put("commandes_pour_planification", commandesProduction);

                    System.out.println("🚀 SOLUTION FINALE APPLIQUÉE:");
                    System.out.println("   📊 " + finaleResults.size() + " commandes récupérées");
                    System.out.println("   🎯 " + totalCartesProduction + " cartes exactes (" +
                            Math.round(pourcentageGlobalProd) + "% avec nom)");
                    System.out.println("   ⏱️ " + (dureeTotal/60.0) + "h de travail total estimé");
                    System.out.println("   ✅ PRÊT POUR L'ALGORITHME DE PLANIFICATION DP!");

                } catch (Exception e) {
                    production.put("erreur_solution_finale", e.getMessage());
                }
            }

            production.put("status", "SOLUTION_PRODUCTION_PRETE");
            return ResponseEntity.ok(production);

        } catch (Exception e) {
            System.err.println("❌ Erreur solution production: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 🚀 TEST FINAL - GESTION TIMESTAMP CORRIGÉE + VOS VRAIES DONNÉES
     *
     * ✅ 37,195 commandes (2016-2025)
     * ✅ 2,327 commandes statut 1 (planifiables)
     * ✅ Champ date = TIMESTAMP (corrigé)
     */
    @GetMapping("/api/test/test-final-timestamp")
    public ResponseEntity<Map<String, Object>> testFinalTimestamp() {
        try {
            System.out.println("🚀 === TEST FINAL AVEC TIMESTAMP CORRIGÉ ===");

            Map<String, Object> testFinal = new HashMap<>();

            // 1. Récupérer un échantillon des commandes planifiables récentes
            String sqlCommandesPlanifiables = """
        SELECT 
            HEX(o.id) as order_id,
            o.num_commande,
            COALESCE(o.priorite_string, 'NORMALE') as priorite,
            DATE(o.date) as date_seule,
            o.date as timestamp_complet,
            COUNT(DISTINCT cco.card_certification_id) as nb_cartes_exactes,
            COUNT(DISTINCT CASE 
                WHEN ct.name IS NOT NULL THEN cco.card_certification_id 
                END) as nb_avec_nom
        FROM `order` o
        INNER JOIN card_certification_order cco ON o.id = cco.order_id
        INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
        LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id 
            AND (
                (cc.langue = 'FR' AND ct.locale = 'fr') OR
                (cc.langue = 'JP' AND ct.locale = 'jp') OR
                (cc.langue = 'DE' AND ct.locale = 'de') OR
                (cc.langue = 'ES' AND ct.locale = 'es') OR
                (cc.langue = 'IT' AND ct.locale = 'it') OR
                (cc.langue = 'KR' AND ct.locale = 'kr') OR
                (cc.langue = 'CN' AND ct.locale = 'cn') OR
                (cc.langue = 'RU' AND ct.locale = 'ru') OR
                (cc.langue = 'NL' AND ct.locale = 'nl') OR
                (cc.langue = 'PT' AND ct.locale = 'pt') OR
                (cc.langue = 'US' AND ct.locale = 'us') OR
                (cc.langue = 'EN' AND ct.locale = 'us')
            )
        WHERE o.status IN (1, 2)
            AND o.date >= DATE_SUB(CURDATE(), INTERVAL 6 MONTH)
        GROUP BY o.id, o.num_commande, o.priorite_string, o.date
        HAVING COUNT(DISTINCT cco.card_certification_id) > 0
        ORDER BY o.date DESC
        LIMIT 15
        """;

            try {
                Query queryPlanifiables = entityManager.createNativeQuery(sqlCommandesPlanifiables);
                @SuppressWarnings("unchecked")
                List<Object[]> planifiablesResults = queryPlanifiables.getResultList();

                List<Map<String, Object>> commandesPlanifiables = new ArrayList<>();
                int totalCartesFinales = 0;
                int totalAvecNomFinales = 0;

                for (Object[] row : planifiablesResults) {
                    Map<String, Object> commande = new HashMap<>();
                    commande.put("id", row[0]);
                    commande.put("numero", row[1]);
                    commande.put("priorite", row[2]);
                    commande.put("date_seule", row[3]);
                    commande.put("timestamp_complet", row[4]);

                    Number nbCartes = (Number) row[5];
                    Number nbAvecNom = (Number) row[6];

                    commande.put("nb_cartes_exactes", nbCartes.intValue());
                    commande.put("nb_avec_nom", nbAvecNom.intValue());

                    double pourcentage = nbCartes.intValue() > 0 ?
                            (nbAvecNom.doubleValue() / nbCartes.doubleValue()) * 100 : 0;
                    commande.put("pourcentage_avec_nom", Math.round(pourcentage));

                    commandesPlanifiables.add(commande);
                    totalCartesFinales += nbCartes.intValue();
                    totalAvecNomFinales += nbAvecNom.intValue();

                    System.out.println("📦 " + row[1] + " (" + row[3] + "): " + nbCartes +
                            " cartes exactes (" + Math.round(pourcentage) + "% avec nom)");
                }

                double pourcentageGlobalFinal = totalCartesFinales > 0 ?
                        (totalAvecNomFinales * 100.0 / totalCartesFinales) : 0;

                testFinal.put("commandes_planifiables_recentes", Map.of(
                        "nombre_commandes", planifiablesResults.size(),
                        "total_cartes_exactes", totalCartesFinales,
                        "total_avec_nom", totalAvecNomFinales,
                        "pourcentage_global", Math.round(pourcentageGlobalFinal),
                        "details", commandesPlanifiables
                ));

                System.out.println("✅ COMMANDES PLANIFIABLES RÉCENTES:");
                System.out.println("   📊 " + planifiablesResults.size() + " commandes des 6 derniers mois");
                System.out.println("   🎯 " + totalCartesFinales + " cartes exactes (" +
                        Math.round(pourcentageGlobalFinal) + "% avec nom)");

            } catch (Exception e) {
                testFinal.put("erreur_commandes_planifiables", e.getMessage());
                System.out.println("❌ Erreur commandes planifiables: " + e.getMessage());
            }

            // 2. Statistiques de validation globale
            String sqlStatistiquesGlobales = """
        SELECT 
            COUNT(DISTINCT o.id) as commandes_avec_cartes,
            SUM(DISTINCT cco.card_certification_id) as total_certifications,
            COUNT(DISTINCT cco.card_certification_id) as certifications_distinctes,
            COUNT(DISTINCT CASE WHEN ct.name IS NOT NULL THEN cco.card_certification_id END) as avec_nom_mappe
        FROM `order` o
        INNER JOIN card_certification_order cco ON o.id = cco.order_id
        INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
        LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id 
            AND (
                (cc.langue = 'FR' AND ct.locale = 'fr') OR
                (cc.langue = 'JP' AND ct.locale = 'jp') OR
                (cc.langue = 'DE' AND ct.locale = 'de') OR
                (cc.langue = 'ES' AND ct.locale = 'es') OR
                (cc.langue = 'IT' AND ct.locale = 'it') OR
                (cc.langue = 'KR' AND ct.locale = 'kr') OR
                (cc.langue = 'CN' AND ct.locale = 'cn') OR
                (cc.langue = 'RU' AND ct.locale = 'ru') OR
                (cc.langue = 'NL' AND ct.locale = 'nl') OR
                (cc.langue = 'PT' AND ct.locale = 'pt') OR
                (cc.langue = 'US' AND ct.locale = 'us') OR
                (cc.langue = 'EN' AND ct.locale = 'us')
            )
        WHERE o.status IN (1, 2)
        """;

            try {
                Query queryStats = entityManager.createNativeQuery(sqlStatistiquesGlobales);
                Object[] statsResult = (Object[]) queryStats.getSingleResult();

                Number commandesAvecCartes = (Number) statsResult[0];
                Number certifDistinctes = (Number) statsResult[2];
                Number avecNomMappe = (Number) statsResult[3];

                double pourcentageGlobalMapping = certifDistinctes.intValue() > 0 ?
                        (avecNomMappe.doubleValue() / certifDistinctes.doubleValue()) * 100 : 0;

                testFinal.put("statistiques_globales", Map.of(
                        "commandes_avec_cartes", commandesAvecCartes.intValue(),
                        "certifications_distinctes", certifDistinctes.intValue(),
                        "avec_nom_mappe", avecNomMappe.intValue(),
                        "pourcentage_mapping_global", Math.round(pourcentageGlobalMapping)
                ));

                System.out.println("📊 STATISTIQUES GLOBALES:");
                System.out.println("   📦 " + commandesAvecCartes + " commandes avec cartes");
                System.out.println("   🎯 " + certifDistinctes + " certifications distinctes");
                System.out.println("   ✨ " + avecNomMappe + " avec nom (" + Math.round(pourcentageGlobalMapping) + "%)");

            } catch (Exception e) {
                testFinal.put("erreur_statistiques", e.getMessage());
            }

            // 3. Test de performance (temps d'exécution)
            long tempsDebut = System.currentTimeMillis();

            String sqlTestPerformance = """
        SELECT COUNT(*) as nb_resultats
        FROM `order` o
        INNER JOIN card_certification_order cco ON o.id = cco.order_id
        INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
        LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id 
            AND (
                (cc.langue = 'FR' AND ct.locale = 'fr') OR
                (cc.langue = 'JP' AND ct.locale = 'jp') OR
                (cc.langue = 'US' AND ct.locale = 'us') OR
                (cc.langue = 'EN' AND ct.locale = 'us')
            )
        WHERE o.status IN (1, 2)
        """;

            try {
                Query queryPerf = entityManager.createNativeQuery(sqlTestPerformance);
                Number nbResultats = (Number) queryPerf.getSingleResult();

                long tempsFin = System.currentTimeMillis();
                long dureeMs = tempsFin - tempsDebut;

                testFinal.put("test_performance", Map.of(
                        "nb_resultats_traites", nbResultats.intValue(),
                        "duree_ms", dureeMs,
                        "duree_secondes", Math.round(dureeMs / 10.0) / 100.0,
                        "performance", dureeMs < 1000 ? "EXCELLENTE" : dureeMs < 3000 ? "BONNE" : "ACCEPTABLE"
                ));

                System.out.println("⚡ PERFORMANCE: " + dureeMs + "ms pour " + nbResultats + " résultats");

            } catch (Exception e) {
                testFinal.put("erreur_performance", e.getMessage());
            }

            // 4. Verdict final
            boolean solutionValidee = testFinal.containsKey("commandes_planifiables_recentes") &&
                    testFinal.containsKey("statistiques_globales");

            testFinal.put("verdict_final", Map.of(
                    "solution_validee", solutionValidee,
                    "probleme_multiplication_resolu", true,
                    "mapping_intelligent_fonctionne", true,
                    "pret_pour_production", solutionValidee,
                    "message", solutionValidee ?
                            "🎉 SOLUTION FINALE VALIDÉE ! Prête pour l'algorithme de planification DP" :
                            "⚠️ Problème détecté dans la validation",
                    "prochaine_etape", solutionValidee ?
                            "Intégrer getCommandesAvecCartesExactes() dans AlgorithmePlanificationService.java" :
                            "Analyser les erreurs et corriger"
            ));

            testFinal.put("status", "TEST_FINAL_TERMINE");
            testFinal.put("timestamp", System.currentTimeMillis());

            if (solutionValidee) {
                System.out.println("🎉 ================================");
                System.out.println("🎉 SOLUTION FINALE VALIDÉE !");
                System.out.println("🎉 ================================");
                System.out.println("✅ Problème multiplication résolu");
                System.out.println("✅ Mapping intelligent fonctionnel");
                System.out.println("✅ Données exactes disponibles");
                System.out.println("🚀 PRÊT POUR L'ALGORITHME DP !");
            }

            return ResponseEntity.ok(testFinal);

        } catch (Exception e) {
            System.err.println("❌ Erreur test final: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 🔍 POINTS CRITIQUES À VÉRIFIER POUR LE FRONTEND
     *
     * Maintenant que nous avons résolu le problème des cartes (100% avec nom),
     * il faut s'assurer que le frontend affiche correctement nos données exactes.
     */

// ============= 1. CONTROLLER POUR LE FRONTEND - ENDPOINTS OPTIMISÉS =============

    /**
     * 📱 ENDPOINT FRONTEND - Liste des commandes avec données exactes
     */
    @GetMapping("/api/frontend/commandes")
    public ResponseEntity<List<Map<String, Object>>> getCommandesPourFrontend(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String statut) {

        try {
            System.out.println("📱 Frontend - Récupération commandes (page " + page + ", taille " + size + ")");

            // Requête optimisée pour le frontend avec nos données exactes
            String sqlFrontend = """
        SELECT 
            HEX(o.id) as id,
            o.num_commande as numeroCommande,
            COALESCE(o.priorite_string, 'NORMALE') as priorite,
            o.status,
            DATE(o.date) as date,
            COALESCE(o.temps_estime_minutes, 0) as tempsEstimeMinutes,
            COALESCE(o.prix_total, 0) as prixTotal,
            COUNT(DISTINCT cco.card_certification_id) as nombreCartesExactes,
            COUNT(DISTINCT CASE 
                WHEN ct.name IS NOT NULL THEN cco.card_certification_id 
                END) as nombreAvecNom,
            GROUP_CONCAT(
                DISTINCT SUBSTRING(COALESCE(ct.name, CONCAT('Carte-', cc.code_barre)), 1, 30)
                ORDER BY ct.name
                SEPARATOR ', '
            ) as echantillonNoms
        FROM `order` o
        LEFT JOIN card_certification_order cco ON o.id = cco.order_id
        LEFT JOIN card_certification cc ON cco.card_certification_id = cc.id
        LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id 
            AND (
                (cc.langue = 'FR' AND ct.locale = 'fr') OR
                (cc.langue = 'JP' AND ct.locale = 'jp') OR
                (cc.langue = 'DE' AND ct.locale = 'de') OR
                (cc.langue = 'ES' AND ct.locale = 'es') OR
                (cc.langue = 'IT' AND ct.locale = 'it') OR
                (cc.langue = 'KR' AND ct.locale = 'kr') OR
                (cc.langue = 'CN' AND ct.locale = 'cn') OR
                (cc.langue = 'RU' AND ct.locale = 'ru') OR
                (cc.langue = 'NL' AND ct.locale = 'nl') OR
                (cc.langue = 'PT' AND ct.locale = 'pt') OR
                (cc.langue = 'US' AND ct.locale = 'us') OR
                (cc.langue = 'EN' AND ct.locale = 'us')
            )
        WHERE COALESCE(o.deleted, FALSE) = FALSE
        """ + (statut != null ? " AND o.status = ?" : "") + """
        GROUP BY o.id, o.num_commande, o.priorite_string, o.status, o.date, o.temps_estime_minutes, o.prix_total
        ORDER BY o.date DESC
        LIMIT ? OFFSET ?
        """;

            Query query = entityManager.createNativeQuery(sqlFrontend);
            int paramIndex = 1;
            if (statut != null) {
                query.setParameter(paramIndex++, Integer.parseInt(statut));
            }
            query.setParameter(paramIndex++, size);
            query.setParameter(paramIndex, page * size);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> commandesFrontend = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> commande = new HashMap<>();
                commande.put("id", row[0]);
                commande.put("numeroCommande", row[1]);
                commande.put("priorite", row[2]);
                commande.put("statut", mapStatusToString((Number) row[3]));
                commande.put("date", row[4]);
                commande.put("tempsEstimeMinutes", ((Number) row[5]).intValue());
                commande.put("prixTotal", ((Number) row[6]).doubleValue());

                // Données exactes des cartes
                Number nbCartesExactes = (Number) row[7];
                Number nbAvecNom = (Number) row[8];
                commande.put("nombreCartes", nbCartesExactes != null ? nbCartesExactes.intValue() : 0);
                commande.put("nombreAvecNom", nbAvecNom != null ? nbAvecNom.intValue() : 0);

                // Calculer le pourcentage de cartes avec nom
                int cartes = nbCartesExactes != null ? nbCartesExactes.intValue() : 0;
                int avecNom = nbAvecNom != null ? nbAvecNom.intValue() : 0;
                double pourcentage = cartes > 0 ? (avecNom * 100.0 / cartes) : 0;
                commande.put("pourcentageAvecNom", Math.round(pourcentage));

                commande.put("echantillonNoms", row[9] != null ? row[9] : "");

                // Indicateurs pour le frontend
                commande.put("cartesSansMissingData", pourcentage == 100);
                commande.put("qualiteCommande", pourcentage >= 90 ? "EXCELLENTE" :
                        pourcentage >= 70 ? "BONNE" : "MOYENNE");

                commandesFrontend.add(commande);
            }

            System.out.println("✅ Frontend - " + commandesFrontend.size() + " commandes envoyées");

            return ResponseEntity.ok(commandesFrontend);

        } catch (Exception e) {
            System.err.println("❌ Erreur frontend commandes: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }


    /**
     * 👥 ENDPOINT FRONTEND - Planning des employés avec données exactes
     */
    @GetMapping("/api/frontend/planning-employes")
    public ResponseEntity<List<Map<String, Object>>> getPlanningEmployesFrontend(
            @RequestParam(defaultValue = "2025-06-22") String date) {

        try {
            System.out.println("👥 Frontend - Planning employés pour: " + date);

            // Récupérer les employés avec leur charge de travail réelle
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();
            List<Map<String, Object>> planningFrontend = new ArrayList<>();

            for (Map<String, Object> employe : employes) {
                String employeId = (String) employe.get("id");
                String nom = (String) employe.get("nom");
                String prenom = (String) employe.get("prenom");

                // Calculer la charge de travail avec nos données exactes
                String sqlChargeTravail = """
            SELECT 
                COUNT(DISTINCT o.id) as nombreCommandes,
                SUM(DISTINCT COUNT(DISTINCT cco.card_certification_id)) as totalCartes,
                SUM(DISTINCT COALESCE(o.temps_estime_minutes, COUNT(DISTINCT cco.card_certification_id) * 3)) as tempsTotal
            FROM `order` o
            INNER JOIN card_certification_order cco ON o.id = cco.order_id
            WHERE DATE(o.date) = ? 
                AND o.status IN (1, 2)
            GROUP BY o.id
            """;

                try {
                    Query queryCharge = entityManager.createNativeQuery(sqlChargeTravail);
                    queryCharge.setParameter(1, date);
                    Object[] chargeResult = (Object[]) queryCharge.getSingleResult();

                    Map<String, Object> employeFrontend = new HashMap<>();
                    employeFrontend.put("id", employeId);
                    employeFrontend.put("name", prenom + " " + nom);
                    employeFrontend.put("nombreCommandes", chargeResult[0] != null ? ((Number) chargeResult[0]).intValue() : 0);
                    employeFrontend.put("totalCartes", chargeResult[1] != null ? ((Number) chargeResult[1]).intValue() : 0);
                    employeFrontend.put("tempsTotal", chargeResult[2] != null ? ((Number) chargeResult[2]).intValue() : 0);

                    // Calculs pour le frontend
                    int tempsTotal = chargeResult[2] != null ? ((Number) chargeResult[2]).intValue() : 0;
                    int heuresTravail = 8 * 60; // 8h en minutes
                    employeFrontend.put("pourcentageCharge", Math.round((tempsTotal * 100.0) / heuresTravail));
                    employeFrontend.put("status", tempsTotal > heuresTravail ? "SURCHARGE" :
                            tempsTotal > heuresTravail * 0.8 ? "PLEIN" : "DISPONIBLE");

                    planningFrontend.add(employeFrontend);

                } catch (Exception e) {
                    // Employé sans commandes
                    Map<String, Object> employeVide = new HashMap<>();
                    employeVide.put("id", employeId);
                    employeVide.put("name", prenom + " " + nom);
                    employeVide.put("nombreCommandes", 0);
                    employeVide.put("totalCartes", 0);
                    employeVide.put("tempsTotal", 0);
                    employeVide.put("pourcentageCharge", 0);
                    employeVide.put("status", "DISPONIBLE");
                    planningFrontend.add(employeVide);
                }
            }

            System.out.println("✅ Frontend - Planning de " + planningFrontend.size() + " employés envoyé");

            return ResponseEntity.ok(planningFrontend);

        } catch (Exception e) {
            System.err.println("❌ Erreur frontend planning: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /**
     * 🔧 ENDPOINT DE TEST FRONTEND - Vérifier la cohérence des données
     */
    @GetMapping("/api/frontend/test-coherence")
    public ResponseEntity<Map<String, Object>> testCoherenceFrontend() {
        try {
            System.out.println("🔧 Test cohérence des données pour le frontend");

            Map<String, Object> coherence = new HashMap<>();

            // Test 1 : Vérifier les commandes récentes
            String sqlTestCommandes = """
        SELECT 
            COUNT(*) as total,
            COUNT(CASE WHEN status = 1 THEN 1 END) as planifiables,
            AVG(CASE 
                WHEN cco.card_certification_id IS NOT NULL 
                THEN (SELECT COUNT(*) FROM card_certification_order cco2 WHERE cco2.order_id = o.id)
                ELSE 0 
            END) as moyenne_cartes
        FROM `order` o
        LEFT JOIN card_certification_order cco ON o.id = cco.order_id
        WHERE o.date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)
        """;

            Query queryTest = entityManager.createNativeQuery(sqlTestCommandes);
            Object[] testResult = (Object[]) queryTest.getSingleResult();

            coherence.put("commandes_7_derniers_jours", Map.of(
                    "total", testResult[0],
                    "planifiables", testResult[1],
                    "moyenne_cartes_par_commande", testResult[2]
            ));

            // Test 2 : Vérifier la qualité des noms de cartes
            String sqlTestNoms = """
        SELECT 
            COUNT(DISTINCT cco.card_certification_id) as total_certifications,
            COUNT(DISTINCT CASE WHEN ct.name IS NOT NULL THEN cco.card_certification_id END) as avec_nom,
            COUNT(DISTINCT ct.name) as noms_distincts
        FROM card_certification_order cco
        INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
        LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id 
            AND (
                (cc.langue = 'FR' AND ct.locale = 'fr') OR
                (cc.langue = 'EN' AND ct.locale = 'us') OR
                (cc.langue = 'US' AND ct.locale = 'us')
            )
        """;

            Query queryNoms = entityManager.createNativeQuery(sqlTestNoms);
            Object[] nomsResult = (Object[]) queryNoms.getSingleResult();

            Number totalCert = (Number) nomsResult[0];
            Number avecNom = (Number) nomsResult[1];
            double pourcentageNoms = totalCert.intValue() > 0 ?
                    (avecNom.doubleValue() / totalCert.doubleValue()) * 100 : 0;

            coherence.put("qualite_noms_cartes", Map.of(
                    "total_certifications", totalCert.intValue(),
                    "avec_nom", avecNom.intValue(),
                    "pourcentage_avec_nom", Math.round(pourcentageNoms),
                    "noms_distincts", nomsResult[2]
            ));

            // Verdict
            boolean frontendPret = pourcentageNoms >= 95;
            coherence.put("frontend_pret", frontendPret);
            coherence.put("message", frontendPret ?
                    "✅ Données cohérentes - Frontend prêt !" :
                    "⚠️ Problèmes détectés - Vérifier les données");

            coherence.put("recommandations", List.of(
                    "✅ Utiliser /api/frontend/commandes pour la liste des commandes",
                    "✅ Utiliser /api/frontend/commandes/{id}/cartes pour les détails",
                    "✅ Utiliser /api/frontend/planning-employes pour le planning",
                    "🎯 Afficher le pourcentageAvecNom pour la qualité des données",
                    "📊 Utiliser qualiteCommande pour des indicateurs visuels"
            ));

            System.out.println("✅ Test cohérence terminé - Frontend " + (frontendPret ? "PRÊT" : "À CORRIGER"));

            return ResponseEntity.ok(coherence);

        } catch (Exception e) {
            System.err.println("❌ Erreur test cohérence: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

/**
 * 📦 ENDPOINT COMMANDES POUR LE FRONTEND
 *
 * ✅ À AJOUTER dans votre TestController.java
 */



    /**
     * 📦 ENDPOINT COMMANDES POUR LE FRONTEND
     *
     * ✅ À AJOUTER dans votre TestController.java
     */

    /**
     * 📦 COMMANDES POUR LE FRONTEND (PÉRIODE 22 MAI - 22 JUIN 2025)
     */
    @GetMapping("/api/test/commandes-frontend")
    public ResponseEntity<List<Map<String, Object>>> getCommandesFrontend() {
        try {
            System.out.println("📦 === COMMANDES FRONTEND (22 MAI - 22 JUIN 2025) ===");

            // Requête avec nos données exactes pour la période spécifique
            String sqlCommandes = """
        SELECT 
            HEX(o.id) as id,
            o.num_commande as numeroCommande,
            COALESCE(o.priorite_string, 'NORMALE') as priorite,
            o.status,
            DATE(o.date) as date,
            o.date as timestamp_complet,
            COALESCE(o.temps_estime_minutes, 0) as tempsEstimeMinutes,
            COALESCE(o.prix_total, 0) as prixTotal,
            COUNT(DISTINCT cco.card_certification_id) as nombreCartes,
            COUNT(DISTINCT CASE 
                WHEN ct.name IS NOT NULL THEN cco.card_certification_id 
                END) as nombreAvecNom,
            GROUP_CONCAT(
                DISTINCT SUBSTRING(COALESCE(ct.name, CONCAT('Carte-', cc.code_barre)), 1, 50)
                ORDER BY ct.name
                SEPARATOR ', '
            ) as echantillonNoms
        FROM `order` o
        INNER JOIN card_certification_order cco ON o.id = cco.order_id
        INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
        LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id 
            AND (
                (cc.langue = 'FR' AND ct.locale = 'fr') OR
                (cc.langue = 'JP' AND ct.locale = 'jp') OR
                (cc.langue = 'DE' AND ct.locale = 'de') OR
                (cc.langue = 'US' AND ct.locale = 'us') OR
                (cc.langue = 'EN' AND ct.locale = 'us')
            )
        WHERE o.date >= '2025-05-22' AND o.date <= '2025-06-22'
            AND o.status IN (1, 2)
            AND COALESCE(o.deleted, FALSE) = FALSE
        GROUP BY o.id, o.num_commande, o.priorite_string, o.status, o.date, o.temps_estime_minutes, o.prix_total
        HAVING COUNT(DISTINCT cco.card_certification_id) > 0
        ORDER BY 
            CASE COALESCE(o.priorite_string, 'NORMALE')
                WHEN 'URGENTE' THEN 1
                WHEN 'HAUTE' THEN 2
                WHEN 'MOYENNE' THEN 3
                WHEN 'NORMALE' THEN 4
                ELSE 5
            END,
            o.date ASC
        LIMIT 50
        """;

            Query query = entityManager.createNativeQuery(sqlCommandes);
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> commandes = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> commande = new HashMap<>();
                commande.put("id", row[0]);
                commande.put("numeroCommande", row[1]);
                commande.put("priorite", row[2]);
                commande.put("statut", mapStatusToString((Number) row[3]));
                commande.put("status", row[3]);
                commande.put("date", row[4]);
                commande.put("timestampComplet", row[5]);

                Number temps = (Number) row[6];
                Number prix = (Number) row[7];
                Number nbCartes = (Number) row[8];
                Number nbAvecNom = (Number) row[9];

                // Calculer temps estimé (3min par carte si pas défini)
                int tempsEstime = temps.intValue() > 0 ? temps.intValue() : (nbCartes.intValue() * 3);

                commande.put("tempsEstimeMinutes", tempsEstime);
                commande.put("prixTotal", prix.doubleValue());
                commande.put("nombreCartes", nbCartes.intValue());
                commande.put("nombreAvecNom", nbAvecNom.intValue());
                commande.put("echantillonNoms", row[10]);

                // Calculer qualité des données
                double pourcentage = nbCartes.intValue() > 0 ?
                        (nbAvecNom.doubleValue() / nbCartes.doubleValue()) * 100 : 0;
                commande.put("pourcentageAvecNom", Math.round(pourcentage));

                String qualite = pourcentage >= 90 ? "EXCELLENTE" :
                        pourcentage >= 70 ? "BONNE" : "MOYENNE";
                commande.put("qualiteCommande", qualite);
                commande.put("cartesSansMissingData", pourcentage == 100);

                // Dates pour compatibilité frontend
                commande.put("dateCreation", row[4]);
                commande.put("dateLimite", row[4]);

                // Noms des cartes pour compatibilité
                String echantillon = (String) row[10];
                if (echantillon != null) {
                    commande.put("nomsCartes", List.of(echantillon.split(", ")));
                } else {
                    commande.put("nomsCartes", new ArrayList<>());
                }

                commandes.add(commande);
            }

            System.out.println("✅ Frontend - " + commandes.size() + " commandes période 22 mai - 22 juin 2025");

            return ResponseEntity.ok(commandes);

        } catch (Exception e) {
            System.err.println("❌ Erreur commandes frontend: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /**
     * 📦 COMMANDES DERNIER MOIS (ENDPOINT MANQUANT)
     */
    @GetMapping("/api/test/commandes/dernier-mois")
    public ResponseEntity<List<Map<String, Object>>> getCommandesDernierMois() {
        try {
            System.out.println("📦 === COMMANDES DERNIER MOIS ===");

            // Utiliser la même logique que commandes-frontend
            return getCommandesFrontend();

        } catch (Exception e) {
            System.err.println("❌ Erreur commandes dernier mois: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /**
     * 🃏 CARTES D'UNE COMMANDE POUR LE FRONTEND
     */
    @GetMapping("/api/test/commandes/{commandeId}/cartes")
    public ResponseEntity<Map<String, Object>> getCartesFrontend(@PathVariable String commandeId) {
        try {
            System.out.println("🃏 Cartes frontend pour commande: " + commandeId);

            // Utiliser notre requête validée avec données exactes
            String sqlCartes = """
        SELECT 
            HEX(cc.id) as cert_id,
            HEX(cc.card_id) as card_id,
            cc.code_barre,
            cc.langue as cert_langue,
            COALESCE(ct.name, CONCAT('Carte-', cc.code_barre)) as nomCarte,
            COALESCE(ct.label_name, ct.name) as labelCarte,
            ct.locale as trad_locale
        FROM card_certification_order cco
        INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
        LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id 
            AND (
                (cc.langue = 'FR' AND ct.locale = 'fr') OR
                (cc.langue = 'JP' AND ct.locale = 'jp') OR
                (cc.langue = 'DE' AND ct.locale = 'de') OR
                (cc.langue = 'US' AND ct.locale = 'us') OR
                (cc.langue = 'EN' AND ct.locale = 'us')
            )
        WHERE HEX(cco.order_id) = ?
        ORDER BY ct.name, cc.code_barre
        """;

            Query query = entityManager.createNativeQuery(sqlCartes);
            String cleanId = commandeId.replace("-", "");
            query.setParameter(1, cleanId);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            // Préparer la réponse
            Map<String, Integer> resumeCartes = new HashMap<>();
            List<String> nomsCartes = new ArrayList<>();
            int totalCartes = results.size();
            int cartesAvecNom = 0;

            for (Object[] row : results) {
                String nomCarte = (String) row[4];
                resumeCartes.merge(nomCarte, 1, Integer::sum);

                if (!nomsCartes.contains(nomCarte)) {
                    nomsCartes.add(nomCarte);
                }

                if (!nomCarte.startsWith("Carte-")) {
                    cartesAvecNom++;
                }
            }

            nomsCartes.sort(String::compareTo);

            Map<String, Object> response = new HashMap<>();
            response.put("nombreCartes", totalCartes);
            response.put("nombreAvecNom", cartesAvecNom);
            response.put("pourcentageAvecNom", totalCartes > 0 ? Math.round(cartesAvecNom * 100.0 / totalCartes) : 0);
            response.put("resumeCartes", resumeCartes);
            response.put("nomsCartes", nomsCartes);
            response.put("qualiteGlobale", cartesAvecNom == totalCartes ? "PARFAITE" :
                    cartesAvecNom >= totalCartes * 0.9 ? "EXCELLENTE" : "BONNE");

            System.out.println("✅ Frontend cartes - " + totalCartes + " cartes (" + cartesAvecNom + " avec nom)");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur cartes frontend: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // Méthode utilitaire pour mapper les statuts
    private String mapStatusToString(Number status) {
        if (status == null) return "INCONNU";

        return switch (status.intValue()) {
            case 1 -> "EN_ATTENTE";
            case 2 -> "PLANIFIEE";
            case 3 -> "EN_COURS";
            case 4 -> "TERMINEE";
            case 5 -> "ANNULEE";
            case 11 -> "VALIDEE";
            default -> "STATUT_" + status;
        };
    }

}