// ── CantStopWinning — per-version build (applied to each Stonecutter node) ──────────
// Mirrors Anvil's build: Stonecutter 0.9.6 + loom-back-compat 0.3, uniform Mojang Mappings.
// `sc` is the Stonecutter node extension, `loomx` is loom-back-compat.

plugins {
    id("dev.kikugie.loom-back-compat")
}

// DO NOT set group = ...!  (loom-back-compat manages it.)
// Jar name: <id>-<mc>-<modversion>.jar  e.g. cantstopwinning-1.21.11-1.2.0.jar
version = "${sc.current.version}-${property("mod.version")}"
base.archivesName = property("mod.id") as String

// Java level required by the active Minecraft version (26.2+ → 25, 1.20.5+ → 21).
val requiredJava: JavaVersion = when {
    sc.current.parsed >= "26.2" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    else -> JavaVersion.VERSION_17
}

dependencies {
    /** Pull a Fabric API module at the active version's fapi build. */
    fun fapi(vararg modules: String) {
        for (m in modules) modImplementation(fabricApi.module(m, sc.properties["deps.fabric_api"]))
    }

    minecraft("com.mojang:minecraft:${sc.current.version}")
    // Mojang Mappings everywhere — same world as Anvil, so its API resolves directly.
    loomx.applyMojangMappings()

    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")

    // Anvil, consumed from the composite build. Gradle substitutes `Anvil:<node>` with the matching
    // Anvil Stonecutter subproject (group defaults to Anvil's rootProject.name, name = node version).
    // `include` nests Anvil's remapped jar inside the CSW jar (jar-in-jar) so the release is
    // self-contained — Anvil is private and not published separately.
    modImplementation("Anvil:${sc.current.version}:${property("mod.version")}+${sc.current.version}")
    include("Anvil:${sc.current.version}:${property("mod.version")}+${sc.current.version}")

    // CSW needs FabricClientCommandSource for the /csw Brigadier tree (Anvil's registerRaw escape
    // hatch), and ItemTooltipCallback for reading Bazaar prices off item tooltips.
    fapi("fabric-command-api-v2", "fabric-item-api-v1")
    // Touching mc.player (LocalPlayer) on 26.x needs these on the compile classpath — Fabric
    // injects interfaces from both modules into LocalPlayer/Entity (PacketContextProvider,
    // EntityLoadData), and 26.x's stricter compile checking chokes without them present even
    // though we never call into them directly. Referenced from PendingPresetImports (both versions).
    fapi("fabric-networking-api-v1", "fabric-lifecycle-events-v1")
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json")

    runConfigs.all {
        preferGradleTask = true
        generateRunConfig = true
        runDirectory = rootProject.file("run")
    }
}

java {
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava

    toolchain {
        vendor = JvmVendorSpec.ADOPTIUM
        languageVersion = JavaLanguageVersion.of(requiredJava.majorVersion)
    }
}

// CswConfigScreen uses GuiGraphics, which 26.x removed. Compile it only on 1.21.11; on 26.x the
// /csw command falls back to Anvil's generated screen (see CswCommands.buildScreen toggle).
if (sc.current.parsed >= "26.2") {
    sourceSets.named("main") {
        java { exclude("**/CswConfigScreen.java") }
    }
}

tasks {
    processResources {
        fun MutableMap<String, String>.register(key: String, property: String) {
            val value: String = sc.properties[property]
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("id", "mod.id")
            register("name", "mod.name")
            register("version", "mod.version")
            register("minecraft", "mod.mc_compat")
        }

        filesMatching("fabric.mod.json") { expand(props) }
    }
}
