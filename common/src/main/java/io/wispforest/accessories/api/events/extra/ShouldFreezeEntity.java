package io.wispforest.accessories.api.events.extra;

import io.wispforest.accessories.api.slot.SlotReference;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.item.ItemStack;

public interface ShouldFreezeEntity {

    Event<ShouldFreezeEntity> EVENT = EventFactory.createArrayBacked(ShouldFreezeEntity.class, invokers -> (stack, reference) -> {
        for (var invoker : invokers) {
            var state = invoker.shouldFreeze(stack, reference);

            if(state != TriState.DEFAULT) return state;
        }

        return TriState.DEFAULT;
    });

    /**
     * @param stack               The specific stack being evaluated
     * @param reference           The reference to the specific location within the Accessories Inventory
     * @return If the given looking entity sees the given entity as disguised
     */
    TriState shouldFreeze(ItemStack stack, SlotReference reference);
}
