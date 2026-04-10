plugins {
    java
}

group = "net.advancedplugins"
version = "9.22.7-folia"

val originalJar = file("libs/AdvancedEnchantments-9.22.7.jar")
val extractDir = layout.buildDirectory.dir("extracted")
val patchedClassesDir = layout.buildDirectory.dir("classes/java/main")

// ─── Dependencies (all from local libs) ───
repositories {
    flatDir { dirs("libs") }
    mavenCentral()
}

dependencies {
    // Server API
    compileOnly(files("libs/canvas-api.jar"))
    compileOnly(files("libs/canvas-server.jar"))
    compileOnly(files("libs/bungeecord-chat.jar"))
    compileOnly(files("libs/adventure-api.jar"))
    compileOnly(files("libs/adventure-key.jar"))
    compileOnly(files("libs/adventure-text-minimessage.jar"))
    compileOnly(files("libs/adventure-text-serializer-gson.jar"))
    compileOnly(files("libs/adventure-text-serializer-plain.jar"))
    compileOnly(files("libs/examination-api.jar"))
    compileOnly(files("libs/jetbrains-annotations.jar"))

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
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    sourceSets.main {
        java {
            srcDir("src-patched")
            exclude("**/MainCommand.java")
        }
    }
}

// ─── Compile stubs first ───
val stubsClassesDir = layout.buildDirectory.dir("classes/stubs")

val compileStubs by tasks.registering(JavaCompile::class) {
    onlyIf { file("stubs-patched").exists() }
    source = fileTree("stubs-patched").matching { include("**/*.java") }
    destinationDirectory.set(stubsClassesDir)
    options.encoding = "UTF-8"
    classpath = files(originalJar) + fileTree("libs")
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(compileStubs)
    options.encoding = "UTF-8"
    doFirst {
        val stubsOut = stubsClassesDir.get().asFile
        if (stubsOut.exists()) {
            classpath = classpath + files(stubsOut)
        }
    }
}

// ─── Extract original JAR ───
val extractOriginalJar = tasks.register<Copy>("extractOriginalJar") {
    from(zipTree(originalJar))
    into(extractDir)
}

// ─── Custom resources override ───
val copyCustomResources = tasks.register<Copy>("copyCustomResources") {
    dependsOn(extractOriginalJar)

    // General extracted resources
    val extractedResources = file("src-patched/main/resources/_extracted")
    if (extractedResources.exists()) {
        from(extractedResources)
        into(extractDir)
    }

    // Dedicated armorSets override (always overwrites original armorSets in JAR)
    val armorSetsDir = file("src-patched/main/resources/armorSets")
    if (armorSetsDir.exists()) {
        from(armorSetsDir)
        into(file("${extractDir.get()}/armorSets"))
    }
}

// ─── Build patched JAR ───
val buildPatchedJar = tasks.register<Jar>("buildPatchedJar") {
    dependsOn(copyCustomResources, tasks.compileJava)

    archiveFileName.set("AdvancedEnchantments-9.22.7-folia-patched.jar")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))

    // Layer 1: Original extracted JAR (exclude classes we're patching)
    from(extractDir) {
        exclude("net/advancedplugins/ae/Core.class")
        exclude("net/advancedplugins/ae/Core\$*.class")
        exclude("net/advancedplugins/ae/handlers/netsharing/MarketInventory.class")
        exclude("net/advancedplugins/ae/handlers/netsharing/MarketInventory\$*.class")
        exclude("net/advancedplugins/ae/globallisteners/listeners/ReloadEvent.class")
        exclude("net/advancedplugins/ae/impl/utils/plugin/UpdateChecker.class")
        exclude("net/advancedplugins/ae/impl/effects/effects/effects/internal/ApplyPotionEffect.class")
        exclude("net/advancedplugins/ae/impl/effects/effects/effects/internal/GuardEffect.class")
        exclude("net/advancedplugins/ae/impl/utils/fanciful/FancyMessage.class")
        exclude("net/advancedplugins/ae/utils/fanciful/FancyMessage.class")
    }

    // Layer 2: Patched classes (no duplicates)
    from(patchedClassesDir)

    // Exclude original META-INF
    exclude("META-INF/**")
}

// ─── Default build ───
tasks.assemble {
    dependsOn(buildPatchedJar)
}

// ─── Deploy ───
val pluginsDir = "/var/lib/pterodactyl/volumes/876535db-74d8-415c-bc52-21157613398a/plugins"
tasks.register<Copy>("deploy") {
    dependsOn(buildPatchedJar)

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
