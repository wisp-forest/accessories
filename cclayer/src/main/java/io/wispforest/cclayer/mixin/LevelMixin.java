package io.wispforest.cclayer.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.damagesource.DamageSources;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import top.theillusivec4.curios.mixin.CuriosImplMixinHooks;

import java.util.Optional;
import java.util.function.Supplier;

@Mixin(Level.class)
public class LevelMixin {

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/Optional;orElseThrow(Ljava/util/function/Supplier;)Ljava/lang/Object;"))
    private <T> T cclayer$preventDimException(Optional optional, Supplier supplier, Operation<T> operation) {
        if(((Level)(Object)this) instanceof CuriosImplMixinHooks.CursedLevel) return null;

        return operation.call(optional, supplier);
    }

    @WrapOperation(method = "<init>", at = @At(value = "NEW", target = "(Lnet/minecraft/core/RegistryAccess;)Lnet/minecraft/world/damagesource/DamageSources;"))
    private DamageSources cclayer$preventSourceLoad(RegistryAccess access, Operation<DamageSources> operation) {
        if(((Level)(Object)this) instanceof CuriosImplMixinHooks.CursedLevel) return null;

        return operation.call(access);
    }

    @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/dimension/DimensionType;coordinateScale()D"))
    private double cclayer$preventBorderStuff(DimensionType type, Operation<Double> operation) {
        if(((Level)(Object)this) instanceof CuriosImplMixinHooks.CursedLevel) return 1.0;

        return operation.call(type);
    }
}
