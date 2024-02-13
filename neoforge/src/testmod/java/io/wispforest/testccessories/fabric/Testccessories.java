package io.wispforest.testccessories.fabric;

import com.mojang.logging.LogUtils;
import io.wispforest.testccessories.fabric.accessories.AppleAccessory;
import io.wispforest.testccessories.fabric.accessories.PointedDripstoneAccessory;
import io.wispforest.testccessories.fabric.accessories.PotatoAccessory;
import io.wispforest.testccessories.fabric.accessories.TntAccessory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;

@Mod(Testccessories.MODID)
public class Testccessories {

    public static final Logger LOGGER = LogUtils.getLogger();

    public static final String MODID = "testccessories";

    public Testccessories(IEventBus bus) {
        bus.addListener(Testccessories::onInitialize);

        LOGGER.debug("BUSSSES");
        LOGGER.debug("BUSSSES");
        LOGGER.debug("BUSSSES");
        LOGGER.debug("BUSSSES");
        LOGGER.debug("BUSSSES");
        LOGGER.debug("BUSSSES");
        LOGGER.debug("BUSSSES");
    }

    public static void onInitialize(FMLCommonSetupEvent event){
        AppleAccessory.init();
        PotatoAccessory.init();
        PointedDripstoneAccessory.init();
        TntAccessory.init();

        LOGGER.debug("WEEE");
        LOGGER.debug("WEEE");
        LOGGER.debug("WEEE");
        LOGGER.debug("WEEE");
        LOGGER.debug("WEEE");
        LOGGER.debug("WEEE");
        LOGGER.debug("WEEE");
        LOGGER.debug("WEEE");
    }
}