package io.wispforest.accessories.pond;

import net.minecraft.world.item.ItemStack;

import java.util.Collection;

public interface DroppedStacksExtension {
    void addToBeDroppedStacks(Collection<ItemStack> list);

    Collection<ItemStack> toBeDroppedStacks();
}
