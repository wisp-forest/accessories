package io.wispforest.accessories.api.slot;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * Context object holding onto a given slot's reference and a given stack loosely bound to it
 */
public record SlotEntryReference(SlotReference reference, ItemStack stack) implements SlotPathWithStack {

    public SlotEntryReference(LivingEntity entity, SlotPath path, ItemStack stack){
        this(SlotReference.of(entity, path), stack);
    }

    @Override
    public SlotPath path() {
        return reference.slotPath();
    }
}
