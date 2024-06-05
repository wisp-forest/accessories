package io.wispforest.accessories.fabric.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.wispforest.accessories.api.events.extra.ExtraEventHandler;
import io.wispforest.accessories.impl.AccessoriesEventHandler;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void accessories$tick(CallbackInfo ci){
        AccessoriesEventHandler.onLivingEntityTick((LivingEntity)(Object)this);
    }

    //--

    @WrapOperation(method = "dropAllDeathLoot", constant = @Constant(classValue = Player.class))
    private boolean accessories$allowAllLivingEntities(Object object, Operation<Boolean> original){
        return object instanceof LivingEntity || original.call(object);
    }

    @ModifyVariable(method = "dropAllDeathLoot", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;getMobLooting(Lnet/minecraft/world/entity/LivingEntity;)I", shift = At.Shift.BY, by = 2))
    private int accessories$adjustLooting(int original, @Local(argsOnly = true) DamageSource source){
        return ExtraEventHandler.lootingAdjustments((LivingEntity)(Object) this, source, original);
    }
}
