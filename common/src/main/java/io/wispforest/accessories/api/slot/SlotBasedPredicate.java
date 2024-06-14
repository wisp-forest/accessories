package io.wispforest.accessories.api.slot;

import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;

/**
 * Predicate used to verify if the given stack is valid for the passed SlotType and index
 */
public interface SlotBasedPredicate {

    /**
     * Predicate method used to check if the given stack is valid for the given slot
     *
     * @param level    The current level for the predicate
     * @param slotType The given slot type being checked
     * @param slot     The given index being referenced
     * @param stack    The stack being checked
     * @return Whether the stack can be equipped into the given slot
     */
    TriState isValid(Level level, SlotType slotType, int slot, ItemStack stack);

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
}
