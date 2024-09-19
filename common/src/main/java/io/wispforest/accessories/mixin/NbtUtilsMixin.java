package io.wispforest.accessories.mixin;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(NbtUtils.class)
public abstract class NbtUtilsMixin {
    @Inject(method = "loadUUID", at = @At(value = "HEAD"), cancellable = true)
    private static void adjustLoadToAllowListVariant(Tag tag, CallbackInfoReturnable<UUID> cir) {
        if(tag instanceof ListTag listTag && listTag.getElementType() == Tag.TAG_INT) {
            if(listTag.size() != 4) {
                throw new IllegalArgumentException("Expected UUID-Array to be of length 4, but found " + listTag.size() + ".");
            }

            var array = new int[listTag.size()];

            for (int i = 0; i < listTag.size(); i++) {
                var tagEntry = listTag.get(i);

                array[i] = ((NumericTag) tagEntry).getAsInt();
            }

            cir.setReturnValue(UUIDUtil.uuidFromIntArray(array));
        }
    }
}
