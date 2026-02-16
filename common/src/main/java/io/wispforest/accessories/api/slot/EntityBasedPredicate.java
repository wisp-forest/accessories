package io.wispforest.accessories.api.slot;

import io.wispforest.accessories.api.action.ActionResponse;
import io.wispforest.accessories.api.action.ActionResponseBuffer;
import io.wispforest.accessories.api.slot.validator.EntitySlotValidator;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Similar to {@link SlotBasedPredicate} but allows for {@link LivingEntity} if such is required
 */
@Deprecated(forRemoval = true)
public interface EntityBasedPredicate extends SlotBasedPredicate, EntitySlotValidator {

    TriState isValid(Level level, @Nullable LivingEntity entity, SlotType slotType, int index, ItemStack stack);

    @Override
    default TriState isValid(Level level, SlotType slotType, int slot, ItemStack stack) {
        return isValid(level, null, slotType, slot, stack);
    }

    @Override
    default void isValidForSlot(Level level, SlotType slotType, int index, ItemStack stack, ActionResponseBuffer buffer) {
        SlotBasedPredicate.super.isValidForSlot(level, slotType, index, stack, buffer);
    }

    @Override
    default void isValidForSlot(@Nullable LivingEntity entity, Level level, SlotType slotType, int index, ItemStack stack, ActionResponseBuffer buffer) {
        var isValid = isValid(level, entity, slotType, index, stack);

        if (isValid == TriState.DEFAULT) return;

        var message = isValid.get()
            ? Component.literal("Such an item is valid to be equipped!")
            : Component.literal("Such an item is not valid to be equipped!");

        buffer.respondWith(ActionResponse.of(isValid.get(), message));
    }
}
