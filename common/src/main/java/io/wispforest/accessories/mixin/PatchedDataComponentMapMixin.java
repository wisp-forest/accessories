package io.wispforest.accessories.mixin;

import io.wispforest.accessories.utils.PatchedDataComponentMapExtension;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

@Mixin(PatchedDataComponentMap.class)
public abstract class PatchedDataComponentMapMixin implements PatchedDataComponentMapExtension {

    private Optional<MutableBoolean> changeCheckStack = Optional.empty();

    @Override
    public void startCheckingForChanges() {
        this.changeCheckStack = Optional.of(new MutableBoolean(false));
    }

    @Override
    public boolean hasChanged() {
        return this.changeCheckStack.map(MutableBoolean::getValue).orElse(false);
    }

    @Override
    public void endCheckingForChanges() {
        this.changeCheckStack = Optional.empty();
    }

    @Inject(method = "set", at = @At("HEAD"))
    private <T> void accessories$updateChangeValue_set(DataComponentType<? super T> component, @Nullable T value, CallbackInfoReturnable<T> cir){
        changeCheckStack.ifPresent(mutableBoolean -> mutableBoolean.setValue(true));
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private <T> void accessories$updateChangeValue_remove(DataComponentType<? super T> component, CallbackInfoReturnable<T> cir){
        changeCheckStack.ifPresent(mutableBoolean -> mutableBoolean.setValue(true));
    }

    @Inject(method = "applyPatch(Lnet/minecraft/core/component/DataComponentType;Ljava/util/Optional;)V", at = @At("HEAD"))
    private void accessories$updateChangeValue_applyPatch(DataComponentType<?> component, Optional<?> value, CallbackInfo ci){
        changeCheckStack.ifPresent(mutableBoolean -> mutableBoolean.setValue(true));
    }

    @Inject(method = "restorePatch", at = @At("HEAD"))
    private void accessories$updateChangeValue_restorePatch(DataComponentPatch patch, CallbackInfo ci){
        changeCheckStack.ifPresent(mutableBoolean -> mutableBoolean.setValue(true));
    }
}
