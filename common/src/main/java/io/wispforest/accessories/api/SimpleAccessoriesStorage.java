package io.wispforest.accessories.api;

import io.wispforest.accessories.impl.core.ExpandedContainer;
import net.minecraft.world.Container;

import java.util.Map;

public record SimpleAccessoriesStorage(boolean isClientSide, String slotName, int size, Map<Integer, Boolean> renderOptions, Container accessories, Container cosmeticAccessories) implements AccessoriesStorage {

    public static AccessoriesStorage copy(AccessoriesStorage storage) {
        return new SimpleAccessoriesStorage(
            storage.isClientSide(),
            storage.getSlotName(),
            storage.getSize(),
            storage.renderOptions(),
            copyContainer(storage.getAccessories()),
            copyContainer(storage.getCosmeticAccessories())
        );
    }

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

    private static Container copyContainer(Container container) {
        if (container instanceof ExpandedContainer expandedContainer) {
            return expandedContainer.toImmutable();
        }

        return container;
    }
}
