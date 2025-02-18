package io.wispforest.accessories.pond;

import net.minecraft.world.entity.LivingEntity;

import java.util.Optional;

public interface LivingEntityRenderStateExtension {
    Optional<LivingEntity> accessories$getEntity();

    void accessories$setEntity(LivingEntity livingEntity);
}
