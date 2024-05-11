package io.wispforest.testccessories.neoforge;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.testccessories.neoforge.accessories.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(Testccessories.MODID)
public class Testccessories {

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final String MODID = "testccessories";

    public static MenuType<TestMenu> TEST_MENU_TYPE;

    public Testccessories(IEventBus bus) {
        bus.addListener(Testccessories::onInitialize);

        TestItems.REGISTER.register(bus);
        bus.addListener(Testccessories::registerStuff);
        bus.addListener(TestItems::addToItemGroup);
    }

    public static void registerStuff(RegisterEvent event) {
        event.register(ForgeRegistries.MENU_TYPES.getRegistryKey(), helper -> helper.register(new ResourceLocation(MODID, "test_menu"), new MenuType<>(TestMenu::new, FeatureFlags.DEFAULT_FLAGS)));
        event.register(ForgeRegistries.ITEMS.getRegistryKey(), TestItems::init);
    }

    public static void onInitialize(FMLCommonSetupEvent event){
        AppleAccessory.init();
        PotatoAccessory.init();
        PointedDripstoneAccessory.init();
        TntAccessory.init();
        RingIncreaserAccessory.init();

        UniqueSlotHandling.EVENT.register(UniqueSlotTest.INSTANCE);
    }
}