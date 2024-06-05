package io.wispforest.accessories.api.events.extra;

import io.wispforest.accessories.api.slot.SlotReference;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;

/**
 * Event callback used to adjust the given {@link ApplyBonusCount}s fortune amount from the upon loot calculation
 * <p/>
 * Such is called within {@link ExtraEventHandler#fortuneAdjustment(LootContext, int)}
 * if any given Accessory was found to implement this interface and/or any registered callback
 * to the {@link FortuneAdjustment#EVENT} returns an adjustment
 */
public interface FortuneAdjustment {

    Event<FortuneAdjustment> EVENT = EventFactory.createArrayBacked(FortuneAdjustment.class, invokers -> (stack, reference, context, currentLevel) -> {
        for (var invoker : invokers) {
            currentLevel += invoker.getFortuneAdjustment(stack, reference, context, currentLevel);
        }

        return currentLevel;
    });

    /**
     * @param stack        The stack being evaluated
     * @param reference    The reference to the specific location within the Accessories Inventory
     * @param context      The given loot context for the loot calculation
     * @param currentLevel The current level that has been calculated so far
     * @return The given fortune adjustment for the given stack
     */
    int getFortuneAdjustment(ItemStack stack, SlotReference reference, LootContext context, int currentLevel);
}
