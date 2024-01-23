package io.wispforest.accessories.api;

import net.minecraft.world.entity.LivingEntity;

public record SlotReference(SlotType type, LivingEntity entity, int slot) {
}
