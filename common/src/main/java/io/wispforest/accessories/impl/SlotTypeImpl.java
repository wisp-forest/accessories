package io.wispforest.accessories.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.wispforest.accessories.api.DropRule;
import io.wispforest.accessories.api.SlotType;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public record SlotTypeImpl(String name, Optional<String> alternativeTranslation, ResourceLocation icon, int order, int amount, Set<ResourceLocation> validators, DropRule dropRule) implements SlotType  {
    public SlotTypeImpl(String name, ResourceLocation icon, int order, int amount, Set<ResourceLocation> validators, DropRule dropRule) {
        this(name, Optional.empty(), icon, order, amount, validators, dropRule);
    }

    @Override
    public String translation() {
        return alternativeTranslation().orElseGet(SlotType.super::translation);
    }

    public static final MapCodec<SlotTypeImpl> CODEC = RecordCodecBuilder.mapCodec(instance -> {
        return instance.group(
                    Codec.STRING.fieldOf("name").forGetter(SlotType::name),
                    Codec.STRING.optionalFieldOf("alternativeTranslation").forGetter(SlotTypeImpl::alternativeTranslation),
                    net.minecraft.resources.ResourceLocation.CODEC.fieldOf("icon").forGetter(SlotType::icon),
                    Codec.INT.fieldOf("order").forGetter(SlotType::order),
                    Codec.INT.fieldOf("amount").forGetter(SlotType::amount),
                    ResourceLocation.CODEC.listOf().xmap(Set::copyOf, List::copyOf).fieldOf("validators").forGetter(SlotType::validators),
                    Codec.STRING.xmap(DropRule::valueOf, DropRule::name).fieldOf("dropRule").forGetter(SlotType::dropRule)
                ).apply(instance, SlotTypeImpl::new);
    });

}
