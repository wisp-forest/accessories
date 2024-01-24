package io.wispforest.accessories.fabric.client;

import io.wispforest.accessories.impl.AccessoriesEvents;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.Minecraft;

public class AccessoriesClientFabric implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            AccessoriesEvents.addTooltipInfo(Minecraft.getInstance().player, stack, lines);
        });
    }
}
