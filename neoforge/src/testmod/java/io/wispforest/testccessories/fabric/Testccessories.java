package io.wispforest.testccessories.fabric;

import io.wispforest.testccessories.fabric.accessories.AppleAccessory;
import io.wispforest.testccessories.fabric.accessories.PointedDripstoneAccessory;
import io.wispforest.testccessories.fabric.accessories.PotatoAccessory;
import io.wispforest.testccessories.fabric.accessories.TntAccessory;
import net.neoforged.fml.common.Mod;

@Mod(Testccessories.MODID)
public class Testccessories {

    public static final String MODID = "testccessories";

    public Testccessories() {
        AppleAccessory.init();
        PotatoAccessory.init();
        PointedDripstoneAccessory.init();
        TntAccessory.init();
    }
}