package io.wispforest.accessories.api.events.extra;

import io.wispforest.accessories.api.slot.SlotReference;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Event implemented on an Accessory to control if the given referenced entity can anger piglins or not
 * <p/>
 * Such event is called within {@link ImplementedEvents#isPiglinsNeutral(LivingEntity)} from either {@link ImplementedEvents#PIGLIN_NEUTRAL_INDUCER_EVENT}
 * or if a given Accessory implements this interface
 */
public interface PiglinNeutralInducer {
    TriState makesPiglinsNeutral(ItemStack stack, SlotReference reference);
}
