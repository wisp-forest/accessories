package io.wispforest.accessories.client.gui.components;

import io.wispforest.owo.ui.core.ParentComponent;
import net.minecraft.world.inventory.Slot;
import org.jetbrains.annotations.Nullable;

public interface AccessoriesContainingComponent extends ParentComponent {

    void onCosmeticToggle(boolean showCosmeticState);

    @Nullable
    default Boolean isHovering_Logical(Slot slot, double mouseX, double mouseY) {
        return null;
    }

    default void setupID() {
        this.id("outer_accessories_layout");
    }

    static String defaultID() {
        return "outer_accessories_layout";
    }
}
