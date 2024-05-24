package io.wispforest.accessories.api.slot;

import net.minecraft.world.entity.LivingEntity;

public record SlotReferenceImpl(LivingEntity entity, String slotName, int slot) implements SlotReference {
}
