import { existsSync, mkdirSync, readdirSync, cpSync, readFileSync } from "node:fs";
import { join } from "node:path";
import { spawn } from "bun";

if (!existsSync("src-decompiled")) {
  console.error("Error: src-decompiled directory not found. Please decompile first.");
  process.exit(1);
}

console.log("Cleaning and copying decompiled sources to src-patched/main/java...");
const targetDir = join("src-patched", "main", "java");
if (existsSync(targetDir)) {
    await spawn(["powershell", "-Command", `Remove-Item -Recurse -Force ${targetDir}`]).exited;
}
mkdirSync(targetDir, { recursive: true });
cpSync("src-decompiled", targetDir, { recursive: true });

if (!existsSync("patches")) {
  console.log("No patches directory found.");
  process.exit(0);
}

const patches = readdirSync("patches")
  .filter(f => f.endsWith(".patch"))
  .sort();

console.log(`Applying ${patches.length} patches...`);

let successCount = 0;
for (const patch of patches) {
  const patchPath = join("patches", patch);
  console.log(`Applying ${patch}...`);
  
  // Use git apply instead of patch command
  const patchProcess = spawn(["git", "apply", "--ignore-whitespace", patchPath], {
    stdout: "inherit",
    stderr: "inherit",
  });
  
  const exitCode = await patchProcess.exited;
  
  if (exitCode !== 0) {
    console.error(`Error: Failed to apply ${patch}`);
    process.exit(1);
  }
  successCount++;
}

console.log(`${successCount} patches applied successfully.`);