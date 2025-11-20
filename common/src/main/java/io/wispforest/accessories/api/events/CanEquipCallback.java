package io.wispforest.accessories.api.events;

import io.wispforest.accessories.api.core.AccessoryNestUtils;
import io.wispforest.accessories.api.core.AccessoryRegistry;
import io.wispforest.accessories.api.slot.SlotReference;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.item.ItemStack;

///
/// Event callback used to allow or denied the ability to equip a given accessory for the given referenced slot
/// type and entity.
///
/// Fired in [AccessoryRegistry#canEquip(ItemStack,SlotReference)]
///
public interface CanEquipCallback {

    Event<CanEquipCallback> EVENT = EventFactory.createArrayBacked(CanEquipCallback.class,
            (invokers) -> (stack, reference) -> {
                var result =  AccessoryNestUtils.recursivelyHandle(stack, reference, (innerStack, innerRef) -> {
                    TriState finalResult = null;

                    for (var invoker : invokers) {
                        var returnResult = invoker.canEquip(innerStack, innerRef);

                        if(returnResult.equals(TriState.FALSE)) {
                            finalResult = returnResult;

                            break;
                        }
                    }

                    return finalResult;
                });

                return result != null ? result : TriState.DEFAULT;
            }
    );

    ///
    /// @param stack     The specific stack being evaluated
    /// @param reference The reference to the specific location within the Accessories Inventory
    /// @return If the given stack can be equipped
    ///
    TriState canEquip(ItemStack stack, SlotReference reference);
}
