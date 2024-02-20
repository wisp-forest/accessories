package io.wispforest.accessories.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.wispforest.accessories.api.slot.SlotGroup;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.resources.ResourceLocation;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record SlotGroupImpl(String name, int order, Set<String> slots, Pair<Integer, ResourceLocation> iconInfo) implements SlotGroup {

    public static final MapCodec<Pair<Integer, ResourceLocation>> ICON_INFO_CODEC = RecordCodecBuilder.mapCodec(instance -> {
        return instance.group(
                Codec.INT.fieldOf("size").forGetter(Pair::first),
                ResourceLocation.CODEC.fieldOf("location").forGetter(Pair::second)
        ).apply(instance, Pair::of);
    });

    public static final MapCodec<SlotGroup> CODEC = RecordCodecBuilder.mapCodec(instance -> {
        return instance.group(
                    Codec.STRING.fieldOf("name").forGetter(SlotGroup::name),
                    Codec.INT.fieldOf("order").forGetter(SlotGroup::order),
                    Codec.STRING.listOf().<Set<String>>xmap(HashSet::new, List::copyOf).fieldOf("slots").forGetter(SlotGroup::slots),
                    ICON_INFO_CODEC.fieldOf("").forGetter(SlotGroup::iconInfo)
                ).apply(instance, SlotGroupImpl::new);
    });
}
