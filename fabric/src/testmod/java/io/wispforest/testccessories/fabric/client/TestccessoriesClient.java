package io.wispforest.testccessories.fabric.client;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.testccessories.fabric.Testccessories;
import io.wispforest.testccessories.fabric.accessories.AppleAccessory;
import io.wispforest.testccessories.fabric.accessories.PointedDripstoneAccessory;
import io.wispforest.testccessories.fabric.accessories.PotatoAccessory;
import io.wispforest.testccessories.fabric.accessories.TntAccessory;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.event.registry.RegistryEntryAddedCallback;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Items;

public class TestccessoriesClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AppleAccessory.clientInit();
        PotatoAccessory.clientInit();
        PointedDripstoneAccessory.clientInit();
        TntAccessory.clientInit();

        AccessoriesRendererRegistry.bindItemToEmptyRenderer(Items.BEDROCK);
        AccessoriesRendererRegistry.bindItemToEmptyRenderer(Items.BAMBOO);
        AccessoriesRendererRegistry.bindItemToEmptyRenderer(Items.STICK);

        MenuScreens.register(Testccessories.TEST_MENU_TYPE, TestScreen::new);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(
                    LiteralArgumentBuilder.<FabricClientCommandSource>literal("open_test_screen")
                            .executes(context -> {
                                AccessoriesNetworking.sendToServer(new TestScreenPacket());

                                return 1;
                            })
            );
        });

        BuiltInRegistries.ITEM.forEach(item -> {
            if (item.equals(Items.DIAMOND_BLOCK) || item.equals(Items.GOLD_BLOCK)) {
                AccessoriesRendererRegistry.bindItemToArmorRenderer(item);
            }
        });

        RegistryEntryAddedCallback.event(BuiltInRegistries.ITEM).register((i, location, item) -> AccessoriesRendererRegistry.bindItemToArmorRenderer(item));
    }
}