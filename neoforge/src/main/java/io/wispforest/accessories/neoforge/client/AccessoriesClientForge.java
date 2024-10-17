package io.wispforest.accessories.neoforge.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.client.AccessoriesClient;
import io.wispforest.accessories.client.AccessoriesRenderLayer;
import io.wispforest.accessories.client.gui.AccessoriesScreenBase;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import io.wispforest.accessories.menu.AccessoriesMenuTypes;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.ArrayList;

import static io.wispforest.accessories.Accessories.MODID;

@Mod(value = Accessories.MODID, dist = Dist.CLIENT)
public class AccessoriesClientForge {

    public AccessoriesClientForge(final IEventBus eventBus) {
        eventBus.addListener(this::registerMenuType);
        eventBus.addListener(this::onInitializeClient);
        eventBus.addListener(this::initKeybindings);
        eventBus.addListener(this::addRenderLayer);
        eventBus.addListener(this::registerShader);
        NeoForge.EVENT_BUS.addListener(this::onJoin);

        AccessoriesClient.initConfigStuff();
    }

    public void registerMenuType(RegisterMenuScreensEvent event) {
        AccessoriesMenuTypes.registerClientMenuConstructors(event::register);
    }

    public void onJoin(ClientPlayerNetworkEvent.LoggingIn loggingInEvent) {
        AccessoriesClient.initalConfigDataSync();
    }

    public void onInitializeClient(FMLClientSetupEvent event) {
        NeoForge.EVENT_BUS.addListener(AccessoriesClientForge::clientTick);
        NeoForge.EVENT_BUS.addListener(AccessoriesClientForge::itemTooltipCallback);

//        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> {
//            return (minecraft, parent) -> AutoConfig.getConfigScreen(AccessoriesConfig.class, parent).get();
//        });

        AccessoriesClient.init();

        AccessoriesNetworking.initClient();
    }

    public void initKeybindings(RegisterKeyMappingsEvent event) {
        AccessoriesClient.OPEN_SCREEN = new KeyMapping(MODID + ".key.open_accessories_screen", GLFW.GLFW_KEY_H, MODID + ".key.category.accessories");

        event.register(AccessoriesClient.OPEN_SCREEN);
    }

    public static void clientTick(ClientTickEvent.Pre event) {
        if (AccessoriesClient.OPEN_SCREEN.consumeClick()) {
            var client = Minecraft.getInstance();
            if (client.screen instanceof AccessoriesScreenBase) {
                client.setScreen(null);
            } else {
                AccessoriesClient.attemptToOpenScreen(client.player.isShiftKeyDown());
            }
        }
    }

    public static void itemTooltipCallback(ItemTooltipEvent event) {
        var player = event.getEntity();

        var stackTooltip = event.getToolTip();

        var tooltipData = new ArrayList<Component>();

        AccessoriesEventHandler.getTooltipData(player, event.getItemStack(), tooltipData, event.getContext(), event.getFlags());

        if (!tooltipData.isEmpty()) stackTooltip.addAll(1, tooltipData);
    }

    public void addRenderLayer(EntityRenderersEvent.AddLayers event) {
        for (EntityType<? extends Entity> entityType : event.getEntityTypes()) {
            try {
                var renderer = event.getRenderer(entityType);

                if (renderer instanceof LivingEntityRenderer<? extends LivingEntity, ?> livingEntityRenderer && livingEntityRenderer.getModel() instanceof HumanoidModel) {
                    livingEntityRenderer.addLayer(new AccessoriesRenderLayer(livingEntityRenderer));
                }
            } catch (ClassCastException ignore) {}
        }

        event.getSkins().forEach(model -> {
            var renderer = event.getSkin(model);

            if (renderer instanceof LivingEntityRenderer<? extends LivingEntity, ?> livingEntityRenderer && livingEntityRenderer.getModel() instanceof HumanoidModel) {
                livingEntityRenderer.addLayer(new AccessoriesRenderLayer(livingEntityRenderer));
            }
        });
    }

    public void registerShader(RegisterShadersEvent event) {
        try {
            event.registerShader(new ShaderInstance(
                    event.getResourceProvider(),
                    AccessoriesClient.BLIT_SHADER_ID,
                    DefaultVertexFormat.BLIT_SCREEN
            ), shaderInstance -> AccessoriesClient.BLIT_SHADER = shaderInstance);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
