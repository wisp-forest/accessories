import io.wispforest.helpers.Extensions.fabricModule
import io.wispforest.helpers.Extensions.modrinth

plugins {
    id("multiloader-mojmap")
    id("multiloader-publishing")
}

dependencies {
    // Core Libs
    compileOnly(libs.mixin.extras.common)
    annotationProcessor(libs.mixin.extras.common)
    // --

    // General Libs
    fabricModule(this::modCompileOnlyApi, "fabric-api-base")
    // --

    modrinth(this::modCompileOnly, "sodium" to "${libs.versions.sodium.get()}-fabric")
}
