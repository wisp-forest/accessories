package io.wispforest.accessories.mixin.owo;

import com.google.common.base.Suppliers;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.util.RecursiveDeserializer;
import io.wispforest.owo.serialization.format.nbt.NbtDeserializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;
import java.util.function.Supplier;

@Mixin(value = NbtDeserializer.class, remap = false)
public abstract class NbtDeserializerMixin extends RecursiveDeserializer<Tag> {

    protected NbtDeserializerMixin(Tag serialized) {
        super(serialized);
    }

    // TODO: FIX OPTIONALS ONCE AGAIN FOR NBT IN ENDEC
    @WrapMethod(method = "readOptional")
    private <V> Optional<V> accessories$patchOptionalIssue(SerializationContext ctx, Endec<V> endec, Operation<Optional<V>> original) {
        var value = this.getValue();

        if (!(value instanceof CompoundTag)) {
            try {
                return Optional.of(tryRead(deserializer1 -> endec.decode(ctx, deserializer1)));
            } catch (Exception e) {
                throw new IllegalStateException("Unable to decode optional NBT value due to being in a improper format: " + e);
            }
        }

        return original.call(ctx, endec);
    }

    @Mixin(targets = "io/wispforest/owo/serialization/format/nbt/NbtDeserializer$Sequence", remap = false)
    public static abstract class SequenceMixin {
        @WrapOperation(method = "next", at = @At(value = "INVOKE", target = "Lio/wispforest/owo/serialization/format/nbt/NbtDeserializer;access$000(Lio/wispforest/owo/serialization/format/nbt/NbtDeserializer;Ljava/util/function/Supplier;Ljava/util/function/Supplier;)Ljava/lang/Object;", remap = false))
        private <T> Object test(NbtDeserializer x0, Supplier<T> x1, Supplier x2, Operation<Object> original) {
            var safeMemoizedLambda = Suppliers.memoize(x1::get);

            return original.call(x0, safeMemoizedLambda, x2);
        }
    }
}
