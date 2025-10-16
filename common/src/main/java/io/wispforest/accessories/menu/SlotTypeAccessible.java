package io.wispforest.accessories.menu;

import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.api.slot.SlotPath;
import io.wispforest.accessories.api.slot.SlotType;

public interface SlotTypeAccessible {

    AccessoriesContainer getContainer();

    int index();

    default String slotName() {
        return getContainer().getSlotName();
    }

    default SlotType slotType() {
        return getContainer().slotType();
    }

    default SlotPath slotPath() {
        return getContainer().createPath(index());
    }

    default boolean isCosmeticSlot() {
        return false;
    }
}
