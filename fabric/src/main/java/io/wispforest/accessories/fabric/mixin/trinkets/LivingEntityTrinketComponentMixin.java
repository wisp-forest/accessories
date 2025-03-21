package io.wispforest.accessories.fabric.mixin.trinkets;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import dev.emi.trinkets.api.LivingEntityTrinketComponent;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(value = LivingEntityTrinketComponent.class, remap = false)
public class LivingEntityTrinketComponentMixin {

    @Inject(method = "readFromNbt", at = @At("HEAD"), remap = false)
    private void stopDuplicateItemDecode(CompoundTag tag, HolderLookup.Provider lookup, CallbackInfo ci, @Local(argsOnly = true) LocalRef<CompoundTag> tagRef) {
        if (tag.contains("data_written_by_accessories")) {
            tagRef.set(new CompoundTag());
        }
    }
}
