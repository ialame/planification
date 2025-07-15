// ============================================================================
// 🎯 ÉTAPE 2 : Contrôleur REST pour l'algorithme glouton
// ============================================================================

// ✅ CRÉEZ : PlanificationGloutonneController.java

package com.pcagrade.order.controller;

import com.pcagrade.order.service.PlanificationGloutonneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/planification-gloutonne")
@CrossOrigin(originPatterns = "*", allowCredentials = "false")
public class PlanificationGloutonneController {

    @Autowired
    private PlanificationGloutonneService planificationService;

    /**
     * Exécute la planification gloutonne depuis une date donnée
     */
    @PostMapping("/executer")
    public ResponseEntity<Map<String, Object>> executerPlanification(
            @RequestParam int jour,
            @RequestParam int mois,
            @RequestParam int annee) {

        try {
            System.out.println("🎲 API: Planification gloutonne pour " + jour + "/" + mois + "/" + annee);

            Map<String, Object> resultat = planificationService.executerPlanificationGloutonne(jour, mois, annee);

            if ((Boolean) resultat.get("success")) {
                System.out.println("✅ API: Planification gloutonne réussie");
                return ResponseEntity.ok(resultat);
            } else {
                System.out.println("❌ API: Planification gloutonne échouée");
                return ResponseEntity.status(500).body(resultat);
            }

        } catch (Exception e) {
            System.err.println("❌ API: Erreur planification gloutonne: " + e.getMessage());
            e.printStackTrace();

            Map<String, Object> erreur = new HashMap<>();
            erreur.put("success", false);
            erreur.put("message", "Erreur interne: " + e.getMessage());
            erreur.put("algorithme", "GLOUTON");

            return ResponseEntity.status(500).body(erreur);
        }
    }

    /**
     * Planification depuis le 1er juin 2025 (raccourci)
     */
    @PostMapping("/juin-2025")
    public ResponseEntity<Map<String, Object>> planifierDepuisJuin2025() {
        return executerPlanification(1, 6, 2025);
    }

    /**
     * Planification pour aujourd'hui (raccourci)
     */
    @PostMapping("/aujourd-hui")
    public ResponseEntity<Map<String, Object>> planifierAujourdHui() {
        LocalDate aujourdHui = LocalDate.now();
        return executerPlanification(
                aujourdHui.getDayOfMonth(),
                aujourdHui.getMonthValue(),
                aujourdHui.getYear()
        );
    }

    /**
     * Test de fonctionnement
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> test = new HashMap<>();
        test.put("success", true);
        test.put("message", "Planification gloutonne prête");
        test.put("algorithme", "GLOUTON");
        test.put("endpoints", Map.of(
                "executer", "POST /api/planification-gloutonne/executer?jour=1&mois=6&annee=2025",
                "juin2025", "POST /api/planification-gloutonne/juin-2025",
                "aujourdHui", "POST /api/planification-gloutonne/aujourd-hui"
        ));
        return ResponseEntity.ok(test);
    }

    @PostMapping("/test-simple")
    public ResponseEntity<Map<String, Object>> testSimple() {
        return executerPlanification(1, 6, 2025);
    }
}

// ============================================================================
// 🚀 ÉTAPES D'INSTALLATION
// ============================================================================

/**
 * INSTRUCTIONS POUR AUJOURD'HUI :
 *
 * 1. ✅ Créez PlanificationGloutonneService.java
 * 2. ✅ Créez PlanificationGloutonneController.java
 * 3. ✅ Redémarrez l'application : mvn spring-boot:run
 * 4. ✅ Testez l'API :
 *    curl -X POST "http://localhost:8080/api/planification-gloutonne/juin-2025"
 *
 * 5. ✅ Si ça marche, vous verrez vos commandes planifiées !
 * 6. ✅ Ensuite on fait le frontend Vue.js
 */

// Tests rapides :
// curl "http://localhost:8080/api/planification-gloutonne/test"
// curl -X POST "http://localhost:8080/api/planification-gloutonne/juin-2025"
// curl -X POST "http://localhost:8080/api/planification-gloutonne/executer?jour=1&mois=6&annee=2025"