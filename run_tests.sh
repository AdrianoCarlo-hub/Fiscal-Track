#!/bin/bash

# Script pour lancer les tests Maven sur Fiscal-Track
# A exécuter depuis le répertoire backend/fiscal-track

set -e

echo "=========================================="
echo "Lancement des tests Fiscal-Track"
echo "=========================================="
echo ""

# Vérifier que Maven est disponible
if ! command -v mvn &> /dev/null; then
    echo "ERREUR: Maven (mvn) n'est pas installé ou n'est pas dans le PATH"
    echo "Solution: Installer Maven ou ajouter son chemin au PATH"
    exit 1
fi

# Vérifier la version de Maven
echo "Version de Maven:"
mvn --version
echo ""

# Lancer les tests
echo "Lancement des tests..."
echo ""

mvn clean test

# Vérifier le résultat
if [ $? -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "✅ TOUS LES TESTS ONT REUSSI"
    echo "=========================================="
else
    echo ""
    echo "=========================================="
    echo "❌ DES TESTS ONT ECHOUE"
    echo "=========================================="
    echo ""
    echo "Voir les logs ci-dessus pour identifier les erreurs."
    echo ""
    echo "Solutions courantes:"
    echo "1. Vérifier que PostgreSQL est démarré"
    echo "2. Vérifier les identifiants de la base de données"
    echo "3. Vérifier que la migration schema.sql a été exécutée"
    echo "4. Vérifier les imports manquants dans les classes"
    exit 1
fi
