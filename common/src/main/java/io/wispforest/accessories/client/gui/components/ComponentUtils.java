package io.wispforest.accessories.client.gui.components;

import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.slot.SlotGroup;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.accessories.client.DrawUtils;
import io.wispforest.accessories.client.gui.AccessoriesScreen;
import io.wispforest.accessories.menu.SlotTypeAccessible;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.networking.server.SyncCosmeticToggle;
import io.wispforest.accessories.pond.ScissorStackManipulation;
import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.container.ScrollContainer;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.util.NinePatchTexture;
import it.unimi.dsi.fastutil.Pair;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.data.AtlasIds;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.*;

import static io.wispforest.owo.ui.container.Containers.horizontalFlow;
import static io.wispforest.owo.ui.container.Containers.verticalFlow;

public class ComponentUtils {

    private static final ResourceLocation SLOT = ResourceLocation.withDefaultNamespace("textures/gui/sprites/container/slot.png");
    private static final ResourceLocation DARK_SLOT = Accessories.of("textures/gui/theme/dark/slot.png");

    public static final Surface BACKGROUND_SLOT_RENDERING_SURFACE = (context, component) -> {
        var slotComponents = new ArrayList<AccessoriesScreen.ExtendedSlotComponent>();

        recursiveSearchSlots(component, slotComponents::add);

        context.push()
            .translate(component.x(), component.y());

        var texture = getSlotTexture();

        for (var slotComponent : slotComponents) {
            DrawUtils.blit(
                context,
                texture,
                slotComponent.x() - component.x() - 1,
                slotComponent.y() - component.y() - 1,
                18, 18
            );
        }

        context.pop();

        // TODO: UNKNOWN WHY THIS MUST BE OUTSIDE OF THE MATRIX TRANSLATION TBH so....
        renderSpectrumOutlines(context, slotComponents);
    };

    public static final Surface SPECTRUM_SLOT_OUTLINE = (context, component) -> {
        var slotComponents = new ArrayList<AccessoriesScreen.ExtendedSlotComponent>();

        recursiveSearchSlots(component, slotComponents::add);

        renderSpectrumOutlines(context, slotComponents);
    };

    public static void renderSpectrumOutlines(OwoUIDrawContext context, List<AccessoriesScreen.ExtendedSlotComponent> slotComponents) {
        for (var slotComponent : slotComponents) {
            var slot = slotComponent.slot();

            if (!(slot instanceof SlotTypeAccessible access) || !access.isCosmeticSlot()) continue;

            DrawUtils.drawRectOutlineWithSpectrum(context, slotComponent.x(), slotComponent.y(), 16, 16, 0.35f, false);
        }
    }

    public static final ScrollContainer.Scrollbar VANILLA = (context, x, y, width, height, trackX, trackY, trackWidth, trackHeight, lastInteractTime, direction, active) -> {
        NinePatchTexture.draw(Accessories.of(("theme/" + checkMode("light", "dark") + "/scrollbar/track")), context, trackX, trackY, trackWidth, trackHeight);
        NinePatchTexture.draw(getScrollabarTexture(direction, active), context, x + 1, y + 1, width - 2, height - 2);
    };

    public static ResourceLocation getScrollabarTexture(ScrollContainer.ScrollDirection direction, boolean active) {
        var scrollBarType = (direction == ScrollContainer.ScrollDirection.VERTICAL ? "vertical" : "horizontal") + (active ? "" : "_disabled");
        var themeType = checkMode("light", "dark");

        return Accessories.of("theme/" + themeType + "/scrollbar/vanilla_" + scrollBarType);
    }

    public static final Surface PANEL_INSET = (context, component) -> {
        NinePatchTexture.draw(Accessories.of(("theme/" + checkMode("light", "dark") + "/inset")), context, component);
    };

    public static final Surface PANEL = (context, component) -> {
        NinePatchTexture.draw(Accessories.of(("theme/" + checkMode("light", "dark") + "/panel")), context, component);
    };

    private static final ButtonComponent.Renderer BUTTON_RENDERER = (context, button, delta) -> {
        NinePatchTexture.draw(getBtnTexture(button), context, button.getX(), button.getY(), button.width(), button.height());
    };

