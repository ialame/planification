#!/bin/bash

# Script de test automatisé pour l'algorithme de planification dynamique
# avec les vraies données de la table order

echo "🎯 === TEST PLANIFICATION DYNAMIQUE AVEC DONNÉES RÉELLES ==="
echo "📅 Date: $(date)"
echo

# Configuration
BASE_URL="http://localhost:8080/api"
DATE_ACTUELLE=$(date +%d)
MOIS_ACTUEL=$(date +%m)
ANNEE_ACTUELLE=$(date +%Y)

echo "🔧 Configuration:"
echo "   • URL de base: $BASE_URL"
echo "   • Date actuelle: $DATE_ACTUELLE/$MOIS_ACTUEL/$ANNEE_ACTUELLE"
echo

# Fonction pour tester un endpoint
test_endpoint() {
    local endpoint=$1
    local description=$2
    local method=${3:-GET}
    local data=${4:-}

    echo "📡 Test: $description"
    echo "   → $method $BASE_URL$endpoint"

    if [ "$method" = "POST" ] && [ -n "$data" ]; then
        response=$(curl -s -X POST "$BASE_URL$endpoint" \
                   -H "Content-Type: application/x-www-form-urlencoded" \
                   -d "$data" \
                   -w "\nSTATUS_CODE:%{http_code}")
    else
        response=$(curl -s -X $method "$BASE_URL$endpoint" \
                   -w "\nSTATUS_CODE:%{http_code}")
    fi

    # Extraire le code de statut
    status_code=$(echo "$response" | grep "STATUS_CODE:" | cut -d: -f2)
    json_response=$(echo "$response" | grep -v "STATUS_CODE:")

    if [ "$status_code" = "200" ]; then
        echo "   ✅ Succès (200)"

        # Extraire des informations clés du JSON si possible
        if command -v jq &> /dev/null; then
            success=$(echo "$json_response" | jq -r '.success // "N/A"')
            message=$(echo "$json_response" | jq -r '.message // "N/A"')

            if [ "$success" = "true" ]; then
                echo "   📊 Résultat: Succès"

                # Afficher des statistiques si disponibles
                nombre_planifications=$(echo "$json_response" | jq -r '.nombrePlanifications // "N/A"')
                methode=$(echo "$json_response" | jq -r '.methode // "N/A"')
                temps=$(echo "$json_response" | jq -r '.tempsCalculMs // "N/A"')

                if [ "$nombre_planifications" != "N/A" ]; then
                    echo "   📋 Planifications créées: $nombre_planifications"
                fi
                if [ "$methode" != "N/A" ]; then
                    echo "   🧮 Méthode utilisée: $methode"
                fi
                if [ "$temps" != "N/A" ]; then
                    echo "   ⏱️ Temps de calcul: ${temps}ms"
                fi
            else
                echo "   ❌ Échec: $message"
            fi
        else
            echo "   📄 Réponse JSON reçue (installez jq pour plus de détails)"
        fi
    else
        echo "   ❌ Erreur HTTP $status_code"
        if [ -n "$json_response" ]; then
            echo "   📄 Détails: $json_response"
        fi
    fi

    echo
}

# Tests progressifs

echo "🔍 === PHASE 1: DIAGNOSTIC DES DONNÉES ==="
test_endpoint "/test-planification/diagnostic" "Diagnostic des données disponibles"

echo "🧪 === PHASE 2: TESTS BASIQUES ==="
test_endpoint "/test-planification/test-rapide" "Test rapide avec données d'aujourd'hui"

echo "📅 === PHASE 3: TESTS AVEC DATES SPÉCIFIQUES ==="
test_endpoint "/test-planification/test-date" "Test avec date actuelle" "POST" "jour=$DATE_ACTUELLE&mois=$MOIS_ACTUEL&annee=$ANNEE_ACTUELLE"

# Test avec le début du mois
DEBUT_MOIS=1
test_endpoint "/test-planification/test-date" "Test depuis début du mois" "POST" "jour=$DEBUT_MOIS&mois=$MOIS_ACTUEL&annee=$ANNEE_ACTUELLE"

echo "🔍 === PHASE 4: VÉRIFICATION DÉTAILLÉE ==="
test_endpoint "/test-planification/verifier-donnees" "Vérification des données" "POST" "jour=$DEBUT_MOIS&mois=$MOIS_ACTUEL&annee=$ANNEE_ACTUELLE"

echo "⚡ === PHASE 5: TESTS DE PERFORMANCE ==="
test_endpoint "/test-planification/test-performance" "Tests de performance comparatifs" "POST"

echo "🎯 === PHASE 6: TEST PLANIFICATION DP DIRECTE ==="
test_endpoint "/planification-dp/executer" "Exécution directe DP" "POST" "jour=$DEBUT_MOIS&mois=$MOIS_ACTUEL&annee=$ANNEE_ACTUELLE"

echo "📊 === RÉSUMÉ DES TESTS ==="
echo "✅ Tests terminés à $(date)"
echo
echo "🔧 UTILISATION MANUELLE:"
echo "   • Diagnostic: curl -X GET $BASE_URL/test-planification/diagnostic"
echo "   • Test rapide: curl -X GET $BASE_URL/test-planification/test-rapide"
echo "   • Test date: curl -X POST $BASE_URL/test-planification/test-date -d 'jour=1&mois=$MOIS_ACTUEL&annee=$ANNEE_ACTUELLE'"
echo
echo "📋 ENDPOINTS DISPONIBLES:"
echo "   • GET  /api/test-planification/diagnostic"
echo "   • GET  /api/test-planification/test-rapide"
echo "   • POST /api/test-planification/test-date"
echo "   • POST /api/test-planification/verifier-donnees"
echo "   • POST /api/test-planification/test-performance"
echo "   • POST /api/planification-dp/executer"
echo
echo "💡 CONSEILS:"
echo "   • Vérifiez d'abord le diagnostic pour voir les données disponibles"
echo "   • Utilisez test-rapide pour un test simple"
echo "   • Ajustez les dates selon vos données en base"
echo "   • Consultez les logs de l'application Spring Boot pour plus de détails"