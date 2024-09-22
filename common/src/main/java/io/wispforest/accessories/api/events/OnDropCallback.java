package io.wispforest.accessories.api.events;

import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Event callback used to change the givens stack {@link DropRule} when the entity has died
 * and the equipped stacks are to be handled {@link AccessoriesEventHandler#dropStack}
 */
public interface OnDropCallback {

    Event<OnDropCallback> EVENT = EventFactory.createArrayBacked(OnDropCallback.class,
            (invokers) -> (dropRule, stack, reference, damageSource) -> {
                for (var invoker : invokers) {
                    var returnRule = invoker.onDrop(dropRule, stack, reference, damageSource);

                    if(returnRule != null && returnRule != DropRule.DEFAULT) dropRule = returnRule;
                }

                return dropRule;
            }
    );

    static DropRule getAlternativeRule(DropRule dropRule, ItemStack stack, SlotReference reference, DamageSource damageSource) {
        var result = OnDropCallback.EVENT.invoker().onDrop(dropRule, stack, reference, damageSource);

        return result != null ? result : dropRule;
    }

    /**
     * @param dropRule  The current drop rule to use for the given stack
     * @param stack     The specific stack being evaluated
     * @param reference The reference to the specific location within the Accessories Inventory
     * @return The override dropRule for the given stack
     */
    @Nullable DropRule onDrop(DropRule dropRule, ItemStack stack, SlotReference reference, DamageSource damageSource);
}
