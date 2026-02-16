package io.wispforest.accessories.client;

import com.mojang.blaze3d.platform.Window;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesClientInternals;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.tooltip.TextWrapperImpl;
import io.wispforest.accessories.api.client.tooltip.TooltipComponentBuilderImpl;
import io.wispforest.accessories.api.client.screen.AccessoriesScreenTransitionHelper;
import io.wispforest.accessories.api.tooltip.TextWrapper;
import io.wispforest.accessories.api.tooltip.TooltipComponentBuilder;
import io.wispforest.accessories.client.gui.AccessoriesScreenBase;
import io.wispforest.accessories.client.gui.components.AccessoriesScreenSettingsLayout;
import io.wispforest.accessories.client.gui.components.ComponentUtils;
import io.wispforest.accessories.compat.config.client.ExtendedConfigScreen;
import io.wispforest.accessories.compat.config.client.Structured;
import io.wispforest.accessories.compat.config.client.components.StructListOptionContainer;
import io.wispforest.accessories.compat.config.client.components.StructOptionContainer;
import io.wispforest.accessories.data.EntitySlotLoader;
import io.wispforest.accessories.impl.option.AccessoriesPlayerOptionsHolder;
import io.wispforest.accessories.impl.option.PlayerOptions;
import io.wispforest.accessories.menu.AccessoriesMenuVariant;
import io.wispforest.accessories.mixin.owo.ConfigWrapperAccessor;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.networking.holder.SyncOptionChange;
import io.wispforest.accessories.networking.server.ScreenOpen;
import io.wispforest.accessories.pond.TooltipFlagExtended;
import io.wispforest.owo.config.ui.ConfigScreenProviders;
import io.wispforest.owo.config.ui.OptionComponentFactory;
import io.wispforest.owo.config.ui.OptionComponents;
import io.wispforest.owo.config.ui.component.OptionValueProvider;
import io.wispforest.owo.config.ui.component.SearchAnchorComponent;
import io.wispforest.owo.ui.base.BaseOwoScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.component.LabelComponent;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.layers.Layers;
import io.wispforest.owo.util.NumberReflection;
import io.wispforest.owo.util.ReflectionUtils;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.BannerItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.phys.EntityHitResult;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static io.wispforest.accessories.Accessories.MODID;

public class AccessoriesClient {

    public static final KeyMapping.Category KEY_CATEGORY = KeyMapping.Category.register(Accessories.of("main"));

    public static final KeyMapping OPEN_SCREEN = new KeyMapping(MODID + ".key.open_accessories_screen", GLFW.GLFW_KEY_H, KEY_CATEGORY);
    public static final KeyMapping OPEN_OTHERS_SCREEN = new KeyMapping(MODID + ".key.open_others_accessories_screen", GLFW.GLFW_KEY_H, KEY_CATEGORY);

