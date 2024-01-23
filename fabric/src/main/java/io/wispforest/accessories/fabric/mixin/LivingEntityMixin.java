package io.wispforest.accessories.fabric.mixin;

import io.wispforest.accessories.impl.AccessoriesEvents;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void accessories$tick(CallbackInfo ci){
        AccessoriesEvents.onLivingEntityTick((LivingEntity)(Object)this);
    }
}
