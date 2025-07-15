package com.pcagrade.order.controller;

import com.github.f4b6a3.ulid.Ulid;
import com.pcagrade.order.entity.Commande;
import com.pcagrade.order.repository.CommandeRepository;
import com.pcagrade.order.repository.EmployeRepository;
import com.pcagrade.order.repository.PlanificationRepository;
import com.pcagrade.order.service.CommandeService;
import com.pcagrade.order.service.EmployeService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/planifications")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "false")
public class PlanificationController {

    @Autowired
    private CommandeService commandeService;

    @Autowired
    private EmployeService employeService;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private PlanificationRepository planificationRepository;

    @Autowired
    private EmployeRepository employeRepository;


    // Endpoint de test simple
    @PostMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "OK");
        result.put("message", "Test endpoint fonctionne");
        result.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(result);
    }

    // Planification automatique avec répartition intelligente - VERSION CORRIGÉE
    @PostMapping("/planifier-automatique")
    @Transactional
    public ResponseEntity<Map<String, Object>> planifierAutomatique(
            @RequestParam(defaultValue = "1") int jour,
            @RequestParam(defaultValue = "5") int mois,
            @RequestParam(defaultValue = "2025") int annee) {

        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("🔍 Planification automatique intelligente - commandes depuis le " +
                    jour + "/" + mois + "/" + annee);

            // Récupérer les commandes à planifier depuis la date donnée
            List<Map<String, Object>> commandes = commandeService.getCommandesAPlanifierDepuisDate(jour, mois, annee);
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();

            System.out.println("📦 Commandes depuis " + jour + "/" + mois + "/" + annee + ": " +
                    commandes.size() + ", 👥 Employés: " + employes.size());

            if (commandes.isEmpty()) {
                result.put("success", true);
                result.put("message", "Aucune commande à planifier depuis le " + jour + "/" + mois + "/" + annee);
                result.put("nombreCommandesPlanifiees", 0);
                return ResponseEntity.ok(result);
            }

            List<Map<String, Object>> planificationsCreees = new ArrayList<>();
            int commandesDejaPlannifiees = 0;
            int commandesNonPlannifiees = 0;

            for (int i = 0; i < commandes.size(); i++) {
                System.out.println("--- Traitement planification " + (i + 1) + " ---");

                Map<String, Object> commande = commandes.get(i);

                try {
                    // ========== CALCUL DE LA DURÉE BASÉE SUR LES CARTES ==========
                    Integer nombreCartes = (Integer) commande.get("nombreCartesReelles");
                    Integer dureeCalculee = (nombreCartes != null && nombreCartes > 0) ? nombreCartes * 3 : 15; // 15 min minimum

                    System.out.println("📊 Commande " + commande.get("numeroCommande") +
                            " : " + nombreCartes + " cartes → " + dureeCalculee + " minutes");

                    // ========== RECHERCHE EMPLOYÉ DISPONIBLE ==========
                    Map<String, Object> employe = null;
                    String employeIdFinal = null;
                    LocalDate datePlanifFinal = LocalDate.now().plusDays(1);

                    // Chercher un employé disponible sur 7 jours
                    for (int tentative = 0; tentative < 7; tentative++) {
                        LocalDate dateTest = LocalDate.now().plusDays(1 + tentative);
                        employeIdFinal = trouverEmployeDisponible(employes, dureeCalculee, dateTest);

                        if (employeIdFinal != null) {
                            datePlanifFinal = dateTest;
                            // Trouver l'objet employé correspondant
                            String finalEmployeIdFinal = employeIdFinal;
                            employe = employes.stream()
                                    .filter(e -> finalEmployeIdFinal.equals(((String) e.get("id")).replace("-", "")))
                                    .findFirst()
                                    .orElse(null);
                            break;
                        }
                    }

                    if (employe == null) {
                        System.out.println("❌ Aucun employé disponible pour " + commande.get("numeroCommande") + " sur 7 jours");
                        commandesNonPlannifiees++;
                        continue;
                    }

                    // ========== PRÉPARATION DES DONNÉES ==========
                    String commandeId = ((String) commande.get("id")).replace("-", "");

                    // Calculer l'heure en fonction de la charge actuelle de l'employé
                    int chargeActuelle = getChargeEmployePourJour(employeIdFinal, datePlanifFinal);
                    int heureEnMinutes = 8 * 60 + chargeActuelle; // Commencer à 8h + charge existante
                    LocalTime heurePlanif = LocalTime.of(heureEnMinutes / 60, heureEnMinutes % 60);

                    System.out.println("✅ Assignation: " + commande.get("numeroCommande") +
                            " -> " + employe.get("prenom") + " " + employe.get("nom") +
                            " le " + datePlanifFinal + " à " + heurePlanif);

                    // ========== VÉRIFICATION ANTI-DOUBLONS ==========
                    Query checkQuery = entityManager.createNativeQuery(
                            "SELECT COUNT(*) FROM planification " +
                                    "WHERE order_id = UNHEX(?) AND employe_id = UNHEX(?) " +
                                    "AND date_planification = ? AND heure_debut = ?"
                    );

                    checkQuery.setParameter(1, commandeId);
                    checkQuery.setParameter(2, employeIdFinal);
                    checkQuery.setParameter(3, datePlanifFinal);
                    checkQuery.setParameter(4, heurePlanif);

                    Number count = (Number) checkQuery.getSingleResult();

                    if (count.intValue() > 0) {
                        System.out.println("⚠️ Planification déjà existante pour " + commande.get("numeroCommande") + " - IGNORÉE");
                        commandesDejaPlannifiees++;
                        continue;
                    }

                    // ========== INSERTION ==========
                    System.out.println("✨ Création nouvelle planification...");

                    String insertSql = "INSERT INTO planification " +
                            "(id, order_id, employe_id, date_planification, heure_debut, duree_minutes, terminee) " +
                            "VALUES (UNHEX(?), UNHEX(?), UNHEX(?), ?, ?, ?, ?)";

                    String newId = UUID.randomUUID().toString().replace("-", "");

                    int rowsAffected = entityManager.createNativeQuery(insertSql)
                            .setParameter(1, newId)
                            .setParameter(2, commandeId)
                            .setParameter(3, employeIdFinal)
                            .setParameter(4, datePlanifFinal)
                            .setParameter(5, heurePlanif)
                            .setParameter(6, dureeCalculee)
                            .setParameter(7, false)
                            .executeUpdate();

                    if (rowsAffected > 0) {
                        planificationsCreees.add(Map.of(
                                "commande", commande.get("numeroCommande"),
                                "employe", employe.get("prenom") + " " + employe.get("nom"),
                                "date", datePlanifFinal.toString(),
                                "heure", heurePlanif.toString()
                        ));
                        System.out.println("✅ Planification " + (i + 1) + " créée avec succès");
                    } else {
                        System.out.println("❌ Échec insertion planification " + (i + 1));
                        commandesNonPlannifiees++;
                    }

                } catch (Exception e) {
                    System.err.println("❌ Erreur planification " + (i + 1) + ": " + e.getMessage());
                    commandesNonPlannifiees++;
                    e.printStackTrace();
                }
            }

            // ✅ Retourner les comptages détaillés
            result.put("success", true);
            result.put("message", "Planifications créées: " + planificationsCreees.size() +
                    ", Déjà planifiées: " + commandesDejaPlannifiees +
                    ", Échecs: " + commandesNonPlannifiees);
            result.put("nombreCommandesPlanifiees", planificationsCreees.size());
            result.put("nombreCommandesNonPlanifiees", commandesNonPlannifiees);
            result.put("commandesDejaPlannifiees", commandesDejaPlannifiees);
            result.put("totalCommandes", commandes.size());
            result.put("planifications", planificationsCreees);

            System.out.println("🔍 Envoi result: " + result);
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("❌ Erreur planification: " + e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }
    // Endpoint pour lister les planifications (temporaire, retourne vide)
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getToutesPlanifications() {
        try {
            // Pour l'instant, retourner une liste vide
            // TODO: Implémenter la récupération des planifications avec méthode native
            return ResponseEntity.ok(List.of());
        } catch (Exception e) {
            System.err.println("❌ Erreur récupération planifications: " + e.getMessage());
            return ResponseEntity.status(500).body(List.of());
        }
    }

    @GetMapping("/periode")
    public ResponseEntity<List<Map<String, Object>>> getPlanificationsByPeriode(
            @RequestParam String debut,
            @RequestParam String fin) {
        try {
            System.out.println("🔍 Récupération planifications pour période: " + debut + " à " + fin);

            LocalDate dateDebut = LocalDate.parse(debut);
            LocalDate dateFin = LocalDate.parse(fin);

            Query query = entityManager.createNativeQuery(
                    "SELECT " +
                            "  HEX(p.id) as planification_id, " +
                            "  HEX(p.order_id) as order_id, " +
                            "  HEX(p.employe_id) as employe_id, " +
                            "  p.date_planification, " +
                            "  p.heure_debut, " +
                            "  p.duree_minutes, " +
                            "  p.terminee, " +
                            "  o.num_commande, " +
                            "  o.priorite_string, " +
                            "  o.temps_estime_minutes, " +
                            "  o.nombre_cartes, " +
                            "  o.prix_total, " +
                            "  o.status as commande_status, " +
                            "  e.prenom, " +
                            "  e.nom, " +
                            "  e.email " +
                            "FROM planification p " +
                            "INNER JOIN `order` o ON p.order_id = o.id " +
                            "INNER JOIN employe e ON p.employe_id = e.id " +
                            "WHERE p.date_planification BETWEEN ? AND ? " +
                            "ORDER BY p.date_planification, p.heure_debut"
            );

            query.setParameter(1, dateDebut);
            query.setParameter(2, dateFin);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();
            List<Map<String, Object>> planifications = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> planification = new HashMap<>();

                // ID de la planification
                planification.put("id", row[0]);

                // EMPLOYÉ
                Map<String, Object> employe = new HashMap<>();
                employe.put("id", row[2]);
                employe.put("nom", row[14]);
                employe.put("prenom", row[13]);
                employe.put("email", row[15]);
                planification.put("employe", employe);

                // COMMANDE
                Map<String, Object> commande = new HashMap<>();
                commande.put("id", row[1]);
                commande.put("numeroCommande", row[7]);
                commande.put("priorite", row[8] != null ? row[8] : "BASSE");
                commande.put("tempsEstimeMinutes", row[9] != null ? row[9] : 60);
                commande.put("nombreCartes", row[10] != null ? row[10] : 0);
                commande.put("prixTotal", row[11] != null ? row[11] : 0.0);
                commande.put("status", row[12]);
                planification.put("commande", commande);

                // === CORRECTION DE LA DATE ===
                LocalDate datePlanif = ((java.sql.Date) row[3]).toLocalDate();

                // LE FRONTEND ATTEND "datePlanifiee" (pas "datePlanification")
                planification.put("datePlanifiee", datePlanif.toString()); // Format ISO: "2025-06-03"

                // === CORRECTION DE L'HEURE ===
                LocalTime heurePlanif = ((java.sql.Time) row[4]).toLocalTime();

                // LE FRONTEND ATTEND "heureDebut" en nombre (pas en string)
                planification.put("heureDebut", heurePlanif.getHour() * 100 + heurePlanif.getMinute()); // 800, 915, 1030

                planification.put("dureeMinutes", row[5]);
                planification.put("terminee", row[6]);

                // Autres champs
                planification.put("description", "Commande " + row[7]);
                planification.put("titre", "Commande " + row[7] + " - " + row[13] + " " + row[14]);

                String statutAffichage = "EN_COURS";
                if ((Boolean) row[6]) {
                    statutAffichage = "TERMINEE";
                }
                planification.put("statut", statutAffichage);

                String prioriteAffichage = "Basse";
                if ("HAUTE".equals(row[8])) {
                    prioriteAffichage = "Haute";
                } else if ("MOYENNE".equals(row[8])) {
                    prioriteAffichage = "Moyenne";
                }
                planification.put("prioriteAffichage", prioriteAffichage);

                planifications.add(planification);
            }

            System.out.println("✅ " + planifications.size() + " planifications formatées");

            // ========== ENRICHISSEMENT AVEC LE VRAI NOMBRE DE CARTES ==========
            System.out.println("🎴 Calcul du vrai nombre de cartes...");

            for (Map<String, Object> planification : planifications) {
                try {
                    String orderId = (String) ((Map<String, Object>) planification.get("commande")).get("id");
                    String numeroCommande = (String) ((Map<String, Object>) planification.get("commande")).get("numeroCommande");

                    if (orderId != null && !orderId.isEmpty()) {
                        // Requête pour compter les vraies cartes
                        Query countQuery = entityManager.createNativeQuery(
                                "SELECT COUNT(cco.card_certification_id) " +
                                        "FROM card_certification_order cco " +
                                        "WHERE cco.order_id = UNHEX(?)"
                        );
                        countQuery.setParameter(1, orderId);

                        Number result = (Number) countQuery.getSingleResult();
                        Integer vraieNombreCartes = result != null ? result.intValue() : 0;

                        Map<String, Object> commande = (Map<String, Object>) planification.get("commande");
                        commande.put("nombreCartes", vraieNombreCartes);
                        commande.put("nomsCartes", new ArrayList<>());

                        System.out.println("🎴 Commande " + numeroCommande + " : " + vraieNombreCartes + " cartes");
                    }
                } catch (Exception e) {
                    System.out.println("❌ Erreur calcul cartes pour " + planification.get("id") + " : " + e.getMessage());
                    Map<String, Object> commande = (Map<String, Object>) planification.get("commande");
                    commande.put("nombreCartesReelles", 0);
                    commande.put("nomsCartes", new ArrayList<>());
                }
            }

            System.out.println("✅ " + planifications.size() + " planifications enrichies avec cartes");


            return ResponseEntity.ok(planifications);

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération planifications: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    @DeleteMapping("/vider")
    @Transactional
    public ResponseEntity<Map<String, Object>> viderToutesPlanifications() {
        try {
            System.out.println("🗑️ Suppression de toutes les planifications...");

            int nbSupprimees = entityManager.createNativeQuery("DELETE FROM planification").executeUpdate();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("message", "Toutes les planifications ont été supprimées");
            result.put("planificationsSupprimees", nbSupprimees);

            System.out.println("✅ " + nbSupprimees + " planifications supprimées");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("❌ Erreur suppression planifications: " + e.getMessage());
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("error", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    /**
     * Convertit une chaîne hex vers ULID pour les requêtes JPA
     */
    private Ulid hexStringToUlid(String hexString) {
        String formatted = hexString.toLowerCase()
                .replaceAll("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5");
        UUID uuid = UUID.fromString(formatted);
        return Ulid.from(uuid);
    }

    // Dans PlanificationController
    private int getChargeEmployePourJour(String employeId, LocalDate date) {
        Query query = entityManager.createNativeQuery(
                "SELECT COALESCE(SUM(duree_minutes), 0) " +
                        "FROM planification " +
                        "WHERE employe_id = UNHEX(?) AND date_planification = ?"
        );
        query.setParameter(1, employeId);
        query.setParameter(2, date);

        Number result = (Number) query.getSingleResult();
        return result != null ? result.intValue() : 0;
    }
    private String trouverEmployeDisponible(List<Map<String, Object>> employes,
                                            int dureeRequiseMinutes, LocalDate datePlanif) {
        System.out.println("🔍 Recherche employé pour " + dureeRequiseMinutes + " min le " + datePlanif);

        for (Map<String, Object> employe : employes) {
            String employeId = ((String) employe.get("id")).replace("-", "");

            // ✅ CORRECTION - utiliser la bonne clé pour les heures
            Integer heuresTravail = (Integer) employe.get("heuresTravailParJour");
            int minutesParJour = (heuresTravail != null) ? heuresTravail * 60 : 480; // 8h par défaut

            int chargeActuelleMinutes = getChargeEmployePourJour(employeId, datePlanif);

            if (chargeActuelleMinutes + dureeRequiseMinutes <= minutesParJour) {
                System.out.println("✅ " + employe.get("prenom") + " " + employe.get("nom") +
                        " disponible : " + chargeActuelleMinutes + "/" + minutesParJour +
                        " minutes (" + heuresTravail + "h)");
                return employeId;
            } else {
                System.out.println("⚠️ " + employe.get("prenom") + " " + employe.get("nom") +
                        " surchargé : " + (chargeActuelleMinutes + dureeRequiseMinutes) +
                        "/" + minutesParJour + " minutes");
            }
        }

        return null; // Personne de disponible ce jour-là
    }


    // ============================================================================
// 🔍 DEBUG PLANIFICATIONS - Diagnostic complet
// ============================================================================

// ✅ AJOUTEZ dans PlanificationController.java (ou créez un nouveau contrôleur) :

    @GetMapping("/debug-planifications")
    public ResponseEntity<Map<String, Object>> debugPlanifications() {
        Map<String, Object> debug = new HashMap<>();

        try {
            System.out.println("🔍 === DEBUG PLANIFICATIONS ===");

            // 1. Vérifier si la table planification existe
            String sqlTables = "SHOW TABLES LIKE 'planification'";
            Query queryTables = entityManager.createNativeQuery(sqlTables);
            @SuppressWarnings("unchecked")
            List<String> tables = queryTables.getResultList();

            debug.put("table_planification_existe", !tables.isEmpty());
            System.out.println("Table 'planification' existe: " + !tables.isEmpty());

            if (tables.isEmpty()) {
                debug.put("erreur", "Table 'planification' n'existe pas");
                debug.put("solution", "Créer la table planification ou utiliser des données de test");
                return ResponseEntity.ok(debug);
            }

            // 2. Décrire la structure de la table
            String sqlDesc = "DESCRIBE planification";
            Query queryDesc = entityManager.createNativeQuery(sqlDesc);
            @SuppressWarnings("unchecked")
            List<Object[]> colonnes = queryDesc.getResultList();

            Map<String, String> structure = new HashMap<>();
            for (Object[] col : colonnes) {
                String nom = (String) col[0];
                String type = (String) col[1];
                structure.put(nom, type);
                System.out.println("  - " + nom + " (" + type + ")");
            }
            debug.put("structure_table", structure);

            // 3. Compter les planifications
            String sqlCount = "SELECT COUNT(*) FROM planification";
            Query queryCount = entityManager.createNativeQuery(sqlCount);
            Object result = queryCount.getSingleResult();
            Long totalPlanifs = ((Number) result).longValue();

            debug.put("total_planifications", totalPlanifs);
            System.out.println("Total planifications: " + totalPlanifs);

            // 4. Échantillon des planifications
            if (totalPlanifs > 0) {
                String sqlSample = "SELECT HEX(id), HEX(order_id), HEX(employe_id), date_planification, heure_debut, duree_minutes, terminee FROM planification LIMIT 5";
                Query querySample = entityManager.createNativeQuery(sqlSample);
                @SuppressWarnings("unchecked")
                List<Object[]> echantillon = querySample.getResultList();

                List<Map<String, Object>> planifs = new ArrayList<>();
                for (Object[] row : echantillon) {
                    Map<String, Object> planif = new HashMap<>();
                    planif.put("id", row[0]);
                    planif.put("order_id", row[1]);
                    planif.put("employe_id", row[2]);
                    planif.put("date_planification", row[3]);
                    planif.put("heure_debut", row[4]);
                    planif.put("duree_minutes", row[5]);
                    planif.put("terminee", row[6]);
                    planifs.add(planif);
                }
                debug.put("echantillon", planifs);
            }

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            System.err.println("❌ Erreur debug planifications: " + e.getMessage());
            e.printStackTrace();
            debug.put("erreur", e.getMessage());
            debug.put("stack_trace", e.getStackTrace());
            return ResponseEntity.status(500).body(debug);
        }
    }

// ✅ AJOUTEZ cette méthode pour créer des planifications de test :

    @PostMapping("/creer-planifications-test")
    public ResponseEntity<Map<String, Object>> creerPlanificationsTest() {
        Map<String, Object> result = new HashMap<>();

        try {
            System.out.println("🧪 Création de planifications de test...");

            // Vérifier si la table existe
            String sqlTables = "SHOW TABLES LIKE 'planification'";
            Query queryTables = entityManager.createNativeQuery(sqlTables);
            @SuppressWarnings("unchecked")
            List<String> tables = queryTables.getResultList();

            if (tables.isEmpty()) {
                result.put("success", false);
                result.put("message", "Table planification n'existe pas");
                return ResponseEntity.ok(result);
            }

            // Récupérer quelques commandes et employés pour les tests
            String sqlCommandes = "SELECT HEX(id), num_commande FROM `order` LIMIT 5";
            Query queryCommandes = entityManager.createNativeQuery(sqlCommandes);
            @SuppressWarnings("unchecked")
            List<Object[]> commandes = queryCommandes.getResultList();

            if (commandes.isEmpty()) {
                result.put("success", false);
                result.put("message", "Aucune commande trouvée pour créer des planifications test");
                return ResponseEntity.ok(result);
            }

            // Employés de test (IDs fixes)
            List<String> employeIds = Arrays.asList(
                    "0193D5E1B2347123845612345678ABC",
                    "0193D5E1C2347123845612345678DEF",
                    "0193D5E1D2347123845612345678GHI"
            );

            List<Map<String, Object>> planificationsCreees = new ArrayList<>();
            int compteur = 0;

            for (Object[] commande : commandes) {
                if (compteur >= 10) break; // Limiter à 10 planifications test

                String commandeId = (String) commande[0];
                String numeroCommande = (String) commande[1];
                String employeId = employeIds.get(compteur % employeIds.size());

                // Dates réparties sur la semaine
                LocalDate datePlanif = LocalDate.now().plusDays(compteur % 7);
                int heure = 9 + (compteur % 8); // Entre 9h et 16h

                try {
                    String sql = """
                    INSERT INTO planification 
                    (id, order_id, employe_id, date_planification, heure_debut, duree_minutes, terminee, date_creation) 
                    VALUES (UNHEX(?), UNHEX(?), UNHEX(?), ?, ?, ?, ?, NOW())
                    """;

                    String newId = UUID.randomUUID().toString().replace("-", "");

                    int rows = entityManager.createNativeQuery(sql)
                            .setParameter(1, newId)
                            .setParameter(2, commandeId)
                            .setParameter(3, employeId)
                            .setParameter(4, datePlanif)
                            .setParameter(5, heure)
                            .setParameter(6, 120) // 2h par défaut
                            .setParameter(7, false)
                            .executeUpdate();

                    if (rows > 0) {
                        Map<String, Object> planifInfo = new HashMap<>();
                        planifInfo.put("commande", numeroCommande);
                        planifInfo.put("date", datePlanif.toString());
                        planifInfo.put("heure", heure + ":00");
                        planificationsCreees.add(planifInfo);
                        compteur++;
                        System.out.println("✅ Planification test créée: " + numeroCommande + " le " + datePlanif + " à " + heure + "h");
                    }

                } catch (Exception e) {
                    System.err.println("❌ Erreur création planification test: " + e.getMessage());
                }
            }

            result.put("success", true);
            result.put("planifications_creees", compteur);
            result.put("details", planificationsCreees);
            result.put("message", compteur + " planifications de test créées");

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            System.err.println("❌ Erreur création planifications test: " + e.getMessage());
            e.printStackTrace();
            result.put("success", false);
            result.put("erreur", e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

// ✅ AJOUTEZ cette méthode pour récupérer les planifications avec jointures :

    @GetMapping("/planifications-avec-details")
    public ResponseEntity<List<Map<String, Object>>> getPlanificationsAvecDetails() {
        try {
            System.out.println("📋 Récupération planifications avec détails...");

            String sql = """
            SELECT 
                HEX(p.id) as id,
                HEX(p.order_id) as orderId,
                HEX(p.employe_id) as employeId,
                p.date_planification as datePlanifiee,
                p.heure_debut as heureDebut,
                p.duree_minutes as dureeMinutes,
                p.terminee as terminee,
                o.num_commande as numeroCommande,
                o.status as statusCommande,
                'Test Employé' as nomEmploye
            FROM planification p
            LEFT JOIN `order` o ON p.order_id = o.id
            WHERE p.date_planification >= CURDATE()
            ORDER BY p.date_planification, p.heure_debut
            LIMIT 50
            """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> resultats = query.getResultList();

            List<Map<String, Object>> planifications = new ArrayList<>();

            for (Object[] row : resultats) {
                Map<String, Object> planif = new HashMap<>();
                planif.put("id", row[0]);
                planif.put("orderId", row[1]);
                planif.put("employeId", row[2]);
                planif.put("datePlanifiee", row[3] != null ? row[3].toString() : null);
                planif.put("heureDebut", row[4]);
                planif.put("dureeMinutes", row[5]);
                planif.put("terminee", row[6]);

                // Détails commande
                Map<String, Object> commande = new HashMap<>();
                commande.put("numeroCommande", row[7]);
                commande.put("status", row[8]);
                planif.put("commande", commande);

                // Détails employé (temporaire)
                Map<String, Object> employe = new HashMap<>();
                employe.put("nom", "Test");
                employe.put("prenom", "Employé");
                planif.put("employe", employe);

                planifications.add(planif);
            }

            System.out.println("✅ " + planifications.size() + " planifications trouvées");
            return ResponseEntity.ok(planifications);

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération planifications: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

// ============================================================================
// 🚀 TESTS À EFFECTUER :
// ============================================================================

/*
1. ✅ Ajoutez ces méthodes dans PlanificationController.java
2. ✅ Redémarrez le backend
3. ✅ Testez le diagnostic :

   curl "http://localhost:8080/api/planifications/debug-planifications"

4. ✅ Si la table existe mais est vide, créez des données test :

   curl -X POST "http://localhost:8080/api/planifications/creer-planifications-test"

5. ✅ Puis testez la récupération :

   curl "http://localhost:8080/api/planifications/planifications-avec-details"

Cela nous dira exactement quel est le problème avec les planifications !
*/


}
