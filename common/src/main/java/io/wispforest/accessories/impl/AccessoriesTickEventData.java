package io.wispforest.accessories.impl;

import io.wispforest.accessories.api.SlotType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class AccessoriesTickEventData {

    private final Map<String, ItemStack> lastEquippedAccessories = new HashMap<>();

    public void setLastEquippedAccessories(Map<String, ItemStack> value){
        this.lastEquippedAccessories.clear();
        this.lastEquippedAccessories.putAll(value);
    }

    public ItemStack previousAccessory(String slotId){
        return lastEquippedAccessories.getOrDefault(slotId, ItemStack.EMPTY);
    }
}
