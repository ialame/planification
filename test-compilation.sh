#!/bin/bash

# Script de test de compilation pour l'algorithme de planification DP
echo "🏗️  === TEST DE COMPILATION ALGORITHME PLANIFICATION DP ==="
echo "📅 Date: $(date)"
echo

# Configuration
PROJECT_DIR="."
JAVA_VERSION="21"

# Couleurs pour les messages
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Fonction pour afficher les messages colorés
print_status() {
    echo -e "${BLUE}🔍 $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# 1. Vérifications préliminaires
print_status "=== VÉRIFICATIONS PRÉLIMINAIRES ==="

# Vérifier Java
if command -v java &> /dev/null; then
    JAVA_CURRENT=$(java -version 2>&1 | head -n1 | cut -d'"' -f2 | cut -d'.' -f1)
    print_success "Java trouvé: version $JAVA_CURRENT"

    if [ "$JAVA_CURRENT" -lt "21" ]; then
        print_warning "Java $JAVA_CURRENT détecté, mais Java 21+ recommandé"
    fi
else
    print_error "Java n'est pas installé ou pas dans le PATH"
    exit 1
fi

# Vérifier Maven
if command -v mvn &> /dev/null; then
    MAVEN_VERSION=$(mvn -version | head -n1 | cut -d' ' -f3)
    print_success "Maven trouvé: version $MAVEN_VERSION"
else
    print_error "Maven n'est pas installé ou pas dans le PATH"
    exit 1
fi

# Vérifier la structure du projet
if [ ! -f "pom.xml" ]; then
    print_error "Fichier pom.xml non trouvé. Êtes-vous dans le bon répertoire?"
    exit 1
fi

print_success "Structure du projet validée"

# 2. Nettoyage préalable
print_status "=== NETTOYAGE PRÉALABLE ==="
mvn clean > /dev/null 2>&1
print_success "Projet nettoyé"

# 3. Compilation
print_status "=== COMPILATION ==="
echo "🔧 Compilation en cours..."

# Compiler avec affichage des erreurs
COMPILE_OUTPUT=$(mvn compile 2>&1)
COMPILE_STATUS=$?

if [ $COMPILE_STATUS -eq 0 ]; then
    print_success "Compilation réussie !"
    echo

    # Afficher les classes compilées importantes
    if [ -d "target/classes/com/pcagrade/order/service" ]; then
        print_status "Classes compilées trouvées:"
        find target/classes/com/pcagrade/order/service -name "*.class" | sed 's|target/classes/||' | sed 's|/|.|g' | sed 's|.class$||' | while read class; do
            echo "  📦 $class"
        done
    fi

else
    print_error "Erreurs de compilation détectées"
    echo
    echo "📋 DÉTAILS DES ERREURS:"
    echo "----------------------------------------"
    echo "$COMPILE_OUTPUT"
    echo "----------------------------------------"

    # Analyser les erreurs courantes
    print_status "=== ANALYSE DES ERREURS COURANTES ==="

    if echo "$COMPILE_OUTPUT" | grep -q "package.*does not exist"; then
        print_warning "Erreur de package - vérifiez les imports"
    fi

    if echo "$COMPILE_OUTPUT" | grep -q "cannot find symbol"; then
        print_warning "Symbole non trouvé - vérifiez les dépendances et noms de méthodes"
    fi

    if echo "$COMPILE_OUTPUT" | grep -q "source release.*requires target release"; then
        print_warning "Problème de version Java - vérifiez la configuration Maven"
    fi

    echo
    print_error "Compilation échouée. Consultez les erreurs ci-dessus."
    exit 1
fi

# 4. Test de compilation spécifique DP
print_status "=== VÉRIFICATION CLASSES DP ==="

# Vérifier que la classe principale DP est compilée
DP_CLASS="target/classes/com/pcagrade/order/service/DynamicProgrammingPlanificationService.class"
if [ -f "$DP_CLASS" ]; then
    print_success "DynamicProgrammingPlanificationService compilée"
else
    print_error "DynamicProgrammingPlanificationService non compilée"
fi

# Vérifier le contrôleur DP
DP_CONTROLLER="target/classes/com/pcagrade/order/controller/DynamicProgrammingController.class"
if [ -f "$DP_CONTROLLER" ]; then
    print_success "DynamicProgrammingController compilé"
else
    print_warning "DynamicProgrammingController non trouvé (optionnel)"
fi

# 5. Test de packaging
print_status "=== TEST DE PACKAGING ==="
echo "📦 Création du package..."

PACKAGE_OUTPUT=$(mvn package -DskipTests=true 2>&1)
PACKAGE_STATUS=$?

if [ $PACKAGE_STATUS -eq 0 ]; then
    print_success "Package créé avec succès"

    # Vérifier le JAR créé
    if [ -f "target/order-2.4.1.jar" ]; then
        JAR_SIZE=$(du -h target/order-2.4.1.jar | cut -f1)
        print_success "JAR créé: order-2.4.1.jar ($JAR_SIZE)"
    fi

else
    print_warning "Erreur lors du packaging (compilation réussie)"
    echo "$PACKAGE_OUTPUT" | tail -20
fi

# 6. Résumé final
echo
print_status "=== RÉSUMÉ DE LA COMPILATION ==="

if [ $COMPILE_STATUS -eq 0 ]; then
    print_success "✅ Compilation: RÉUSSIE"

    if [ $PACKAGE_STATUS -eq 0 ]; then
        print_success "✅ Packaging: RÉUSSI"
    else
        print_warning "⚠️  Packaging: ÉCHOUÉ (mais compilation OK)"
    fi

    echo
    echo "🎯 PROCHAINES ÉTAPES:"
    echo "  1. Lancer l'application: mvn spring-boot:run"
    echo "  2. Tester l'API DP: curl -X POST 'http://localhost:8080/api/planification-dp/executer?jour=1&mois=1&annee=2025'"
    echo "  3. Vérifier les logs pour détecter d'éventuels problèmes runtime"
    echo

else
    print_error "❌ Compilation: ÉCHOUÉE"
    echo
    echo "🔧 ACTIONS CORRECTIVES:"
    echo "  1. Corriger les erreurs de compilation listées ci-dessus"
    echo "  2. Vérifier que toutes les dépendances sont présentes dans pom.xml"
    echo "  3. Relancer ce script après correction"
    echo
fi

# 7. Informations système pour debug
print_status "=== INFORMATIONS SYSTÈME ==="
echo "📋 Java: $(java -version 2>&1 | head -n1)"
echo "📋 Maven: $(mvn -version | head -n1)"
echo "📋 Répertoire: $(pwd)"
echo "📋 Timestamp: $(date)"

echo
echo "🏁 Test de compilation terminé."