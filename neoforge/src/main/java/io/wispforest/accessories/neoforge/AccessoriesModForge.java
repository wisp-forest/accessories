package io.wispforest.accessories.neoforge;

import io.wispforest.accessories.AccessoriesMod;
import net.neoforged.fml.common.Mod;

@Mod(AccessoriesMod.MODID)
public class AccessoriesModForge {
    public AccessoriesModForge() {
        // Submit our event bus to let architectury register our content on the right time
        AccessoriesMod.init();
    }
}
