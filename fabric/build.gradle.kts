import io.wispforest.helpers.Extensions.modrinth
import io.wispforest.helpers.Extensions.modrinthImplementation

plugins {
    id("multiloader-platform")
    id("multiloader-publishing")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    // Core Libs
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)
    // --

    // General Libs
    modCompileOnly(libs.modmenu)
    modLocalRuntime(libs.modmenu)
    //--

//    modrinth(this::modLocalRuntime, "ok-boomer" to "0.1.3+1.21")
//    modrinth(this::modLocalRuntime, "sodium" to "${libs.versions.sodium.get()}-fabric")

    modCompileOnly(libs.trinkets)
}

repositories {}