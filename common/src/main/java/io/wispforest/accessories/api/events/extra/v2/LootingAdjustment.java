package io.wispforest.accessories.api.events.extra.v2;

import io.wispforest.accessories.api.events.extra.ExtraEventHandler;
import io.wispforest.accessories.api.slot.SlotReference;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

/**
 * Event callback used to adjust the given {@link LivingEntity}s looting amount from the {@link LivingEntity#dropAllDeathLoot} method
 * <p/>
 * This is called within {@link ExtraEventHandler#lootingAdjustments(LivingEntity, LootContext, int)}
 * if any given Accessory was found to implement this interface and/or any registered callback
 * to the {@link LootingAdjustment#EVENT} returns an adjustment
 */
public interface LootingAdjustment {

    Event<LootingAdjustment> EVENT = EventFactory.createArrayBacked(LootingAdjustment.class, invokers -> (stack, reference, target, context, damageSource, currentLevel) -> {
        var additionalLevels = 0;

        for (var invoker : invokers) {
            additionalLevels += invoker.getLootingAdjustment(stack, reference, target, context, damageSource, additionalLevels + currentLevel);
        }

        return additionalLevels;
    });

    /**
     * @param stack        The stack being evaluated
     * @param reference    The reference to the specific location within the Accessories Inventory
     * @param target       The given target entity for which the attack occurred on
     * @param context      The given loot context for the calculation
     * @param damageSource The specific source of damage used against the target
     * @param currentLevel The current level that has been calculated so far
     * @return The given looting adjustment for the given stack
     */
    int getLootingAdjustment(ItemStack stack, SlotReference reference, LivingEntity target, LootContext context, DamageSource damageSource, int currentLevel);
}
