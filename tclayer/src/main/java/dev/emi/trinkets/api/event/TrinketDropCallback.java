package dev.emi.trinkets.api.event;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.compat.WrappingTrinketsUtils;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.events.AccessoriesEvents;
import io.wispforest.accessories.impl.event.WrappedEvent;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface TrinketDropCallback {
    Event<TrinketDropCallback> EVENT = new WrappedEvent<>(AccessoriesEvents.ON_DROP_EVENT, callback -> {
        return (dropRule, stack, ref) -> {
            var reference = WrappingTrinketsUtils.createReference(ref);

            if(reference.isEmpty()) return DropRule.DEFAULT;

            return TrinketEnums.convert(callback.drop(TrinketEnums.convert(dropRule), stack, reference.get(), ref.entity()));
        };
    });

    TrinketEnums.DropRule drop(TrinketEnums.DropRule rule, ItemStack stack, SlotReference ref, LivingEntity entity);
}
