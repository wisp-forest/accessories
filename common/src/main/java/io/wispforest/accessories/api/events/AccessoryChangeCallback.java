package io.wispforest.accessories.api.events;

import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Event callback fired upon detecting a change with a given Accessory Stack equipped in a given slot with.
 * <p>
 * Fired at the end of evaluation for a given slot within the {@link AccessoriesEventHandler#onLivingEntityTick(LivingEntity)}
 * from the {@link AccessoryChangeCallback#EVENT}
 */
public interface AccessoryChangeCallback {

    Event<AccessoryChangeCallback> EVENT = EventFactory.createArrayBacked(AccessoryChangeCallback.class,
            (invokers) -> (reference, prevStack, currentStack, stateChange) -> {
                for (var invoker : invokers) invoker.onChange(reference, prevStack, currentStack, stateChange);
            }
    );

    /**
     * @param prevStack    The previous stack from the Accessory inventory
     * @param currentStack The current stack within the Accessory Inventory
     * @param reference    The reference for the stacks
     * @param stateChange  The type of change to which occurred
     */
    void onChange(ItemStack prevStack, ItemStack currentStack, SlotReference reference, SlotStateChange stateChange);
}
