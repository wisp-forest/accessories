package io.wispforest.accessories.impl;

import io.wispforest.accessories.Accessories;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.enchantment.Enchantment;

@Deprecated(forRemoval = true)
public class AccessoriesTags {

    public static final TagKey<EntityType<?>> MODIFIABLE_ENTITY_WHITELIST = io.wispforest.accessories.api.data.AccessoriesTags.MODIFIABLE_ENTITY_WHITELIST;
    public static final TagKey<EntityType<?>> MODIFIABLE_ENTITY_BLACKLIST = io.wispforest.accessories.api.data.AccessoriesTags.MODIFIABLE_ENTITY_BLACKLIST;

    public static final TagKey<Enchantment> VALID_FOR_REDIRECTION = ofTag(Registries.ENCHANTMENT, "valid_for_redirection");

    public static <T, R extends Registry<T>> TagKey<T> ofTag(ResourceKey<R> resourceKey, String path) {
        return TagKey.create(resourceKey, Accessories.of(path));
    }
}
