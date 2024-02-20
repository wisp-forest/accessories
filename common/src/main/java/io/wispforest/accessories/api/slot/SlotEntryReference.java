package io.wispforest.accessories.api.slot;

import net.minecraft.world.item.ItemStack;

public record SlotEntryReference(SlotReference reference, ItemStack stack) {
}
