package io.wispforest.accessories.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(CompoundTag.class)
public abstract class NbtCompoundMixin {

    @Shadow @Nullable public abstract Tag get(String string);

    @Inject(method = "hasUUID", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;get(Ljava/lang/String;)Lnet/minecraft/nbt/Tag;", shift = At.Shift.BY, by = 2), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true)
    private void adjustCheckForListVariants(String key, CallbackInfoReturnable<Boolean> cir, Tag tag) {
        if(tag instanceof ListTag listTag && listTag.getElementType() == Tag.TAG_INT && listTag.size() == 4) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getByteArray", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;contains(Ljava/lang/String;I)Z"), cancellable = true)
    private void adjustByteArrayForRegularList(String key, CallbackInfoReturnable<byte[]> cir) {
        var tag = get(key);

        if(tag instanceof ListTag listTag && listTag.getElementType() == Tag.TAG_BYTE) {
            var array = new byte[listTag.size()];

            for (int i = 0; i < listTag.size(); i++) {
                var tagEntry = listTag.get(i);

                array[i] = ((NumericTag) tagEntry).getAsByte();
            }

            cir.setReturnValue(array);
        }
    }

    @Inject(method = "getIntArray", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;contains(Ljava/lang/String;I)Z"), cancellable = true)
    private void adjustIntArrayForRegularList(String key, CallbackInfoReturnable<int[]> cir) {
        var tag = get(key);

        if(tag instanceof ListTag listTag && listTag.getElementType() == Tag.TAG_INT) {
            var array = new int[listTag.size()];

            for (int i = 0; i < listTag.size(); i++) {
                var tagEntry = listTag.get(i);

                array[i] = ((NumericTag) tagEntry).getAsInt();
            }

            cir.setReturnValue(array);
        }
    }

    @Inject(method = "getLongArray", at = @At(value = "INVOKE", target = "Lnet/minecraft/nbt/CompoundTag;contains(Ljava/lang/String;I)Z"), cancellable = true)
    private void adjustLongArrayForRegularList(String key, CallbackInfoReturnable<long[]> cir) {
        var tag = get(key);

        if(tag instanceof ListTag listTag && listTag.getElementType() == Tag.TAG_LONG) {
            var array = new long[listTag.size()];

            for (int i = 0; i < listTag.size(); i++) {
                var tagEntry = listTag.get(i);

                array[i] = ((NumericTag) tagEntry).getAsLong();
            }

            cir.setReturnValue(array);
        }
    }
}
