import { readFileSync, writeFileSync, existsSync, mkdirSync, rmSync } from "node:fs";
import { join } from "node:path";
import { spawnSync } from "bun";

const registryPath = "patch-registry.json";
if (!existsSync(registryPath)) {
  console.error("Error: patch-registry.json not found.");
  process.exit(1);
}

const registry = JSON.parse(readFileSync(registryPath, "utf-8"));

// Clear old patches
if (existsSync("patches")) {
  rmSync("patches", { recursive: true, force: true });
}
mkdirSync("patches");

for (const patch of registry.patches) {
  const { id, name, patchFile, files, issue, fix, risk, excludes } = patch;
  
  console.log(`Generating ${patchFile}...`);
  let fullDiff = `From: Folia Compatibility Patch
Subject: [PATCH ${id}] ${name}: ${fix}

AE PATCH REASON: ${issue}
AE PATCH FIX: ${fix}
AE PATCH RISK: ${risk}
AE PATCH EXCLUDES: ${excludes.join(", ")}

---
`;

  for (const classPath of files) {
    const decompiledFile = join("src-decompiled", classPath).replace(/\\/g, "/");
    const patchedFile = join("src-patched", "main", "java", classPath).replace(/\\/g, "/");
    
    if (!existsSync(decompiledFile)) {
      console.warn(`Warning: Decompiled file missing: ${decompiledFile}`);
      continue;
    }
    if (!existsSync(patchedFile)) {
      console.warn(`Warning: Patched file missing: ${patchedFile}`);
      continue;
    }
    
    // spawnSync is safer for capturing output from commands that return exit code 1 (like git diff)
    const result = spawnSync([
      "git", "diff", "--no-index", 
      "--", 
      decompiledFile, 
      patchedFile
    ]);
    
    const stdout = result.stdout.toString();

    if (stdout.length === 0) {
        console.warn(`No differences found for ${classPath}`);
        continue;
    }

    // Normalize line endings
    let cleanedStdout = stdout.replace(/\r\n/g, "\n");
    
    // Clean headers:
    const lines = cleanedStdout.split("\n");
    const cleanedLines = lines.map(line => {
      if (line.startsWith("--- a/src-decompiled/")) {
        return "--- a/" + line.substring(21);
      }
      if (line.startsWith("+++ b/src-patched/main/java/")) {
        return "+++ b/" + line.substring(28);
      }
      if (line.startsWith("diff --git")) {
          return `diff --git a/${classPath} b/${classPath}`;
      }
      return line;
    });

    fullDiff += cleanedLines.join("\n") + "\n";
  }
  
  writeFileSync(patchFile, fullDiff, { encoding: "utf-8" });
}

console.log("Patch generation complete.");