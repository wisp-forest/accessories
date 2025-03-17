package io.wispforest.testccessories.neoforge;

import com.mojang.logging.LogUtils;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.testccessories.neoforge.accessories.*;
import io.wispforest.testccessories.neoforge.client.TestScreenPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import org.slf4j.Logger;

@Mod(Testccessories.MODID)
public class Testccessories {

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final String MODID = "testccessories";

    public static MenuType<TestMenu> TEST_MENU_TYPE;

    public static IEventBus EVENT_BUS;

    public Testccessories(IEventBus bus) {
        EVENT_BUS = bus;
        bus.addListener(Testccessories::onInitialize);

        TestItems.REGISTER.register(bus);
        bus.addListener(Testccessories::registerStuff);
        bus.addListener(TestItems::addToItemGroup);
    }

    public void commonInit(FMLCommonSetupEvent event) {
        AccessoriesNetworking.CHANNEL.registerClientboundDeferred(TestScreenPacket.class, TestScreenPacket.ENDEC);
    }

    public static void registerStuff(RegisterEvent event) {
        event.register(Registries.MENU, helper -> {
            TEST_MENU_TYPE = new MenuType<>(TestMenu::new, FeatureFlags.DEFAULT_FLAGS);

            helper.register(of("test_menu"), TEST_MENU_TYPE);
        });
        event.register(Registries.ITEM, TestItems::init);
    }

    public static void onInitialize(FMLCommonSetupEvent event){
        AppleAccessory.init();
        PotatoAccessory.init();
        PointedDripstoneAccessory.init();
        TntAccessory.init();
        RingIncreaserAccessory.init();

        UniqueSlotHandling.EVENT.register(UniqueSlotTest.INSTANCE);
    }

    public static ResourceLocation of(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}