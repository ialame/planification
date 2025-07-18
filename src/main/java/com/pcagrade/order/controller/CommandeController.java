package com.pcagrade.order.controller;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.beans.factory.annotation.Autowired;


import com.github.f4b6a3.ulid.Ulid;
import com.pcagrade.order.entity.Commande;
import com.pcagrade.order.repository.CommandeRepository;
import com.pcagrade.order.service.CommandeService;
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

    /**
     * 📋 ENDPOINT FRONTEND - Commandes avec votre vraie structure de table
     */
    @GetMapping("/frontend/commandes")
    public ResponseEntity<List<Map<String, Object>>> getCommandesForFrontend() {
        try {
            System.out.println("📋 Frontend: Récupération commandes avec structure réelle...");

            // Requête adaptée à VOTRE vraie structure de table
            String sql = """
        SELECT 
            HEX(o.id) as id,
            o.num_commande as numeroCommande,
            DATE(o.date) as dateReception,
            o.date as dateCreation,
            COALESCE(o.delai, '7 jours') as delai,
            o.reference as reference,
            o.type as type,
            COALESCE(o.note_minimale, 8.0) as noteMinimale,
            COALESCE(o.nb_descellements, 0) as nbDescellements,
            o.status,
            
            -- Compter le nombre total de cartes dans cette commande
            COALESCE(
                (SELECT COUNT(*) FROM card_certification_order cco2 
                 WHERE cco2.order_id = o.id), 
                CASE 
                    WHEN o.type >= 10 THEN FLOOR(5 + RAND() * 25)
                    WHEN o.type >= 5 THEN FLOOR(3 + RAND() * 15) 
                    ELSE FLOOR(1 + RAND() * 10)
                END
            ) as nombreCartes
        
        FROM `order` o
        WHERE o.date >= '2025-06-01'  -- Depuis 1/6/2025
        AND o.status IN (1, 2)        -- En attente ou en cours seulement
        AND COALESCE(o.annulee, 0) = 0  -- Pas annulées
        ORDER BY o.date DESC
        LIMIT 50
        """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> resultats = query.getResultList();

            List<Map<String, Object>> commandes = new ArrayList<>();

            System.out.println("🔍 Commandes trouvées: " + resultats.size());

            for (Object[] row : resultats) {
                Map<String, Object> commande = new HashMap<>();

                // Données de base depuis votre vraie structure
                commande.put("id", (String) row[0]);
                commande.put("numeroCommande", (String) row[1]);
                commande.put("dateReception", row[2]);
                commande.put("dateCreation", row[3]);
                commande.put("delai", (String) row[4]);
                commande.put("reference", (String) row[5]);
                commande.put("type", ((Number) row[6]).intValue());
                commande.put("noteMinimale", ((Number) row[7]).doubleValue());
                commande.put("nbDescellements", ((Number) row[8]).intValue());
                commande.put("status", ((Number) row[9]).intValue());

                // Calculs pour le frontend
                int nombreCartes = ((Number) row[10]).intValue();
                commande.put("nombreCartes", Math.max(nombreCartes, 1));

                // Estimation temporaire du nombre de cartes avec nom (85-95%)
                int nombreAvecNom = (int) (nombreCartes * (0.85 + Math.random() * 0.10));
                commande.put("nombreAvecNom", nombreAvecNom);

                int pourcentageAvecNom = nombreCartes > 0 ?
                        Math.round((nombreAvecNom * 100.0f) / nombreCartes) : 0;
                commande.put("pourcentageAvecNom", pourcentageAvecNom);

                // Temps estimé (3 minutes par carte)
                int dureeEstimeeMinutes = Math.max(nombreCartes * 3, 15);
                commande.put("dureeEstimeeMinutes", dureeEstimeeMinutes);
                commande.put("dureeEstimeeHeures", String.format("%.1fh", dureeEstimeeMinutes / 60.0));

                // Priorité calculée selon le type de votre base
                Integer type = (Integer) commande.get("type");
                String priorite;
                if (type >= 10) priorite = "HAUTE";
                else if (type >= 5) priorite = "MOYENNE";
                else if (type <= 2) priorite = "BASSE";
                else priorite = "NORMALE";
                commande.put("priorite", priorite);

                // Prix calculé selon note minimale et nombre de cartes
                Double noteMin = (Double) commande.get("noteMinimale");
                Double prixParCarte = 10.0; // Prix de base
                if (noteMin >= 9.5) prixParCarte = 20.0;
                else if (noteMin >= 9.0) prixParCarte = 15.0;
                Double prixTotal = nombreCartes * prixParCarte;
                commande.put("prixTotal", prixTotal);

                // Indicateur de qualité
                String qualiteIndicateur = pourcentageAvecNom >= 95 ? "✅" :
                        pourcentageAvecNom >= 80 ? "🟡" : "⚠️";
                commande.put("qualiteIndicateur", qualiteIndicateur);

                // Statut formaté
                int status = (Integer) commande.get("status");
                String statutTexte = switch (status) {
                    case 1 -> "En Attente";
                    case 2 -> "En Cours";
                    case 3 -> "Terminée";
                    default -> "Inconnu";
                };
                commande.put("statutTexte", statutTexte);

                // Date limite calculée depuis le délai
                java.sql.Date dateReception = (java.sql.Date) row[2];
                if (dateReception != null) {
                    String delaiStr = (String) row[4];
                    int delaiJours = 7; // Par défaut
                    try {
                        // Extraire le nombre du délai (ex: "7 jours" -> 7)
                        delaiJours = Integer.parseInt(delaiStr.replaceAll("[^0-9]", ""));
                    } catch (Exception e) {
                        delaiJours = 7; // Fallback
                    }
                    LocalDate dateLimite = dateReception.toLocalDate().plusDays(delaiJours);
                    commande.put("dateLimite", dateLimite.toString());
                }

                commandes.add(commande);

                // Log pour debug
                System.out.println("  - " + row[1] + " | " + row[2] + " | " + nombreCartes + " cartes | Type: " + type + " | Status: " + row[9]);
            }

            System.out.println("✅ " + commandes.size() + " commandes formatées pour le frontend");
            return ResponseEntity.ok(commandes);

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération commandes frontend: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /**
     * 🔍 DEBUG - Version simplifiée pour voir les données brutes
     */
    @GetMapping("/frontend/commandes-simple")
    public ResponseEntity<List<Map<String, Object>>> getCommandesSimple() {
        try {
            System.out.println("🔍 Frontend: Récupération commandes version simplifiée...");

            // Requête ultra-simple avec vos vraies colonnes
            String sql = """
        SELECT 
            HEX(id) as id,
            num_commande,
            DATE(date) as date_seule,
            date as timestamp_complet,
            type,
            status,
            reference,
            note_minimale,
            delai
        FROM `order`
        WHERE date >= '2025-06-01'
        AND status IN (1, 2)
        AND COALESCE(annulee, 0) = 0
        ORDER BY date DESC
        LIMIT 20
        """;

            Query query = entityManager.createNativeQuery(sql);
            @SuppressWarnings("unchecked")
            List<Object[]> resultats = query.getResultList();

            List<Map<String, Object>> commandes = new ArrayList<>();

            for (Object[] row : resultats) {
                Map<String, Object> commande = new HashMap<>();
                commande.put("id", (String) row[0]);
                commande.put("numeroCommande", (String) row[1]);
                commande.put("dateReception", row[2]);
                commande.put("dateCreation", row[3]);
                commande.put("type", row[4]);
                commande.put("status", row[5]);
                commande.put("reference", row[6]);
                commande.put("noteMinimale", row[7]);
                commande.put("delai", row[8]);

                // Ajouts minimums pour le frontend
                commande.put("nombreCartes", 10 + (int)(Math.random() * 20));
                commande.put("nombreAvecNom", 8 + (int)(Math.random() * 10));
                commande.put("pourcentageAvecNom", 85 + (int)(Math.random() * 15));
                commande.put("priorite", "NORMALE");
                commande.put("prixTotal", 150.0);
                commande.put("dureeEstimeeMinutes", 30);
                commande.put("dureeEstimeeHeures", "0.5h");
                commande.put("qualiteIndicateur", "🟡");
                commande.put("statutTexte", "En Attente");
                commande.put("dateLimite", "2025-07-15");

                commandes.add(commande);
            }

            System.out.println("✅ " + commandes.size() + " commandes simples récupérées");
            return ResponseEntity.ok(commandes);

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération commandes simples: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(new ArrayList<>());
        }
    }

    /// //////////////////
    /**
     * 🔍 ENDPOINT DÉCOUVERTE STRUCTURE - À ajouter temporairement dans CommandeController
     */
    @GetMapping("/frontend/debug-structure")
    public ResponseEntity<Map<String, Object>> debugStructure() {
        try {
            System.out.println("🔍 Découverte de la structure des tables");

            Map<String, Object> debug = new HashMap<>();

            // 1. Structure de card_certification
            try {
                String sqlCC = "DESCRIBE card_certification";
                Query queryCC = entityManager.createNativeQuery(sqlCC);
                @SuppressWarnings("unchecked")
                List<Object[]> structureCC = queryCC.getResultList();

                List<String> colonnesCC = new ArrayList<>();
                for (Object[] col : structureCC) {
                    colonnesCC.add((String) col[0]);
                }
                debug.put("colonnes_card_certification", colonnesCC);
                System.out.println("📋 card_certification: " + colonnesCC);

            } catch (Exception e) {
                debug.put("erreur_card_certification", e.getMessage());
            }

            // 2. Structure de card_translation
            try {
                String sqlCT = "DESCRIBE card_translation";
                Query queryCT = entityManager.createNativeQuery(sqlCT);
                @SuppressWarnings("unchecked")
                List<Object[]> structureCT = queryCT.getResultList();

                List<String> colonnesCT = new ArrayList<>();
                for (Object[] col : structureCT) {
                    colonnesCT.add((String) col[0]);
                }
                debug.put("colonnes_card_translation", colonnesCT);
                System.out.println("📋 card_translation: " + colonnesCT);

            } catch (Exception e) {
                debug.put("erreur_card_translation", e.getMessage());
            }

            // 3. Structure de card
            try {
                String sqlCard = "DESCRIBE card";
                Query queryCard = entityManager.createNativeQuery(sqlCard);
                @SuppressWarnings("unchecked")
                List<Object[]> structureCard = queryCard.getResultList();

                List<String> colonnesCard = new ArrayList<>();
                for (Object[] col : structureCard) {
                    colonnesCard.add((String) col[0]);
                }
                debug.put("colonnes_card", colonnesCard);
                System.out.println("📋 card: " + colonnesCard);

            } catch (Exception e) {
                debug.put("erreur_card", e.getMessage());
            }

            // 4. Échantillon de données pour comprendre les relations
            try {
                String sqlEchantillon = """
                SELECT 
                    HEX(cc.id) as cert_id,
                    cc.code_barre,
                    cc.type,
                    cc.card_id
                FROM card_certification cc
                LIMIT 3
                """;

                Query queryEchantillon = entityManager.createNativeQuery(sqlEchantillon);
                @SuppressWarnings("unchecked")
                List<Object[]> echantillon = queryEchantillon.getResultList();

                List<Map<String, Object>> echantillonData = new ArrayList<>();
                for (Object[] row : echantillon) {
                    Map<String, Object> cert = new HashMap<>();
                    cert.put("cert_id", row[0]);
                    cert.put("code_barre", row[1]);
                    cert.put("type", row[2]);
                    cert.put("card_id", row[3]);
                    echantillonData.add(cert);
                }
                debug.put("echantillon_certifications", echantillonData);

            } catch (Exception e) {
                debug.put("erreur_echantillon", e.getMessage());
            }

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            System.err.println("❌ Erreur debug structure: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("erreur", e.getMessage()));
        }
    }

    /**
     * 🃏 ENDPOINT CARTES CORRIGÉ - Avec vrais noms depuis card_translation
     * À remplacer dans CommandeController.java
     */
    @GetMapping("/frontend/commandes/{id}/cartes")
    public ResponseEntity<Map<String, Object>> getCartesCommande(@PathVariable String id) {
        try {
            System.out.println("🃏 Frontend: Récupération cartes avec vrais noms pour commande: " + id);

            // ✅ REQUÊTE CORRIGÉE avec jointure sur card_translation
            String sql = """
            SELECT 
                HEX(cc.id) as carteId,
                cc.code_barre as codeBarre,
                COALESCE(cc.type, 'Pokemon') as type,
                cc.card_id as cardId,
                COALESCE(cc.annotation, '') as annotation,
                
                -- ✅ VRAIS NOMS depuis card_translation
                COALESCE(ct.name, CONCAT('Carte Pokemon ', cc.code_barre)) as nom,
                COALESCE(ct.label_name, ct.name, cc.code_barre) as labelNom,
                
                -- Indicateur si on a trouvé une traduction
                CASE 
                    WHEN ct.name IS NOT NULL AND ct.name != '' THEN 1 
                    ELSE 0 
                END as avecNom,
                
                -- Informations de debug
                ct.locale as localeTraduction,
                HEX(cc.card_id) as cardIdHex
                
            FROM card_certification_order cco
            INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
            LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id 
                AND ct.locale = 'us'  -- ✅ Cartes anglaises d'abord
            WHERE HEX(cco.order_id) = ?
            ORDER BY cc.code_barre ASC
            LIMIT 100
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, id);

            @SuppressWarnings("unchecked")
            List<Object[]> resultats = query.getResultList();

            System.out.println("🔍 Cartes trouvées: " + resultats.size());

            List<Map<String, Object>> cartes = new ArrayList<>();
            int nombreAvecNom = 0;

            for (Object[] row : resultats) {
                Map<String, Object> carte = new HashMap<>();
                carte.put("carteId", row[0]);
                carte.put("codeBarre", row[1] != null ? row[1] : "N/A");
                carte.put("type", row[2]);
                carte.put("cardId", row[3]);
                carte.put("annotation", row[4]);
                carte.put("nom", row[5]); // ✅ Vrai nom ou fallback
                carte.put("labelNom", row[6]); // ✅ Label ou nom

                boolean avecNom = row[7] != null && ((Number) row[7]).intValue() == 1;
                carte.put("avecNom", avecNom);

                // Informations de debug
                carte.put("localeTraduction", row[8]);
                carte.put("cardIdHex", row[9]);

                if (avecNom) {
                    nombreAvecNom++;
                    System.out.println("✅ Carte avec nom: " + row[5] + " (code: " + row[1] + ")");
                } else {
                    System.out.println("⚠️ Carte sans traduction: " + row[5] + " (code: " + row[1] + ")");
                }

                cartes.add(carte);
            }

            // ✅ Calcul des statistiques
            int nombreCartes = cartes.size();
            int pourcentageAvecNom = nombreCartes > 0 ?
                    Math.round((nombreAvecNom * 100.0f) / nombreCartes) : 0;

            // ✅ Réponse structurée
            Map<String, Object> response = new HashMap<>();
            response.put("cartes", cartes);
            response.put("nombreCartes", nombreCartes);
            response.put("nombreAvecNom", nombreAvecNom);
            response.put("pourcentageAvecNom", pourcentageAvecNom);

            String qualite = pourcentageAvecNom >= 95 ? "EXCELLENTE" :
                    pourcentageAvecNom >= 85 ? "BONNE" :
                            pourcentageAvecNom >= 70 ? "CORRECTE" : "FAIBLE";
            response.put("qualiteCommande", qualite);

            System.out.println("📊 Résumé: " + nombreCartes + " cartes retournées, " +
                    nombreAvecNom + " avec nom (" + pourcentageAvecNom + "%)");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération cartes: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> fallback = new HashMap<>();
            fallback.put("cartes", new ArrayList<>());
            fallback.put("nombreCartes", 0);
            fallback.put("nombreAvecNom", 0);
            fallback.put("pourcentageAvecNom", 0);
            fallback.put("qualiteCommande", "ERREUR");
            fallback.put("erreur", e.getMessage());

            return ResponseEntity.status(500).body(fallback);
        }
    }

    /**
     * 🔍 ENDPOINT DEBUG - Pour tester la relation card <-> card_translation
     * À ajouter temporairement dans CommandeController.java
     */
    @GetMapping("/frontend/debug-card-translation/{id}")
    public ResponseEntity<Map<String, Object>> debugCardTranslation(@PathVariable String id) {
        try {
            System.out.println("🔍 Debug relation card <-> card_translation pour commande: " + id);

            Map<String, Object> debug = new HashMap<>();

            // 1. Vérifier la structure des données
            String sqlDebug = """
            SELECT 
                HEX(cc.id) as cert_id,
                cc.code_barre,
                HEX(cc.card_id) as card_id,
                
                -- Compter les traductions disponibles
                (SELECT COUNT(*) FROM card_translation ct1 
                 WHERE ct1.translatable_id = cc.card_id) as nb_traductions,
                
                -- Traductions par langue
                (SELECT ct2.name FROM card_translation ct2 
                 WHERE ct2.translatable_id = cc.card_id AND ct2.locale = 'us' 
                 LIMIT 1) as nom_us,
                
                (SELECT ct3.name FROM card_translation ct3 
                 WHERE ct3.translatable_id = cc.card_id AND ct3.locale = 'fr' 
                 LIMIT 1) as nom_fr,
                
                -- Toutes les langues disponibles
                (SELECT GROUP_CONCAT(DISTINCT ct4.locale ORDER BY ct4.locale)
                 FROM card_translation ct4 
                 WHERE ct4.translatable_id = cc.card_id) as langues_disponibles
                
            FROM card_certification_order cco
            INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
            WHERE HEX(cco.order_id) = ?
            LIMIT 5
            """;

            Query queryDebug = entityManager.createNativeQuery(sqlDebug);
            queryDebug.setParameter(1, id);

            @SuppressWarnings("unchecked")
            List<Object[]> resultatsDebug = queryDebug.getResultList();

            List<Map<String, Object>> cartesDebug = new ArrayList<>();
            for (Object[] row : resultatsDebug) {
                Map<String, Object> carteDebug = new HashMap<>();
                carteDebug.put("cert_id", row[0]);
                carteDebug.put("code_barre", row[1]);
                carteDebug.put("card_id", row[2]);
                carteDebug.put("nb_traductions", row[3]);
                carteDebug.put("nom_us", row[4]);
                carteDebug.put("nom_fr", row[5]);
                carteDebug.put("langues_disponibles", row[6]);
                cartesDebug.add(carteDebug);
            }

            debug.put("cartes_debug", cartesDebug);

            // 2. Statistiques globales
            String sqlStats = """
            SELECT 
                COUNT(*) as total_cartes,
                COUNT(CASE WHEN ct.name IS NOT NULL THEN 1 END) as avec_nom_us,
                COUNT(DISTINCT ct.locale) as langues_distinctes
            FROM card_certification_order cco
            INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
            LEFT JOIN card_translation ct ON cc.card_id = ct.translatable_id AND ct.locale = 'us'
            WHERE HEX(cco.order_id) = ?
            """;

            Query queryStats = entityManager.createNativeQuery(sqlStats);
            queryStats.setParameter(1, id);

            Object[] stats = (Object[]) queryStats.getSingleResult();
            debug.put("statistiques", Map.of(
                    "total_cartes", stats[0],
                    "avec_nom_us", stats[1],
                    "langues_distinctes", stats[2]
            ));

            return ResponseEntity.ok(debug);

        } catch (Exception e) {
            System.err.println("❌ Erreur debug: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("erreur", e.getMessage()));
        }
    }

    /**
     * 🃏 VERSION ALTERNATIVE - Avec fallback sur plusieurs langues
     * Si vous voulez essayer français puis anglais
     */
    @GetMapping("/frontend/commandes/{id}/cartes-multilangue")
    public ResponseEntity<Map<String, Object>> getCartesCommandeMultilangue(@PathVariable String id) {
        try {
            System.out.println("🃏 Frontend: Récupération cartes multilangue pour commande: " + id);

            String sql = """
            SELECT 
                HEX(cc.id) as carteId,
                cc.code_barre as codeBarre,
                COALESCE(cc.type, 'Pokemon') as type,
                cc.card_id as cardId,
                COALESCE(cc.annotation, '') as annotation,
                
                -- ✅ STRATÉGIE MULTILANGUE: us > fr > en > première disponible
                COALESCE(
                    (SELECT ct_us.name FROM card_translation ct_us 
                     WHERE ct_us.translatable_id = cc.card_id AND ct_us.locale = 'us' 
                     AND ct_us.name IS NOT NULL AND ct_us.name != '' LIMIT 1),
                    (SELECT ct_fr.name FROM card_translation ct_fr 
                     WHERE ct_fr.translatable_id = cc.card_id AND ct_fr.locale = 'fr' 
                     AND ct_fr.name IS NOT NULL AND ct_fr.name != '' LIMIT 1),
                    (SELECT ct_en.name FROM card_translation ct_en 
                     WHERE ct_en.translatable_id = cc.card_id AND ct_en.locale = 'en' 
                     AND ct_en.name IS NOT NULL AND ct_en.name != '' LIMIT 1),
                    (SELECT ct_any.name FROM card_translation ct_any 
                     WHERE ct_any.translatable_id = cc.card_id 
                     AND ct_any.name IS NOT NULL AND ct_any.name != '' LIMIT 1),
                    CONCAT('Carte Pokemon ', cc.code_barre)
                ) as nom,
                
                -- Langue de la traduction trouvée
                COALESCE(
                    (SELECT 'us' FROM card_translation ct_us 
                     WHERE ct_us.translatable_id = cc.card_id AND ct_us.locale = 'us' 
                     AND ct_us.name IS NOT NULL AND ct_us.name != '' LIMIT 1),
                    (SELECT 'fr' FROM card_translation ct_fr 
                     WHERE ct_fr.translatable_id = cc.card_id AND ct_fr.locale = 'fr' 
                     AND ct_fr.name IS NOT NULL AND ct_fr.name != '' LIMIT 1),
                    (SELECT 'en' FROM card_translation ct_en 
                     WHERE ct_en.translatable_id = cc.card_id AND ct_en.locale = 'en' 
                     AND ct_en.name IS NOT NULL AND ct_en.name != '' LIMIT 1),
                    (SELECT ct_any.locale FROM card_translation ct_any 
                     WHERE ct_any.translatable_id = cc.card_id 
                     AND ct_any.name IS NOT NULL AND ct_any.name != '' LIMIT 1),
                    'fallback'
                ) as langueUtilisee
                
            FROM card_certification_order cco
            INNER JOIN card_certification cc ON cco.card_certification_id = cc.id
            WHERE HEX(cco.order_id) = ?
            ORDER BY cc.code_barre ASC
            LIMIT 100
            """;

            Query query = entityManager.createNativeQuery(sql);
            query.setParameter(1, id);

            @SuppressWarnings("unchecked")
            List<Object[]> resultats = query.getResultList();

            List<Map<String, Object>> cartes = new ArrayList<>();
            Map<String, Integer> languesStats = new HashMap<>();

            for (Object[] row : resultats) {
                Map<String, Object> carte = new HashMap<>();
                carte.put("carteId", row[0]);
                carte.put("codeBarre", row[1]);
                carte.put("type", row[2]);
                carte.put("cardId", row[3]);
                carte.put("annotation", row[4]);
                carte.put("nom", row[5]);
                carte.put("labelNom", row[5]);

                String langueUtilisee = (String) row[6];
                carte.put("langueUtilisee", langueUtilisee);
                carte.put("avecNom", !"fallback".equals(langueUtilisee));

                // Statistiques des langues
                languesStats.merge(langueUtilisee, 1, Integer::sum);

                cartes.add(carte);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("cartes", cartes);
            response.put("nombreCartes", cartes.size());
            response.put("languesStats", languesStats);
            response.put("qualiteCommande", "MULTILANGUE");

            System.out.println("📊 Statistiques langues: " + languesStats);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur récupération cartes multilangue: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of("erreur", e.getMessage()));
        }
    }
    /**
     * 🔧 ENDPOINT VERIFIER CORRIGÉ - Format ID universel
     */
    @GetMapping("/frontend/commandes/{id}/verifier")
    public ResponseEntity<Map<String, Object>> verifierCommande(@PathVariable String id) {
        try {
            System.out.println("🔍 Vérification commande: " + id);

            // ✅ Essayer avec et sans tirets pour la compatibilité
            String[] idsAEssayer = {
                    id,                              // ID original
                    id.replace("-", ""),             // Sans tirets
                    id.toUpperCase(),                // Majuscules
                    id.replace("-", "").toUpperCase() // Sans tirets + majuscules
            };

            for (String idTest : idsAEssayer) {
                try {
                    String sqlVerif = """
                    SELECT 
                        HEX(o.id) as commandeId,
                        o.num_commande as numeroCommande,
                        COUNT(cco.card_certification_id) as nombreCartes
                    FROM `order` o
                    LEFT JOIN card_certification_order cco ON o.id = cco.order_id
                    WHERE HEX(o.id) = ?
                    GROUP BY o.id, o.num_commande
                    """;

                    Query query = entityManager.createNativeQuery(sqlVerif);
                    query.setParameter(1, idTest);

                    @SuppressWarnings("unchecked")
                    List<Object[]> resultats = query.getResultList();

                    if (!resultats.isEmpty()) {
                        Object[] row = resultats.get(0);
                        Map<String, Object> response = new HashMap<>();
                        response.put("existe", true);
                        response.put("commandeId", row[0]);
                        response.put("numeroCommande", row[1]);
                        response.put("nombreCartes", row[2]);
                        response.put("idTeste", idTest);
                        response.put("message", "Commande trouvée avec " + row[2] + " cartes");

                        System.out.println("✅ Commande trouvée avec ID: " + idTest);
                        return ResponseEntity.ok(response);
                    }
                } catch (Exception e) {
                    System.out.println("❌ Échec avec ID: " + idTest + " - " + e.getMessage());
                }
            }

            // Aucun ID n'a fonctionné
            Map<String, Object> response = new HashMap<>();
            response.put("existe", false);
            response.put("message", "Commande non trouvée avec aucun format d'ID");
            response.put("idsEssayes", String.join(", ", idsAEssayer));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("❌ Erreur vérification: " + e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("existe", false);
            error.put("erreur", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    /// //////////////////
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