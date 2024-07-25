package io.wispforest.accessories.neoforge.client;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.client.AccessoriesClient;
import io.wispforest.accessories.client.AccessoriesRenderLayer;
import io.wispforest.accessories.compat.AccessoriesConfig;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.ForgeRegistries;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.util.ArrayList;

import static io.wispforest.accessories.Accessories.MODID;

public class AccessoriesClientForge {

    public static KeyMapping OPEN_SCREEN;

    public AccessoriesClientForge(final IEventBus eventBus) {
        eventBus.addListener(this::onInitializeClient);
        eventBus.addListener(this::registerClientReloadListeners);
        eventBus.addListener(this::initKeybindings);
        eventBus.addListener(this::addRenderLayer);
        eventBus.addListener(this::registerShader);
    }

    public void onInitializeClient(FMLClientSetupEvent event) {
        AccessoriesClient.init();

        MinecraftForge.EVENT_BUS.addListener(AccessoriesClientForge::clientTick);
        MinecraftForge.EVENT_BUS.addListener(AccessoriesClientForge::itemTooltipCallback);

        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory((minecraft, parent) -> AutoConfig.getConfigScreen(AccessoriesConfig.class, parent).get()));
    }

    public void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<Void>() {
            @Override
            protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {return null;}

            @Override
            protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
                AccessoriesRendererRegistry.onReload();
            }
        });
    }

    public void initKeybindings(RegisterKeyMappingsEvent event) {
        OPEN_SCREEN = new KeyMapping(MODID + ".key.open_accessories_screen", GLFW.GLFW_KEY_H, MODID + ".key.category.accessories");

        event.register(OPEN_SCREEN);
    }

    public static void clientTick(TickEvent.ClientTickEvent event) {
        if(!event.phase.equals(TickEvent.Phase.START)) return;

        if (OPEN_SCREEN.consumeClick()) {
            AccessoriesClient.attemptToOpenScreen(Minecraft.getInstance().player.isShiftKeyDown());
        }
    }

    public static void itemTooltipCallback(ItemTooltipEvent event) {
        var player = event.getEntity();

        var stackTooltip = event.getToolTip();

        var tooltipData = new ArrayList<Component>();

        AccessoriesEventHandler.getTooltipData(player, event.getItemStack(), tooltipData, event.getFlags());

        if (!tooltipData.isEmpty()) stackTooltip.addAll(1, tooltipData);
    }

    public void addRenderLayer(EntityRenderersEvent.AddLayers event) {
        for (EntityType<?> entityType : ForgeRegistries.ENTITY_TYPES) {
            try {
                var renderer = event.getRenderer((EntityType<LivingEntity>) entityType);

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
