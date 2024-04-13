package io.wispforest.accessories.api.events.extra;

import io.wispforest.accessories.api.slot.SlotReference;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.item.ItemStack;

/**
 * Event implemented on an Accessory to control if the given referenced entity can anger the given enderman or not
 * <p/>
 * Such event is called within {@link ImplementedEvents#isEndermanMask(LivingEntity, EnderMan)} from either {@link ImplementedEvents#ENDERMAN_MASKED_EVENT}
 * or if a given Accessory implements this interface
 */
public interface EndermanMasked {
    TriState isEndermanMasked(EnderMan enderMan, ItemStack stack, SlotReference reference);
}
