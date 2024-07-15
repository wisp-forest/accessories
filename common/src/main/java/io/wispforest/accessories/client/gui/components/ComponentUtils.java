package io.wispforest.accessories.client.gui.components;

import com.mojang.blaze3d.systems.RenderSystem;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.api.menu.AccessoriesBasedSlot;
import io.wispforest.accessories.networking.server.SyncCosmeticToggle;
import io.wispforest.owo.ui.component.ButtonComponent;
import io.wispforest.owo.ui.component.Components;
import io.wispforest.owo.ui.util.NinePatchTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ComponentUtils {

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
            //RenderSystem.enableDepthTest();
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
}