    private static ResourceLocation getBtnTexture(ButtonComponent btn) {
        var btnType = (btn.isActive() ? (btn.isHovered() ? "hovered" : "active") : "disabled");
        var themeType = checkMode("light", "dark");

        return Accessories.of("theme/" + themeType + "/button/" + btnType);
    }

    public static <T> T checkMode(T lightMode, T darkMode) {
        return Accessories.config().screenOptions.isDarkMode() ? darkMode : lightMode;
    }

    private record ThemeHook<O, T>(Predicate<O> isValidStill, BiConsumer<O, T> setCallback) {
        private boolean isHookValid(Object hookedObject) {
            return this.isValidStill.test((O) hookedObject);
        }

        private void setValue(Object hookedObject, Object themeResult) {
            setCallback.accept((O) hookedObject, (T) themeResult);
        }
    }

    private static final Map<Object, ThemeHook<?, ?>> activeHooks = new WeakHashMap<>();
    private static boolean hookSetup = false;

    public static <O, T> void addModeCheckHook(T lightMode, T darkMode, O o, Predicate<O> isValidStill, BiConsumer<O, T> setCallback) {
        Function<Boolean, T> getterFunc = isDarkMode -> isDarkMode ? darkMode : lightMode;

        activeHooks.put(o, new ThemeHook<O, T>(isValidStill, setCallback));

        if (!hookSetup) {
            hookSetup = true;

            Accessories.config().screenOptions.subscribeToIsDarkMode(isDarkMode -> {
                for (var object : activeHooks.keySet()) {
                    var hook = activeHooks.get(object);

                    if (!hook.isHookValid(object)) {
                        activeHooks.remove(object);

                        continue;
                    }

                    hook.setValue(object, getterFunc.apply(isDarkMode));
                }
            });
        }

        setCallback.accept(o, getterFunc.apply(Accessories.config().screenOptions.isDarkMode()));
    }

    public static ResourceLocation getSlotTexture() {
        return checkMode(SLOT, DARK_SLOT);
    }

    public static Surface getPanelSurface() {
        return PANEL;
    }

    public static Surface getInsetPanelSurface() {
        return PANEL_INSET;
    }

    public static Surface getPanelWithInset(int insetWidth) {
        return (context, component) -> {
            var location = Accessories.of(("theme/" + checkMode("light", "dark") + "/inset"));

            NinePatchTexture.draw(location, context, component.x() + insetWidth, component.y() + insetWidth, component.width() - insetWidth * 2, component.height() - insetWidth * 2);
        };
    }

    public static ButtonComponent.Renderer getButtonRenderer() {
        return BUTTON_RENDERER;
    }

    public static ScrollContainer.Scrollbar getScrollbarRenderer() {
        return VANILLA;
    }

    public static void recursiveSearchSlots(ParentComponent parentComponent, Consumer<AccessoriesScreen.ExtendedSlotComponent> action) {
        recursiveSearch(parentComponent, AccessoriesScreen.ExtendedSlotComponent.class, action);
    }

    public static <C extends io.wispforest.owo.ui.core.Component> void recursiveSearch(ParentComponent parentComponent, Class<C> target, Consumer<C> action) {
        if(parentComponent == null) return;

        for (var child : parentComponent.children()) {
            if(target.isInstance(child)) action.accept((C) child);
            if(child instanceof ParentComponent childParent) recursiveSearch(childParent, target, action);
        }
    }

    public static <S extends Slot & SlotTypeAccessible> Pair<io.wispforest.owo.ui.core.Component, PositionedRectangle> createSlotWithToggle(S slot, Function<Integer, AccessoriesScreen.ExtendedSlotComponent> slotBuilder) {
        return createSlotWithToggle(slot, slotBuilder, true);
    }

