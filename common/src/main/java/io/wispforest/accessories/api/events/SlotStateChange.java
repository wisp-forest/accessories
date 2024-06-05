package io.wispforest.accessories.api.events;

public enum SlotStateChange {
    MUTATION,   // The stack was not unequipped but the stack NBT data was changed
    REPLACEMENT // The stack was unequipped with such leading to a different ItemStack either with different Item, NBT or both
}
