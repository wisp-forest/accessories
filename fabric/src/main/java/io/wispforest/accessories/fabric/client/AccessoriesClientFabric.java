package io.wispforest.accessories.fabric.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.client.AccessoriesClient;
import io.wispforest.accessories.client.AccessoriesRenderLayer;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.fabric.AccessoriesFabric;
import io.wispforest.accessories.fabric.AccessoriesFabricNetworkHandler;
import io.wispforest.accessories.impl.AccessoriesCapabilityImpl;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import io.wispforest.accessories.networking.base.HandledPacketPayload;
import io.wispforest.accessories.networking.base.PacketBuilderConsumer;
import io.wispforest.endec.Endec;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.CoreShaderRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

import static io.wispforest.accessories.Accessories.MODID;

public class AccessoriesClientFabric implements ClientModInitializer {

    public static KeyMapping OPEN_SCREEN;

    @Override
    public void onInitializeClient() {
        AccessoriesClient.init();

        {
            var afterOthers = Accessories.of("accessories_after_others");

            ItemTooltipCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, afterOthers);

            ItemTooltipCallback.EVENT.register(afterOthers, (stack, tooltipContext, tooltipType, lines) -> {
                var tooltipData = new ArrayList<Component>();

                AccessoriesEventHandler.getTooltipData(Minecraft.getInstance().player, stack, tooltipData, tooltipContext, tooltipType);

                if(!tooltipData.isEmpty()) lines.addAll(1, tooltipData);
            });
        }

        AccessoriesFabricNetworkHandler.INSTANCE.initClient(new PacketBuilderConsumer() {
            @Override
            public <M extends HandledPacketPayload> void accept(Class<M> messageType, Endec<M> endec) {
                ClientPlayNetworking.registerGlobalReceiver(AccessoriesFabricNetworkHandler.INSTANCE.getId(messageType), (packet, context) -> packet.handle(context.player()));
            }
        });

        OPEN_SCREEN = KeyBindingHelper.registerKeyBinding(new KeyMapping(MODID + ".key.open_accessories_screen", GLFW.GLFW_KEY_H, MODID + ".key.category.accessories"));

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            if (OPEN_SCREEN.consumeClick()){
                AccessoriesClient.attemptToOpenScreen(client.player.isShiftKeyDown());
            }
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

            AccessoriesClient.handleConfigChangesSync(Accessories.getConfig());
        });

        CoreShaderRegistrationCallback.EVENT.register(context -> context.register(Accessories.of("fish"), DefaultVertexFormat.BLIT_SCREEN, shaderInstance -> AccessoriesClient.BLIT_SHADER = shaderInstance));
    }
}
