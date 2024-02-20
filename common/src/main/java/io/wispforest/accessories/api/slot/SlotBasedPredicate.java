package io.wispforest.accessories.api.slot;

import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.item.ItemStack;

public interface SlotBasedPredicate {
    TriState isValid(SlotReference reference, ItemStack stack);
}
