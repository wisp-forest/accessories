package io.wispforest.testccessories.fabric;

import io.wispforest.testccessories.fabric.accessories.AppleAccessory;
import io.wispforest.testccessories.fabric.accessories.PointedDripstoneAccessory;
import io.wispforest.testccessories.fabric.accessories.PotatoAccessory;
import io.wispforest.testccessories.fabric.accessories.TntAccessory;
import net.fabricmc.api.ModInitializer;

public class Testccessories implements ModInitializer {
    @Override
    public void onInitialize() {
        AppleAccessory.init();
        PotatoAccessory.init();
        PointedDripstoneAccessory.init();
        TntAccessory.init();
    }
}