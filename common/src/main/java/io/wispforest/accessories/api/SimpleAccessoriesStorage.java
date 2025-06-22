package io.wispforest.accessories.api;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;

import java.util.List;

public record SimpleAccessoriesStorage(boolean isClientSide, String slotName, int size, List<Boolean> renderOptions, Container accessories, Container cosmeticAccessories) implements AccessoriesStorage {

    @Override
    public String getSlotName() {
        return slotName;
    }

    @Override
    public Container getAccessories() {
        return accessories;
    }

    @Override
    public Container getCosmeticAccessories() {
        return cosmeticAccessories;
    }

    @Override
    public int getSize() {
        return size;
    }
}
