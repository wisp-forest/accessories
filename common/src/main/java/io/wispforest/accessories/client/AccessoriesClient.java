package io.wispforest.accessories.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.client.gui.ScreenVariantSelectionScreen;
import io.wispforest.accessories.client.gui.components.ComponentUtils;
import io.wispforest.accessories.compat.AccessoriesConfig;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.impl.ExpandedSimpleContainer;
import io.wispforest.accessories.menu.AccessoriesMenuVariant;
import io.wispforest.accessories.mixin.client.AbstractContainerScreenAccessor;
import io.wispforest.accessories.networking.holder.HolderProperty;
import io.wispforest.accessories.networking.holder.SyncHolderChange;
import io.wispforest.accessories.networking.server.ScreenOpen;
import io.wispforest.accessories.menu.AccessoriesMenuTypes;
import io.wispforest.owo.mixin.itemgroup.CreativeInventoryScreenMixin;
import io.wispforest.owo.shader.GlProgram;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Positioning;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.layers.Layer;
import io.wispforest.owo.ui.layers.Layers;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

import java.util.Optional;
import java.util.function.Consumer;

public class AccessoriesClient {

    public static final ResourceLocation BLIT_SHADER_ID = Accessories.of("fish");
    public static ShaderInstance BLIT_SHADER;
    public static GlProgram SPECTRUM_PROGRAM;

    public static final Event<WindowResizeCallback> WINDOW_RESIZE_CALLBACK_EVENT = EventFactory.createArrayBacked(WindowResizeCallback.class, callbacks -> (client, window) -> {
        for (var callback : callbacks) callback.onResized(client, window);
    });

    public static boolean IS_PLAYER_INVISIBLE = false;

    public static void init(){
        AccessoriesMenuTypes.registerClientMenuConstructors();

        Accessories.CONFIG_HOLDER.registerSaveListener((manager, data) -> {
            handleConfigLoad(data);

            return InteractionResult.SUCCESS;
        });

        Accessories.CONFIG_HOLDER.registerLoadListener((manager, data) -> {
            handleConfigLoad(data);

            return InteractionResult.SUCCESS;
        });

        ClientLifecycleEvents.END_DATA_PACK_RELOAD.register((client, success) -> {
            AccessoriesRendererRegistry.onReload();
        });

        AccessoriesClient.SPECTRUM_PROGRAM = new GlProgram(Accessories.of("spectrum_position_tex"), DefaultVertexFormat.POSITION_TEX_COLOR);

        initLayer();
    }

    private static void handleConfigLoad(AccessoriesConfig config) {
        var currentPlayer = Minecraft.getInstance().player;

        if(currentPlayer == null || Minecraft.getInstance().level == null) return;

        var holder = currentPlayer.accessoriesHolder();

        if(holder == null) return;

        if(holder.equipControl() != config.clientData.equipControl) {
            AccessoriesInternals.getNetworkHandler().sendToServer(SyncHolderChange.of(HolderProperty.EQUIP_CONTROL, config.clientData.equipControl));
        }
    }

    public interface WindowResizeCallback {
        void onResized(Minecraft client, Window window);
    }

    private static boolean displayUnusedSlotWarning = false;

    public static boolean attemptToOpenScreen() {
        return attemptToOpenScreen(false);
    }

    public static boolean attemptToOpenScreen(boolean targetingLookingEntity) {
        return attemptToOpenScreen(targetingLookingEntity, Accessories.getConfig().clientData.selectedScreenType);
    }

    private static boolean attemptToOpenScreen(boolean targetingLookingEntity, AccessoriesConfig.ScreenType screenType) {
        var player = Minecraft.getInstance().player;

        var selectedVariant = AccessoriesMenuVariant.getVariant(screenType);

        if(targetingLookingEntity) {
            var result = ProjectileUtil.getHitResultOnViewVector(player, e -> e instanceof LivingEntity, player.entityInteractionRange());

            var bl = !(result instanceof EntityHitResult entityHitResult) ||
                    !(entityHitResult.getEntity() instanceof LivingEntity living)
                    || EntitySlotLoader.getEntitySlots(living).isEmpty();

            if(bl) return false;
        } else {
            var slots = AccessoriesAPI.getUsedSlotsFor(player);

            var holder = player.accessoriesHolder();

            if(holder == null) return false;

            if(slots.isEmpty() && !holder.showUnusedSlots() && !displayUnusedSlotWarning && !Accessories.getConfig().clientData.disableEmptySlotScreenError) {
                player.displayClientMessage(Component.literal("[Accessories]: No Used Slots found by any mod directly, the screen will show empty unless a item is found to implement slots!"), false);

                displayUnusedSlotWarning = true;
            }
        }

        if(selectedVariant != null) {
            AccessoriesInternals.getNetworkHandler().sendToServer(ScreenOpen.of(targetingLookingEntity, selectedVariant));
        } else {
            Minecraft.getInstance().setScreen(new ScreenVariantSelectionScreen(variant -> {
                AccessoriesInternals.getNetworkHandler().sendToServer(ScreenOpen.of(targetingLookingEntity, variant));
            }));
        }

        return true;
    }



    public static void initLayer() {
        Layers.add(Containers::verticalFlow, instance -> {
            var creativeScreen = instance.screen instanceof CreativeModeInventoryScreen;

            instance.adapter.rootComponent.allowOverflow(true);

            var data = Accessories.getConfig().clientData.screenButtonPositions;

            var xOffset = creativeScreen ? data.creativeInventoryButtonXOffset : data.inventoryButtonXOffset;
            var yOffset = creativeScreen ? data.creativeInventoryButtonYOffset : data.inventoryButtonYOffset;

            var button = (ButtonComponent) Components.button(Component.literal(""), (btn) -> AccessoriesClient.attemptToOpenScreen())
                    .renderer((context, btn, delta) -> {
                        ButtonComponent.Renderer.VANILLA.draw(context, btn, delta);

                        context.push();

                        var groupIcon = Accessories.of("gui/group/misc");

                        var textureAtlasSprite = Minecraft.getInstance()
                                .getTextureAtlas(ResourceLocation.withDefaultNamespace("textures/atlas/gui.png"))
                                .apply(groupIcon);

                        var color = Color.BLACK.interpolate(Color.WHITE, 0.4f);

                        RenderSystem.depthMask(false);

                        context.blit(btn.x() + 2, btn.y() + 2, 2, btn.horizontalSizing().get().value - 4, btn.verticalSizing().get().value - 4, textureAtlasSprite, color.red(), color.green(), color.blue(), 1f);

                        RenderSystem.depthMask(true);

                        context.pop();
                    })
                    .tooltip(Component.translatable(Accessories.translationKey("open.screen")))
                    .margins(Insets.of(1, 0, 0, 1))
                    .sizing(Sizing.fixed(creativeScreen ? 8 : 12));

            if(creativeScreen){
                var extension = ((ComponentUtils.CreativeScreenExtension) instance.screen);

                button.visible = extension.getTab().getType().equals(CreativeModeTab.Type.INVENTORY);

                extension.getEvent().register(tab -> button.visible = tab.getType().equals(CreativeModeTab.Type.INVENTORY));
            }

            instance.adapter.rootComponent.child(button);

            instance.alignComponentToHandledScreenCoordinates(button, xOffset, yOffset);

        }, InventoryScreen.class, CreativeModeInventoryScreen.class);
    }
}
