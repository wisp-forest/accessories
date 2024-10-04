package io.wispforest.accessories.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoriesHolder;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.client.gui.ScreenVariantSelectionScreen;
import io.wispforest.accessories.client.gui.components.ComponentUtils;
import io.wispforest.accessories.compat.config.ScreenType;
import io.wispforest.accessories.compat.config.client.ExtendedConfigScreen;
import io.wispforest.accessories.compat.config.client.Structured;
import io.wispforest.accessories.compat.config.client.components.StructListOptionContainer;
import io.wispforest.accessories.compat.config.client.components.StructOptionContainer;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.menu.AccessoriesMenuVariant;
import io.wispforest.accessories.mixin.client.owo.ConfigWrapperAccessor;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.networking.holder.HolderProperty;
import io.wispforest.accessories.networking.holder.SyncHolderChange;
import io.wispforest.accessories.networking.server.ScreenOpen;
import io.wispforest.owo.config.ui.ConfigScreenProviders;
import io.wispforest.owo.config.ui.OptionComponentFactory;
import io.wispforest.owo.config.ui.component.OptionValueProvider;
import io.wispforest.owo.config.ui.component.SearchAnchorComponent;
import io.wispforest.owo.shader.GlProgram;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.core.Color;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.layers.Layers;
import io.wispforest.owo.util.NumberReflection;
import io.wispforest.owo.util.ReflectionUtils;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.phys.EntityHitResult;

import java.util.List;
import java.util.function.Consumer;

public class AccessoriesClient {

    public static final ResourceLocation BLIT_SHADER_ID = Accessories.of("fish");
    public static ShaderInstance BLIT_SHADER;
    public static GlProgram SPECTRUM_PROGRAM;

    public static final Event<WindowResizeCallback> WINDOW_RESIZE_CALLBACK_EVENT = EventFactory.createArrayBacked(WindowResizeCallback.class, callbacks -> (client, window) -> {
        for (var callback : callbacks) callback.onResized(client, window);
    });

    public static boolean IS_PLAYER_INVISIBLE = false;

    public static void initConfigStuff() {
        ConfigScreenProviders.register(
                Accessories.MODID,
                ExtendedConfigScreen.buildFunc(
                        Accessories.config(),
                        (config, factoryRegister) -> {
                            factoryRegister.registerFactory(
                                    option -> {
                                        var field = option.backingField().field();
                                        if (field.getType() != List.class) return false;

                                        var listType = ReflectionUtils.getTypeArgument(field.getGenericType(), 0);
                                        if (listType == null) return false;

                                        return String.class != listType && !NumberReflection.isNumberType(listType);
                                    },
                                    (uiModel, option) -> {
                                        var layout = new StructListOptionContainer<>(uiModel, option);
                                        return new OptionComponentFactory.Result<>(layout, layout);
                                    });

                            var builder = ((ConfigWrapperAccessor) config).accessories$builder();

                            factoryRegister.registerFactory(
                                    option -> option.backingField().field().isAnnotationPresent(Structured.class),
                                    (model, option) -> {
                                        var annotationData = option.backingField().field().getAnnotation(Structured.class);

                                        var title = net.minecraft.network.chat.Component.translatable("text.config." + option.configName() + ".option." + option.key().asString());
                                        var titleLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content());
                                        titleLayout.padding(Insets.of(5, 5, 5, 0));

                                        title = title.copy().withStyle(ChatFormatting.UNDERLINE);
                                        titleLayout.child(Components.label(title));

                                        var component = StructOptionContainer.of(model, option, builder, annotationData.sideBySide());

                                        titleLayout.child(new SearchAnchorComponent(
                                                titleLayout,
                                                option.key(),
                                                () -> I18n.get("text.config." + option.configName() + ".option." + option.key().asString()),
                                                () -> component.parsedValue().toString()
                                        ));

                                        var mainLayout = Containers.verticalFlow(Sizing.content(), Sizing.content());

                                        mainLayout.child(titleLayout)
                                                .child(component);

                                        return new OptionComponentFactory.Result<io.wispforest.owo.ui.core.Component, OptionValueProvider>(mainLayout, component);
                                    });
                        }));

