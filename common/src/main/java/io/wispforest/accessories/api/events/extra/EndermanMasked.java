package io.wispforest.accessories.api.events.extra;

import io.wispforest.accessories.api.slot.SlotReference;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Event callback used to adjust the given {@link EnderMan#isLookingAtMe(Player)}s method calls return
 * either indicating the user is masked or not
 * <p/>
 * Such is called within {@link ExtraEventHandler#isEndermanMask(LivingEntity, EnderMan)} (LivingEntity)}
 * if any given Accessory was found to implement this interface and/or any registered callback
 * to the {@link EndermanMasked#EVENT} returns an adjustment
 */
public interface EndermanMasked {

    Event<EndermanMasked> EVENT = EventFactory.createArrayBacked(EndermanMasked.class, invokers -> (enderMan, stack, reference) -> {
        for (var invoker : invokers) {
            var state = invoker.isEndermanMasked(enderMan, stack, reference);

            if(state != TriState.DEFAULT) return state;
        }

        return TriState.DEFAULT;
    });

    /**
     * @param enderMan  The specific {@link EnderMan} for the given check
     * @param stack     The specific stack being evaluated
     * @param reference The reference to the specific location within the Accessories Inventory
     * @return If the given enderman sees a mask on the given passed referenced entity
     */
    TriState isEndermanMasked(EnderMan enderMan, ItemStack stack, SlotReference reference);
}
