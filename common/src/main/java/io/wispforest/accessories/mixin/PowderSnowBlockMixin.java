package io.wispforest.accessories.mixin;

import io.wispforest.accessories.api.events.extra.ExtraEventHandler;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.PowderSnowBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PowderSnowBlock.class)
public abstract class PowderSnowBlockMixin {

    @Inject(method = "canEntityWalkOnPowderSnow", at = @At("HEAD"), cancellable = true)
    private static void adjustSnowWalkingAbility(Entity entity, CallbackInfoReturnable<Boolean> cir){
        if(entity instanceof LivingEntity livingEntity){
            var state = ExtraEventHandler.allowWalkingOnSnow(livingEntity);

            if(state != TriState.DEFAULT) cir.setReturnValue(state.orElse(false));
        }
    }
}
