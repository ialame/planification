package com.pcagrade.order.service;

import com.pcagrade.order.repository.CommandeRepository;
import com.pcagrade.order.repository.EmployeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardService {

    @Autowired
    private CommandeRepository commandeRepository;

    @Autowired
    private EmployeRepository employeRepository;

    /**
     * ✅ MÉTHODE PRINCIPALE : Statistiques du dashboard
     */
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Statistiques des commandes
            long totalCommandes = commandeRepository.count();
            long commandesEnAttente = commandeRepository.countByStatus(1);
            long commandesEnCours = commandeRepository.countByStatus(2);
            long commandesTerminees = commandeRepository.countByStatus(3);

            stats.put("totalCommandes", totalCommandes);
            stats.put("commandesEnAttente", commandesEnAttente);
            stats.put("commandesEnCours", commandesEnCours);
            stats.put("commandesTerminees", commandesTerminees);

            // Statistiques des employés
            long totalEmployes = employeRepository.count();
            long employesActifs;
            try {
                employesActifs = employeRepository.countByActif(true);
            } catch (Exception e) {
                // Fallback si la méthode n'existe pas encore
                employesActifs = employeRepository.findByActifTrue().size();
            }

            stats.put("totalEmployes", totalEmployes);
            stats.put("employesActifs", employesActifs);

            // Métadonnées
            stats.put("status", "success");
            stats.put("timestamp", System.currentTimeMillis());
            stats.put("lastUpdated", LocalDateTime.now().toString());

            System.out.println("✅ Dashboard stats chargées: " + stats);
            return stats;
        } catch (Exception e) {
            System.err.println("❌ Erreur calcul stats: " + e.getMessage());
            e.printStackTrace();

            // Stats par défaut en cas d'erreur
            stats.put("status", "error");
            stats.put("error", e.getMessage());
            stats.put("totalCommandes", 0);
            stats.put("commandesEnAttente", 0);
            stats.put("commandesEnCours", 0);
            stats.put("commandesTerminees", 0);
            stats.put("employesActifs", 0);
            return stats;
        }

    }
}
