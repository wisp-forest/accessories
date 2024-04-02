package io.wispforest.testccessories.fabric;

import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.testccessories.fabric.accessories.AppleAccessory;
import io.wispforest.testccessories.fabric.accessories.PointedDripstoneAccessory;
import io.wispforest.testccessories.fabric.accessories.PotatoAccessory;
import io.wispforest.testccessories.fabric.accessories.TntAccessory;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public class Testccessories implements ModInitializer {

    public static final String MODID = "testccessories";

    public static MenuType<TestMenu> TEST_MENU_TYPE;

    @Override
    public void onInitialize() {
        TEST_MENU_TYPE = Registry.register(BuiltInRegistries.MENU, new ResourceLocation(MODID, "test_menu"), new MenuType<>(TestMenu::new, FeatureFlags.DEFAULT_FLAGS));

        AppleAccessory.init();
        PotatoAccessory.init();
        PointedDripstoneAccessory.init();
        TntAccessory.init();

        UniqueSlotHandling.EVENT.register(UniqueSlotTest.INSTANCE);

        TestItems.init();
    }
}