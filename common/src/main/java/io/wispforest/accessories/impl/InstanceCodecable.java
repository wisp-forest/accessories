package io.wispforest.accessories.impl;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

import java.util.function.Supplier;

public interface InstanceCodecable {

    void write(CompoundTag tag);

    void read(CompoundTag tag);

    static <T extends InstanceCodecable> Codec<T> constructed(Supplier<T> supplier){
        return new Codec<>() {
            @Override
            public <T1> DataResult<Pair<T, T1>> decode(DynamicOps<T1> ops, T1 input) {
                var tag = ops.convertTo(NbtOps.INSTANCE, input);

                if (!(tag instanceof CompoundTag compoundTag)) {
                    return DataResult.error(() -> "Unable to decode InstanceCodecable due to input not being a map type!");
                }

                var instance = supplier.get();

                instance.read(compoundTag);

                return DataResult.success(new Pair<>(instance, input));
            }

            @Override
            public <T1> DataResult<T1> encode(T input, DynamicOps<T1> ops, T1 prefix) {
                var tag = new CompoundTag();

                input.write(tag);

                return DataResult.success(NbtOps.INSTANCE.convertTo(ops, tag));
            }
        };
    }
}
