plugins {
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("multiloader-base")
}

architectury {
    platformSetupLoomIde()
    neoForge {
        platformPackage = "neoforge"
    }
}

val common by configurations.creating
val shadowCommon by configurations.creating

configurations {
    common
    shadowCommon // Don't use shadow from the shadow plugin since it *excludes* files.
    compileClasspath { extendsFrom(common) }
    runtimeClasspath { extendsFrom(common) }
    "developmentNeoForge" { extendsFrom(common) }
}

repositories {
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.su5ed.dev/releases")
    maven("https://maven.shedaniel.me/")
    maven("https://maven.architectury.dev/")
    maven("https://maven.terraformersmc.com/releases")
    maven("https://maven.blamejared.com/") // location of the maven that hosts JEI files since January 2023
    maven("https://modmaven.dev") // location of a maven mirror for JEI files, as a fallback
    maven("https://maven.wispforest.io/releases")
    maven("https://api.modrinth.com/maven")
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/") {
        content { includeGroup("software.bernie.geckolib") }
    }
    mavenLocal()
}

sourceSets {
    create("testmod") {
        runtimeClasspath += sourceSets["main"].runtimeClasspath
        compileClasspath += sourceSets["main"].compileClasspath
    }
}

dependencies {
    neoForge("net.neoforged:neoforge:${rootProject.property("neoforge_version")}")

    "common"(project(":common", "namedElements")) { this.setTransitive(false) }
    "shadowCommon"(project(":common", "transformProductionNeoForge")) { this.setTransitive(false) }

    //--

    modApi("io.wispforest:owo-lib-neoforge:${rootProject.property("owo_neo_version")}")

    //modApi("org.sinytra.forgified-fabric-api:fabric-api-base:0.4.42+d1308dedd1") { exclude group: "fabric-api" }

    forgeRuntimeLibrary(implementation("blue.endless:jankson:1.2.2")!!)

    forgeRuntimeLibrary("io.wispforest:endec:${rootProject.property("endec_version")}")
    forgeRuntimeLibrary("io.wispforest.endec:gson:${rootProject.property("endec_gson_version")}")
    forgeRuntimeLibrary("io.wispforest.endec:jankson:${rootProject.property("endec_jankson_version")}")
    forgeRuntimeLibrary("io.wispforest.endec:netty:${rootProject.property("endec_netty_version")}")

    // Temp fix for issues with needing higher Endec Version
    include("io.wispforest:endec:${rootProject.property("endec_version")}")
    include("io.wispforest.endec:gson:${rootProject.property("endec_gson_version")}")
    include("io.wispforest.endec:jankson:${rootProject.property("endec_jankson_version")}")
    include("io.wispforest.endec:netty:${rootProject.property("endec_netty_version")}")

    "testmodImplementation"(sourceSets.main.get().output)

    //--

    modCompileOnly("software.bernie.geckolib:geckolib-neoforge-${rootProject.property("geckolib_minecraft_version")}:${rootProject.property("geckolib_version")}")

    //modLocalRuntime("maven.modrinth:sodium:mc1.21-0.6.0-beta.2-neoforge")

    val type = rootProject.properties.get("item_viewer_type");

    modCompileOnly("me.shedaniel:RoughlyEnoughItems-api-neoforge:${rootProject.property("rei_version")}")
    modCompileOnly("dev.emi:emi-neoforge:${rootProject.property("emi_version")}:api")
    modCompileOnly("mezz.jei:jei-${rootProject.property("jei_minecraft_version")}-neoforge-api:${rootProject.property("jei_version")}")

    modCompileOnly("top.theillusivec4.curios:curios-neoforge:${rootProject.property("curios_version")}")

    if(type == "rei") {
        modLocalRuntime("me.shedaniel:RoughlyEnoughItems-neoforge:${rootProject.property("rei_version")}")
        modLocalRuntime("dev.architectury:architectury-neoforge:${rootProject.property("arch_api")}")
    } else if(type == "emi") {
        modLocalRuntime("dev.emi:emi-neoforge:${rootProject.property("emi_version")}")
    } else if(type == "jei") {
        modLocalRuntime("mezz.jei:jei-${rootProject.property("jei_minecraft_version")}-neoforge:${rootProject.property("jei_version")}")
    } else if(type != "none") {
        throw IllegalStateException("Unable to locate the given item viewer!")
    }
}

tasks.processResources {
    val finalVersion = "${project.property("mod_version")}+${rootProject.property("minecraft_base_version")}"

    filesMatching("META-INF/neoforge.mods.toml") {
        expand(Pair("version", finalVersion))
    }
    inputs.property("META-INF/neoforge.mods.toml", finalVersion)
}

tasks.named<ProcessResources>("processTestmodResources") {
    filesMatching("META-INF/neoforge.mods.toml") {
        expand(Pair("version", project.version))
    }
    inputs.property("META-INF/neoforge.mods.toml", project.version)
}

loom {
    runs {
        create("testmodClient") {
            client()
            forgeTemplate("client")
            ideConfigGenerated(true)
            name("Testmod Client")
            mods {
                create("testccessories") { sourceSet(sourceSets["testmod"]) }
                create("${project.property("archives_base_name")}"){ sourceSet(sourceSets["main"]) }
            }
            source(sourceSets["testmod"])
        }
        create("testmodServer") {
            server()
            ideConfigGenerated(true)
            name("Testmod Server")
            mods {
                create("testccessories") { sourceSet(sourceSets["testmod"]) }
                create("${project.property("archives_base_name")}"){ sourceSet(sourceSets["main"]) }
            }
            source(sourceSets["testmod"])
        }
    }

    accessWidenerPath = project(":common").loom.accessWidenerPath

    neoForge  {}
}

tasks.shadowJar {
    exclude("fabric.mod.json")
    exclude("architectury.common.json")

    configurations = mutableListOf<FileCollection>(project.configurations["shadowCommon"]);
    archiveClassifier.set("dev-shadow")
}

tasks.remapJar {
    inputFile.set(tasks.shadowJar.get().archiveFile)
    dependsOn(tasks.shadowJar)
    archiveClassifier.set("")
}

tasks.sourcesJar {
    val commonSources = project(":common").tasks.sourcesJar
    dependsOn(commonSources)
    from(commonSources.get().archiveFile.map { zipTree(it) })
}

with(components["java"] as AdhocComponentWithVariants) {
    withVariantsFromConfiguration(configurations["shadowRuntimeElements"]) { skip() }
}