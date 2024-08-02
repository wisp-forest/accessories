package dev.emi.trinkets.api.event;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.Trinket;
import dev.emi.trinkets.compat.WrappingTrinketsUtils;
import io.wispforest.accessories.api.events.AccessoryChangeCallback;
import io.wispforest.accessories.api.events.SlotStateChange;
import io.wispforest.accessories.impl.event.WrappedEvent;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface TrinketUnequipCallback {
    Event<TrinketUnequipCallback> EVENT = new WrappedEvent<>(AccessoryChangeCallback.EVENT, callback -> {
        return (prevStack, currentStack, reference, stateChange) -> {
            var slotReference = WrappingTrinketsUtils.createTrinketsReference(reference);

            if(slotReference.isEmpty()) return;

            callback.onUnequip(prevStack, slotReference.get(), reference.entity());
        };
    }, accessoryChangeCallbackEvent -> (stack, slot, entity) -> {
        var ref = WrappingTrinketsUtils.createAccessoriesReference(slot).get();

        accessoryChangeCallbackEvent.invoker().onChange(stack, ref.getStack(), ref, SlotStateChange.REPLACEMENT);
    });

    /**
     * Called when an entity un-equips a trinket, after the {@link Trinket#onUnequip} method of the Trinket
     *
     * @param stack The stack being unequipped
     * @param slot The slot the stack was unequipped from
     * @param entity The entity that unequipped the stack
     */
    void onUnequip(ItemStack stack, SlotReference slot, LivingEntity entity);
}
