package io.wispforest.accessories.api.events.extra;

import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

/**
 * Event implemented on an Accessory to control the given looting of the given referenced entity
 * <p/>
 * Such event is called within {@link ImplementedEvents#lootingAdjustments(LivingEntity, DamageSource, int)} from either {@link ImplementedEvents#LOOTING_ADJUSTMENT_EVENT}
 * or if a given Accessory implements this interface
 */
public interface LootingAdjustment {
    int getLootingAdjustment(ItemStack stack, SlotReference reference, LivingEntity target, DamageSource damageSource, int currentLevel);
}
