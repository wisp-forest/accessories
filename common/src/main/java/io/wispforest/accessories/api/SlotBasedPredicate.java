package io.wispforest.accessories.api;

import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface SlotBasedPredicate {
    TriState isValid(SlotReference reference, ItemStack stack);
}
