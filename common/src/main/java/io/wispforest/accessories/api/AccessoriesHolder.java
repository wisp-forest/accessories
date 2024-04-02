package io.wispforest.accessories.api;

import io.wispforest.accessories.pond.AccessoriesAPIAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

public interface AccessoriesHolder {

    static Optional<AccessoriesHolder> get(@NotNull LivingEntity livingEntity){
        return ((AccessoriesAPIAccess) livingEntity).accessoriesHolder();
    }

    //--

    Map<String, AccessoriesContainer> getSlotContainers();

    //--

    boolean cosmeticsShown();

    AccessoriesHolder cosmeticsShown(boolean value);

    boolean linesShown();

    AccessoriesHolder linesShown(boolean value);

    int scrolledSlot();

    AccessoriesHolder scrolledSlot(int slot);

    boolean showUnusedSlots();

    AccessoriesHolder showUnusedSlots(boolean value);

}