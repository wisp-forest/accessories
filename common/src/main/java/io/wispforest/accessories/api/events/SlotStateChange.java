package io.wispforest.accessories.api.events;

public enum SlotStateChange {
    MUTATION,   // The stack was not unequipped but the stack NBT data was changed
    REPLACEMENT // The stack was unequipped and replaced with an ItemStack with a different item, NBT or both
}
