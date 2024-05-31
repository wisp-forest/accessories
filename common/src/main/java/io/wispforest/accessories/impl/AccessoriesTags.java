package io.wispforest.accessories.impl;

import io.wispforest.accessories.Accessories;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.enchantment.Enchantment;

public class AccessoriesTags {

    public static final TagKey<EntityType<?>> MODIFIABLE_ENTITY_WHITELIST = ofTag(Registries.ENTITY_TYPE,"modifiable_entity_accessories_whitelist");
    public static final TagKey<EntityType<?>> MODIFIABLE_ENTITY_BLACKLIST = ofTag(Registries.ENTITY_TYPE,"modifiable_entity_accessories_blacklist");

    public static final TagKey<Enchantment> VALID_FOR_REDIRECTION = ofTag(Registries.ENCHANTMENT,"valid_for_redirection");

    public static <T, R extends Registry<T>> TagKey<T> ofTag(ResourceKey<R> resourceKey, String path) {
        return TagKey.create(resourceKey, Accessories.of(path));
    }
}
