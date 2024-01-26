package io.wispforest.accessories.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.SlotType;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Set;

public record SlotTypeImpl(String name, ResourceLocation icon, int order, int amount, Set<ResourceLocation> validators, DropRule dropRule) implements SlotType  {

    public static final MapCodec<SlotType> CODEC = RecordCodecBuilder.mapCodec(instance -> {
        return instance.group(
                    Codec.STRING.fieldOf("name").forGetter(SlotType::name),
                    ResourceLocation.CODEC.fieldOf("icon").forGetter(SlotType::icon),
                    Codec.INT.fieldOf("order").forGetter(SlotType::order),
                    Codec.INT.fieldOf("amount").forGetter(SlotType::amount),
                    ResourceLocation.CODEC.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("validators").forGetter(SlotType::validators),
                    Codec.STRING.xmap(DropRule::valueOf, DropRule::name).fieldOf("dropRule").forGetter(SlotType::dropRule)
                ).apply(instance, SlotTypeImpl::new);
    });

}
