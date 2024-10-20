package io.wispforest.accessories.pond;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

public interface ContainerScreenExtension {

    @Nullable
    default Boolean isHovering_Logical(Slot slot, double mouseX, double mouseY) {
        return null;
    }

    @Nullable
    default Boolean isHovering_Rendering(Slot slot, double mouseX, double mouseY) {
        return null;
    }

    @Nullable
    default Boolean shouldRenderSlot(Slot slot) {
        return null;
    }

    default void forceRenderSlot(GuiGraphics context, Slot slot) {
        throw new IllegalStateException("Interface injected method not implemented!");
    }

    default int hoverStackOffset() {
        return 0;
    }
}