    //public static final ShaderProgram BLIT_SHADER_KEY = new ShaderProgram(Accessories.of("core/fish"), DefaultVertexFormat.BLIT_SCREEN, ShaderDefines.EMPTY);

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
                            factoryRegister.registerTypedFactory(AccessoriesPlayerOptionsHolder.class, (model, option) -> {
                                var optionComponent = model.expandTemplate(FlowLayout.class,
                                    "boolean-toggle-config-option",
                                    OptionComponents.packParameters(option.translationKey(), Objects.toString(option.value()))
                                );

                                var holderValue = new MutableObject<>(AccessoriesPlayerOptionsHolder.createOrCopy(option.value()));

                                var resetButton = optionComponent.childById(ButtonComponent.class, "reset-button")
                                    .onPress(button -> {
                                        holderValue.setValue(AccessoriesPlayerOptionsHolder.createOrCopy(option.defaultValue()));
                                        // TODO: ADD RESET FUNCTION
                                        button.active = false;
                                    });

                                Runnable checkResetButton = () -> resetButton.active = holderValue.getValue() != null && !holderValue.getValue().isDefaultedValues();

                                checkResetButton.run();

                                var btnLayout = optionComponent.childById(FlowLayout.class, "controls-flow");
                                var tempBtn = optionComponent.childById(ButtonComponent.class, "toggle-button");

                                btnLayout.removeChild(tempBtn);

                                var toggleButton = (ButtonComponent) Components.button(Component.literal("Edit"), btn -> {})
                                    .verticalSizing(tempBtn.verticalSizing().get())
                                    .horizontalSizing(tempBtn.horizontalSizing().get());

                                btnLayout.child(0, toggleButton);

                                toggleButton.onPress(btn -> {
                                    var currentScreen = Minecraft.getInstance().screen;

                                    var newScreen = new BaseOwoScreen<FlowLayout>() {
                                        @Override
                                        protected @NotNull OwoUIAdapter<FlowLayout> createAdapter() {
                                            return OwoUIAdapter.create(this, Containers::verticalFlow);
                                        }

                                        @Override
                                        protected void build(FlowLayout rootComponent) {
                                            rootComponent.child(
                                                Containers.verticalFlow(Sizing.fixed(178), Sizing.content())
                                                    .child(
                                                        Containers.horizontalFlow(Sizing.content(), Sizing.fixed(14))
                                                            .child(
                                                                Containers.horizontalFlow(Sizing.expand(), Sizing.content())
                                                                    .child(
                                                                        Components.label(
                                                                            Component.literal("Default Screen Options")
                                                                        )
                                                                    ).horizontalAlignment(HorizontalAlignment.LEFT)
                                                            )
                                                            .child(
                                                                Components.button(Component.literal("Back"), btn -> onClose())
                                                                    .verticalSizing(Sizing.fixed(14))
                                                            ).verticalAlignment(VerticalAlignment.CENTER)
                                                    )
                                                    .child(
                                                        Containers.verticalFlow(Sizing.fill(), Sizing.fixed(186))
                                                            .child(new AccessoriesScreenSettingsLayout(holderValue.getValue(), this::component).shouldNetworkSync(false).updateLive(true))
                                                            .padding(Insets.of(1))
                                                            .surface(ComponentUtils.getInsetPanelSurface())
                                                    )
                                                    .gap(3)
                                                    .padding(Insets.of(7))
                                                    .surface(ComponentUtils.getPanelSurface())
                                            );

                                            rootComponent
                                                .surface(Surface.optionsBackground())
                                                .verticalAlignment(VerticalAlignment.CENTER)
                                                .horizontalAlignment(HorizontalAlignment.CENTER);
                                        }

                                        @Override
                                        public void onClose() {
                                            checkResetButton.run();

                                            Minecraft.getInstance().setScreen(currentScreen);
                                        }
                                    };
                                    var client = Minecraft.getInstance();

                                    client.screen = newScreen;
                                    client.screen.added();

                                    client.mouseHandler.releaseMouse();
                                    KeyMapping.releaseAll();
                                    newScreen.init(client, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
                                    client.noRender = false;
                                });

                                optionComponent.child(new SearchAnchorComponent(
                                    optionComponent,
                                    option.key(),
                                    () -> optionComponent.childById(LabelComponent.class, "option-name").text().getString(),
                                    () -> toggleButton.getMessage().getString()
                                ));

                                return new OptionComponentFactory.Result<>(optionComponent, new OptionValueProvider() {
                                    @Override public boolean isValid() { return true; }
                                    @Override public Object parsedValue() { return holderValue.getValue(); }
                                });
                            });

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
                if(holder.getDefaultedData(PlayerOptions.EQUIP_CONTROL) == value) return;

                AccessoriesNetworking.sendToServer(SyncOptionChange.of(PlayerOptions.EQUIP_CONTROL, value));
            });
        });

        Accessories.config().screenOptions.subscribeToShowUnusedSlots(value -> {
            attemptAction(holder -> {
                if(holder.getDefaultedData(PlayerOptions.SHOW_UNUSED_SLOTS) == value) return;

                AccessoriesNetworking.sendToServer(SyncOptionChange.of(PlayerOptions.SHOW_UNUSED_SLOTS, value));
            });
        });

        Accessories.config().screenOptions.subscribeToAlwaysShowCraftingGrid(value -> {
            attemptAction(holder -> {
                if(holder.getDefaultedData(PlayerOptions.SHOW_CRAFTING_GRID) == value) return;

                AccessoriesNetworking.sendToServer(SyncOptionChange.of(PlayerOptions.SHOW_CRAFTING_GRID, value));
            });
        });
    }

    private static final boolean IS_OSX = Util.getPlatform() == Util.OS.OSX;
    private static final int EDIT_SHORTCUT_KEY_MODIFIER = IS_OSX ? GLFW.GLFW_MOD_SUPER : GLFW.GLFW_MOD_CONTROL;

    public static void init(){
        AccessoriesClientInternals.setInstance(new AccessoriesClientInternals() {
            @Override
            public TooltipComponentBuilder createTooltipBuilder() {
                return new TooltipComponentBuilderImpl();
            }

            @Override
            public TextWrapper createWrapper(int maxWidth, Style overrideStyle) {
                return new TextWrapperImpl(maxWidth, overrideStyle);
            }

            @Override
            public TooltipFlag createTooltipFlag() {
                return TooltipFlagExtended.create(Minecraft.getInstance().options.advancedItemTooltips);
            }

            @Override
            public int createBitFlag() {
                var inst = Minecraft.getInstance();
                return createBitFlag(inst.hasShiftDown(), inst.hasControlDown(), inst.hasAltDown());
            }

            @Override
            public int createBitFlag(boolean hasShift, boolean hasControl, boolean hasAlt) {
                return (hasShift ? GLFW.GLFW_MOD_SHIFT : 0)
                    | (hasControl ? EDIT_SHORTCUT_KEY_MODIFIER : 0)
                    | (hasAlt ? GLFW.GLFW_MOD_ALT : 0);
            }
        });

        ClientLifecycleEvents.END_DATA_PACK_RELOAD.register((client, success) -> {
            if (!success) return; // LOADING PROBLEM HAS OCCURRED SO THINGS WILL GO WRONG IF WE TRY DOING OUR STUFF

            BuiltInRegistries.ITEM.forEach(item -> {
                var defaultStack = item.getDefaultInstance();

                if (item instanceof BannerItem || defaultStack.has(DataComponents.GLIDER)) {
                    if (!AccessoriesRendererRegistry.hasRenderer(item)) {
                        // TODO: Replace with better method of targeting only specific slots to disable default rendering
                        AccessoriesRendererRegistry.bindItemToEmptyRenderer(item);
                    }
                }
            });

            AccessoriesRendererRegistry.onReload();
        });

        initLayer();
    }

    public static boolean isInventoryKey(Predicate<KeyMapping> predicate) {
        return predicate.test(OPEN_SCREEN) || predicate.test(OPEN_OTHERS_SCREEN);
    }

    public static void handleKeyMappings(Minecraft client) {
        while (AccessoriesClient.OPEN_SCREEN.consumeClick()){
            var player = client.player;

            if (Accessories.config().screenOptions.prioritizeCreativeScreen() && player != null && player.isCreative()) {
                if (client.gameMode.isServerControlledInventory()) {
                    player.sendOpenInventory();
                } else {
                    client.getTutorial().onOpenInventory();
                    client.setScreen(new InventoryScreen(player));
                }

                return;
            }

            AccessoriesClient.openScreenFromKey(AccessoriesClient.OPEN_SCREEN);
        }

        if (!AccessoriesClient.OPEN_OTHERS_SCREEN.same(AccessoriesClient.OPEN_SCREEN)) {
            while (AccessoriesClient.OPEN_OTHERS_SCREEN.consumeClick()){
                AccessoriesClient.openScreenFromKey(AccessoriesClient.OPEN_OTHERS_SCREEN);
            }
        }
    }

    public static void openScreenFromKey(KeyMapping keyMapping) {
        var minecraft = Minecraft.getInstance();
        var currentScreen = minecraft.screen;

        if (currentScreen instanceof AccessoriesScreenBase) {
            minecraft.setScreen(null);
        } else if (currentScreen == null) {
            EntityTarget target;

            if (keyMapping == OPEN_OTHERS_SCREEN) {
                target = EntityTarget.LOOKING_ENTITY;
            } else if (!OPEN_SCREEN.same(OPEN_OTHERS_SCREEN)) {
                target = EntityTarget.PLAYER;
            } else {
                target = minecraft.player.isShiftKeyDown() ? EntityTarget.LOOKING_ENTITY : EntityTarget.PLAYER;
            }

            AccessoriesClient.attemptToOpenScreen(target);
        }
        // TODO: REMOVE AFTER MORE THINKING AS THIS KIND OF DOSE NOT MAKE SENSE AND WILL MOST LIKELY NOT BE AS USEFUL BUT MUST THINK ABOUT SUCH A BIT
//        else {
//            LivingEntity targetEntity = null;
//
//            if (currentScreen instanceof AbstractContainerScreen<?> containerScreen) {
//                targetEntity = AccessoriesScreenTransitionHelper.getTargetEntity((AbstractContainerScreen<AbstractContainerMenu>) containerScreen);
//            }
//
//            if (targetEntity == null) targetEntity = minecraft.player;
//
//            AccessoriesClient.attemptToOpenScreenFromEntity(targetEntity);
//        }
    }

    private static void attemptAction(Consumer<AccessoriesPlayerOptionsHolder> consumer) {
        var currentPlayer = Minecraft.getInstance().player;

        if (currentPlayer == null || Minecraft.getInstance().level == null) return;

        var options = AccessoriesPlayerOptionsHolder.getOptions(currentPlayer);

        if (options != null) consumer.accept(options);
    }

    public static void initalConfigDataSync() {
        var currentPlayer = Minecraft.getInstance().player;

        if(currentPlayer == null || Minecraft.getInstance().level == null) return;

        var options = AccessoriesPlayerOptionsHolder.getOptions(currentPlayer);

        if(options == null) return;

        var equipControl = Accessories.config().clientOptions.equipControl();

        if(options.getDefaultedData(PlayerOptions.EQUIP_CONTROL) != equipControl) {
            AccessoriesNetworking.sendToServer(PlayerOptions.EQUIP_CONTROL.toPacket(equipControl));
        }

        var showUnusedSlots = Accessories.config().screenOptions.showUnusedSlots();

        if(options.getDefaultedData(PlayerOptions.SHOW_UNUSED_SLOTS) != showUnusedSlots) {
            AccessoriesNetworking.sendToServer(PlayerOptions.SHOW_UNUSED_SLOTS.toPacket(showUnusedSlots));
        }

        var alwaysShowCraftingGrid = Accessories.config().screenOptions.alwaysShowCraftingGrid();

        if(options.getDefaultedData(PlayerOptions.SHOW_CRAFTING_GRID) != alwaysShowCraftingGrid) {
            AccessoriesNetworking.sendToServer(PlayerOptions.SHOW_CRAFTING_GRID.toPacket(true));
        }
    }

    public interface WindowResizeCallback {
        void onResized(Minecraft client, Window window);
    }

    private static boolean displayUnusedSlotWarning = false;

    public static boolean attemptToOpenScreen(EntityTarget entityTarget) {
        var player = Minecraft.getInstance().player;

        LivingEntity targetEntity = Accessories.config().screenOptions.keybindIgnoresOtherTargets()
                ? null
                : AccessoriesScreenTransitionHelper.getTargetEntity(player);

        if(targetEntity == null) {
            if (entityTarget.equals(EntityTarget.LOOKING_ENTITY)) {
                var result = ProjectileUtil.getHitResultOnViewVector(player, e -> e instanceof LivingEntity, player.entityInteractionRange());

                if (result instanceof EntityHitResult entitResult && entitResult.getEntity() instanceof LivingEntity living) {
                    targetEntity = living;
                }
            }
        }

        if (targetEntity == null || entityTarget.equals(EntityTarget.PLAYER)){
            return attemptToOpenScreenFromEntity(player);
        }

        return !EntitySlotLoader.getEntitySlots(targetEntity).isEmpty() && attemptToOpenScreenFromEntity(targetEntity);
    }

    public static boolean attemptToOpenScreenFromEntity(LivingEntity targetingEntity) {
        var player = Minecraft.getInstance().player;

        if(targetingEntity.equals(player)) {
            var slots = AccessoriesCapability.getUsedSlotsFor(player);

            var options = AccessoriesPlayerOptionsHolder.getOptions(player);

            if(slots.isEmpty() && !options.getDefaultedData(PlayerOptions.SHOW_UNUSED_SLOTS) && !displayUnusedSlotWarning && !Accessories.config().clientOptions.disableEmptySlotScreenError()) {
                player.displayClientMessage(Component.literal("[Accessories]: No Used Slots found by any mod directly, the screen will show empty unless a item is found to implement slots!"), false);

                displayUnusedSlotWarning = true;
            }
        }

        var selectedVariant = AccessoriesMenuVariant.PRIMARY_V2;

        ItemStack creativeCarriedStack = (Minecraft.getInstance().screen instanceof CreativeModeInventoryScreen screen)
                ? screen.getMenu().getCarried()
                : null;

        AccessoriesNetworking.sendToServer(ScreenOpen.of(targetingEntity, selectedVariant, creativeCarriedStack));

        return true;
    }

    public static void attemptToOpenSelectionScreen(int entityId, boolean targetLookEntity, Player player) {
        var selectedVariant = AccessoriesMenuVariant.PRIMARY_V2;

        ItemStack creativeCarriedStack = (Minecraft.getInstance().screen instanceof CreativeModeInventoryScreen screen)
            ? screen.getMenu().getCarried()
            : null;

        Function<AccessoriesMenuVariant, ScreenOpen> packetBuilder = (menuVariant) -> {
            return new ScreenOpen(targetLookEntity ? -1 : entityId, targetLookEntity, menuVariant, creativeCarriedStack);
        };

        AccessoriesNetworking.sendToServer(packetBuilder.apply(selectedVariant));
    }

    //--

    public static void initLayer() {
        AccessoriesScreenTransitionHelper.init();

        Layers.add(Containers::verticalFlow, instance -> {
            // THIS IS HERE TO HAVE UPDATE POSITION EVERY FRAME BEFORE RENDER TO STOP STUPID POSITIONING PROBLEMS!!!
            instance.aggressivePositioning = true;

            instance.adapter.rootComponent.allowOverflow(true);

            var injectionData = AccessoriesScreenTransitionHelper.getInjection(instance.screen);

            if (injectionData == null) return;

            var button = (ButtonComponent) Components.button(Component.literal(""), (btn) -> {
                        var target = AccessoriesScreenTransitionHelper.getTargetEntity(instance.screen);

                        if (target == null) target = Minecraft.getInstance().player;

                        AccessoriesClient.attemptToOpenScreenFromEntity(target);
                    })
                    .renderer((context, btn, delta) -> {
                        DrawUtils.blit(context,
                                Accessories.of("textures/gui/accessories_open_icon" + (btn.isHovered() ? "_hovered" : "") + ".png"),
                                btn.x(), btn.y(), 0, 0, 8, 8, 8, 8
                        );
                    })
                    .tooltip(Component.translatable(Accessories.translationKey("open.screen")))
                    .margins(Insets.of(1, 0, 0, 1))
                    .sizing(Sizing.fixed(8));

            if(instance.screen instanceof CreativeModeInventoryScreen){
                var extension = ((ComponentUtils.CreativeScreenExtension) instance.screen);

                button.visible = extension.getTab().getType().equals(CreativeModeTab.Type.INVENTORY);

                extension.getEvent().register(tab -> button.visible = tab.getType().equals(CreativeModeTab.Type.INVENTORY));
            }

            instance.adapter.rootComponent.child(button);

            instance.alignComponentToHandledScreenCoordinates(button, injectionData.xOffset(), injectionData.yOffset());

        }, AccessoriesScreenTransitionHelper.getScreenClasses());
    }
}
