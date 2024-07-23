package io.wispforest.accessories.api;

import net.minecraft.world.item.Item;

/**
 * Helper base class for accessory items with automatic registration.
 */
public class AccessoryItem extends Item implements Accessory {
    public AccessoryItem(Properties properties) {
        super(properties);

        AccessoriesAPI.registerAccessory(this, this);
    }
}
