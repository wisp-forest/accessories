package io.wispforest.accessories.fabric;

import io.wispforest.accessories.AccessoriesMod;
import net.fabricmc.api.ModInitializer;

public class AccessoriesModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        AccessoriesMod.init();
    }
}
