package io.wispforest.accessories.api;

import net.minecraft.world.item.Item;

import java.util.function.Predicate;

/**
 * Controls how {@link AccessoriesCapability} methods handle cosmetic accessories.
 *
 * @see AccessoriesCapability#getFirstEquipped(Item, EquipmentChecking)
 * @see AccessoriesCapability#getFirstEquipped(Predicate, EquipmentChecking)
 */
public enum EquipmentChecking {
    /**
     * Only non-cosmetic accessories are considered.
     */
    ACCESSORIES_ONLY,
    /**
     * Cosmetic accessories are preferred over non-cosmetic accessories.
     */
    COSMETICALLY_OVERRIDABLE
}
