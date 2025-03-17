package io.wispforest.accessories.fabric.mixin;

import io.wispforest.accessories.fabric.ExtraEntityTrackingEvents;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerEntity.class)
public abstract class ServerEntityEvent {

    @Shadow @Final private Entity entity;

    @Inject(method = "addPairing", at = @At("TAIL"))
    private void onStartTracking(ServerPlayer player, CallbackInfo ci) {
        ExtraEntityTrackingEvents.POST_START_TRACKING.invoker().onStartTracking(this.entity, player);
    }
}
