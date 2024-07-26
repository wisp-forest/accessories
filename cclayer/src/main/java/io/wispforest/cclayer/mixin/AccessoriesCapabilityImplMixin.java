package io.wispforest.cclayer.mixin;

import io.wispforest.accessories.impl.AccessoriesCapabilityImpl;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.compat.WrappedCurioItemHandler;

@Mixin(AccessoriesCapabilityImpl.class)
public class AccessoriesCapabilityImplMixin {

    @Inject(method = "<init>", at = @At("TAIL"))
    private void attemptCurioConversion(LivingEntity entity, CallbackInfo ci) {
        WrappedCurioItemHandler.attemptConversion(() -> (AccessoriesCapabilityImpl) (Object) this);
    }
}
