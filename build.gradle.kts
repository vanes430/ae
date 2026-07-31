plugins {
    java
}

group = "net.advancedplugins"

// Auto-detect version from libs/AdvancedEnchantments-*.jar
val aeJars = file("libs").listFiles { f -> f.name.matches(Regex("AdvancedEnchantments-\\d+\\.\\d+\\.\\d+\\.jar")) }?.toList()
    ?: error("libs/ directory not found")
require(aeJars.size == 1) { "Expected 1 AdvancedEnchantments JAR in libs/, found ${aeJars.size}: ${aeJars.map { it.name }}" }
val originalJar = aeJars[0]
val detectedVersion = originalJar.name.removePrefix("AdvancedEnchantments-").removeSuffix(".jar")
version = detectedVersion

val extractDir = layout.buildDirectory.dir("extracted")
val patchedClassesDir = layout.buildDirectory.dir("classes/java/main")

// ─── Dependencies ───
repositories {
    flatDir { dirs("libs") }
}

dependencies {
    // Server API (Paper/Bukkit)
    compileOnly(files("libs/canvas-api.jar"))
    compileOnly(files("libs/bungeecord-chat.jar"))
    
    // Adventure API (highest available versions)
    compileOnly(files(
        "libs/adventure-api-4.26.1.jar",
        "libs/adventure-key-4.26.1.jar",
        "libs/adventure-nbt-4.26.1.jar",
        "libs/adventure-text-minimessage-4.26.1.jar",
        "libs/adventure-text-serializer-gson-4.26.1.jar",
        "libs/adventure-text-serializer-json-4.26.1.jar",
        "libs/adventure-text-serializer-plain-4.26.1.jar",
        "libs/adventure-text-serializer-legacy-4.26.1.jar",
        "libs/examination-api-1.3.0.jar",
    ))
    
    compileOnly(files("libs/annotations-24.1.0.jar"))

    // Plugin hooks
    compileOnly(files("libs/Vault.jar"))
    compileOnly(files("libs/placeholderapi.jar"))

    // Original AE jar (contains internal classes we reference)
    compileOnly(files(originalJar))

    // Gson & Guava (needed for FancyMessage)
    compileOnly(files("libs/gson-2.10.1.jar"))
    compileOnly(files("libs/guava-33.0.0-jre.jar"))
}

// Derive patched file list from patches/ headers
val patchedFiles = file("patches").listFiles()
    ?.filter { it.extension == "patch" }
    ?.mapNotNull { patch ->
        patch.useLines { lines ->
            lines.firstOrNull { it.startsWith("diff --git") }
                ?.substringAfter(" b/")
                ?.trim()
        }
    }
    ?.distinct()
    ?: emptyList()

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))

    sourceSets.main {
        java {
            srcDir("src-patched")
            patchedFiles.forEach { include(it) }
            // MainCommand now patched — compiled from src-patched
        }
        resources {
            srcDir("src-patched/main/resources")
            exclude("_extracted/**")
        }
    }
}

// ─── Compile Java ───
tasks.named<JavaCompile>("compileJava") {
    options.encoding = "UTF-8"
}

// ─── Extract original JAR ───
val extractOriginalJar = tasks.register<Copy>("extractOriginalJar") {
    from(zipTree(originalJar))
    into(extractDir)
}

// ─── Inject folia-supported: true into plugin.yml ───
val injectFoliaSupport = tasks.register<Exec>("injectFoliaSupport") {
    dependsOn(extractOriginalJar)
    val script = file("tools/scripts/inject-folia-support.sh")
    val pluginYml = file("${extractDir.get()}/plugin.yml")
    commandLine("bash", script.absolutePath, pluginYml.absolutePath)
}

// ─── Standard Jar Task (Patched) ───
tasks.named<Jar>("jar") {
    dependsOn(extractOriginalJar, injectFoliaSupport)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    archiveFileName.set("AdvancedEnchantments-${detectedVersion}-folia-patched.jar")

    // Layer 1: Original JAR minus patched classes (replaced by compiled versions)
    from(extractDir) {
        patchedFiles.forEach {
            val classPath = it.replace(".java", ".class")
            exclude(classPath)
            exclude(classPath.replace(".class", "\$*.class"))
        }
    }

    // Layer 2: Patched classes (compiled versions override originals)
    from(patchedClassesDir)

    // Layer 3: Resource overrides
    from("resources")

    // Post-process: re-pack with `jar cf` so `file` command
    // detects as "Java archive data (JAR)" not "Zip archive data".
    // Gradle shadowJar uses ZIP data descriptors that confuse libmagic.
    doLast {
        val jarFile = archiveFile.get().asFile
        val tmpDir = file(temporaryDir).resolve("jarfix")
        tmpDir.deleteRecursively()
        tmpDir.mkdirs()
        copy {
            from(zipTree(jarFile))
            into(tmpDir)
        }
        val cmd = arrayOf("jar", "cf", jarFile.absolutePath, "-C", tmpDir.absolutePath, ".")
        val proc = ProcessBuilder(*cmd).inheritIO().start()
        val exit = proc.waitFor()
        if (exit != 0) {
            throw GradleException("jar cf repack failed (exit $exit)")
        }
        tmpDir.deleteRecursively()
    }
}

// ─── Deploy ───
val pluginsDir = "/var/lib/pterodactyl/volumes/d64a444d-cbe1-44eb-99f6-1aa116292bef/plugins"
tasks.register<Copy>("deploy") {
    from(layout.buildDirectory.file("libs/AdvancedEnchantments-${detectedVersion}-folia-patched.jar"))
    into(pluginsDir)
    rename { "AdvancedEnchantments-${detectedVersion}-folia-patched.jar" }

    doLast {
        val deployedFile = file("$pluginsDir/AdvancedEnchantments-${detectedVersion}-folia-patched.jar")
        if (deployedFile.exists()) {
            println("✅ Deployed to ${deployedFile.absolutePath}")
            println("   Size: ${deployedFile.length()} bytes")
        }
    }
}
