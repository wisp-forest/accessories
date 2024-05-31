package dev.emi.trinkets.api.event;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.compat.WrappingTrinketsUtils;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.events.AccessoriesEvents;
import io.wispforest.accessories.impl.event.WrappedEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface TrinketDropCallback {
    Event<TrinketDropCallback> EVENT = new WrappedEvent<>(AccessoriesEvents.ON_DROP_EVENT, callback -> {
        return (eventContext) -> {
            var reference = WrappingTrinketsUtils.createReference(eventContext.reference());

            if(reference.isEmpty()) return;

            var value = TrinketEnums.convert(callback.drop(TrinketEnums.convert(eventContext.getReturn()), eventContext.stack(), reference.get(), eventContext.reference().entity()));

            if(!value.equals(DropRule.DEFAULT)) eventContext.setReturn(value);
        };
    });

    TrinketEnums.DropRule drop(TrinketEnums.DropRule rule, ItemStack stack, SlotReference ref, LivingEntity entity);
}
