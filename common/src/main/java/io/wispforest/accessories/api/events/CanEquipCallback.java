package io.wispforest.accessories.api.events;

import io.wispforest.accessories.api.action.ActionResponseBuffer;
import io.wispforest.accessories.api.action.ValidationState;
import io.wispforest.accessories.api.core.AccessoryNestUtils;
import io.wispforest.accessories.api.core.AccessoryRegistry;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.impl.core.UnknownResponse;
import io.wispforest.accessories.impl.event.WrappedEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.item.ItemStack;

///
/// @deprecated Use [io.wispforest.accessories.api.events.v2.CanEquipCallback] instead!
/// Event callback used to allow or denied the ability to equip a given accessory for the given referenced slot
/// type and entity.
///
/// Fired in [AccessoryRegistry#canEquip(ItemStack,SlotReference)]
///
@Deprecated(forRemoval = true)
public interface CanEquipCallback {

    Event<CanEquipCallback> EVENT = new WrappedEvent<>(
        io.wispforest.accessories.api.events.v2.CanEquipCallback.EVENT,
        canEquipCallback -> (stack, reference, callback) -> {
            var result = canEquipCallback.canEquip(stack, reference);

            if (result != TriState.DEFAULT) callback.respondWith(new UnknownResponse(ValidationState.of(result)));
        },
        canEquipCallbackEvent -> {
            return (stack, reference) -> {
                var buffer = new ActionResponseBuffer(true);

                canEquipCallbackEvent.invoker().canEquip(stack, reference, buffer);

                return buffer.canPerformAction().toTriState();
            };
        }
    );

    ///
    /// @param stack     The specific stack being evaluated
    /// @param reference The reference to the specific location within the Accessories Inventory
    /// @return If the given stack can be equipped
    ///
    TriState canEquip(ItemStack stack, SlotReference reference);
}
