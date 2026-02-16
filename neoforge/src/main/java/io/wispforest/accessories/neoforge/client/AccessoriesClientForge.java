package io.wispforest.accessories.neoforge.client;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.client.AccessoriesClient;
import io.wispforest.accessories.client.AccessoriesPipelines;
import io.wispforest.accessories.client.AccessoriesRenderLayer;
import io.wispforest.accessories.data.api.SyncedDataHelperManager;
import io.wispforest.accessories.impl.event.AccessoriesEventHandler;
import io.wispforest.accessories.menu.AccessoriesMenuTypes;
import io.wispforest.accessories.neoforge.AccessoriesNeoforgeInternals;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.component.TooltipDisplay;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.ClientTooltipFlag;
import net.neoforged.neoforge.client.event.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.stream.Stream;

@Mod(value = Accessories.MODID, dist = Dist.CLIENT)
public class AccessoriesClientForge {

    public AccessoriesClientForge(final IEventBus eventBus) {
        eventBus.addListener(this::registerMenuType);
        eventBus.addListener(this::onInitializeClient);
        eventBus.addListener(this::initKeybindings);
        eventBus.addListener(this::addRenderLayer);
        eventBus.addListener(this::registerReloadListeners);
        NeoForge.EVENT_BUS.addListener(this::onJoin);
        eventBus.<RegisterRenderPipelinesEvent>addListener(event -> AccessoriesPipelines.registerPipelines(event::registerPipeline));

        AccessoriesClient.initConfigStuff();
    }

    public void registerReloadListeners(AddClientReloadListenersEvent event){
        var loaders = AccessoriesNeoforgeInternals.TO_BE_LOADED.getOrDefault(PackType.CLIENT_RESOURCES, new LinkedHashSet<>());

        loaders.forEach((endecDataLoader) -> event.addListener(endecDataLoader.getId(), endecDataLoader));

        loaders.forEach((endecDataLoader) -> {
            for (var dependencyId : endecDataLoader.getDependencyIds()) {
                event.addDependency(dependencyId, endecDataLoader.getId());
            }
        });
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
        SyncedDataHelperManager.initClient(AccessoriesNetworking.CHANNEL);
    }

    public void initKeybindings(RegisterKeyMappingsEvent event) {
        event.register(AccessoriesClient.OPEN_SCREEN);
    }

    public static void clientTick(ClientTickEvent.Pre event) {
        AccessoriesClient.handleKeyMappings(Minecraft.getInstance());
    }

    public static void itemTooltipCallback(ItemTooltipEvent event) {
        var player = event.getEntity();
        var stackTooltip = event.getToolTip();
        var stack = event.getItemStack();
        var tooltipDisplay = stack.getOrDefault(DataComponents.TOOLTIP_DISPLAY, TooltipDisplay.DEFAULT);

        var tooltipData = new ArrayList<Component>();

        AccessoriesEventHandler.getTooltipData(player, stack, tooltipData, tooltipDisplay,  event.getContext(), event.getFlags());

        if (!tooltipData.isEmpty()) stackTooltip.addAll(1, tooltipData);
    }

    public void addRenderLayer(EntityRenderersEvent.AddLayers event) {
        for (var entityType : event.getEntityTypes()) {
            try {
                var renderer = event.getRenderer(entityType);

                if (renderer instanceof LivingEntityRenderer<? extends LivingEntity, ? extends LivingEntityRenderState, ?> livingEntityRenderer && livingEntityRenderer.getModel() instanceof HumanoidModel) {
                    livingEntityRenderer.addLayer(new AccessoriesRenderLayer(livingEntityRenderer));
                }
            } catch (ClassCastException ignore) {}
        }

        event.getSkins().stream()
            .flatMap(type -> Stream.<AvatarRenderer<?>>of(event.getPlayerRenderer(type), event.getMannequinRenderer(type)))
            .filter(Objects::nonNull)
            .forEach(renderer -> renderer.addLayer(new AccessoriesRenderLayer<>(renderer)));
    }
}
