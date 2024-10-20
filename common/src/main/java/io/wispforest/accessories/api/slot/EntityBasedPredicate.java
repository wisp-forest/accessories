package io.wispforest.accessories.api.slot;

import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Similar to {@link SlotBasedPredicate} but allows for {@link LivingEntity} if such is required
 */
public interface EntityBasedPredicate extends SlotBasedPredicate {

    TriState isValid(Level level, @Nullable LivingEntity entity, SlotType slotType, int slot, ItemStack stack);

    @Override
    default TriState isValid(Level level, SlotType slotType, int slot, ItemStack stack) {
        return isValid(level, null, slotType, slot, stack);
    }
}
