package io.wispforest.accessories.neoforge.client;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.client.AccessoriesClient;
import io.wispforest.accessories.client.AccessoriesRenderLayer;
import io.wispforest.accessories.compat.AccessoriesConfig;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.gui.ConfigScreenProvider;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;

import static io.wispforest.accessories.Accessories.MODID;

@Mod(value = Accessories.MODID, dist = Dist.CLIENT)
public class AccessoriesClientForge {

    public static KeyMapping OPEN_SCREEN;

    public AccessoriesClientForge(final IEventBus eventBus) {
        eventBus.addListener(this::onInitializeClient);
        eventBus.addListener(this::registerClientReloadListeners);
        eventBus.addListener(this::initKeybindings);
        eventBus.addListener(this::addRenderLayer);
    }

    public void onInitializeClient(FMLClientSetupEvent event) {
        AccessoriesClient.init();

        NeoForge.EVENT_BUS.addListener(AccessoriesClientForge::clientTick);
        NeoForge.EVENT_BUS.addListener(AccessoriesClientForge::itemTooltipCallback);

        ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> {
            return (minecraft, parent) -> AutoConfig.getConfigScreen(AccessoriesConfig.class, parent).get();
        });
    }

    public void registerClientReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SimplePreparableReloadListener<Void>() {
            @Override protected Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) { return null; }
            @Override
            protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
                AccessoriesRendererRegistry.onReload();
            }
        });
    }

    public void initKeybindings(RegisterKeyMappingsEvent event){
        OPEN_SCREEN = new KeyMapping(MODID + ".key.open_accessories_screen", GLFW.GLFW_KEY_H, MODID + ".key.category.accessories");

        event.register(OPEN_SCREEN);
    }

    public static void clientTick(ClientTickEvent.Pre event){
        if (OPEN_SCREEN.consumeClick()){
            AccessoriesClient.attemptToOpenScreen(Minecraft.getInstance().player.isShiftKeyDown());
        }
    }

    public static void itemTooltipCallback(ItemTooltipEvent event){
        var player = event.getEntity();

        if(player == null) return;

        var stackTooltip = event.getToolTip();

        var tooltipData = new ArrayList<Component>();

        AccessoriesEventHandler.addTooltipInfo(player, event.getItemStack(), stackTooltip);

        stackTooltip.addAll(1, tooltipData);
    }

    public void addRenderLayer(EntityRenderersEvent.AddLayers event){
        for (EntityType<? extends Entity> entityType : event.getEntityTypes()) {
            try {
                var renderer = event.getRenderer(entityType);

                if(renderer instanceof LivingEntityRenderer<? extends LivingEntity, ?> livingEntityRenderer && livingEntityRenderer.getModel() instanceof HumanoidModel){
                    livingEntityRenderer.addLayer(new AccessoriesRenderLayer(livingEntityRenderer));
                }
            } catch (ClassCastException ignore){}
        }

        event.getSkins().forEach(model -> {
            var renderer = event.getSkin(model);

            if(renderer instanceof LivingEntityRenderer<? extends LivingEntity, ?> livingEntityRenderer && livingEntityRenderer.getModel() instanceof HumanoidModel){
                livingEntityRenderer.addLayer(new AccessoriesRenderLayer(livingEntityRenderer));
            }
        });
    }
}