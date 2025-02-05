package io.wispforest.accessories.mixin.client;

import io.wispforest.accessories.pond.LivingEntityRenderStateExtension;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Objects;
import java.util.Optional;

@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityRenderStateMixin implements LivingEntityRenderStateExtension {

    @Unique
    @Nullable
    private LivingEntity livingEntity = null;

    @Override
    public Optional<LivingEntity> getEntity() {
        return Optional.ofNullable(this.livingEntity);
    }

    @Override
    public void setEntity(LivingEntity livingEntity) {
        this.livingEntity = livingEntity;
    }
}
