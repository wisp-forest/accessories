package io.wispforest.accessories.menu;

import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.slot.SlotType;

public interface SlotTypeAccessible {
    String slotName();

    SlotType slotType();

    AccessoriesContainer getContainer();

    default boolean isCosmeticSlot() {
        return false;
    }
}
