#!/bin/bash
set -euo pipefail

REGISTRY_PATH="patch-registry.json"
if [ ! -f "$REGISTRY_PATH" ]; then
  echo "Error: patch-registry.json not found." >&2
  exit 1
fi

VERSION=$(jq -r '.version' "$REGISTRY_PATH")
ORIGINAL_JAR="libs/AdvancedEnchantments-${VERSION}.jar"

if [ ! -f "$ORIGINAL_JAR" ]; then
  echo "Error: Original JAR not found: $ORIGINAL_JAR" >&2
  exit 1
fi

# 1. Create clean temp directory
TEMP_DIR="build/temp-extract"
rm -rf "$TEMP_DIR"
mkdir -p "$TEMP_DIR"

echo "Extracting original JAR..."
unzip -q "$ORIGINAL_JAR" -d "$TEMP_DIR"

# 2. Collect classes to decompile (including inner classes)
CLASSES_DIR="build/temp-classes"
rm -rf "$CLASSES_DIR"
mkdir -p "$CLASSES_DIR"

echo "Locating classes and their inner classes..."
num_patches=$(jq '.patches | length' "$REGISTRY_PATH")

for ((i=0; i<num_patches; i++)); do
  patch=$(jq -c ".patches[$i]" "$REGISTRY_PATH")
  num_files=$(echo "$patch" | jq '.files | length')
  
  for ((j=0; j<num_files; j++)); do
    fileRelPath=$(echo "$patch" | jq -r ".files[$j]")
    classRelPath="${fileRelPath%.java}.class"
    classDirName=$(dirname "$classRelPath")
    className=$(basename "$classRelPath" .class)
    
    sourceClassDir="$TEMP_DIR/$classDirName"
    destClassDir="$CLASSES_DIR/$classDirName"
    mkdir -p "$destClassDir"
    
    if [ ! -d "$sourceClassDir" ]; then
      echo "Warning: Source directory not found: $sourceClassDir" >&2
      continue
    fi
    
    # Copy main class and all matching inner classes
    find "$sourceClassDir" -maxdepth 1 \( -name "${className}.class" -o -name "${className}\$*.class" \) -exec cp {} "$destClassDir/" \;
  done
done

# 3. Download vineflower if not present
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

# 4. Decompile using vineflower
if [ -d "src-decompiled" ]; then
  rm -rf "src-decompiled"
fi
mkdir -p "src-decompiled"

echo "Running Vineflower decompiler..."

JAVA_CMD="java"
if [ -n "${JAVA_HOME:-}" ] && [ -x "$JAVA_HOME/bin/java" ]; then
  JAVA_CMD="$JAVA_HOME/bin/java"
fi

"$JAVA_CMD" -jar "$VINEFLOWER_JAR" -dgs=1 "$CLASSES_DIR" "src-decompiled"

# Clean up temp dirs
rm -rf "$TEMP_DIR"
rm -rf "$CLASSES_DIR"

echo "Decompilation successful. Decompiled sources placed in src-decompiled/"
