package io.wispforest.accessories.fabric.client;

import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.client.AccessoriesClient;
import io.wispforest.accessories.client.AccessoriesRenderLayer;
import io.wispforest.accessories.fabric.AccessoriesFabric;
import io.wispforest.accessories.fabric.AccessoriesFabricNetworkHandler;
import io.wispforest.accessories.impl.AccessoriesCapabilityImpl;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import io.wispforest.accessories.networking.server.ScreenOpen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

import static io.wispforest.accessories.Accessories.MODID;

public class AccessoriesClientFabric implements ClientModInitializer {

    public static KeyMapping OPEN_SCREEN;

    @Override
    public void onInitializeClient() {
        AccessoriesClient.init();

        AccessoriesFabricNetworkHandler.INSTANCE.initClient();

        OPEN_SCREEN = KeyBindingHelper.registerKeyBinding(new KeyMapping(MODID + ".key.open_accessories_screen", GLFW.GLFW_KEY_H, MODID + ".key.category.accessories"));

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (OPEN_SCREEN.consumeClick()){
                AccessoriesInternals.getNetworkHandler().sendToServer(new ScreenOpen());
            }
        });

        ItemTooltipCallback.EVENT.register((stack, context, lines) -> {
            var player = Minecraft.getInstance().player;

            if(player == null) return;

            AccessoriesEventHandler.addTooltipInfo(player, stack, lines);
        });

        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if(!(entityRenderer.getModel() instanceof HumanoidModel)) return;

            registrationHelper.register(new AccessoriesRenderLayer<>(entityRenderer));
        });

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            for (EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
                var lookup = AccessoriesFabric.CAPABILITY;

                if(lookup.getProvider(entityType) != null) continue;

                lookup.registerForType((entity, unused) -> {
                    if(!(entity instanceof LivingEntity livingEntity)) return null;

                    var slots = EntitySlotLoader.getEntitySlots(livingEntity);

                    if(slots.isEmpty()) return null;

                    return new AccessoriesCapabilityImpl(livingEntity);
                }, entityType);
            }
        });
    }
}