package io.wispforest.accessories.api.slot.validator;

import io.wispforest.accessories.api.action.ActionResponse;
import io.wispforest.accessories.api.action.ActionResponseBuffer;
import io.wispforest.accessories.api.slot.SlotType;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.function.Predicate;

public interface SlotValidator {

    void isValidForSlot(Level level, SlotType slotType, int index, ItemStack stack, ActionResponseBuffer buffer);

    static SlotValidator ofItem(Predicate<Item> predicate) {
        return ofItem(predicate, SlotValidatorReasons.INVALID_ITEM);
    }

    /**
     * @return Predicate that checks solely using the passed stacks item
     */
    static SlotValidator ofItem(Predicate<Item> predicate, Component invalidReason) {
        return (level, slotType, index, stack, buffer) -> {
            var isSuccess = predicate.test(stack.getItem());

            if (!isSuccess) {
                buffer.respondWith(ActionResponse.of(false, invalidReason));
            } else {
                buffer.respondWith(ActionResponse.SUCCESS);
            }
        };
    }

    static <T> SlotValidator ofClass(Class<T> clazz) {
        return ofClass(clazz, SlotValidatorReasons.INVALID_ITEM);
    }

    /**
     * @return Predicate that checks if the passed {@link Item} from the stack is instance of the given {@link Class}
     */
    static <T> SlotValidator ofClass(Class<T> clazz, Component invalidReason) {
        return (level, slotType, index, stack, buffer) -> {
            var isSuccess = clazz.isInstance(stack.getItem());

            if (!isSuccess) {
                buffer.respondWith(ActionResponse.of(false, invalidReason));
            } else {
                buffer.respondWith(ActionResponse.SUCCESS);
            }
        };
    }

    static SlotValidator withEntity(EntitySlotValidator entitySlotValidator) {
        return entitySlotValidator;
    }
}
