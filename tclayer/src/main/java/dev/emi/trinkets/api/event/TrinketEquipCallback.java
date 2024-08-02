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

public interface TrinketEquipCallback {
    Event<TrinketEquipCallback> EVENT = new WrappedEvent<>(AccessoryChangeCallback.EVENT, callback -> {
        return (prevStack, currentStack, reference, stateChange) -> {
            var slotReference = WrappingTrinketsUtils.createTrinketsReference(reference);

            if(slotReference.isEmpty()) return;

            callback.onEquip(currentStack, slotReference.get(), reference.entity());
        };
    }, accessoryChangeCallbackEvent -> (stack, slot, entity) -> {
        var ref = WrappingTrinketsUtils.createAccessoriesReference(slot).get();

        accessoryChangeCallbackEvent.invoker().onChange(ref.getStack(), stack, ref, SlotStateChange.REPLACEMENT);
    });

    /**
     * Called when an entity equips a trinket, after the {@link Trinket#onEquip} method of the Trinket
     *
     * @param stack The stack being equipped
     * @param slot The slot the stack is equipped to
     * @param entity The entity that equipped the stack
     */
    void onEquip(ItemStack stack, SlotReference slot, LivingEntity entity);
}
