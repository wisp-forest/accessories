package io.wispforest.accessories.api.data;

import io.wispforest.accessories.Accessories;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

import static io.wispforest.accessories.api.data.AccessoriesBaseData.*;

public class AccessoriesTags {

    /**
     * Below are tags for the base implemented slots acting as common slots for people to use.
     */
    public static final TagKey<Item> ANKLET_TAG = itemTag(ANKLET_SLOT);
    public static final TagKey<Item> BACK_TAG = itemTag(BACK_SLOT);
    public static final TagKey<Item> BELT_TAG = itemTag(BELT_SLOT);
    public static final TagKey<Item> CAPE_TAG = itemTag(CAPE_SLOT);
    public static final TagKey<Item> CHARM_TAG = itemTag(CHARM_SLOT);
    public static final TagKey<Item> FACE_TAG = itemTag(FACE_SLOT);
    public static final TagKey<Item> HAND_TAG = itemTag(HAND_SLOT);
    public static final TagKey<Item> HAT_TAG = itemTag(HAT_SLOT);
    public static final TagKey<Item> NECKLACE_TAG = itemTag(NECKLACE_SLOT);
    public static final TagKey<Item> RING_TAG = itemTag(RING_SLOT);
    public static final TagKey<Item> SHOES_TAG = itemTag(SHOES_SLOT);
    public static final TagKey<Item> WRIST_TAG = itemTag(WRIST_SLOT);

    /**
     * @deprecated Use {@link #ANY_TAG} instead!
     */
    @Deprecated(forRemoval = true)
    public static final TagKey<Item> ALL_TAG = itemTag("all");

    /**
     * Slot tag used to allow for this given items contained inside to be
     * equipped to any slot if the such has the {@link AccessoriesBaseData#TAG_PREDICATE_ID}
     */
    public static final TagKey<Item> ANY_TAG = itemTag("any");

    /**
     * Tag used to add to the default binding added by Accessories
     */
    public static final TagKey<EntityType<?>> DEFAULTED_TARGETS_BINDING = entityTag("defaulted_targets");

    public static final TagKey<EntityType<?>> EQUIPMENT_MANAGEABLE = TagKey.create(Registries.ENTITY_TYPE, Accessories.of("equipment_manageable"));

    public static final TagKey<EntityType<?>> MODIFIABLE_ENTITY_BLACKLIST = entityTag("modifiable_entity_accessories_blacklist");
    public static final TagKey<EntityType<?>> MODIFIABLE_ENTITY_WHITELIST = entityTag("modifiable_entity_accessories_whitelist");

    /**
     * Tag used to disallow the given {@link Enchantment}'s within the tag to be
     * redirected to an Accessory {@link ItemStack} when iterated
     */
    public static final TagKey<Enchantment> INVALID_FOR_REDIRECTION = of(Registries.ENCHANTMENT, "invalid_for_redirection");

    public static TagKey<Item> itemTag(String path) {
        return of(Registries.ITEM, path);
    }

    public static TagKey<EntityType<?>> entityTag(String path) {
        return of(Registries.ENTITY_TYPE, path);
    }

    public static <T> TagKey<T> of(ResourceKey<? extends Registry<T>> key, String path) {
        return TagKey.create(key, Accessories.of(path));
    }
}
