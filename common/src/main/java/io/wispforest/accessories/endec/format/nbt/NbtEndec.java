package io.wispforest.accessories.endec.format.nbt;

import com.google.common.io.ByteStreams;
import io.wispforest.endec.*;
import net.minecraft.nbt.*;

import java.io.IOException;

public class NbtEndec implements Endec<Tag> {

    public static final Endec<Tag> ELEMENT = new NbtEndec();
    public static final Endec<CompoundTag> COMPOUND = new NbtEndec().xmap(CompoundTag.class::cast, compound -> compound);
    public static final Endec<ListTag> LIST = new NbtEndec().xmap(ListTag.class::cast, listTag -> listTag);

    private NbtEndec() {}

    @Override
    public void encode(SerializationContext ctx, Serializer<?> serializer, Tag value) {
        if (serializer instanceof SelfDescribedSerializer<?>) {
            NbtDeserializer.of(value).readAny(ctx, serializer);
            return;
        }

        try {
            var output = ByteStreams.newDataOutput();
            NbtIo.writeUnnamedTag(value, output);

            serializer.writeBytes(ctx, output.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode binary NBT in NbtEndec", e);
        }
    }

    @Override
    public Tag decode(SerializationContext ctx, Deserializer<?> deserializer) {
        if (deserializer instanceof SelfDescribedDeserializer<?> selfDescribedDeserializer) {
            var nbt = NbtSerializer.of();
            selfDescribedDeserializer.readAny(ctx, nbt);

            return nbt.result();
        }

        try {
            return NbtIo.read(ByteStreams.newDataInput(deserializer.readBytes(ctx)), NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse binary NBT in NbtEndec", e);
        }
    }
}
