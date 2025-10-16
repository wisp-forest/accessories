package io.wispforest.accessories.utils;

import io.wispforest.accessories.pond.stack.ItemStackExtension;
import io.wispforest.owo.util.EventStream;
import net.minecraft.world.item.ItemStack;

public interface ItemStackResize {
    static EventStream<ItemStackResize> getEvent(ItemStack stack) {
        return ((ItemStackExtension) (Object) stack).accessories$getResizeEvent();
    }

    void onResize(ItemStack stack, int prevSize);
}
