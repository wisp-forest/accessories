package io.wispforest.testccessories.neoforge.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.testccessories.neoforge.Testccessories;
import io.wispforest.testccessories.neoforge.accessories.AppleAccessory;
import io.wispforest.testccessories.neoforge.accessories.PointedDripstoneAccessory;
import io.wispforest.testccessories.neoforge.accessories.PotatoAccessory;
import io.wispforest.testccessories.neoforge.accessories.TntAccessory;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

import static io.wispforest.accessories.Accessories.MODID;

@EventBusSubscriber(modid = Testccessories.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class TestccessoriesClient {

    @SubscribeEvent
    public static void onInitializeClient(FMLClientSetupEvent event) {
        AppleAccessory.clientInit();
        PotatoAccessory.clientInit();
        PointedDripstoneAccessory.clientInit();
        TntAccessory.clientInit();

        MenuScreens.register(Testccessories.TEST_MENU_TYPE, TestScreen::new);

        NeoForge.EVENT_BUS.addListener(TestccessoriesClient::initCommand);
    }

    public static void initCommand(RegisterClientCommandsEvent event){
        var dispatcher = event.getDispatcher();

        dispatcher.register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("open_test_screen")
                        .executes(context -> {
                            AccessoriesInternals.getNetworkHandler().sendToServer(new TestScreenPacket());

                            return 1;
                        })
        );
    }
}