plugins {
    java
}

group = "net.advancedplugins"
version = "9.22.7-folia"

val originalJar = file("libs/AdvancedEnchantments-9.22.7.jar")
val extractDir = layout.buildDirectory.dir("extracted")
val patchedClassesDir = layout.buildDirectory.dir("classes/java/main")

// ─── Dependencies ───
repositories {
    mavenCentral()
    flatDir { dirs("libs") }
}

dependencies {
    // Server API
    compileOnly(files("libs/canvas-api.jar"))
    compileOnly(files("libs/canvas-server.jar"))
    compileOnly(files("libs/bungeecord-chat.jar"))
    
    // Adventure API (Maven)
    val adventureVersion = "4.17.0"
    compileOnly("net.kyori:adventure-api:$adventureVersion")
    compileOnly("net.kyori:adventure-text-minimessage:$adventureVersion")
    compileOnly("net.kyori:adventure-text-serializer-gson:$adventureVersion")
    compileOnly("net.kyori:adventure-text-serializer-plain:$adventureVersion")
    compileOnly("net.kyori:adventure-key:$adventureVersion")
    compileOnly("net.kyori:examination-api:1.3.0")
    
    compileOnly("org.jetbrains:annotations:24.1.0")

    // Plugin hooks
    compileOnly(files("libs/Vault.jar"))
    compileOnly(files("libs/placeholderapi.jar"))

    // Original AE jar (contains internal classes we reference)
    compileOnly(files(originalJar))

    // Gson & Guava (needed for FancyMessage)
    compileOnly("com.google.code.gson:gson:2.10.1")
    compileOnly("com.google.guava:guava:33.0.0-jre")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
    sourceSets.main {
        java {
            srcDir("src-patched")
            exclude("**/MainCommand.java")
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

// ─── Custom resources override ───
val copyCustomResources = tasks.register<Copy>("copyCustomResources") {
    dependsOn(extractOriginalJar)

    // General extracted resources (EXCEPT armorSets, customWeapons, enchantments.yml)
    val extractedResources = file("src-patched/main/resources/_extracted")
    if (extractedResources.exists()) {
        from(extractedResources) {
            exclude("armorSets/**")
            exclude("customWeapons/**")
            exclude("enchantments.yml")
        }
        into(extractDir)
    }

    // Our edited enchantments.yml override
    val editedEnchantments = file("resources/overrides/enchantments.yml")
    if (editedEnchantments.exists()) {
        from(editedEnchantments)
        into(extractDir)
    }

    // ArmorSets override (overwrite original armorSets in JAR)
    val armorSetsDir = file("resources/overrides/armorSets")
    if (armorSetsDir.exists()) {
        from(armorSetsDir)
        into(file("${extractDir.get()}/armorSets"))
    }

    // Custom Weapons override (overwrite original customWeapons in JAR)
    val customWeaponsDir = file("resources/overrides/customWeapons")
    if (customWeaponsDir.exists()) {
        from(customWeaponsDir)
        into(file("${extractDir.get()}/customWeapons"))
    }
}

// ─── Standard Jar Task (Patched) ───
tasks.named<Jar>("jar") {
    dependsOn(extractOriginalJar, copyCustomResources)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    archiveFileName.set("AdvancedEnchantments-9.22.7-folia-patched.jar")

    // Layer 1: Original extracted JAR (exclude classes + resources we're overriding)
    from(extractDir) {
        val registryFile = file("patch-registry.json")
        if (registryFile.exists()) {
            @Suppress("UNCHECKED_CAST")
            val registry = groovy.json.JsonSlurper().parse(registryFile) as Map<String, Any>
            @Suppress("UNCHECKED_CAST")
            val patches = registry["patches"] as List<Map<String, Any>>
            val allExcludes = patches.flatMap { 
                @Suppress("UNCHECKED_CAST")
                it["excludes"] as List<String> 
            }
            allExcludes.forEach { exclude(it) }
        }

        exclude("net/advancedplugins/ae/features/weapons/AdvancedWeapon.class")
        // Exclude original armorSets and customWeapons (we override entirely)
        exclude("armorSets/**")
        exclude("customWeapons/**")
        exclude("enchantments.yml")
    }

    // Layer 2: Patched classes (from main sourceSet)
    from(patchedClassesDir)

    // Layer 3: Our overrides
    from("resources/overrides/armorSets") { into("armorSets") }
    from("resources/overrides/customWeapons") { into("customWeapons") }
    from("resources/overrides/enchantments.yml") { into("") }
    from("resources/overrides/tools") { into("tools") }

    // Exclude original META-INF
    exclude("META-INF/**")
}

// ─── Deploy ───
val pluginsDir = "/var/lib/pterodactyl/volumes/876535db-74d8-415c-bc52-21157613398a/plugins"
tasks.register<Copy>("deploy") {
    dependsOn(tasks.jar)

    from(layout.buildDirectory.file("libs/AdvancedEnchantments-9.22.7-folia-patched.jar"))
    into(pluginsDir)
    rename { "AdvancedEnchantments-9.22.7.jar" }

    doLast {
        val deployedFile = file("$pluginsDir/AdvancedEnchantments-9.22.7.jar")
        if (deployedFile.exists()) {
            println("✅ Deployed to ${deployedFile.absolutePath}")
            println("   Size: ${deployedFile.length()} bytes")
        }
    }
}
