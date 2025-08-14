plugins {
    id("multiloader-mojmap")
    id("multiloader-publishing")
}

architectury {
    common((rootProject.property("enabled_platforms") as String).split(","))
}

loom {
    accessWidenerPath = file("src/main/resources/accessories.accesswidener")
}

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.shedaniel.me/")
    maven("https://maven.su5ed.dev/releases")
    maven("https://maven.architectury.dev/")
    maven("https://maven.terraformersmc.com/")
    maven("https://maven.blamejared.com/") // location of the maven that hosts JEI files since January 2023
    maven("https://modmaven.dev") // location of a maven mirror for JEI files, as a fallback
    maven("https://api.modrinth.com/maven")
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/") { content { includeGroup("software.bernie.geckolib") } }
    mavenLocal()
}

dependencies {
    modImplementation("net.fabricmc:fabric-loader:${rootProject.property("fabric_loader_version")}")

    //--

    compileOnly(annotationProcessor("io.github.llamalad7:mixinextras-common:0.5.0")!!)

    modApi(annotationProcessor("io.wispforest:owo-lib:${project.property("owo_version")}")!!)

    modCompileOnlyApi(fabricApi.module("fabric-api-base", rootProject.property("fabric_api_version")!!.toString())){
        (this as ModuleDependency).exclude(group = "fabric-api", module = "")
    }

    //--

    modCompileOnly("software.bernie.geckolib:geckolib-fabric-${rootProject.property("geckolib_minecraft_version")}:${rootProject.property("geckolib_version")}")

    modCompileOnly("maven.modrinth:sodium:${rootProject.property("sodium_version")}-fabric")

    modCompileOnly("me.shedaniel:RoughlyEnoughItems-api:${rootProject.property("rei_version")}")
    modCompileOnly("me.shedaniel:RoughlyEnoughItems-default-plugin:${rootProject.property("rei_version")}")

    modCompileOnly("dev.emi:emi-xplat-intermediary:${rootProject.property("emi_version")}:api")

    modCompileOnly("mezz.jei:jei-${rootProject.property("jei_minecraft_version")}-common-api:${rootProject.property("jei_version")}")
}

sourceSets {
    main {
        resources.srcDirs.add(File("src/generated"))
    }
}
