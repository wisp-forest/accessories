package io.wispforest.accessories.api.slot.validator;

import io.wispforest.accessories.api.action.ActionResponseBuffer;
import io.wispforest.accessories.api.slot.SlotType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public interface EntitySlotValidator extends SlotValidator {

    void isValidForSlot(@Nullable LivingEntity entity, Level level, SlotType slotType, int index, ItemStack stack, ActionResponseBuffer buffer);

    @Override
    default void isValidForSlot(Level level, SlotType slotType, int index, ItemStack stack, ActionResponseBuffer buffer) {
        try {
            isValidForSlot(level, slotType, index, stack, buffer);
        } catch (Exception e) {
            throw new IllegalStateException("Caught an Exception when calling isValidForSlot() for a EntitySlotValidator possible due to improper handling of null entity!", e);
        }
    }
}
