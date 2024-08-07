package io.wispforest.accessories.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.client.GuiGraphicsUtils;
import io.wispforest.accessories.client.gui.AccessoriesExperimentalScreen;
import io.wispforest.accessories.networking.server.SyncCosmeticToggle;
import io.wispforest.owo.ui.base.BaseComponent;
import io.wispforest.owo.ui.base.BaseOwoHandledScreen;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.container.Containers;
import io.wispforest.owo.ui.container.FlowLayout;
import io.wispforest.owo.ui.core.Insets;
import io.wispforest.owo.ui.core.ParentComponent;
import io.wispforest.owo.ui.core.Sizing;
import io.wispforest.owo.ui.core.Surface;
import io.wispforest.owo.ui.util.NinePatchTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ComponentUtils {

    private static final ResourceLocation SLOT = Accessories.of("textures/gui/slot.png");

    public static final Surface BACKGROUND_SLOT_RENDERING_SURFACE = (context, component) -> {
        var slotComponents = new ArrayList<AccessoriesExperimentalScreen.ExtendedSlotComponent>();

        recursiveSearch(component, AccessoriesExperimentalScreen.ExtendedSlotComponent.class, slotComponents::add);

        context.push();
        context.translate(component.x(), component.y(), 0);

        GuiGraphicsUtils.batched(context, SLOT, slotComponents, (bufferBuilder, poseStack, slotComponent) -> {
            var slot = slotComponent.slot();

            if (!slot.isActive()) return;

            GuiGraphicsUtils.blit(bufferBuilder, poseStack, slotComponent.x() - component.x() - 1, slotComponent.y() - component.y() - 1, 18);
        });

        context.pop();
    };

    public static <C extends io.wispforest.owo.ui.core.Component> void recursiveSearch(ParentComponent parentComponent, Class<C> target, Consumer<C> action) {
        for (var child : parentComponent.children()) {
            if(target.isInstance(child)) {
                action.accept((C) child);
            } else if(child instanceof ParentComponent childParent) {
                recursiveSearch(childParent, target, action);
            }
        }
    }

    public static ButtonComponent ofSlot(AccessoriesBasedSlot slot) {
        return toggleBtn(Component.literal(""),
                () -> slot.accessoriesContainer.shouldRender(slot.getContainerSlot()),
                (btn) -> {
                    AccessoriesInternals.getNetworkHandler()
                            .sendToServer(SyncCosmeticToggle.of(slot.entity.equals(Minecraft.getInstance().player) ? null : slot.entity, slot.accessoriesContainer.slotType(), slot.getContainerSlot()));
                });
    }

    public static ButtonComponent toggleBtn(net.minecraft.network.chat.Component message, Supplier<Boolean> stateSupplier, Consumer<ButtonComponent> onToggle) {
        ButtonComponent.Renderer renderer = (context, btn, delta) -> {
            RenderSystem.enableDepthTest();
            var state = stateSupplier.get();

            ResourceLocation texture;

            if(btn.isHovered()) {
                texture = ButtonComponent.HOVERED_TEXTURE;
            } else {
                texture = (state.booleanValue()) ? ButtonComponent.ACTIVE_TEXTURE : ButtonComponent.DISABLED_TEXTURE;
            }

            context.push();

            NinePatchTexture.draw(texture, context, btn.getX(), btn.getY(), btn.width(), btn.height());

            context.pop();
        };

        return Components.button(message, onToggle)
                .renderer(renderer);
    }

    public static  <C extends BaseOwoHandledScreen.SlotComponent> io.wispforest.owo.ui.core.Component createPlayerInv(int end, Function<Integer, C> componentFactory, Consumer<Integer> slotEnabler) {
        var playerLayout = Containers.verticalFlow(Sizing.content(), Sizing.content());

        playerLayout.allowOverflow(true);

        playerLayout.padding(Insets.of(6))
                .surface(Surface.PANEL);

        int row = 0;

        var rowLayout = Containers.horizontalFlow(Sizing.content(), Sizing.content())
                .configure((FlowLayout layout) -> {
                    layout.surface(BACKGROUND_SLOT_RENDERING_SURFACE)
                            .allowOverflow(true);
                });

        int rowCount = 0;

        for (int i = 0; i < end; i++) {
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

        return playerLayout;
    }
}
