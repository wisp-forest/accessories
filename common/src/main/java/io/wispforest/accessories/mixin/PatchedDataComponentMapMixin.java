package io.wispforest.accessories.mixin;


import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.wispforest.accessories.pond.stack.PatchedDataComponentMapExtension;
import io.wispforest.accessories.utils.EnhancedEventStream;
import io.wispforest.accessories.utils.ItemStackMutation;
import io.wispforest.owo.util.EventStream;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Mixin(PatchedDataComponentMap.class)
public abstract class PatchedDataComponentMapMixin implements PatchedDataComponentMapExtension {

    @Unique
    private boolean changeCheckStack = false;

    @Nullable
    @Unique
    private ItemStack itemStack = null;

    @Nullable
    @Unique
    private EventStream<ItemStackMutation> mutationEvent = null;

    @Override
    public EventStream<ItemStackMutation> accessories$getMutationEvent(ItemStack itemStack) {
        Objects.requireNonNull(itemStack);

        this.itemStack = itemStack;

        if (this.mutationEvent == null) {
            this.mutationEvent = EnhancedEventStream.of((invokers, barrier) -> (stack, types) -> {
                try (barrier) {
                    invokers.forEach(itemStackMutation -> itemStackMutation.onMutation(stack, types));
                }
            }, () -> this.mutationEvent = null);
        }

        return this.mutationEvent;
    }

    @Override
    public boolean accessories$hasChanged() {
        var bl = this.changeCheckStack;

        this.changeCheckStack = false;

        return bl;
    }

    @Inject(method = "set", at = @At("HEAD"))
    private <T> void accessories$updateChangeValue_set(DataComponentType<? super T> component, @Nullable T value, CallbackInfoReturnable<T> cir){
        this.changeCheckStack = true;

        this.accessories$handleMutationEvent(List.of(component));
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private <T> void accessories$updateChangeValue_remove(DataComponentType<? super T> component, CallbackInfoReturnable<T> cir){
        this.changeCheckStack = true;

        this.accessories$handleMutationEvent(List.of(component));
    }

    @Unique
    private boolean inApplyPatchLock = false;

    //TODO: FIGURE OUT WHY ARCH LOOM DON'T REMAP WRAP METHOD
    @WrapMethod(method = {
            "applyPatch(Lnet/minecraft/core/component/DataComponentPatch;)V", // Mojmap
            "method_57936(Lnet/minecraft/class_9326;)V",                      // Yarn Interm.
            "applyChanges(Lnet/minecraft/component/ComponentChanges;)V"       // Yarn
    }, expect = 1, require = 1, allow = 1)
    private void accessories$updateChangeValue(DataComponentPatch patch, Operation<Void> original) {
        this.changeCheckStack = true;

        this.inApplyPatchLock = true;

        original.call(patch);

        this.inApplyPatchLock = false;

        var changedDataTypes = (List<DataComponentType<?>>) (List) patch.entrySet().stream().map(Map.Entry::getKey).toList();

        this.accessories$handleMutationEvent(changedDataTypes);
    }

    @Inject(method = "applyPatch(Lnet/minecraft/core/component/DataComponentType;Ljava/util/Optional;)V", at = @At("HEAD"))
    private void accessories$updateChangeValue_applyPatch(DataComponentType<?> component, Optional<?> value, CallbackInfo ci){
        this.changeCheckStack = true;

        if (!this.inApplyPatchLock) {
            this.accessories$handleMutationEvent(List.of(component));
        }
    }

    @Inject(method = "restorePatch", at = @At("HEAD"))
    private void accessories$updateChangeValue_restorePatch(DataComponentPatch patch, CallbackInfo ci){
        this.changeCheckStack = true;

        var changedDataTypes = (List<DataComponentType<?>>) (List) patch.entrySet().stream().map(Map.Entry::getKey).toList();

        this.accessories$handleMutationEvent(changedDataTypes);
    }

    @Unique
    private void accessories$handleMutationEvent(List<DataComponentType<?>> changedDataTypes) {
        if(this.mutationEvent == null) return;

        this.mutationEvent.sink().onMutation(this.itemStack, changedDataTypes);
    }
}
