package io.wispforest.accessories.api.events.extra;

import io.wispforest.accessories.api.slot.SlotReference;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.PowderSnowBlock;

/**
 * Event callback used to adjust the given {@link PowderSnowBlock#canEntityWalkOnPowderSnow(Entity)}s method calls return
 * either allowing or denying the ability to walk on powder snow
 * <p/>
 * Such is called within {@link ExtraEventHandler#allowWalkingOnSnow(LivingEntity)}
 * if any given Accessory was found to implement this interface and/or any registered callback
 * to the {@link AllowWalkingOnSnow#EVENT} returns an adjustment
 */
public interface AllowWalkingOnSnow {

    Event<AllowWalkingOnSnow> EVENT = EventFactory.createArrayBacked(AllowWalkingOnSnow.class, invokers -> (stack, reference) -> {
        for (var invoker : invokers) {
            var state = invoker.allowWalkingOnSnow(stack, reference);

            if(state != TriState.DEFAULT) return state;
        }

        return TriState.DEFAULT;
    });

    /**
     * @param stack     The specific stack being evaluated
     * @param reference The reference to the specific location within the Accessories Inventory
     * @return If the given entity should be able to walk on snow
     */
    TriState allowWalkingOnSnow(ItemStack stack, SlotReference reference);
}
