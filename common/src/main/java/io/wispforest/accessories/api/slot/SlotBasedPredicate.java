package io.wispforest.accessories.api.slot;

import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;

/**
 * Predicate used to verify if the given stack is valid for the passed SlotType and index
 */
public interface SlotBasedPredicate {

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
