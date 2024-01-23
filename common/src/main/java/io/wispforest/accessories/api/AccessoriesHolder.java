package io.wispforest.accessories.api;

import net.minecraft.nbt.CompoundTag;

import java.util.Map;
import java.util.Set;

public interface AccessoriesHolder extends InstanceCodecable {

    Map<String, AccessoriesContainer> getSlotContainers();

    Set<AccessoriesContainer> updatedContainers();

    @Override
    void write(CompoundTag tag);

    @Override
    void read(CompoundTag tag);
}
