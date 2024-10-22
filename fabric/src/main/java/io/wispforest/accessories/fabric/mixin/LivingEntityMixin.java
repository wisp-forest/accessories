package io.wispforest.accessories.fabric.mixin;

import io.wispforest.accessories.impl.AccessoriesEventHandler;
import io.wispforest.accessories.pond.DroppedStacksExtension;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void accessories$tick(CallbackInfo ci){
        AccessoriesEventHandler.onLivingEntityTick((LivingEntity)(Object)this);
    }

    @Inject(method = "dropAllDeathLoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;dropEquipment(Lnet/minecraft/server/level/ServerLevel;)V"))
    private void handleAccessoriesDrop(ServerLevel level, DamageSource damageSource, CallbackInfo ci) {
        var entity = (LivingEntity) (Object) this;
        var droppedStacks = AccessoriesEventHandler.onDeath(entity, damageSource);

        if (droppedStacks == null) return;

        if (this instanceof DroppedStacksExtension playerExtension) {
            playerExtension.addToBeDroppedStacks(droppedStacks);
        } else {
            for (var droppedStack : droppedStacks) {
                entity.spawnAtLocation(level, droppedStack);
            }
        }
    }
}
