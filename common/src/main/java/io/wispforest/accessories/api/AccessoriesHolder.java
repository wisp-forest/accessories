package io.wispforest.accessories.api;

import net.minecraft.nbt.CompoundTag;

import java.util.Map;
import java.util.Set;

public interface AccessoriesHolder extends InstanceCodecable {

    Map<String, AccessoriesContainer> getSlotContainers();

    boolean cosmeticsShown();

    AccessoriesHolder cosmeticsShown(boolean value);

    int scrolledSlot();

    AccessoriesHolder scrolledSlot(int slot);

    @Override
    void write(CompoundTag tag);

    @Override
    void read(CompoundTag tag);
}
