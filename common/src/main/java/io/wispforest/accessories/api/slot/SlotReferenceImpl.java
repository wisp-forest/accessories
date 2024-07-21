package io.wispforest.accessories.api.slot;

import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public record SlotReferenceImpl(LivingEntity entity, String slotName, int slot) implements SlotReference {
}
