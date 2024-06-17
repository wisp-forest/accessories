package io.wispforest.accessories.impl;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import io.wispforest.accessories.endec.EdmUtils;
import io.wispforest.endec.Deserializer;
import io.wispforest.endec.Endec;
import io.wispforest.endec.SerializationContext;
import io.wispforest.endec.Serializer;
import io.wispforest.endec.format.edm.EdmElement;
import io.wispforest.endec.format.edm.EdmEndec;
import io.wispforest.endec.format.edm.EdmMap;
import io.wispforest.endec.util.MapCarrier;
import net.minecraft.Util;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

import java.util.HashMap;
import java.util.function.Supplier;

public interface InstanceEndec {

    void write(MapCarrier carrier, SerializationContext ctx);

    void read(MapCarrier carrier, SerializationContext ctx);

    static <T extends InstanceEndec> Endec<T> constructed(Supplier<T> supplier) {
        return EdmEndec.MAP.xmapWithContext(
                (ctx, edmMap) -> Util.make(supplier.get(), t -> t.read(edmMap, ctx)),
                (ctx, t) -> Util.make(EdmUtils.newMap(), map -> t.write(map, ctx)));
    }
}
