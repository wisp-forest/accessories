package io.wispforest.accessories.api;

import io.wispforest.accessories.client.gui.AccessoriesScreen;
import io.wispforest.accessories.pond.AccessoriesAPIAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Optional;

/**
 * Internal Holder object that has all the container data attached to the given player
 */
public interface AccessoriesHolder {

    static AccessoriesHolder get(@NotNull LivingEntity livingEntity){
        return ((AccessoriesAPIAccess) livingEntity).accessoriesHolder();
    }

    //--

    /**
     * @return If the cosmetic slots should be shown within the {@link AccessoriesScreen}
     */
    boolean cosmeticsShown();

    AccessoriesHolder cosmeticsShown(boolean value);

    /**
     * @return If the fancy line rendering utility should be shown within the {@link AccessoriesScreen}
     */
    boolean linesShown();

    AccessoriesHolder linesShown(boolean value);

    int scrolledSlot();

    AccessoriesHolder scrolledSlot(int slot);

    /**
     * @return If unused accessory slots should be present within the {@link AccessoriesScreen}
     */
    boolean showUnusedSlots();

    AccessoriesHolder showUnusedSlots(boolean value);

    /**
     * @return If unique accessory slots should be present within the {@link AccessoriesScreen}
     */
    boolean showUniqueSlots();

    AccessoriesHolder showUniqueSlots(boolean value);

}