package io.wispforest.accessories.utils;

import io.wispforest.accessories.mixin.ItemStackAccessor;
import io.wispforest.accessories.pond.stack.PatchedDataComponentMapExtension;
import io.wispforest.owo.util.EventStream;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public interface ItemStackMutation {

    static EventStream<ItemStackMutation> getEvent(ItemStack stack) {
        return ((PatchedDataComponentMapExtension) (Object) ((ItemStackAccessor) (Object) stack).accessories$components()).accessories$getMutationEvent(stack);
    }

    void onMutation(ItemStack stack, List<DataComponentType<?>> types);
}
