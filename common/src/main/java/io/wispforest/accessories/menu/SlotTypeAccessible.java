package io.wispforest.accessories.menu;

import io.wispforest.accessories.api.slot.SlotType;

public interface SlotTypeAccessible {
    String slotName();

    SlotType slotType();

    default boolean isCosmeticSlot() {
        return false;
    }
}
