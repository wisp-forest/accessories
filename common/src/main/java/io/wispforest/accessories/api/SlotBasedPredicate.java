package io.wispforest.accessories.api;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface SlotBasedPredicate {
    InteractionResult isValid(LivingEntity entity, SlotReference reference, ItemStack stack);
}