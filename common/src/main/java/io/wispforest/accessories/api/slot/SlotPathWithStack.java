package io.wispforest.accessories.api.slot;

import net.minecraft.world.item.ItemStack;

public interface SlotPathWithStack {

    static SlotPathWithStack of(SlotPath slotPath, ItemStack stack) {
        return new SlotPathWithStack() {
            @Override public SlotPath path() { return slotPath; }
            @Override public ItemStack stack() { return stack; }
        };
    }

    SlotPath path();

    ItemStack stack();
}
