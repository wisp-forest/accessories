package io.wispforest.accessories.api;

import io.wispforest.accessories.pond.AccessoriesAPIAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public interface AccessoriesHolder extends InstanceCodecable {

    static Optional<AccessoriesHolder> get(@NotNull LivingEntity livingEntity){
        return ((AccessoriesAPIAccess) livingEntity).accessoriesHolder();
    }

    //--

    Map<String, AccessoriesContainer> getSlotContainers();

    boolean cosmeticsShown();

    AccessoriesHolder cosmeticsShown(boolean value);

    boolean linesShown();

    AccessoriesHolder linesShown(boolean value);

    int scrolledSlot();

    AccessoriesHolder scrolledSlot(int slot);

    @Override
    void write(CompoundTag tag);

    @Override
    void read(CompoundTag tag);
}