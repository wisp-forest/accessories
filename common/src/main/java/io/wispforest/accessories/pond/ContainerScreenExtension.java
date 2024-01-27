package io.wispforest.accessories.pond;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

public interface ContainerScreenExtension {

    @Nullable
    default Boolean isHovering(Slot slot, double mouseX, double mouseY) {
        return null;
    }
}
