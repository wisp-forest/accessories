package io.wispforest.accessories.api.slot;

import net.minecraft.world.item.ItemStack;

/**
 * Context object holding onto a given slots reference and a given stack loosely bound to such
 */
public record SlotEntryReference(SlotReference reference, ItemStack stack) {
}
