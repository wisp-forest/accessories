package io.wispforest.accessories.api.slot;

import net.minecraft.world.item.ItemStack;

/**
 * Context object holding onto a given slot's reference and a given stack loosely bound to it
 */
public record SlotEntryReference(SlotReference reference, ItemStack stack) {}
