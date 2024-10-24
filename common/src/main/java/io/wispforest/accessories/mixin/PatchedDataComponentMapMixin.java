package io.wispforest.accessories.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.wispforest.accessories.pond.stack.PatchedDataComponentMapExtension;
import io.wispforest.accessories.utils.ItemStackMutation;
import io.wispforest.owo.util.EventStream;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
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
import java.util.Optional;

@Mixin(PatchedDataComponentMap.class)
public abstract class PatchedDataComponentMapMixin implements PatchedDataComponentMapExtension {

    @Unique
    private boolean changeCheckStack = false;

    private ItemStack itemStack;

    private EventStream<ItemStackMutation> mutationEvent = new EventStream<>(invokers -> (stack, types) -> {
        invokers.forEach(itemStackMutation -> itemStackMutation.onMutation(stack, types));
    });

    @Override
    public EventStream<ItemStackMutation> accessories$getMutationEvent(ItemStack stack) {
        this.itemStack = stack;

        return mutationEvent;
    }

    @Override
    public boolean accessories$hasChanged() {
        var bl = changeCheckStack;

        this.changeCheckStack = false;

        return bl;
    }

    @Inject(method = "set", at = @At("HEAD"))
    private <T> void accessories$updateChangeValue_set(DataComponentType<? super T> component, @Nullable T value, CallbackInfoReturnable<T> cir){
        this.changeCheckStack = true;

        this.mutationEvent.sink().onMutation(this.itemStack, List.of(component));
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private <T> void accessories$updateChangeValue_remove(DataComponentType<? super T> component, CallbackInfoReturnable<T> cir){
        this.changeCheckStack = true;

        this.mutationEvent.sink().onMutation(this.itemStack, List.of(component));
    }

    @Unique
    private boolean inApplyPatchLock = false;

    @WrapMethod(method = "applyPatch(Lnet/minecraft/core/component/DataComponentPatch;)V")
    private void accessories$updateChangeValue_applyPatch(DataComponentPatch patch, Operation<Void> original){
        this.changeCheckStack = true;

        var changedDataTypes = (List<DataComponentType<?>>) (List) patch.entrySet().stream().map(Map.Entry::getKey).toList();

        this.inApplyPatchLock = true;

        original.call(patch);

        this.inApplyPatchLock = false;

        this.mutationEvent.sink().onMutation(this.itemStack, changedDataTypes);
    }

    @Inject(method = "applyPatch(Lnet/minecraft/core/component/DataComponentType;Ljava/util/Optional;)V", at = @At("HEAD"))
    private void accessories$updateChangeValue_applyPatch(DataComponentType<?> component, Optional<?> value, CallbackInfo ci){
        this.changeCheckStack = true;

        if (!this.inApplyPatchLock) {
            this.mutationEvent.sink().onMutation(this.itemStack, List.of(component));
        }
    }

    @Inject(method = "restorePatch", at = @At("HEAD"))
    private void accessories$updateChangeValue_restorePatch(DataComponentPatch patch, CallbackInfo ci){
        this.changeCheckStack = true;

        var changedDataTypes = (List<DataComponentType<?>>) (List) patch.entrySet().stream().map(Map.Entry::getKey).toList();

        this.mutationEvent.sink().onMutation(this.itemStack, changedDataTypes);
    }
}
