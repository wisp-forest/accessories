package io.wispforest.accessories.pond.stack;

import io.wispforest.accessories.utils.ItemStackMutation;
import io.wispforest.owo.util.EventStream;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.world.item.ItemStack;

public interface PatchedDataComponentMapExtension {
    boolean accessories$hasChanged();

    EventStream<ItemStackMutation> accessories$getMutationEvent(ItemStack stack);
}
