package io.wispforest.accessories.mixin.temp_fixes;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Debug;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

// TODO: REMOVE IN THE FUTURE 1.21.5?
@Debug(export = true)
@Mixin(CompoundTag.class)
public abstract class NbtCompoundMixin {

    @Shadow @Nullable public abstract Tag get(String string);

    @Inject(method = "hasUUID", at = @At(value = "JUMP", opcode = Opcodes.IFNULL, ordinal = 0), cancellable = true)
    private void adjustCheckForListVariants(String key, CallbackInfoReturnable<Boolean> cir, @Local(ordinal = 0) Tag tag) {
        if(tag instanceof ListTag listTag && listTag.getElementType() == Tag.TAG_INT && listTag.size() == 4) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "getByteArray", at = @At(value = "HEAD"), cancellable = true)
    private void adjustByteArrayForRegularList(String key, CallbackInfoReturnable<byte[]> cir) {
        if(get(key) instanceof ListTag listTag && listTag.getElementType() == Tag.TAG_BYTE) {
            var array = new byte[listTag.size()];

            for (int i = 0; i < listTag.size(); i++) {
                var tagEntry = listTag.get(i);

                array[i] = ((NumericTag) tagEntry).getAsByte();
            }

            cir.setReturnValue(array);
        }
    }

    @Inject(method = "getIntArray", at = @At(value = "HEAD"), cancellable = true)
    private void adjustIntArrayForRegularList(String key, CallbackInfoReturnable<int[]> cir) {
        if(get(key) instanceof ListTag listTag && listTag.getElementType() == Tag.TAG_INT) {
            var array = new int[listTag.size()];

            for (int i = 0; i < listTag.size(); i++) {
                var tagEntry = listTag.get(i);

                array[i] = ((NumericTag) tagEntry).getAsInt();
            }

            cir.setReturnValue(array);
        }
    }

    @Inject(method = "getLongArray", at = @At(value = "HEAD"), cancellable = true)
    private void adjustLongArrayForRegularList(String key, CallbackInfoReturnable<long[]> cir) {
        if(get(key) instanceof ListTag listTag && listTag.getElementType() == Tag.TAG_LONG) {
            var array = new long[listTag.size()];

            for (int i = 0; i < listTag.size(); i++) {
                var tagEntry = listTag.get(i);

                array[i] = ((NumericTag) tagEntry).getAsLong();
            }

            cir.setReturnValue(array);
        }
    }
}
