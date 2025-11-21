import io.wispforest.helpers.Extensions.modrinth
import io.wispforest.helpers.neoForgeDataFile

plugins {
    id("multiloader-platform")
    id("multiloader-publishing")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    // Core Libs
    neoForge(libs.neoforge)
    // --

    // Neoforge: Required as these are General Libs that are used by owo and can be used but must be added to runtime path due to not being mods
    forgeRuntimeLibrary(libs.endec)
    forgeRuntimeLibrary(libs.endec.netty)
    forgeRuntimeLibrary(libs.endec.gson)
    forgeRuntimeLibrary(libs.endec.jankson)
    forgeRuntimeLibrary(libs.jankson)
    // --

    //modrinth(this::modLocalRuntime, "sodium" to "${libs.versions.sodium.get()}-neoforge")

    modCompileOnly(libs.curios)
}

neoForgeDataFile {
    publishAccessTransformersFile("src/main/resources/META-INF/accesstransformer.cfg")
    publishInterfaceInjectionsFile("src/main/resources/interfaces.json")
}

repositories {}