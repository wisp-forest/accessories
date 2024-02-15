package io.wispforest.testccessories.fabric.client;

import io.wispforest.testccessories.fabric.Testccessories;
import io.wispforest.testccessories.fabric.accessories.AppleAccessory;
import io.wispforest.testccessories.fabric.accessories.PointedDripstoneAccessory;
import io.wispforest.testccessories.fabric.accessories.PotatoAccessory;
import io.wispforest.testccessories.fabric.accessories.TntAccessory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = Testccessories.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TestccessoriesClient {

    @SubscribeEvent
    public static void onInitializeClient(FMLClientSetupEvent event) {
        AppleAccessory.clientInit();
        PotatoAccessory.clientInit();
        PointedDripstoneAccessory.clientInit();
        TntAccessory.clientInit();

        Testccessories.LOGGER.debug("CLIENT");
        Testccessories.LOGGER.debug("CLIENT");
        Testccessories.LOGGER.debug("CLIENT");
        Testccessories.LOGGER.debug("CLIENT");
        Testccessories.LOGGER.debug("CLIENT");
        Testccessories.LOGGER.debug("CLIENT");
        Testccessories.LOGGER.debug("CLIENT");
        Testccessories.LOGGER.debug("CLIENT");
        Testccessories.LOGGER.debug("CLIENT");
    }
}