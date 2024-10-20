package io.wispforest.testccessories.neoforge.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.testccessories.neoforge.Testccessories;
import io.wispforest.testccessories.neoforge.accessories.AppleAccessory;
import io.wispforest.testccessories.neoforge.accessories.PointedDripstoneAccessory;
import io.wispforest.testccessories.neoforge.accessories.PotatoAccessory;
import io.wispforest.testccessories.neoforge.accessories.TntAccessory;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.commands.CommandSourceStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = Testccessories.MODID, dist = Dist.CLIENT)
public class TestccessoriesClient {

    public TestccessoriesClient(IEventBus eventBus) {
        eventBus.addListener(this::onInitializeClient);
    }

    public void onInitializeClient(FMLClientSetupEvent event) {
        AppleAccessory.clientInit();
        PotatoAccessory.clientInit();
        PointedDripstoneAccessory.clientInit();
        TntAccessory.clientInit();

        MenuScreens.register(Testccessories.TEST_MENU_TYPE, TestScreen::new);

        NeoForge.EVENT_BUS.addListener(this::initCommand);

        AccessoriesNetworking.CHANNEL.registerClientbound(TestScreenPacket.class, TestScreenPacket.ENDEC, AccessoriesNetworking.clientHandler(TestScreenPacket::handlePacket));
    }

    public void initCommand(RegisterClientCommandsEvent event){
        var dispatcher = event.getDispatcher();

        dispatcher.register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("open_test_screen")
                        .executes(context -> {
                            AccessoriesNetworking.sendToServer(new TestScreenPacket());

                            return 1;
                        })
        );
    }
}