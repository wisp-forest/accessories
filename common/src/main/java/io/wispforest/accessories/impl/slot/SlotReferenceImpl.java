package io.wispforest.accessories.impl.slot;

import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public record SlotReferenceImpl(LivingEntity entity, String slotName, int slot) implements SlotReference {

    public SlotReferenceImpl {
        if(slot < -1) {
            throw new IndexOutOfBoundsException("A given Slot Reference was attempted to be created with a negative index!");
        }
    }
}
