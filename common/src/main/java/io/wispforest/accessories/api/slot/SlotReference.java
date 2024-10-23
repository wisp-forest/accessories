package io.wispforest.accessories.api.slot;

import com.google.common.collect.ImmutableList;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.impl.slot.NestedSlotReferenceImpl;
import io.wispforest.accessories.impl.slot.SlotReferenceImpl;
import io.wispforest.accessories.networking.AccessoriesNetworking;
import io.wispforest.accessories.networking.client.AccessoryBreak;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A reference to a specific accessory slot of a {@link LivingEntity}.
 */
public interface SlotReference {

    static SlotReference of(LivingEntity livingEntity, String slotName, int slot) {
        return new SlotReferenceImpl(livingEntity, slotName, slot);
    }

    static SlotReference ofNest(LivingEntity livingEntity, String slotName, int initialHolderSlot, List<Integer> innerSlotIndices) {
        return new NestedSlotReferenceImpl(livingEntity, slotName, initialHolderSlot, ImmutableList.copyOf(innerSlotIndices));
    }

    /**
     * @return the referenced slot name
     */
    String slotName();

    /**
     * @return the referenced entity
     */
    LivingEntity entity();

    /**
     * @return the referenced slot index
     */
    int slot();

    //--

    /**
     * Helper method to trigger effects of a given accessory being broken on any tracking clients for the given entity
     */
    default void breakStack() {
        AccessoriesNetworking.sendToTrackingAndSelf(this.entity(), AccessoryBreak.of(this));
    }

    default boolean isValid() {
        var capability = this.capability();

        if(capability == null) return false;

        var container = capability.getContainers().get(this.slotName());

        if(container == null) return false;

        return slot() < container.getSize();
    }

    default String createSlotPath() {
        return this.slotName().replace(":", "-") + "/" + this.slot();
    }

    @Nullable
    default SlotType type(){
        return SlotTypeLoader.getSlotType(entity().level(), slotName());
    }

    @Nullable
    default AccessoriesCapability capability() {
        return this.entity().accessoriesCapability();
    }

    @Nullable
    default AccessoriesContainer slotContainer() {
        var capability = this.capability();

        if(capability == null) return null;

        return capability.getContainers().get(slotName());
    }

    /**
     * @return the current referenced stack
     */
    @Nullable
    default ItemStack getStack() {
        var container = this.slotContainer();

        if(container == null) return null;

        return container.getAccessories().getItem(slot());
    }

    /**
     * @return {@code true} if the stack was successfully set, {@code false} otherwise
     */
    default boolean setStack(ItemStack stack) {
        var container = this.slotContainer();

        if(container == null) return false;

        container.getAccessories().setItem(slot(), stack);

        return true;
    }
}
