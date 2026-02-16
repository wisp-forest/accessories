package io.wispforest.accessories.api.events.v2;

import io.wispforest.accessories.api.action.ActionResponseBuffer;
import io.wispforest.accessories.api.core.AccessoryNestUtils;
import io.wispforest.accessories.api.core.AccessoryRegistry;
import io.wispforest.accessories.api.slot.SlotReference;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.item.ItemStack;

///
/// Event callback used to allow or denied the ability to equip a given accessory for the given referenced slot
/// type and entity.
///
/// Fired in [AccessoryRegistry#canEquip(ItemStack,SlotReference)]
///
public interface CanEquipCallback {

    Event<CanEquipCallback> EVENT = EventFactory.createArrayBacked(CanEquipCallback.class,
            (invokers) -> (stack, reference, buffer) -> {
                AccessoryNestUtils.recursivelyHandle(stack, reference, (stack1, reference1) -> {
                    for (var invoker : invokers) {
                        invoker.canEquip(stack1, reference1, buffer);

                        if(buffer.shouldReturnEarly()) return true;
                    }

                    return null;
                });
            }
    );

    ///
    /// @param stack     The specific stack being evaluated
    /// @param reference The reference to the specific location within the Accessories Inventory
    /// @param buffer    The buffer to send a response to if the Accessory can or can not be equipped
    ///
    void canEquip(ItemStack stack, SlotReference reference, ActionResponseBuffer buffer);
}
