package io.wispforest.accessories.api.slot;

import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.function.Predicate;

public interface SlotBasedPredicate {
    TriState isValid(SlotType slotType, int slot, ItemStack stack);

    static SlotBasedPredicate ofItem(Predicate<Item> predicate) {
        return (slotType, slot, stack) -> TriState.of(predicate.test(stack.getItem()));
    }

    static <T> SlotBasedPredicate ofClass(Class<T> clazz) {
        return (slotType, slot, stack) -> TriState.of(clazz.isInstance(stack.getItem()));
    }
}
