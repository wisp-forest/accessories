package io.wispforest.testccessories.neoforge.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.testccessories.neoforge.Testccessories;
import io.wispforest.testccessories.neoforge.accessories.AppleAccessory;
import io.wispforest.testccessories.neoforge.accessories.PointedDripstoneAccessory;
import io.wispforest.testccessories.neoforge.accessories.PotatoAccessory;
import io.wispforest.testccessories.neoforge.accessories.TntAccessory;
import net.minecraft.client.KeyMapping;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

import static io.wispforest.accessories.Accessories.MODID;

@Mod.EventBusSubscriber(modid = Testccessories.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TestccessoriesClient {

    @SubscribeEvent
    public static void onInitializeClient(FMLClientSetupEvent event) {
        AppleAccessory.clientInit();
        PotatoAccessory.clientInit();
        PointedDripstoneAccessory.clientInit();
        TntAccessory.clientInit();

        Testccessories.LOGGER.info("CLIENT");
        Testccessories.LOGGER.info("CLIENT");
        Testccessories.LOGGER.info("CLIENT");
        Testccessories.LOGGER.info("CLIENT");
        Testccessories.LOGGER.info("CLIENT");
        Testccessories.LOGGER.info("CLIENT");
        Testccessories.LOGGER.info("CLIENT");
        Testccessories.LOGGER.info("CLIENT");
        Testccessories.LOGGER.info("CLIENT");
    }

    @SubscribeEvent
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