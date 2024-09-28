package io.wispforest.accessories.endec;

import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationAttributes;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.impl.KeyedEndec;
import io.wispforest.endec.util.MapCarrier;
import io.wispforest.owo.serialization.format.nbt.NbtDeserializer;
import io.wispforest.owo.serialization.format.nbt.NbtEndec;
import io.wispforest.owo.serialization.format.nbt.NbtSerializer;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.NotNull;

public record NbtMapCarrier(CompoundTag compoundTag) implements MapCarrier {

    public static final Endec<NbtMapCarrier> ENDEC = NbtEndec.COMPOUND.xmap(NbtMapCarrier::new, NbtMapCarrier::compoundTag);

    public static NbtMapCarrier of() {
        return new NbtMapCarrier(new CompoundTag());
    }

    @Override
    public <T> T getWithErrors(SerializationContext ctx, @NotNull KeyedEndec<T> key) {
        if (!this.has(key)) return key.defaultValue();
        return key.endec().decodeFully(ctx.withAttributes(SerializationAttributes.HUMAN_READABLE), NbtDeserializer::of, this.compoundTag().get(key.key()));
    }

    @Override
    public <T> void put(SerializationContext ctx, @NotNull KeyedEndec<T> key, @NotNull T value) {
        this.compoundTag().put(key.key(), key.endec().encodeFully(ctx.withAttributes(SerializationAttributes.HUMAN_READABLE), NbtSerializer::of, value));
    }

    @Override
    public <T> void delete(@NotNull KeyedEndec<T> key) {
        this.compoundTag().remove(key.key());
    }

    @Override
    public <T> boolean has(@NotNull KeyedEndec<T> key) {
        return this.compoundTag().contains(key.key());
    }
}
