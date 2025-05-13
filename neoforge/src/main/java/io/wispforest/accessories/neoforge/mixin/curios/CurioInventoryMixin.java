package io.wispforest.accessories.neoforge.mixin.curios;

import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;
import top.theillusivec4.curios.common.capability.CurioInventory;

@Pseudo
@Mixin(value = CurioInventory.class, remap = false)
public abstract class CurioInventoryMixin {
    @Shadow(remap = false) CompoundTag deserialized;
    @Shadow(remap = false) boolean markDeserialized;

    // Prevent duplicate data if both Accessories and Curios is installed and the data was saved using CCLayer
    @Inject(method = "init", at = @At(value = "HEAD"), remap = false)
    private void accessories$preventDuplicateDataDecode(ICuriosItemHandler curiosItemHandler, CallbackInfo ci) {
        if (this.deserialized.getBooleanOr("AccessoriesEncoded", false)) {
            this.markDeserialized = false;
            this.deserialized = new CompoundTag();
        }
    }
}
