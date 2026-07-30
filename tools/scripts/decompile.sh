#!/bin/bash
set -euo pipefail

ORIGINAL_JAR=$(ls libs/AdvancedEnchantments-*.jar 2>/dev/null | head -1)
if [ -z "$ORIGINAL_JAR" ]; then
  echo "Error: No AdvancedEnchantments JAR found in libs/" >&2
  exit 1
fi
VERSION=$(basename "$ORIGINAL_JAR" | sed 's/AdvancedEnchantments-//;s/\.jar//')

# 1. Download vineflower if not present
VINEFLOWER_DIR="tools"
VINEFLOWER_JAR="$VINEFLOWER_DIR/vineflower.jar"

if [ ! -f "$VINEFLOWER_JAR" ]; then
  echo "Downloading latest Vineflower from GitHub..."
  mkdir -p "$VINEFLOWER_DIR"

  LATEST_TAG=$(curl -s https://api.github.com/repos/Vineflower/vineflower/releases/latest | jq -r '.tag_name')
  if [ -z "$LATEST_TAG" ] || [ "$LATEST_TAG" = "null" ]; then
    echo "Error: Failed to fetch latest Vineflower version" >&2
    exit 1
  fi

  DOWNLOAD_URL="https://github.com/Vineflower/vineflower/releases/download/${LATEST_TAG}/vineflower-${LATEST_TAG}.jar"
  echo "Downloading Vineflower ${LATEST_TAG}..."
  curl -sL "$DOWNLOAD_URL" -o "$VINEFLOWER_JAR"

  if [ ! -f "$VINEFLOWER_JAR" ]; then
    echo "Error: Failed to download Vineflower" >&2
    exit 1
  fi
  echo "Downloaded Vineflower ${LATEST_TAG}"
fi

# 2. Extract all net/advancedplugins/ classes from JAR
TEMP_DIR="build/temp-extract"
rm -rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR"

echo "Extracting net/advancedplugins/ classes from ${ORIGINAL_JAR}..."
unzip -q "$ORIGINAL_JAR" -d "$TEMP_DIR" "net/advancedplugins/*.class"

# 3. Decompile using vineflower
rm -rf "src-decompiled"
mkdir -p "src-decompiled"

echo "Running Vineflower decompiler..."

JAVA_CMD="java"
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
fi

"$JAVA_CMD" -jar "$VINEFLOWER_JAR" -dgs=1 "$TEMP_DIR" "src-decompiled"

# Strip shaded/relocated library code (not AE source)
rm -rf "src-decompiled/net/advancedplugins/ae/libs"

# 4. Count and report
DECOMPILED_COUNT=$(find "src-decompiled" -name '*.java' | wc -l)
echo "Decompilation successful. $DECOMPILED_COUNT files placed in src-decompiled/"

# Clean up temp dirs
rm -rf "$TEMP_DIR"
