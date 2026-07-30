#!/bin/bash
# Decompile AdvancedEnchantments JAR → src-decompiled/
set -euo pipefail

ORIGINAL_JAR=$(ls libs/AdvancedEnchantments-*.jar 2>/dev/null | head -1)
if [ -z "$ORIGINAL_JAR" ]; then
  echo "Error: No AdvancedEnchantments JAR found in libs/" >&2
  exit 1
fi

# 1. Download Vineflower if not present
VINEFLOWER_DIR="tools"
VINEFLOWER_JAR="$VINEFLOWER_DIR/vineflower.jar"

if [ ! -f "$VINEFLOWER_JAR" ]; then
  LATEST_TAG=$(curl -s https://api.github.com/repos/Vineflower/vineflower/releases/latest | jq -r '.tag_name')
  [ -n "$LATEST_TAG" ] && [ "$LATEST_TAG" != "null" ] || { echo "Error: Failed to fetch latest Vineflower version" >&2; exit 1; }
  echo "Downloading Vineflower ${LATEST_TAG}..."
  curl -sL "https://github.com/Vineflower/vineflower/releases/download/${LATEST_TAG}/vineflower-${LATEST_TAG}.jar" -o "$VINEFLOWER_JAR"
  [ -f "$VINEFLOWER_JAR" ] || { echo "Error: Failed to download Vineflower" >&2; exit 1; }
fi

# 2. Extract AE classes
TEMP_DIR="build/temp-extract"
rm -rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR"
echo "Extracting net/advancedplugins/ classes from ${ORIGINAL_JAR}..."
unzip -q "$ORIGINAL_JAR" -d "$TEMP_DIR" "net/advancedplugins/*.class"

# 3. Decompile
rm -rf "src-decompiled"
mkdir -p "src-decompiled"
JAVA_CMD="${JAVA_HOME:+$JAVA_HOME/bin/java}"
JAVA_CMD="${JAVA_CMD:-java}"
"$JAVA_CMD" -jar "$VINEFLOWER_JAR" -dgs=1 "$TEMP_DIR" "src-decompiled"

# Strip shaded libs
rm -rf "src-decompiled/net/advancedplugins/ae/libs"

# 4. Report
find "src-decompiled" -name '*.java' | wc -l | xargs echo "Decompilation successful. files placed in src-decompiled/"
rm -rf "$TEMP_DIR"
