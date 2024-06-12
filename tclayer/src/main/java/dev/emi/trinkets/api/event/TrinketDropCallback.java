package dev.emi.trinkets.api.event;

import dev.emi.trinkets.api.SlotReference;
import dev.emi.trinkets.api.TrinketEnums;
import dev.emi.trinkets.compat.WrappingTrinketsUtils;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.events.OnDropCallback;
import io.wispforest.accessories.impl.event.WrappedEvent;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public interface TrinketDropCallback {
    Event<TrinketDropCallback> EVENT = new WrappedEvent<>(OnDropCallback.EVENT, callback -> {
        return (dropRule, stack, reference, damageSource) -> {
            var slotReference = WrappingTrinketsUtils.createReference(reference);

            if(slotReference.isEmpty()) return DropRule.DEFAULT;

            return TrinketEnums.convert(callback.drop(TrinketEnums.convert(dropRule), stack, slotReference.get(), reference.entity()));
        };
    });

    TrinketEnums.DropRule drop(TrinketEnums.DropRule rule, ItemStack stack, SlotReference ref, LivingEntity entity);
}
