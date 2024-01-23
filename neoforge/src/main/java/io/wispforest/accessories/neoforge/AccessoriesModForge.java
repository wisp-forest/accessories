package io.wispforest.accessories.neoforge;

import io.wispforest.accessories.Accessories;
import net.neoforged.fml.common.Mod;

@Mod(Accessories.MODID)
public class AccessoriesModForge {
    public AccessoriesModForge() {
        // Submit our event bus to let architectury register our content on the right time
        Accessories.init();
    }
}
