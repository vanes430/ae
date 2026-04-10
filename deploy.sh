#!/bin/bash
# Deploy — copy Gradle-built JAR to server plugins directory
# Or use: ./gradlew deploy

set -e

PLUGINS_DIR="/var/lib/pterodactyl/volumes/876535db-74d8-415c-bc52-21157613398a/plugins"
PATCHED_JAR="build/libs/AdvancedEnchantments-9.22.7-folia-patched.jar"
TARGET_JAR="$PLUGINS_DIR/AdvancedEnchantments-9.22.7.jar"

if [ ! -f "$PATCHED_JAR" ]; then
    echo "ERROR: Patched JAR not found at $PATCHED_JAR"
    echo "Run './gradlew build' first."
    exit 1
fi

echo "Deploying patched AdvancedEnchantments to $PLUGINS_DIR ..."
cp "$PATCHED_JAR" "$TARGET_JAR"
echo "Done. Deployed: $(basename "$TARGET_JAR") ($(stat -c%s "$TARGET_JAR") bytes)"
