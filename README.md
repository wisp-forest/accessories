# Accessories

Accessories is a Data-Driven Accessory mod for NeoForge and Fabric with emphasis on using a Common API for both platforms when possible.

<p/>
  
Such API is based on the works of [Curios](https://github.com/TheIllusiveC4/Curios) and [Trinkets](https://github.com/emilyploszaj/trinkets) with credit going to both [TheIllusiveC4](https://github.com/TheIllusiveC4) and [emilyploszaj](https://github.com/emilyploszaj) for their work on Accessory mods for Minecraft.

<center><img width=600 src="https://cdn.modrinth.com/data/jtmvUHXj/images/225eb5f172da0586a1e4ff184c5345d489032214.png" /></center>

## Build Setup

### Groovy
```groovy
repositories {
    maven { url 'https://maven.wispforest.io' }
}

dependencies {
    // Fabric
    modImplementation("io.wispforest:accessories-fabric:${project.accessories_version}")
    
    // Neoforge 
    fg.deobf("io.wispforest:accessories-neoforge:${project.accessories_version}")
}
```
<details>
<summary><h3>Kotlin DSL</h3></summary>
  
```kotlin
repositories {
    maven("https://maven.wispforest.io")
}

dependencies {
    // Fabric
    modImplementation("io.wispforest:accessories-fabric:${properties["accessories_version"]}")
    
    // Neoforge 
    fg.deobf("io.wispforest:accessories-neoforge:${properties["accessories_version"]}")
}
```
</details>

## Features
- Compatibility Layers with existing Accessory Mods like [Curios](https://github.com/TheIllusiveC4/Curios) and [Trinkets](https://github.com/emilyploszaj/trinkets)
- Full Support for NBT-based Accessories (More Info on Wiki {TODO: ADD LINK TO SUCH})
- Existing API Events for Piglin Neutral Items, Enderman Masks, Looting Adjustments, Fortune Adjustments, and Snow Walking Ability.
- Unique Slot API for Mod Specific Accessories (More Info on Wiki {TODO: ADD LINK TO SUCH})
