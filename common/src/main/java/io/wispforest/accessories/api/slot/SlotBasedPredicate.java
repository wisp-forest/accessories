package io.wispforest.accessories.api.slot;

import io.wispforest.accessories.api.action.ActionResponse;
import io.wispforest.accessories.api.action.ActionResponseBuffer;
import io.wispforest.accessories.api.slot.validator.SlotValidator;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;

/**
 * Predicate used to verify if the given stack is valid for the passed SlotType and index
 */
@Deprecated(forRemoval = true)
public interface SlotBasedPredicate extends SlotValidator {

    /**
     * Predicate method used to check if the given stack is valid for the given slot
     *
     * @param level    The current level for the predicate
     * @param slotType The given slot type being checked
     * @param index     The given index being referenced
     * @param stack    The stack being checked
     * @return Whether the stack can be equipped into the given slot
     */
    TriState isValid(Level level, SlotType slotType, int index, ItemStack stack);

    /**
     * @return Predicate that checks solely using the passed stacks item
     */
    static SlotBasedPredicate ofItem(Predicate<Item> predicate) {
        return (level, slotType, slot, stack) -> TriState.of(predicate.test(stack.getItem()));
    }

    /**
     * @return Predicate that checks if the passed {@link Item} from the stack is instance of the given {@link Class}
     */
    static <T> SlotBasedPredicate ofClass(Class<T> clazz) {
        return (level, slotType, slot, stack) -> TriState.of(clazz.isInstance(stack.getItem()));
    }

    static SlotBasedPredicate withEntity(EntityBasedPredicate entityBasedPredicate) {
        return entityBasedPredicate;
    }

    //--

    @Override
    default void isValidForSlot(Level level, SlotType slotType, int index, ItemStack stack, ActionResponseBuffer buffer) {
        var isValid = isValid(level, slotType, index, stack);

        if (isValid == TriState.DEFAULT) return;

        var message = isValid.get()
            ? Component.literal("Such an item is valid to be equipped!")
            : Component.literal("Such an item is not valid to be equipped!");

        buffer.respondWith(ActionResponse.of(isValid.get(), message));
    }
}
