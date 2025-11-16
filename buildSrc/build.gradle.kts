plugins {
    `kotlin-dsl`
}

repositories {
    maven("https://maven.fabricmc.net/")
    maven("https://maven.architectury.dev/")
    maven("https://maven.neoforged.net/releases/")
    maven("https://maven.parchmentmc.org")
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("architectury-plugin:architectury-plugin.gradle.plugin:3.4-SNAPSHOT")
    implementation("dev.architectury:architectury-loom:1.11-SNAPSHOT")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.2.2")

    // Required to get Version Catalogs to show in buildSrc
    implementation(files(libs.javaClass.superclass.protectionDomain.codeSource.location))

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.6.2")
}

