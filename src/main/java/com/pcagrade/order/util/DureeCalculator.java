package com.pcagrade.order.util;

import org.springframework.stereotype.Component;
import java.util.Map;

/**
 * Classe utilitaire pour standardiser le calcul de durée des commandes
 *
 * RÈGLE MÉTIER : Durée = 3 × nombre de cartes
 * - Chaque carte nécessite 3 unités de temps de certification
 * - Durée minimale : 3 minutes (1 carte minimum)
 * - Durée par défaut si données manquantes : 60 minutes
 */
@Component
public class DureeCalculator {

    /**
     * Constante : temps de certification par carte (en minutes)
     */
    public static final int TEMPS_CERTIFICATION_PAR_CARTE = 3;

    /**
     * Durée par défaut si impossible de calculer (en minutes)
     */
    public static final int DUREE_DEFAUT_MINUTES = 60;

    /**
     * Durée minimale d'une commande (en minutes)
     */
    public static final int DUREE_MINIMALE_MINUTES = 3; // 1 carte minimum

    /**
     * Calcule la durée d'une commande basée sur le nombre de cartes
     *
     * @param nombreCartes Nombre de cartes dans la commande
     * @return Durée en minutes (3 × nombreCartes)
     */
    public static int calculerDureeMinutes(int nombreCartes) {
        if (nombreCartes <= 0) {
            return DUREE_MINIMALE_MINUTES; // 1 carte minimum
        }
        return nombreCartes * TEMPS_CERTIFICATION_PAR_CARTE;
    }

    /**
     * Calcule la durée d'une commande à partir des données de la base
     *
     * @param commandeData Map contenant les données de la commande
     * @return Durée en minutes calculée selon les règles métier
     */
    public static int calculerDureeDepuisCommande(Map<String, Object> commandeData) {
        try {
            // 1. Priorité au nombre de cartes réelles
            Integer nombreCartesReelles = (Integer) commandeData.get("nombreCartesReelles");
            if (nombreCartesReelles != null && nombreCartesReelles > 0) {
                return calculerDureeMinutes(nombreCartesReelles);
            }

            // 2. Fallback sur nombreCartes
            Integer nombreCartes = (Integer) commandeData.get("nombreCartes");
            if (nombreCartes != null && nombreCartes > 0) {
                return calculerDureeMinutes(nombreCartes);
            }

            // 3. Fallback sur dureeMinutes existante si cohérente
            Integer dureeExistante = (Integer) commandeData.get("dureeMinutes");
            if (dureeExistante != null && dureeExistante >= DUREE_MINIMALE_MINUTES) {
                // Vérifier si cette durée est cohérente avec la règle des cartes
                int cartesImpliquees = dureeExistante / TEMPS_CERTIFICATION_PAR_CARTE;
                if (dureeExistante == cartesImpliquees * TEMPS_CERTIFICATION_PAR_CARTE) {
                    return dureeExistante; // Durée cohérente
                }
            }

            // 4. Dernier recours : durée par défaut
            System.out.println("⚠️ Aucune donnée de cartes pour commande " +
                    commandeData.get("numeroCommande") + ", utilisation durée par défaut");
            return DUREE_DEFAUT_MINUTES;

        } catch (Exception e) {
            System.err.println("❌ Erreur calcul durée pour commande " +
                    commandeData.get("numeroCommande") + ": " + e.getMessage());
            return DUREE_DEFAUT_MINUTES;
        }
    }

    /**
     * Convertit une durée en minutes vers un format lisible
     *
     * @param dureeMinutes Durée en minutes
     * @return Format "Xh Ymin" ou "Ymin"
     */
    public static String formaterDuree(int dureeMinutes) {
        if (dureeMinutes < 60) {
            return dureeMinutes + "min";
        }

        int heures = dureeMinutes / 60;
        int minutes = dureeMinutes % 60;

        if (minutes == 0) {
            return heures + "h";
        } else {
            return heures + "h " + minutes + "min";
        }
    }

    /**
     * Calcule le nombre de cartes théorique basé sur la durée
     * Utile pour la validation inverse
     *
     * @param dureeMinutes Durée en minutes
     * @return Nombre de cartes théorique
     */
    public static int calculerNombreCartesTheorique(int dureeMinutes) {
        return Math.max(1, dureeMinutes / TEMPS_CERTIFICATION_PAR_CARTE);
    }

    /**
     * Valide la cohérence entre nombre de cartes et durée
     *
     * @param nombreCartes Nombre de cartes déclaré
     * @param dureeMinutes Durée déclarée
     * @return true si cohérent, false sinon
     */
    public static boolean validerCoherence(int nombreCartes, int dureeMinutes) {
        int dureeAttendue = calculerDureeMinutes(nombreCartes);
        return dureeAttendue == dureeMinutes;
    }

    /**
     * Génère un rapport de calcul de durée pour debug
     *
     * @param commandeData Données de la commande
     * @return Rapport détaillé du calcul
     */
    public static String genererRapportCalcul(Map<String, Object> commandeData) {
        StringBuilder rapport = new StringBuilder();
        rapport.append("🔍 Calcul durée commande ").append(commandeData.get("numeroCommande")).append(":\n");

        Integer nombreCartesReelles = (Integer) commandeData.get("nombreCartesReelles");
        Integer nombreCartes = (Integer) commandeData.get("nombreCartes");
        Integer dureeExistante = (Integer) commandeData.get("dureeMinutes");

        rapport.append("- Cartes réelles: ").append(nombreCartesReelles).append("\n");
        rapport.append("- Cartes déclarées: ").append(nombreCartes).append("\n");
        rapport.append("- Durée existante: ").append(dureeExistante).append(" min\n");

        int dureeCalculee = calculerDureeDepuisCommande(commandeData);
        rapport.append("- Durée calculée: ").append(dureeCalculee).append(" min (");
        rapport.append(formaterDuree(dureeCalculee)).append(")\n");
        rapport.append("- Cartes impliquées: ").append(calculerNombreCartesTheorique(dureeCalculee));

        return rapport.toString();
    }
}