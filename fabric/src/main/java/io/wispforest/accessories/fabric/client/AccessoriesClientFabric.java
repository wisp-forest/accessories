package io.wispforest.accessories.fabric.client;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesAccess;
import io.wispforest.accessories.client.AccessoriesClient;
import io.wispforest.accessories.client.AccessoriesRenderLayer;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import io.wispforest.accessories.networking.server.ScreenOpen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import org.lwjgl.glfw.GLFW;

public class AccessoriesClientFabric implements ClientModInitializer {

    public static KeyMapping OPEN_SCREEN;

    @Override
    public void onInitializeClient() {
        AccessoriesClient.init();

        OPEN_SCREEN = KeyBindingHelper.registerKeyBinding(new KeyMapping("open_accessories_screen", GLFW.GLFW_KEY_H, Accessories.MODID));

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (OPEN_SCREEN.consumeClick()){
                AccessoriesAccess.getHandler().sendToServer(new ScreenOpen());
            }
        });

        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            AccessoriesEventHandler.addTooltipInfo(Minecraft.getInstance().player, stack, lines);
        });

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if(!(entityRenderer.getModel() instanceof HumanoidModel)) return;

            registrationHelper.register(new AccessoriesRenderLayer<>(entityRenderer));
        });


    }
}
