package io.wispforest.accessories.api;

import net.minecraft.world.item.ItemStack;

public interface EquipCheck {
    boolean isValid(ItemStack stack, boolean isSwapping);
}
