plugins {
    id("net.neoforged.moddev") version "2.0.112"
    id("maven-publish")
    id("java-library")
}

apply(plugin = "maven-publish")

var targetProject = rootProject.project("neoforge")

version = targetProject.version
group = targetProject.group

repositories {
    maven("https://maven.terraformersmc.com/releases/")
    maven("https://maven.shedaniel.me/")
    maven("https://api.modrinth.com/maven") { content { includeGroup("maven.modrinth") } }
    maven("https://maven.nucleoid.xyz/")
    maven("https://maven.wispforest.io/releases")
    maven("https://maven.su5ed.dev/releases")
    mavenLocal()
}

dependencies {
    implementation("io.wispforest:endec:0.1.8")
    implementation("io.wispforest.endec:netty:0.1.4")
    implementation("io.wispforest.endec:gson:0.1.5")
    implementation("io.wispforest.endec:jankson:0.1.5")

    implementation("blue.endless:jankson:${rootProject.property("jankson_version")}")

    api("org.sinytra.forgified-fabric-api:fabric-api-base:0.4.42+d1308dedd1") { exclude(group = "fabric-api")  }

    implementation("io.wispforest:owo-lib-neoforge:${rootProject.property("owo_neo_version")}")
}

neoForge {
    version = rootProject.property("neoforge_version").toString()

    validateAccessTransformers = true

    accessTransformers {
        from(targetProject.file("src/main/resources/META-INF/accesstransformer.cfg"))
        publish(targetProject.file("src/main/resources/META-INF/accesstransformer.cfg"))
    }

    interfaceInjectionData {
        from(targetProject.file("src/main/resources/interfaces.json"))
        publish(targetProject.file("src/main/resources/interfaces.json"))
    }
}

val targetJavaVersion = 21
tasks.withType<JavaCompile>().configureEach {
    this.options.encoding = "UTF-8"
    if (targetJavaVersion >= 10 || JavaVersion.current().isJava10Compatible()) {
        this.options.release = targetJavaVersion
    }
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
    base.archivesName.set(targetProject.base.archivesName.get())
    withSourcesJar()

    val data: MutableMap<String, Set<PublishArtifact>> = mutableMapOf();

    for (cfg in targetProject.configurations) {
        //if (cfg.name.equals("runtimeElements")) continue;

        with(cfg.artifacts) {
            val publishArtifact = this.filter {
                    publishArtifact -> return@filter publishArtifact.file.name.contains("accessories");
            }.toSet()

            data[cfg.name] = publishArtifact

            if (publishArtifact.isNotEmpty()) this.removeAll(publishArtifact);
        }
    }

    for (cfg in project.configurations) {
        with(cfg.artifacts) {
            this.filter {
                    publishArtifact -> return@filter publishArtifact.file.name.contains("accessories");
            }.toSet().let {
                    publishArtifact -> this.removeAll(publishArtifact)
            }

            data[cfg.name]?.let { this.addAll(it) }
        }
    }
}

val ENV = System.getenv()

publishing {
    publications {
        create<MavenPublication>("mavenCommon") {
            this.from(components["java"])

            val name = targetProject.name

            artifactId = "${rootProject.property("archives_base_name")}${(if (name.isEmpty()) "" else "-${name.replace("-mojmap", "")}")}"
        }
    }

    var mavenUrl = ENV["MAVEN_URL"]
    var mavenUser = ENV["MAVEN_USER"]
    var mavenPassword = ENV["MAVEN_PASSWORD"]

    if (mavenUrl != null && mavenUser != null && mavenPassword != null) {
        repositories {
            maven {
                url = uri(mavenUrl)
                credentials {
                    username = mavenUser
                    password = mavenPassword
                }
            }
        }
    }
}