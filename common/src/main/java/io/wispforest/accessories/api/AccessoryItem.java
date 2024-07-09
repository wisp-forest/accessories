package io.wispforest.accessories.api;

import net.minecraft.world.item.Item;

/**
 * An Implemented version of the {@link Accessory} interface with the {@link Item} class with
 * automatic registration of such object at the end of the constructor call.
 */
public class AccessoryItem extends Item implements Accessory {
    public AccessoryItem(Properties properties) {
        super(properties);

        AccessoriesAPI.registerAccessory(this, this);
    }
}
