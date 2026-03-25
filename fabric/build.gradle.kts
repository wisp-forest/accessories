import io.wispforest.helpers.Utils
import io.wispforest.helpers.Extensions.modrinth
import io.wispforest.helpers.Extensions.modrinthImplementation

plugins {
    id("multiloader-platform")
    id("multiloader-publishing")
    id("com.github.johnrengelman.shadow")
}

dependencies {
    // Core Libs
    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    // --

    // General Libs
    compileOnly(libs.modmenu) { isTransitive = false }
    //--

//    modrinth(this::runtimeOnly, "ok-boomer" to "0.1.3+1.21")
    modrinth(this::runtimeOnly, "sodium" to "${libs.versions.sodium.get()}-fabric")

    // compileOnly(libs.trinkets) // TODO: update trinkets for 26.1
}

repositories {}