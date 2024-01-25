package io.wispforest.accessories.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.wispforest.accessories.api.SlotGroup;
import io.wispforest.accessories.api.SlotType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record SlotGroupImpl(String name, int order, Set<String> slots) implements SlotGroup {

    public static final MapCodec<SlotGroup> CODEC = RecordCodecBuilder.mapCodec(instance -> {
        return instance.group(
                    Codec.STRING.fieldOf("name").forGetter(SlotGroup::name),
                    Codec.INT.fieldOf("order").forGetter(SlotGroup::order),
                    Codec.STRING.listOf().<Set<String>>xmap(HashSet::new, List::copyOf).fieldOf("slots").forGetter(SlotGroup::slots)
                ).apply(instance, SlotGroupImpl::new);
    });
}
