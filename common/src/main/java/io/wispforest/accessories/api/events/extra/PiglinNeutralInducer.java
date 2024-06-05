package io.wispforest.accessories.api.events.extra;

import io.wispforest.accessories.api.slot.SlotReference;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.item.ItemStack;

/**
 * Event callback used to adjust the given {@link PiglinAi#isWearingGold(LivingEntity)}s method calls return
 * either making the given piglin neutral or not
 * <p/>
 * Such is called within {@link ExtraEventHandler#isPiglinsNeutral(LivingEntity)}
 * if any given Accessory was found to implement this interface and/or any registered callback
 * to the {@link LootingAdjustment#EVENT} returns an adjustment
 */
public interface PiglinNeutralInducer {

    Event<PiglinNeutralInducer> EVENT = EventFactory.createArrayBacked(PiglinNeutralInducer.class, invokers -> (stack, reference) -> {
        for (var invoker : invokers) {
            var state = invoker.makePiglinsNeutral(stack, reference);

            if(state != TriState.DEFAULT) return state;
        }

        return TriState.DEFAULT;
    });

    /**
     * @param stack     The specific stack being evaluated
     * @param reference The reference to the specific location within the Accessories Inventory
     * @return If the given piglin should be neutral to the given referenced entity
     */
    TriState makePiglinsNeutral(ItemStack stack, SlotReference reference);
}
