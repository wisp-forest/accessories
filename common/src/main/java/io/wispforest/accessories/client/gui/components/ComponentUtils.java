package io.wispforest.accessories.client.gui.components;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.slot.SlotGroup;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.accessories.client.GuiGraphicsUtils;
import io.wispforest.accessories.client.gui.AccessoriesExperimentalScreen;
import io.wispforest.accessories.menu.SlotTypeAccessible;
import io.wispforest.accessories.menu.variants.AccessoriesExperimentalMenu;
import io.wispforest.accessories.networking.server.SyncCosmeticToggle;
import io.wispforest.accessories.pond.owo.ComponentExtension;
import io.wispforest.accessories.pond.owo.MutableBoundingArea;
import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.*;
import io.wispforest.owo.ui.util.NinePatchTexture;
import io.wispforest.owo.ui.util.ScissorStack;
import it.unimi.dsi.fastutil.Pair;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.CreativeModeTab;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ComponentUtils {

    public static final ResourceLocation ENABLED_TEXTURE = Accessories.of("button/enabled");
    public static final ResourceLocation ENABLED_HOVERED_TEXTURE = Accessories.of("button/enabled_hovered");
    public static final ResourceLocation DISABLED_TEXTURE = Accessories.of("button/disabled");
    public static final ResourceLocation DISABLED_HOVERED_TEXTURE = Accessories.of("button/disabled_hovered");

    private static final ResourceLocation SLOT = Accessories.of("textures/gui/slot.png");

    public static final Surface BACKGROUND_SLOT_RENDERING_SURFACE = (context, component) -> {
        var slotComponents = new ArrayList<AccessoriesExperimentalScreen.ExtendedSlotComponent>();

        recursiveSearch(component, AccessoriesExperimentalScreen.ExtendedSlotComponent.class, slotComponents::add);

        context.push();
        context.translate(component.x(), component.y(), 0);

        GuiGraphicsUtils.batched(context, SLOT, slotComponents, (bufferBuilder, poseStack, slotComponent) -> {
            GuiGraphicsUtils.blit(bufferBuilder, poseStack, slotComponent.x() - component.x() - 1, slotComponent.y() - component.y() - 1, 18);
        });

        context.pop();
    };

    public static <C extends io.wispforest.owo.ui.core.Component> void recursiveSearch(ParentComponent parentComponent, Class<C> target, Consumer<C> action) {
        if(parentComponent == null) return;

        for (var child : parentComponent.children()) {
            if(target.isInstance(child)) action.accept((C) child);
            if(child instanceof ParentComponent childParent) recursiveSearch(childParent, target, action);
        }
    }

    public static <S extends Slot & SlotTypeAccessible> Pair<io.wispforest.owo.ui.core.Component, PositionedRectangle> slotAndToggle(S slot, Function<Integer, AccessoriesExperimentalScreen.ExtendedSlotComponent> slotBuilder) {
        return slotAndToggle(slot, true, slotBuilder);
    }

    public static <S extends Slot & SlotTypeAccessible> Pair<io.wispforest.owo.ui.core.Component, PositionedRectangle> slotAndToggle(S slot, boolean isBatched, Function<Integer, AccessoriesExperimentalScreen.ExtendedSlotComponent> slotBuilder) {
        var btnPosition = Positioning.absolute(14, -1); //15, -1

        var toggleBtn = ComponentUtils.slotToggleBtn(slot)
                .configure(component -> {
                    component.zIndex(360)
                            .sizing(Sizing.fixed(5))
                            .positioning(btnPosition);
                });

        ((ComponentExtension)(toggleBtn)).allowIndividualOverdraw(true);

        var combinedLayout = Containers.verticalFlow(Sizing.fixed(18), Sizing.fixed(18))
                .child(
                        slotBuilder.apply(slot.index)
                                .isBatched(isBatched)
                                .margins(Insets.of(1))
                ).child(toggleBtn);

        var combinedArea = ((MutableBoundingArea) combinedLayout);

        //combinedArea.addInclusionZone(toggleBtn);
        //combinedArea.deepRecursiveChecking(true);

        return Pair.of(
                combinedLayout,
                toggleBtn
        );
    }

    public static <S extends Slot & SlotTypeAccessible> ButtonComponent slotToggleBtn(S slot) {
        return toggleBtn(Component.literal(""),
                () -> slot.getContainer().shouldRender(slot.getContainerSlot()),
                (btn) -> {
                    var entity = slot.getContainer().capability().entity();

                    AccessoriesInternals.getNetworkHandler()
                            .sendToServer(SyncCosmeticToggle.of(entity.equals(Minecraft.getInstance().player) ? null : entity, slot.slotType(), slot.getContainerSlot()));
                });
    }

    public static ButtonComponent groupToggleBtn(AccessoriesExperimentalScreen screen, SlotGroup group) {
        var btn = toggleBtn(
                Component.empty(),
                () -> {
                    var menu = screen.getMenu();

                    return menu.isGroupSelected(group);
                },
                buttonComponent -> {
                    var menu = screen.getMenu();

                    if(menu.isGroupSelected(group)) {
                        menu.removeSelectedGroup(group);
                    } else {
                        menu.addSelectedGroup(group);
                    }

                    screen.rebuildAccessoriesComponent();
                },
                (context, button, delta) -> {
                    var textureAtlasSprite = Minecraft.getInstance()
                            .getTextureAtlas(ResourceLocation.withDefaultNamespace("textures/atlas/blocks.png"))
                            .apply(group.icon());

                    var color = Color.WHITE;

                    RenderSystem.depthMask(false);

                    RenderSystem.setShaderColor(color.red(), color.green(), color.blue(), 1f);
                    RenderSystem.enableBlend();
                    RenderSystem.blendFunc(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE);

                    context.blit(button.x() + 3, button.y() + 3, 2, 8, 8, textureAtlasSprite, color.red(), color.green(), color.blue(), 1f);

                    RenderSystem.depthMask(true);
                });

        var tooltipData = new ArrayList<Component>();

        tooltipData.add(Component.translatable(group.translation()));
        if (UniqueSlotHandling.isUniqueGroup(group.name(), true)) tooltipData.add(Component.literal(group.name()).withStyle(ChatFormatting.BLUE, ChatFormatting.ITALIC));

        btn.sizing(Sizing.fixed(14))
                .tooltip(tooltipData);

        return btn;
    }

    public static ButtonComponent toggleBtn(net.minecraft.network.chat.Component message, Supplier<Boolean> stateSupplier, Consumer<ButtonComponent> onToggle) {
        return toggleBtn(message, stateSupplier, onToggle, (context, button, delta) -> {});
    }

    public static ButtonComponent toggleBtn(net.minecraft.network.chat.Component message, Supplier<Boolean> stateSupplier, Consumer<ButtonComponent> onToggle, ButtonComponent.Renderer extraRendering) {
        ButtonComponent.Renderer texturedRenderer = (context, btn, delta) -> {
            RenderSystem.enableDepthTest();
            var state = stateSupplier.get();

            ResourceLocation texture;

            if(btn.isHovered()) {
                texture = (state) ? ENABLED_HOVERED_TEXTURE : DISABLED_HOVERED_TEXTURE;
            } else {
                texture = (state) ? ENABLED_TEXTURE : DISABLED_TEXTURE;
            }

            context.push();

            Runnable drawCall = () -> {
                NinePatchTexture.draw(texture, context, btn.getX(), btn.getY(), btn.width(), btn.height());
                extraRendering.draw(context, btn, delta);
            };

            if(btn instanceof ComponentExtension<?> extension && extension.allowIndividualOverdraw()) {
                ScissorStack.popFramesAndDraw(7, drawCall);
            } else {
                drawCall.run();
            }

            context.pop();
        };

        return Components.button(message, onToggle)
                .renderer(texturedRenderer);
    }

    public static <C extends BaseOwoHandledScreen.SlotComponent> io.wispforest.owo.ui.core.Component createCraftingComponent(int start, int end, Function<Integer, C> componentFactory, Consumer<Integer> slotEnabler, boolean isVertical) {
        var craftingLayout = isVertical ? Containers.verticalFlow(Sizing.fixed(18 * 2), Sizing.content()) : Containers.horizontalFlow(Sizing.content(), Sizing.fixed(18 * 2));

        slotEnabler.accept(0);
        slotEnabler.accept(1);
        slotEnabler.accept(2);
        slotEnabler.accept(3);
        slotEnabler.accept(4);

        craftingLayout.configure((FlowLayout layout) -> {
            layout.surface(BACKGROUND_SLOT_RENDERING_SURFACE)
                    .allowOverflow(true);
        });

        var childrenList = new ArrayList<io.wispforest.owo.ui.core.Component>();

        childrenList.add(
                (!isVertical ? Containers.verticalFlow(Sizing.content(), Sizing.content()) : Containers.horizontalFlow(Sizing.content(), Sizing.content()))
                        .child(componentFactory.apply(start + 1).margins(Insets.of(1)))
                        .child(componentFactory.apply(start + 2).margins(Insets.of(1)))
        );
        childrenList.add(
                (!isVertical ? Containers.verticalFlow(Sizing.content(), Sizing.content()) : Containers.horizontalFlow(Sizing.content(), Sizing.content()))
                        .child(componentFactory.apply(start + 3).margins(Insets.of(1)))
                        .child(componentFactory.apply(start + 4).margins(Insets.of(1)))
        );
        childrenList.add(
                new ArrowComponent((isVertical) ? ArrowComponent.Direction.DOWN : ArrowComponent.Direction.RIGHT)
                        .centered(true)
                        .margins(Insets.of(3, 3, 1, 1))
                        .id("crafting_arrow")
                //Components.spacer().sizing(Sizing.fixed(4))
        );
        childrenList.add(
                (!isVertical ? Containers.verticalFlow(Sizing.content(), Sizing.expand()) : Containers.horizontalFlow(Sizing.expand(), Sizing.content()))
                        .child(componentFactory.apply(start).margins(Insets.of(1)))
                        .horizontalAlignment(HorizontalAlignment.CENTER)
                        .verticalAlignment(VerticalAlignment.CENTER)
        );

        craftingLayout.children(childrenList).horizontalAlignment(HorizontalAlignment.CENTER)
                .verticalAlignment(VerticalAlignment.CENTER);

        return craftingLayout;
    }

    public static <C extends BaseOwoHandledScreen.SlotComponent> io.wispforest.owo.ui.core.Component createPlayerInv(int start, int end, Function<Integer, C> componentFactory, Consumer<Integer> slotEnabler) {
        var playerLayout = Containers.verticalFlow(Sizing.content(), Sizing.content());

        int row = 0;

        var rowLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content())
                .configure((FlowLayout layout) -> {
                    layout.surface(BACKGROUND_SLOT_RENDERING_SURFACE)
                            .allowOverflow(true);
                });

        int rowCount = 0;

        for (int i = start; i < end; i++) {
            var slotComponent = componentFactory.apply(i);

            slotEnabler.accept(i);

            rowLayout.child(slotComponent.margins(Insets.of(1)));

            if(row >= 8) {
                playerLayout.child(rowLayout);

                rowLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content())
                        .configure((FlowLayout layout) -> {
                            layout.surface(BACKGROUND_SLOT_RENDERING_SURFACE)
                                    .allowOverflow(true);
                        });

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
