package io.wispforest.accessories.client;

import com.mojang.blaze3d.platform.Window;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.client.gui.ScreenVariantSelectionScreen;
import io.wispforest.accessories.compat.AccessoriesConfig;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.menu.AccessoriesMenuVariant;
import io.wispforest.accessories.networking.holder.HolderProperty;
import io.wispforest.accessories.networking.holder.SyncHolderChange;
import io.wispforest.accessories.networking.server.ScreenOpen;
import io.wispforest.accessories.menu.AccessoriesMenuTypes;
import io.wispforest.owo.shader.GlProgram;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.EntityHitResult;

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
    }

    private static void handleConfigLoad(AccessoriesConfig config) {
        var currentPlayer = Minecraft.getInstance().player;

        if(currentPlayer == null || Minecraft.getInstance().level == null) return;

        var holder = currentPlayer.accessoriesHolder();

        if(holder == null) return;

        if(holder.showUniqueSlots() && !config.clientData.showUniqueRendering) {
            AccessoriesInternals.getNetworkHandler().sendToServer(SyncHolderChange.of(HolderProperty.UNIQUE_PROP, false));
        }

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
}
