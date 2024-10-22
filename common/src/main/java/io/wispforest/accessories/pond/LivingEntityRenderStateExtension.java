package io.wispforest.accessories.pond;

import net.minecraft.world.entity.LivingEntity;

public interface LivingEntityRenderStateExtension {
    LivingEntity getEntity();

    void setEntity(LivingEntity livingEntity);
}
