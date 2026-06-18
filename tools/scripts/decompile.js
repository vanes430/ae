import { readFileSync, existsSync, mkdirSync, rmSync, cpSync, readdirSync } from "node:fs";
import { join, dirname } from "node:path";
import { spawnSync } from "bun";

const registryPath = "patch-registry.json";
if (!existsSync(registryPath)) {
  console.error("Error: patch-registry.json not found.");
  process.exit(1);
}

const registry = JSON.parse(readFileSync(registryPath, "utf-8"));

// 1. Create clean temp directory
const tempDir = join("build", "temp-extract");
if (existsSync(tempDir)) {
  rmSync(tempDir, { recursive: true, force: true });
}
mkdirSync(tempDir, { recursive: true });

console.log("Extracting original JAR...");
const extractResult = spawnSync(["jar", "xf", "../../libs/AdvancedEnchantments-9.22.7.jar"], {
  cwd: tempDir
});

if (extractResult.exitCode !== 0) {
  console.error("Failed to extract original jar:", extractResult.stderr.toString());
  process.exit(1);
}

// 2. Collect classes to decompile (including inner classes)
const classesDir = join("build", "temp-classes");
if (existsSync(classesDir)) {
  rmSync(classesDir, { recursive: true, force: true });
}
mkdirSync(classesDir, { recursive: true });

console.log("Locating classes and their inner classes...");
for (const patch of registry.patches) {
  for (const fileRelPath of patch.files) {
    const classRelPath = fileRelPath.replace(/\.java$/, ".class");
    const classDirName = dirname(classRelPath);
    const className = classRelPath.split("/").pop().replace(/\.class$/, "");

    const sourceClassDir = join(tempDir, classDirName);
    const destClassDir = join(classesDir, classDirName);
    mkdirSync(destClassDir, { recursive: true });

    if (!existsSync(sourceClassDir)) {
      console.warn(`Warning: Source directory not found: ${sourceClassDir}`);
      continue;
    }

    // Copy the main class and all matching inner classes
    const filesInDir = readdirSync(sourceClassDir);
    for (const f of filesInDir) {
      if (f === `${className}.class` || f.startsWith(`${className}$`)) {
        const srcFile = join(sourceClassDir, f);
        const destFile = join(destClassDir, f);
        cpSync(srcFile, destFile);
      }
    }
  }
}

// 3. Decompile using vineflower
if (existsSync("src-decompiled")) {
  rmSync("src-decompiled", { recursive: true, force: true });
}
mkdirSync("src-decompiled", { recursive: true });

console.log("Running Vineflower decompiler...");
const vineflowerJar = join("tools", "vineflower-1.11.2.jar");
const decompileResult = spawnSync([
  "java", "-jar", vineflowerJar,
  "-dgs=1", // decompile generic signatures
  classesDir,
  "src-decompiled"
]);

if (decompileResult.exitCode !== 0) {
  console.error("Decompilation failed:", decompileResult.stderr.toString());
  process.exit(1);
}

// Clean up temp dirs
rmSync(tempDir, { recursive: true, force: true });
rmSync(classesDir, { recursive: true, force: true });

console.log("Decompilation successful. decompiled sources placed in src-decompiled/");
