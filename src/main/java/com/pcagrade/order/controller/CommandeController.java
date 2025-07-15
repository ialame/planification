package com.pcagrade.order.controller;

import com.pcagrade.order.util.UlidUtils;
import jakarta.persistence.NoResultException;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.github.f4b6a3.ulid.Ulid;
import com.pcagrade.order.entity.Card;
import com.pcagrade.order.entity.CardCertification;
import com.pcagrade.order.entity.CardTranslation;
import com.pcagrade.order.entity.Commande;
import com.pcagrade.order.repository.CommandeRepository;
import com.pcagrade.order.service.CommandeService;
import jakarta.persistence.EntityManager;

import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;


@RestController
@RequestMapping("/api/commandes")
public class CommandeController {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private CommandeService commandeService;
    @Autowired
    private CommandeRepository commandeRepository;

    // Vos méthodes existantes...
    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> getCommandes() {
        try {
            List<Map<String, Object>> commandes = commandeService.getCommandesAPlanifierDepuisDate(1, 5, 2025);
            return ResponseEntity.ok(commandes);
        } catch (Exception e) {
            System.err.println("❌ Erreur récupération commandes: " + e.getMessage());
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    @GetMapping("/depuis")
    public ResponseEntity<List<Map<String, Object>>> getCommandesDepuisDate(
            @RequestParam int jour,
            @RequestParam int mois,
            @RequestParam int annee) {
        try {
            List<Map<String, Object>> commandes = commandeService.getCommandesAPlanifierDepuisDate(jour, mois, annee);
            return ResponseEntity.ok(commandes);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    @GetMapping("/periode")
    public ResponseEntity<List<Map<String, Object>>> getCommandesPeriode(
            @RequestParam int jourDebut,
            @RequestParam int moisDebut,
            @RequestParam int anneeDebut,
            @RequestParam int jourFin,
            @RequestParam int moisFin,
            @RequestParam int anneeFin) {
        try {
            List<Map<String, Object>> commandes = commandeService.getCommandesPeriode(
                    jourDebut, moisDebut, anneeDebut,
                    jourFin, moisFin, anneeFin
            );
            return ResponseEntity.ok(commandes);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    @GetMapping("/stats/depuis")
    public ResponseEntity<Map<String, Object>> getStatsDepuisDate(
            @RequestParam int jour,
            @RequestParam int mois,
            @RequestParam int annee) {
        try {
            LocalDateTime dateDebut = LocalDateTime.of(annee, mois, jour, 0, 0, 0);

            Query query = entityManager.createNativeQuery(
                    "SELECT " +
                            "  COUNT(*) as total, " +
                            "  SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) as en_attente, " +
                            "  SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) as en_cours, " +
                            "  SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END) as terminees, " +
                            "  SUM(CASE WHEN delai = 'X' THEN 1 ELSE 0 END) as urgentes, " +
                            "  SUM(CASE WHEN employe_id IS NULL THEN 1 ELSE 0 END) as non_assignees " +
                            "FROM commandes_db.`order` " +
                            "WHERE date >= ?"
            );

            query.setParameter(1, dateDebut);
            Object[] result = (Object[]) query.getSingleResult();

            Map<String, Object> stats = new HashMap<>();
            stats.put("total", result[0]);
            stats.put("enAttente", result[1]);
            stats.put("enCours", result[2]);
            stats.put("terminees", result[3]);
            stats.put("urgentes", result[4]);
            stats.put("nonAssignees", result[5]);
            stats.put("dateDebut", jour + "/" + mois + "/" + annee);

            return ResponseEntity.ok(stats);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // NOUVELLE MÉTHODE CORRIGÉE
    // ✅ Ajoutez cette méthode corrigée pour les cartes
    @GetMapping("/{commandeId}/cartes")
    public ResponseEntity<Map<String, Object>> getCartesCommande(@PathVariable String commandeId) {
        try {
            System.out.println("🔍 Récupération cartes pour commande: " + commandeId);

            // ✅ Requête native pour éviter les problèmes de désérialisation
            String sql = """
                SELECT 
                    o.num_commande,
                    COUNT(cco.card_certification_id) as nombre_cartes_reel,
                    GROUP_CONCAT(DISTINCT ct.name SEPARATOR ', ') as noms_cartes
                FROM `order` o
                LEFT JOIN card_certification_order cco ON o.id = cco.order_id
                LEFT JOIN card_certification cc ON cco.card_certification_id = cc.id
                LEFT JOIN card c ON cc.card_id = c.id
                LEFT JOIN card_translation ct ON c.id = ct.translatable_id AND ct.locale = 'en'
                WHERE HEX(o.id) = ?
                GROUP BY o.id, o.num_commande
                """;

            Query nativeQuery = entityManager.createNativeQuery(sql);
            nativeQuery.setParameter(1, commandeId);

            Object[] result = (Object[]) nativeQuery.getSingleResult();

            // Construire la réponse
            Map<String, Object> response = new HashMap<>();
            response.put("commandeId", commandeId);
            response.put("numeroCommande", result[0]);
            response.put("nombreCartes", result[1] != null ? ((Number) result[1]).intValue() : 0);

            // Parser les noms des cartes
            String nomsCartesStr = (String) result[2];
            List<String> nomsCartes = new ArrayList<>();
            if (nomsCartesStr != null && !nomsCartesStr.trim().isEmpty()) {
                nomsCartes = Arrays.asList(nomsCartesStr.split(", "));
            }
            response.put("nomsCartes", nomsCartes);

            // Créer un résumé (comptage par nom de carte)
            Map<String, Long> resumeCartes = new HashMap<>();
            for (String nom : nomsCartes) {
                resumeCartes.put(nom, resumeCartes.getOrDefault(nom, 0L) + 1);
            }
            response.put("resumeCartes", resumeCartes);

            System.out.println("✅ Cartes trouvées: " + response.get("nombreCartes"));
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération cartes: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Erreur lors de la récupération des cartes");
            errorResponse.put("commandeId", commandeId);
            errorResponse.put("details", e.getMessage());

            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // ✅ SOLUTION 2 : Remplacez la méthode debugCommande

    @GetMapping("/debug/{commandeId}")
    public ResponseEntity<Map<String, Object>> debugCommande(@PathVariable String commandeId) {
        Map<String, Object> debug = new HashMap<>();

        try {
            System.out.println("🔍 DEBUG pour commande: " + commandeId);

            debug.put("commandeIdOriginal", commandeId);
            debug.put("longueur", commandeId.length());
            debug.put("format", detecterFormat(commandeId));

            try {
                UUID uuid = convertStringToUuid(commandeId);
                debug.put("conversionReussie", true);
                debug.put("uuidResultat", uuid.toString());

                boolean exists = commandeRepository.existsById(uuid);
                debug.put("existeDansBD", exists);

                if (exists) {
                    Commande commande = commandeRepository.findById(uuid).get();
                    debug.put("numeroCommande", commande.getNumeroCommande());
                    debug.put("status", commande.getStatus());
                }

            } catch (Exception e) {
                debug.put("conversionReussie", false);
                debug.put("erreurConversion", e.getMessage());
            }

            try {
                int count = compterCartesNative(commandeId);
                debug.put("comptageNatifReussi", true);
                debug.put("nombreCartesNative", count);
            } catch (Exception e) {
                debug.put("comptageNatifReussi", false);
                debug.put("erreurComptage", e.getMessage());
            }

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            debug.put("erreurGenerale", e.getMessage());
            return ResponseEntity.status(500).body(debug);
        }
    }

// MÉTHODES UTILITAIRES

    // ✅ SOLUTION 3 : Méthode convertStringToUuid corrigée

    private UUID convertStringToUuid(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID vide ou null");
        }

        id = id.trim();
        System.out.println("🔄 Conversion ID: '" + id + "' (longueur: " + id.length() + ")");

        try {
            // Format ULID standard (26 caractères)
            if (id.length() == 26 && id.matches("[0-9A-Z]+")) {
                Ulid ulid = Ulid.from(id);
                return ulid.toUuid();
            }

            // Format UUID avec tirets (36 caractères)
            if (id.length() == 36 && id.contains("-")) {
                return UUID.fromString(id);
            }

            // Format Hex sans tirets (32 caractères)
            if (id.length() == 32 && id.matches("[0-9A-Fa-f]+")) {
                String formatted = id.toLowerCase()
                        .replaceAll("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5");
                return UUID.fromString(formatted);
            }

            // Tentative de conversion directe
            return UUID.fromString(id);

        } catch (Exception e) {
            throw new IllegalArgumentException("Format ID non reconnu: " + id + " - " + e.getMessage(), e);
        }
    }
    private String detecterFormat(String id) {
        if (id == null) return "null";
        if (id.length() == 26) return "ULID-26";
        if (id.length() == 36) return "UUID-36";
        if (id.length() == 32) return "HEX-32";
        return "UNKNOWN-" + id.length();
    }

    private int compterCartesNative(String commandeId) {
        try {
            String sql = "SELECT COUNT(cco.card_certification_id) " +
                    "FROM card_certification_order cco " +
                    "WHERE cco.order_id = UNHEX(?)";

            Query query = entityManager.createNativeQuery(sql);
            String cleanId = commandeId.replace("-", "");
            query.setParameter(1, cleanId);

            Number result = (Number) query.getSingleResult();
            return result != null ? result.intValue() : 0;

        } catch (Exception e) {
            System.err.println("❌ Erreur comptage natif: " + e.getMessage());
            return 0;
        }
    }

    private List<Map<String, Object>> getDetailsCartes(String commandeId) {
        try {
            String sql = """
            
                    SELECT 
                HEX(cc.id) as certification_id,
                HEX(c.id) as card_id,
                ct.name as card_name,
                ct.label_name as label_name,
                c.num as card_number,
                cc.langue,
                cc.edition
            FROM card_certification_order cco
            INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
            INNER JOIN card c ON cc.card_id = c.id
            LEFT JOIN card_translation ct ON c.id = ct.translatable_id 
                AND ct.locale = 'en'
            WHERE cco.order_id = UNHEX(?)
            ORDER BY ct.name, c.num
            """;

            Query query = entityManager.createNativeQuery(sql);
            String cleanId = commandeId.replace("-", "");
            query.setParameter(1, cleanId);

            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();

            List<Map<String, Object>> cartes = new ArrayList<>();

            for (Object[] row : results) {
                Map<String, Object> carte = new HashMap<>();
                carte.put("certificationId", row[0]);
                carte.put("cardId", row[1]);
                carte.put("name", row[2] != null ? row[2] : "Carte #" + row[4]);
                carte.put("labelName", row[3] != null ? row[3] : row[2]);
                carte.put("number", row[4]);
                carte.put("langue", row[5] != null ? row[5] : "FR");
                carte.put("edition", row[6] != null ? row[6] : 1);
                cartes.add(carte);
            }

            return cartes;

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération détails: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<String> extraireNomsCartes(List<Map<String, Object>> cartes) {
        return cartes.stream()
                .map(carte -> (String) carte.get("name"))
                .distinct()
                .sorted()
                .toList();
    }

    private Map<String, Integer> creerResumeCartes(List<Map<String, Object>> cartes) {
        Map<String, Integer> resume = new HashMap<>();

        for (Map<String, Object> carte : cartes) {
            String nom = (String) carte.get("name");
            resume.merge(nom, 1, Integer::sum);
        }

        return resume;
    }


    @GetMapping("/debug/ulid/{commandeId}")
    public ResponseEntity<Map<String, Object>> debugUlidConversion(@PathVariable String commandeId) {
        Map<String, Object> debug = new HashMap<>();

        try {
            debug.put("input", commandeId);
            debug.put("inputLength", commandeId.length());

            // Conversion vers UUID (compatible avec votre base)
            UUID commandeUuid;
            if (commandeId.length() == 32) {
                // Format hex
                String formatted = commandeId.toLowerCase()
                        .replaceAll("(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5");
                debug.put("formatted", formatted);
                commandeUuid = UUID.fromString(formatted);
            } else if (commandeId.length() == 26) {
                // Format ULID, convertir en UUID
                Ulid ulid = Ulid.from(commandeId);
                commandeUuid = ulid.toUuid();
            } else {
                // Format UUID standard
                commandeUuid = UUID.fromString(commandeId);
            }

            debug.put("uuid", commandeUuid.toString());
            debug.put("uuidHex", commandeUuid.toString().replace("-", ""));

            // Test existence en base avec UUID
            boolean exists = commandeRepository.existsById(commandeUuid);
            debug.put("existsInDB", exists);

            if (exists) {
                Optional<Commande> cmd = commandeRepository.findById(commandeUuid);
                if (cmd.isPresent()) {
                    debug.put("numeroCommande", cmd.get().getNumeroCommande());
                    debug.put("status", cmd.get().getStatus());
                }
            }

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            debug.put("error", e.getMessage());
            debug.put("stackTrace", e.getStackTrace()[0].toString());
            return ResponseEntity.status(500).body(debug);
        }
    }

    @GetMapping("/juin-2025")
    public ResponseEntity<List<Map<String, Object>>> getCommandesJuin2025() {
        try {
            System.out.println("📋 API: Récupération commandes juin 2025...");

            // Récupérer toutes les commandes de juin 2025
            List<Map<String, Object>> commandes = commandeService.getCommandesPeriode(
                    1, 6, 2025,    // Du 1er juin 2025
                    30, 6, 2025    // Au 30 juin 2025
            );

            System.out.println("✅ " + commandes.size() + " commandes trouvées pour juin 2025");
            return ResponseEntity.ok(commandes);

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération commandes juin 2025: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    // ============================================================================
// 🔍 DEBUG : Pourquoi les commandes sont vides ?
// ============================================================================

// ✅ AJOUTEZ ces méthodes de debug dans CommandeController.java :

    @GetMapping("/debug-structure")
    public ResponseEntity<Map<String, Object>> debugStructure() {
        Map<String, Object> debug = new HashMap<>();

        try {
            System.out.println("🔍 === DEBUG STRUCTURE TABLE ORDER ===");

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
            for (Object[] col : colonnes) {
                String nomColonne = (String) col[0];
                String typeColonne = (String) col[1];
                structureTable.put(nomColonne, typeColonne);
                System.out.println("  - " + nomColonne + " (" + typeColonne + ")");
            }
            debug.put("structure_table", structureTable);

            // 3. Compter le nombre total de commandes
            String sqlCount = "SELECT COUNT(*) FROM `order`";
            Query queryCount = entityManager.createNativeQuery(sqlCount);
            Object totalResult = queryCount.getSingleResult();
            Long totalCommandes = ((Number) totalResult).longValue();

            debug.put("total_commandes", totalCommandes);
            System.out.println("Total commandes dans la table: " + totalCommandes);

            // 4. Voir les dates disponibles
            String sqlDates = "SELECT DATE(date) as date_commande, COUNT(*) as nb FROM `order` GROUP BY DATE(date) ORDER BY date_commande DESC LIMIT 10";
            Query queryDates = entityManager.createNativeQuery(sqlDates);
            @SuppressWarnings("unchecked")
            List<Object[]> dates = queryDates.getResultList();

            Map<String, Long> datesCommandes = new HashMap<>();
            System.out.println("Dates avec des commandes:");
            for (Object[] dateRow : dates) {
                String date = String.valueOf(dateRow[0]);
                Long nb = ((Number) dateRow[1]).longValue();
                datesCommandes.put(date, nb);
                System.out.println("  - " + date + ": " + nb + " commandes");
            }
            debug.put("dates_commandes", datesCommandes);

            // 5. Vérifier spécifiquement juin 2025
            String sqlJuin2025 = "SELECT COUNT(*) FROM `order` WHERE date >= '2025-06-01' AND date < '2025-07-01'";
            Query queryJuin = entityManager.createNativeQuery(sqlJuin2025);
            Object juinResult = queryJuin.getSingleResult();
            Long commandesJuin = ((Number) juinResult).longValue();

            debug.put("commandes_juin_2025", commandesJuin);
            System.out.println("Commandes en juin 2025: " + commandesJuin);

            // 6. Vérifier les premières commandes (peu importe la date)
            String sqlTop5 = "SELECT HEX(id), num_commande, date, status FROM `order` ORDER BY date DESC LIMIT 5";
            Query queryTop5 = entityManager.createNativeQuery(sqlTop5);
            @SuppressWarnings("unchecked")
            List<Object[]> topCommandes = queryTop5.getResultList();

            List<Map<String, Object>> echantillon = new ArrayList<>();
            System.out.println("Échantillon des dernières commandes:");
            for (Object[] cmd : topCommandes) {
                Map<String, Object> commande = new HashMap<>();
                commande.put("id", cmd[0]);
                commande.put("numeroCommande", cmd[1]);
                commande.put("date", cmd[2]);
                commande.put("status", cmd[3]);
                echantillon.add(commande);
                System.out.println("  - " + cmd[1] + " | " + cmd[2] + " | Status: " + cmd[3]);
            }
            debug.put("echantillon_commandes", echantillon);

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            System.err.println("❌ Erreur debug structure: " + e.getMessage());
            e.printStackTrace();
            debug.put("erreur", e.getMessage());
            return ResponseEntity.status(500).body(debug);
        }
    }

    @GetMapping("/debug-toutes")
    public ResponseEntity<List<Map<String, Object>>> debugToutesCommandes() {
        try {
            System.out.println("🔍 === DEBUG TOUTES LES COMMANDES ===");

            // Récupérer TOUTES les commandes sans filtrage de date
            String sql = """
            SELECT 
                HEX(o.id) as id,
                o.num_commande as numeroCommande,
                o.date as dateReception,
                o.temps_estime_minutes as tempsEstimeMinutes,
                o.nombre_cartes as nombreCartes,
                o.priorite_string as priorite,
                o.prix_total as prixTotal,
                o.status as status,
                o.date_creation as dateCreation
            FROM `order` o
            ORDER BY o.date DESC
            LIMIT 20
            """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> resultats = query.getResultList();
            List<Map<String, Object>> commandes = new ArrayList<>();

            System.out.println("Commandes trouvées: " + resultats.size());

            for (Object[] row : resultats) {
                Map<String, Object> commande = new HashMap<>();
                commande.put("id", (String) row[0]);
                commande.put("numeroCommande", (String) row[1]);
                commande.put("dateReception", row[2]);
                commande.put("tempsEstimeMinutes", row[3] != null ? row[3] : 120);
                commande.put("nombreCartes", row[4] != null ? row[4] : 1);
                commande.put("priorite", (String) row[5]);
                commande.put("prixTotal", row[6] != null ? row[6] : 0.0);
                commande.put("status", row[7]);
                commande.put("dateCreation", row[8]);

                // Mapping du statut pour le frontend
                commande.put("statut", mapStatusToString((Integer) row[7]));

                commandes.add(commande);

                System.out.println("  - " + row[1] + " | " + row[2] + " | Status: " + row[7]);
            }

            return ResponseEntity.ok(commandes);

        } catch (Exception e) {
            System.err.println("❌ Erreur debug toutes commandes: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    @GetMapping("/dernier-mois")
    public ResponseEntity<List<Map<String, Object>>> getCommandesDernierMois() {
        try {
            System.out.println("📋 API: Récupération commandes juin 2025 (champ 'date' uniquement)...");

            // ✅ REQUÊTE SIMPLIFIÉE avec seulement le champ 'date' qui est renseigné
            String sql = """
            SELECT 
                HEX(o.id) as id,
                o.num_commande as numeroCommande,
                o.date as dateCommande,
                o.status as status,
                o.delai as delai,
                o.reference as reference,
                o.type as type,
                o.note_minimale as noteMinimale,
                o.nb_descellements as nbDescellements,
                COUNT(cco.card_certification_id) as nombreCartes
            FROM `order` o
            LEFT JOIN card_certification_order cco ON o.id = cco.order_id
            WHERE o.date >= '2025-06-01' 
            AND o.date < '2025-07-01'
            AND o.status IN (1, 11)
            GROUP BY o.id, o.num_commande, o.date, o.status, o.delai, o.reference, o.type, o.note_minimale, o.nb_descellements
            ORDER BY o.date DESC
            LIMIT 50
            """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> resultats = query.getResultList();
            List<Map<String, Object>> commandes = new ArrayList<>();

            System.out.println("Résultats SQL: " + resultats.size());

            for (Object[] row : resultats) {
                Map<String, Object> commande = new HashMap<>();

                // IDs et références
                commande.put("id", (String) row[0]);
                commande.put("numeroCommande", (String) row[1]);

                // Dates - utiliser uniquement le champ 'date' pour tout
                Object dateCommande = row[2];
                commande.put("dateCreation", dateCommande);
                commande.put("dateLimite", dateCommande); // Même date car les autres sont null
                commande.put("dateReception", dateCommande);

                // Calculs basés sur vos vraies données
                Integer nombreCartes = row[9] != null ? ((Number) row[9]).intValue() : 1;
                commande.put("nombreCartes", Math.max(nombreCartes, 1)); // Au moins 1 carte
                commande.put("tempsEstimeMinutes", nombreCartes * 3); // 3 min par carte

                // Priorité calculée selon le type
                Integer type = row[6] != null ? ((Number) row[6]).intValue() : 1;
                String priorite = "NORMALE";
                if (type >= 10) priorite = "HAUTE";
                else if (type >= 5) priorite = "MOYENNE";
                else if (type <= 2) priorite = "BASSE";
                commande.put("priorite", priorite);

                // Prix calculé selon note minimale et nombre de cartes
                Double noteMin = row[7] != null ? ((Number) row[7]).doubleValue() : 8.0;
                Double prixParCarte = 10.0; // Prix de base
                if (noteMin >= 9.5) prixParCarte = 20.0;
                else if (noteMin >= 9.0) prixParCarte = 15.0;
                Double prixTotal = nombreCartes * prixParCarte;
                commande.put("prixTotal", prixTotal);

                // Données brutes de votre DB
                commande.put("status", row[3]);
                commande.put("delai", row[4]);
                commande.put("reference", row[5]);
                commande.put("type", type);
                commande.put("noteMinimale", noteMin);
                commande.put("nbDescellements", row[8]);

                // Mapping du statut pour le frontend
                commande.put("statut", mapStatusToString((Integer) row[3]));

                commandes.add(commande);

                // Log pour debug
                System.out.println("  - " + row[1] + " | " + dateCommande + " | " + nombreCartes + " cartes | Status: " + row[3]);
            }

            System.out.println("✅ " + commandes.size() + " commandes trouvées pour juin 2025");
            return ResponseEntity.ok(commandes);

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération commandes: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

// ✅ VERSION SIMPLIFIÉE sans jointure (plus rapide pour tester) :

    @GetMapping("/juin-2025-simple")
    public ResponseEntity<List<Map<String, Object>>> getCommandesJuin2025Simple() {
        try {
            System.out.println("📋 API: Récupération commandes juin 2025 SIMPLE...");

            // Requête simple sans jointure pour test rapide
            String sql = """
            SELECT 
                HEX(o.id) as id,
                o.num_commande as numeroCommande,
                o.date as dateCommande,
                o.status as status,
                o.delai as delai,
                o.reference as reference,
                o.type as type,
                o.note_minimale as noteMinimale,
                o.nb_descellements as nbDescellements
            FROM `order` o
            WHERE o.date >= '2025-06-01' 
            AND o.date < '2025-07-01'
            AND o.status IN (1, 11)
            ORDER BY o.date DESC
            LIMIT 100
            """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> resultats = query.getResultList();
            List<Map<String, Object>> commandes = new ArrayList<>();

            System.out.println("Résultats SQL simple: " + resultats.size());

            for (Object[] row : resultats) {
                Map<String, Object> commande = new HashMap<>();

                // Données de base
                commande.put("id", (String) row[0]);
                commande.put("numeroCommande", (String) row[1]);

                // Toutes les dates = date de commande
                Object dateCommande = row[2];
                commande.put("dateCreation", dateCommande);
                commande.put("dateLimite", dateCommande);
                commande.put("dateReception", dateCommande);

                // Valeurs par défaut pour l'interface
                commande.put("nombreCartes", 1); // Défaut, on calculera plus tard
                commande.put("tempsEstimeMinutes", 120); // 2h par défaut
                commande.put("priorite", "NORMALE");
                commande.put("prixTotal", 50.0); // Prix par défaut

                // Données brutes
                commande.put("status", row[3]);
                commande.put("delai", row[4]);
                commande.put("reference", row[5]);
                commande.put("type", row[6]);
                commande.put("noteMinimale", row[7]);
                commande.put("nbDescellements", row[8]);

                // Statut mappé
                commande.put("statut", mapStatusToString((Integer) row[3]));

                commandes.add(commande);
            }

            System.out.println("✅ " + commandes.size() + " commandes simples trouvées");
            return ResponseEntity.ok(commandes);

        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

// ✅ Mapping des statuts selon vos données :

    private String mapStatusToString(Integer status) {
        if (status == null) return "EN_ATTENTE";

        switch (status) {
            case 1: return "EN_ATTENTE";      // Statut principal dans vos données
            case 2: return "EN_COURS";
            case 3: return "TERMINEE";
            case 11: return "EN_COURS";       // Autre statut dans vos données
            default: return "EN_ATTENTE";
        }
    }


}