        Accessories.config().clientOptions.subscribeToEquipControl(value -> {
            attemptAction(holder -> {
                if(holder.equipControl() == value) return;

                AccessoriesNetworking.sendToServer(SyncHolderChange.of(HolderProperty.EQUIP_CONTROL, value));
            });
        });

        Accessories.config().screenOptions.subscribeToShowUnusedSlots(value -> {
            attemptAction(holder -> {
                if(holder.showUnusedSlots() == value) return;

                AccessoriesNetworking.sendToServer(SyncHolderChange.of(HolderProperty.UNUSED_PROP, value));
            });
        });
    }

    public static void init(){
        ClientLifecycleEvents.END_DATA_PACK_RELOAD.register((client, success) -> {
            AccessoriesRendererRegistry.onReload();
        });

        AccessoriesClient.SPECTRUM_PROGRAM = new GlProgram(Accessories.of("spectrum_position_tex"), DefaultVertexFormat.POSITION_TEX_COLOR);

        initLayer();
    }

    private static void attemptAction(Consumer<AccessoriesHolder> consumer) {
        var currentPlayer = Minecraft.getInstance().player;

        if (currentPlayer == null || Minecraft.getInstance().level == null) return;

        var holder = currentPlayer.accessoriesHolder();

        if (holder != null) consumer.accept(holder);
    }

    public static void initalConfigDataSync() {
        var currentPlayer = Minecraft.getInstance().player;

        if(currentPlayer == null || Minecraft.getInstance().level == null) return;

        var holder = currentPlayer.accessoriesHolder();

        if(holder == null) return;

        var equipControl = Accessories.config().clientOptions.equipControl();

        if(holder.equipControl() != equipControl) {
            AccessoriesNetworking.sendToServer(SyncHolderChange.of(HolderProperty.EQUIP_CONTROL, equipControl));
        }

        var showUnusedSlots = Accessories.config().screenOptions.showUnusedSlots();

        if(holder.showUnusedSlots() != showUnusedSlots) {
            AccessoriesNetworking.sendToServer(SyncHolderChange.of(HolderProperty.UNUSED_PROP, showUnusedSlots));
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
        return attemptToOpenScreen(targetingLookingEntity, Accessories.config().screenOptions.selectedScreenType());
    }

    private static boolean attemptToOpenScreen(boolean targetingLookingEntity, ScreenType screenType) {
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

            if(slots.isEmpty() && !holder.showUnusedSlots() && !displayUnusedSlotWarning && !Accessories.config().clientOptions.disableEmptySlotScreenError()) {
                player.displayClientMessage(Component.literal("[Accessories]: No Used Slots found by any mod directly, the screen will show empty unless a item is found to implement slots!"), false);

                displayUnusedSlotWarning = true;
            }
        }

        if(selectedVariant != null) {
            AccessoriesNetworking.sendToServer(ScreenOpen.of(targetingLookingEntity, selectedVariant));
        } else {
            Minecraft.getInstance().setScreen(new ScreenVariantSelectionScreen(variant -> {
                AccessoriesNetworking.sendToServer(ScreenOpen.of(targetingLookingEntity, variant));
            }));
        }

        return true;
    }

    public static void initLayer() {
        Layers.add(Containers::verticalFlow, instance -> {
            var creativeScreen = instance.screen instanceof CreativeModeInventoryScreen;

            instance.adapter.rootComponent.allowOverflow(true);

            var offset = creativeScreen
                    ? Accessories.config().screenOptions.creativeInventoryButtonOffset()
                    : Accessories.config().screenOptions.inventoryButtonOffset();

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

            instance.alignComponentToHandledScreenCoordinates(button, offset.x, offset.y);

        }, InventoryScreen.class, CreativeModeInventoryScreen.class);
    }
}
