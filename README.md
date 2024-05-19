<h1 align="center">
  <img src="https://cdn.modrinth.com/data/jtmvUHXj/14fabf4859e845b0bd6659daf2375be3e88f59ec.png" width=230>
  <br>
  αccessories
  <br>
  <a href="https://modrinth.com/mod/owo-lib">
      <img src="https://img.shields.io/badge/-modrinth-gray?style=for-the-badge&labelColor=green&labelWidth=15&logo=appveyor&logoColor=white">
  </a>
  <br>
  <a href="https://maven.wispforest.io/#/releases/io/wispforest/accessories-fabric">
    <img alt="Maven metadata URL" src="https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fmaven.wispforest.io%2Freleases%2Fio%2Fwispforest%2Faccessories-fabric%2Fmaven-metadata.xml&style=for-the-badge">
  </a>
  <a href="https://discord.gg/xrwHKktV2d">
      <img src="https://img.shields.io/discord/825828008644313089?label=wisp%20forest&logo=discord&logoColor=white&style=for-the-badge">
  </a>
</h1>

## Overview
Accessories is a Data-Driven Accessory mod for NeoForge and Fabric with emphasis on using a Common API for both platforms when possible

<p/>
  
Such API is based on the works of [Curios](https://github.com/TheIllusiveC4/Curios) and [Trinkets](https://github.com/emilyploszaj/trinkets) with credit going to both [TheIllusiveC4](https://github.com/TheIllusiveC4) and [emilyploszaj](https://github.com/emilyploszaj) for their work on Accessory mods for Minecraft.

<p align="center">
  <img width=600 src="https://cdn.modrinth.com/data/jtmvUHXj/images/e40c711b48f2962a31f808c34792ba4f71978ca3.png"/>
</p>

## Build Setup

### Groovy
```groovy
repositories {
    maven { url 'https://maven.wispforest.io' }
    maven { url "https://maven.su5ed.dev/releases" }
    maven { url 'https://maven.fabricmc.net' }
    maven { url 'https://maven.minecraftforge.net' }
}

dependencies {
    // Fabric
    modImplementation("io.wispforest:accessories-fabric:${project.accessories_version}")
    
    // Neoforge 
    fg.deobf("io.wispforest:accessories-neoforge:${project.accessories_version}")

    // Arch Common
    modImplementation("io.wispforest:accessories-common:${project.accessories_version}")
}
```
<details>
<summary><h3>Kotlin DSL</h3></summary>
  
```kotlin
repositories {
    maven("https://maven.wispforest.io")
    maven("https://maven.su5ed.dev/releases")
    maven("https://maven.fabricmc.net")
    maven("https://maven.minecraftforge.net")
}

dependencies {
    // Fabric
    modImplementation("io.wispforest:accessories-fabric:${properties["accessories_version"]}")
    
    // Neoforge 
    fg.deobf("io.wispforest:accessories-neoforge:${properties["accessories_version"]}")

    // Arch Common
    modImplementation("io.wispforest:accessories-common:${properties["accessories_version"]}")
}
```
</details>

## Features
- Compatibility Layers with existing Accessory Mods like [Curios](https://github.com/TheIllusiveC4/Curios) and [Trinkets](https://github.com/emilyploszaj/trinkets)
- Full Support for NBT-based Accessories (More Info on Wiki {TODO: ADD LINK TO SUCH})
- Existing API Events for Piglin Neutral Items, Enderman Masks, Looting Adjustments, Fortune Adjustments, and Snow Walking Ability.
- Unique Slot API for Mod Specific Accessories (More Info on Wiki {TODO: ADD LINK TO SUCH})
