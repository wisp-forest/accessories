package io.wispforest.accessories.api;

import net.minecraft.world.item.Item;

public class AccessoryItem extends Item implements Accessory {
    public AccessoryItem(Properties properties) {
        super(properties);

        AccessoriesAPI.registerAccessory(this, this);
    }
}