    public static <S extends Slot & SlotTypeAccessible> Pair<io.wispforest.owo.ui.core.Component, @Nullable PositionedRectangle> createSlotWithToggle(S slot, Function<Integer, AccessoriesScreen.ExtendedSlotComponent> slotBuilder, boolean createButton) {
        var btnPosition = Positioning.absolute(14, -1); //15, -1

        @Nullable ButtonComponent toggleBtn = null;

        if (createButton) {
            toggleBtn = ComponentUtils.createSlotToggle(slot)
                    .configure(component -> {
                        component.sizing(Sizing.fixed(5))
                                .positioning(btnPosition);

//                        ((ComponentExtension) component).allowIndividualOverdraw(true);
                    });
        }

        var combinedLayout = verticalFlow(Sizing.fixed(18), Sizing.fixed(18))
                .child(
                        slotBuilder.apply(slot.index)
                                .margins(Insets.of(1))
                );

        if (toggleBtn != null) combinedLayout.child(toggleBtn);

        return Pair.of(combinedLayout, toggleBtn);
    }

    public static <S extends Slot & SlotTypeAccessible> ButtonComponent createSlotToggle(S slot) {
        return createToggle(
                () -> slot.getContainer().shouldRender(slot.getContainerSlot()),
                (btn) -> {
                    var entity = slot.getContainer().capability().entity();

                    AccessoriesNetworking
                            .sendToServer(SyncCosmeticToggle.of(entity.equals(Minecraft.getInstance().player) ? null : entity, slot.slotType(), slot.getContainerSlot()));
                },
                (context, button, delta) -> {});
    }

    public static io.wispforest.owo.ui.core.Component createGroupToggle(AccessoriesScreen screen, SlotGroup group) {
        var tooltipData = new ArrayList<Component>();

        tooltipData.add(Component.translatable(group.translation()));

        if (UniqueSlotHandling.isUniqueGroup(group.name(), true)) {
            tooltipData.add(Component.literal(group.name()).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));
        }

