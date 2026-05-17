import { existsSync, readFileSync } from "node:fs";
import { join } from "node:path";

console.log("Verifying patches...");

const registryPath = "patch-registry.json";
if (!existsSync(registryPath)) {
  console.error("Error: patch-registry.json not found.");
  process.exit(1);
}

const registry = JSON.parse(readFileSync(registryPath, "utf-8"));
let failed = false;

for (const patch of registry.patches) {
  const { id, name, patchFile, classPath } = patch;
  const patchedFile = join("src-patched", "main", "java", classPath);
  
  let status = "✅";
  let reason = "";
  
  if (!existsSync(patchFile)) {
    status = "❌";
    reason += ` Patch file ${patchFile} missing.`;
  }
  
  if (!existsSync(patchedFile)) {
    status = "❌";
    reason += ` Class file ${patchedFile} missing.`;
  }
  
  if (status === "❌") {
    failed = true;
  }
  
  console.log(`${status} [${id}] ${name}${reason}`);
}

if (failed) {
  console.error("Patch verification failed.");
  process.exit(1);
} else {
  console.log("All patches verified successfully.");
}