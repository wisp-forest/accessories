plugins {
    id("multiloader-base")
    id("multiloader-publishing")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

architectury {
    platformSetupLoomIde()
    fabric {
        platformPackage = "fabric"
    }
}

val common by configurations.creating
val shadowCommon by configurations.creating

configurations {
    common
    shadowCommon // Don't use shadow from the shadow plugin since it *excludes* files.
    compileClasspath { extendsFrom(common) }
    runtimeClasspath { extendsFrom(common) }
    "developmentFabric" { extendsFrom(common) }
}

repositories {
    maven("https://api.modrinth.com/maven")
    maven("https://maven.wispforest.io/releases")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://maven.shedaniel.me/")
    maven("https://maven.architectury.dev/")
    maven("https://maven.terraformersmc.com/")
    maven("https://maven.blamejared.com/") // location of the maven that hosts JEI files since January 2023
    maven("https://modmaven.dev") // location of a maven mirror for JEI files, as a fallback
    maven("https://api.modrinth.com/maven")
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/") {
        content { includeGroup("software.bernie.geckolib") }
    }
    maven("https://maven.ladysnake.org/releases")
    mavenLocal()
}

sourceSets {
    create("testmod") {
        runtimeClasspath += sourceSets["main"].runtimeClasspath
        compileClasspath += sourceSets["main"].compileClasspath
    }
}

dependencies {
    modImplementation("net.fabricmc:fabric-loader:${rootProject.property("fabric_loader_version")}")
    modApi("net.fabricmc.fabric-api:fabric-api:${rootProject.property("fabric_api_version")}")

    "common"(project(":common", "namedElements")) { this.setTransitive(false) }
    "shadowCommon"(project(":common", "transformProductionFabric")) { this.setTransitive(false) }

    //--

    modImplementation("io.wispforest:owo-lib:${project.property("owo_version")}")
    include("io.wispforest:owo-sentinel:${project.property("owo_version")}")

    "testmodImplementation"(sourceSets.main.get().output)

    //--

    modCompileOnly(modLocalRuntime("com.terraformersmc:modmenu:${rootProject.property("modmenu_version")}")!!)

    modCompileOnly("software.bernie.geckolib:geckolib-fabric-${rootProject.property("geckolib_minecraft_version")}:${rootProject.property("geckolib_version")}")

    //modLocalRuntime("maven.modrinth:ok-boomer:0.1.3+1.21")
    //modLocalRuntime("maven.modrinth:sodium:mc1.21-0.6.0-beta.2-fabric")
    modCompileOnly("dev.emi:trinkets:3.10.0")

    val type = rootProject.properties.get("item_viewer_type");

    modCompileOnly("me.shedaniel:RoughlyEnoughItems-api-fabric:${rootProject.property("rei_version")}")
    modCompileOnly("dev.emi:emi-fabric:${rootProject.property("emi_version")}:api")
    modCompileOnly("mezz.jei:jei-${rootProject.property("jei_minecraft_version")}-fabric-api:${rootProject.property("jei_version")}")

    if(type == "rei") {
        modLocalRuntime("me.shedaniel:RoughlyEnoughItems-fabric:${rootProject.property("rei_version")}")
    } else if(type == "emi") {
        modLocalRuntime("dev.emi:emi-fabric:${rootProject.property("emi_version")}")
    } else if(type == "jei") {
        modLocalRuntime("mezz.jei:jei-${rootProject.property("jei_minecraft_version")}-fabric:${rootProject.property("jei_version")}")
    } else if(type != "none") {
        throw IllegalStateException("Unable to locate the given item viewer!")
    }
}

tasks.processResources {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand(Pair("version", project.version))
    }
}

tasks.named<ProcessResources>("processTestmodResources") {
    inputs.property("version", project.version)

    filesMatching("fabric.mod.json") {
        expand(Pair("version", project.version))
    }
}

loom {
    runs {
        create("testmodClient") {
            client()
            ideConfigGenerated(true)
            name("Testmod Client")
            source(sourceSets["testmod"])
            vmArg("-XX:+AllowEnhancedClassRedefinition")
        }
        create("testmodClientRenderDoc") {
            client()
            ideConfigGenerated(true)
            name("Testmod Client - (RenderDoc)")
            source(sourceSets["testmod"])
            vmArg("-Dowo.renderdocPath=${System.getenv("renderDocPath")}")
            vmArg("-XX:+AllowEnhancedClassRedefinition")
        }
        create("testmodServer") {
            server()
            ideConfigGenerated(true)
            name("Testmod Server")
            source(sourceSets["testmod"])
            vmArg("-XX:+AllowEnhancedClassRedefinition")
        }
        create("clientRenderDoc") {
            client()
            ideConfigGenerated(true)
            name("Minecraft Client - (RenderDoc)")
            source(sourceSets["main"])
            vmArg("-Dowo.renderdocPath=${System.getenv("renderDocPath")}")
            vmArg("-XX:+AllowEnhancedClassRedefinition")
        }
        create("clientMixinDebug") {
            client()
            ideConfigGenerated(true)
            name("Minecraft Client - (Mixin Debug)")
            vmArg("-Dfabric.dli.config=${file(".gradle/loom-cache/launch.cfg").toString()}")
            vmArg("-Dfabric.dli.env=client")
            vmArg("-Dfabric.dli.main=net.fabricmc.loader.impl.launch.knot.KnotClient")

            try {
                afterEvaluate {
                    val mixin = this.configurations.compileClasspath.get()
                        .allDependencies
                        .asIterable()
                        .firstOrNull { it.name == "sponge-mixin" }
                    if (mixin != null) {
                        vmArg("-javaagent:\"${this.configurations.compileClasspath.get().files(mixin).first().path}\"")
                        println("[Info]: Mixin Hotswap Run should be working")
                    } else {
                        println("[Warning]: Unable to locate file path for Mixin Jar, HotSwap Run will not work!!!")
                    }
                }
            } catch (e: Exception) {
                println("[Error]: MixinHotswap Run had a issue!")
                e.printStackTrace()
            }

            //vmArg("-Dowo.renderdocPath=C:\\Program Files\\RenderDoc\\renderdoc.dll")

            vmArg("-Dlog4j.configurationFile=${file(".gradle/loom-cache/log4j.xml").toString()}")
            vmArg("-Dfabric.log.disableAnsi=false")
            vmArg("-Dmixin.debug.export=true")
            vmArg("-XX:+AllowEnhancedClassRedefinition")
        }
    }

    accessWidenerPath = file("src/main/resources/accessories-fabric.accesswidener")
}

tasks.shadowJar {
    exclude("architectury.common.json")

    configurations = mutableListOf<FileCollection>(project.configurations["shadowCommon"]);
    archiveClassifier.set("dev-shadow")
}

tasks.remapJar {
    injectAccessWidener = true
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