        return createToggle(
                () -> screen.getMenu().isGroupSelected(group),
                buttonComponent -> {
                    screen.getMenu().toggleSelectedGroup(group);
                    screen.rebuildAccessoriesComponent();
                },
                (context, button, delta) -> {
                    var textureAtlasSprite = Minecraft.getInstance()
                            .getAtlasManager()
                            .getAtlasOrThrow(AtlasIds.GUI)
                            .getSprite(group.icon());

                    DrawUtils.blitSpriteWithColor(context, textureAtlasSprite, button.x() + 3, button.y() + 3, 8, 8, Color.WHITE);
                })
                .sizing(Sizing.fixed(14))
                .tooltip(tooltipData);
    }

    public static ButtonComponent createToggle(Supplier<Boolean> stateSupplier, Consumer<ButtonComponent> onToggle, ButtonComponent.Renderer extraRendering) {
        ButtonComponent.Renderer texturedRenderer = (context, btn, delta) -> {
            var state = stateSupplier.get();

            var btnType = (state ? "enabled" : "disabled") + (btn.isHovered() ? "_hovered" : "");
            var themeType = checkMode("light", "dark");

            var texture = Accessories.of("theme/" + themeType + "/button/toggle/rounded/" + btnType);

            context.push();

            Runnable drawCall = () -> {
                NinePatchTexture.draw(texture, context, btn.getX(), btn.getY(), btn.width(), btn.height());
                extraRendering.draw(context, btn, delta);
            };

            ((ScissorStackManipulation) context).accessories$renderWithoutAny(drawCall);

            context.pop();
        };

        return Components.button(Component.empty(), onToggle)
                .renderer(texturedRenderer);
    }

    public static io.wispforest.owo.ui.core.Component createIconButton(Consumer<ButtonComponent> action, int size, Consumer<ButtonComponent> builder, Function<ButtonComponent, ResourceLocation> textureGetter) {
        return createIconButton(action, size, null, builder, (context, buttonComponent) -> textureGetter.apply(buttonComponent));
    }

    public static io.wispforest.owo.ui.core.Component createIconButton(Consumer<ButtonComponent> action, int size, Consumer<ButtonComponent> builder, BiFunction<OwoUIDrawContext, ButtonComponent, ResourceLocation> textureGetter) {
        return createIconButton(action, size, null, builder, textureGetter);
    }

    public static io.wispforest.owo.ui.core.Component createIconButton(Consumer<ButtonComponent> action, int size, String id, Consumer<ButtonComponent> builder, BiFunction<OwoUIDrawContext, ButtonComponent, ResourceLocation> textureGetter) {
        return verticalFlow(Sizing.content(), Sizing.content())
            .child(
                Components.button(Component.empty(), action)
                    .sizing(Sizing.fixed(size))
                    .configure(builder)
                    .renderer((ctx, btn, delta) -> {
                        ctx.push();
                        DrawUtils.blit(ctx, textureGetter.apply(ctx, btn), btn.getX(), btn.getY(), btn.width(), btn.height());
                        ctx.pop();
                    }).id(id)
            );
    }

    public static <C extends BaseOwoHandledScreen.SlotComponent> io.wispforest.owo.ui.core.Component createCraftingComponent(int start, Function<Integer, C> componentFactory, Consumer<Integer> slotEnabler, boolean isVertical) {
        for (int i = start; i < 5 + start; i++) slotEnabler.accept(i);

        var craftingLayout = isVertical ? verticalFlow(Sizing.fixed(18 * 2), Sizing.content()) : horizontalFlow(Sizing.content(), Sizing.fixed(18 * 2));

        craftingLayout.child(
                (!isVertical ? verticalFlow(Sizing.content(), Sizing.content()) : horizontalFlow(Sizing.content(), Sizing.content()))
                        .child(componentFactory.apply(start + 1).margins(Insets.of(1)))
                        .child(componentFactory.apply(start + 2).margins(Insets.of(1)))
        ).child(
                (!isVertical ? verticalFlow(Sizing.content(), Sizing.content()) : horizontalFlow(Sizing.content(), Sizing.content()))
                        .child(componentFactory.apply(start + 3).margins(Insets.of(1)))
                        .child(componentFactory.apply(start + 4).margins(Insets.of(1)))
        ).child(
                new ArrowComponent((isVertical) ? ArrowComponent.Direction.DOWN : ArrowComponent.Direction.RIGHT)
                        .centered(true)
                        .margins(Insets.of(3, 3, 1, 1))
                        .id("crafting_arrow")
        ).child(
                (!isVertical ? verticalFlow(Sizing.content(), Sizing.expand()) : horizontalFlow(Sizing.expand(), Sizing.content()))
                        .child(componentFactory.apply(start).margins(Insets.of(1)))
                        .horizontalAlignment(HorizontalAlignment.CENTER)
                        .verticalAlignment(VerticalAlignment.CENTER)
        ).allowOverflow(true)
                .horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        return Containers.verticalFlow(Sizing.content(), Sizing.content())
                .child(craftingLayout)
                .padding(Insets.of(7, 7, 4, 7));
    }

    public static <C extends BaseOwoHandledScreen.SlotComponent> io.wispforest.owo.ui.core.Component createPlayerInv(int start, int end, Function<Integer, C> componentFactory, Consumer<Integer> slotEnabler) {
        var playerLayout = verticalFlow(Sizing.content(), Sizing.content());

        int row = 0;

        var rowLayout = horizontalFlow(Sizing.content(), Sizing.content())
                .configure((FlowLayout layout) -> layout.allowOverflow(true));

        int rowCount = 0;

        for (int i = start; i < end; i++) {
            var slotComponent = componentFactory.apply(i);

            slotEnabler.accept(i);

            rowLayout.child(slotComponent.margins(Insets.of(1)));

            if(row >= 8) {
                playerLayout.child(rowLayout);

                rowLayout = horizontalFlow(Sizing.content(), Sizing.content())
                        .configure((FlowLayout layout) -> layout.allowOverflow(true));

                rowCount++;

                if(rowCount == 3) rowLayout.margins(Insets.top(4));

                row = 0;
            } else {
                row++;
            }
        }

        return playerLayout.allowOverflow(true);
    }

    public interface CreativeScreenExtension {
        Event<OnCreativeTabChange> getEvent();

        CreativeModeTab getTab();
    }

    public interface OnCreativeTabChange {
        void onTabChange(CreativeModeTab tab);
    }
}
