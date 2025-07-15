# 🎯 Algorithme Planification DP - Utilisation avec Données Réelles

## 📋 Vue d'Ensemble

Ce système utilise maintenant **vos vraies données** de la table `order` (entité `Commande`) pour effectuer une planification optimisée avec l'algorithme de programmation dynamique.

## 🚀 Démarrage Rapide

### 1. Vérification des Données

Avant de lancer une planification, vérifiez que vos données sont disponibles :

```bash
curl -X GET http://localhost:8080/api/test-planification/diagnostic
```

**Réponse attendue :**
```json
{
  "success": true,
  "commandes": {
    "totalSysteme": 150,
    "enAttente": 45,
    "aPlanifierDepuisMois": 23
  },
  "employes": {
    "nombreActifs": 3,
    "liste": ["Jean Dupont", "Marie Martin", "Paul Durand"]
  }
}
```

### 2. Test Rapide

Lancez un test avec les données d'aujourd'hui :

```bash
curl -X GET http://localhost:8080/api/test-planification/test-rapide
```

### 3. Planification Complète

Exécutez la planification depuis le début du mois :

```bash
curl -X POST http://localhost:8080/api/planification-dp/executer \
  -d "jour=1&mois=12&annee=2024"
```

## 📊 Structure des Données

### Entité Commande Utilisée

Le système lit directement vos données depuis la table `order` :

```sql
SELECT HEX(o.id) as id, o.num_commande, o.date, o.prix_total, 
       o.temps_estime_minutes, o.nombre_cartes, o.priorite_string,
       COUNT(cco.card_certification_id) as nombre_cartes_reel
FROM `order` o
LEFT JOIN card_certification_order cco ON o.id = cco.order_id
WHERE o.date >= ?
GROUP BY o.id
ORDER BY o.date ASC
```

### Calculs Automatiques

Le système effectue des **calculs intelligents** pour optimiser la planification :

#### 🕒 Durée Estimée
1. **Priorité 1** : `temps_estime_minutes` si disponible
2. **Priorité 2** : Estimation selon `nombre_cartes` → `30 + cartes × 15` minutes
3. **Minimum** : 60 minutes par commande

#### 📅 Date Limite
1. **Priorité 1** : `date_limite` si disponible
2. **Priorité 2** : `date` + `delai` (parsing automatique)
3. **Priorité 3** : Délai par défaut selon priorité :
    - `URGENTE` →

{
"recommandations": [
"✅ Utiliser /api/frontend/commandes pour la liste des commandes",
"✅ Utiliser /api/frontend/commandes/{id}/cartes pour les détails",
"✅ Utiliser /api/frontend/planning-employes pour le planning",
"🎯 Afficher le pourcentageAvecNom pour la qualité des données",
"📊 Utiliser qualiteCommande pour des indicateurs visuels"
],
"qualite_noms_cartes": {
"noms_distincts": 10461,
"avec_nom": 473140,
"total_certifications": 559170,
"pourcentage_avec_nom": 85
},
"commandes_7_derniers_jours": {
"moyenne_cartes_par_commande": 100.5818,
"total": 1387,
"planifiables": 1148
},
"frontend_pret": false,
"message": "⚠️ Problèmes détectés - Vérifier les données"
}
