package com.pcagrade.order.controller;

import com.github.f4b6a3.ulid.Ulid;
import com.pcagrade.order.entity.Employe;
import com.pcagrade.order.entity.Commande;
import com.pcagrade.order.entity.Planification;
import com.pcagrade.order.service.EmployeService;
import com.pcagrade.order.service.PlanificationService;
import com.pcagrade.order.repository.CommandeRepository;
import com.pcagrade.order.util.UlidUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @Autowired
    private EmployeService employeService;

    @Autowired(required = false) // Optional si PlanificationService n'existe pas encore
    private PlanificationService planificationService;

    @Autowired
    private CommandeRepository commandeRepository;

    @GetMapping("/test-planification")
    public String testPlanification() {
        try {
            System.out.println("🧪 === TEST PLANIFICATION DEBUG ===");

            // ✅ CORRIGÉ : Récupération des employés
            List<Map<String, Object>> employesData = employeService.getTousEmployesActifs();
            if (employesData.isEmpty()) {
                return "❌ Aucun employé trouvé";
            }

            // ✅ CORRIGÉ : Récupération des commandes
            List<Commande> orders = commandeRepository.findAll();
            if (orders.isEmpty()) {
                return "❌ Aucune commande trouvée";
            }

            // ✅ CORRIGÉ : Conversion des IDs - Éviter les conversions problématiques
            Map<String, Object> employeData = employesData.get(0);
            String employeIdString = (String) employeData.get("id");

            // ✅ SOLUTION : Utiliser directement les IDs comme String
            Commande order = orders.get(0);
            String orderIdString = order.getId().toString();

            System.out.println("=== TEST CRÉATION PLANIFICATION ===");
            System.out.println("Employé ID (string): " + employeIdString);
            System.out.println("Commande ID (string): " + orderIdString);

            // ✅ CORRIGÉ : Test simplifié sans création d'objet Planification
            if (planificationService != null) {
                System.out.println("✅ PlanificationService disponible");

                // Test de la méthode automatique
                Map<String, Object> resultat = planificationService.executerPlanificationAutomatique();

                if ((Boolean) resultat.get("success")) {
                    return "✅ Test planification réussi - " + resultat.get("nombreCommandesPlanifiees") + " commandes planifiées";
                } else {
                    return "❌ Échec test planification: " + resultat.get("erreur");
                }
            } else {
                System.out.println("⚠️ PlanificationService non disponible");
                return "✅ Test planification basique réussi - Employé: " + employeData.get("prenom") + " " + employeData.get("nom");
            }

        } catch (Exception e) {
            System.out.println("❌ ERREUR lors du test de planification:");
            System.out.println("Message: " + e.getMessage());
            e.printStackTrace();
            return "❌ ERREUR: " + e.getMessage();
        }
    }

    @GetMapping("/test-employes")
    public String testEmployes() {
        try {
            System.out.println("🧪 === TEST EMPLOYÉS ===");

            // ✅ CORRIGÉ : Utiliser la méthode qui existe
            List<Map<String, Object>> employes = employeService.getTousEmployesActifs();

            System.out.println("Employés trouvés: " + employes.size());
            for (Map<String, Object> emp : employes) {
                System.out.println("  - " + emp.get("prenom") + " " + emp.get("nom") + " (" + emp.get("email") + ")");
            }

            return "✅ " + employes.size() + " employés trouvés";

        } catch (Exception e) {
            System.err.println("❌ Erreur test employés: " + e.getMessage());
            e.printStackTrace();
            return "❌ ERREUR: " + e.getMessage();
        }
    }

    @GetMapping("/structure-db")
    public String decouvrirStructure() {
        try {
            employeService.decouvrirStructureDB();
            return "✅ Structure DB analysée - voir logs";
        } catch (Exception e) {
            return "❌ Erreur analyse structure: " + e.getMessage();
        }
    }
}
