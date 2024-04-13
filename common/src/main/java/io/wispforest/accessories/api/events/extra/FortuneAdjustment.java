package io.wispforest.accessories.api.events.extra;

import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;

/**
 * Event implemented on an Accessory to control the given fortune of the given referenced entity
 * <p/>
 * Such event is called within {@link ImplementedEvents#fortuneAdjustment(LootContext, int)} from either {@link ImplementedEvents#FORTUNE_ADJUSTMENT_EVENT}
 * or if a given Accessory implements this interface
 */
public interface FortuneAdjustment {
    int getFortuneAdjustment(ItemStack stack, SlotReference reference, LootContext context, int currentLevel);
}
