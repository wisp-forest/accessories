package io.wispforest.accessories.api.slot;

import com.google.common.collect.ImmutableList;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.AccessoriesContainer;
import io.wispforest.accessories.data.SlotTypeLoader;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public sealed interface SlotReference permits NestedSlotReferenceImpl, SlotReferenceImpl {

    static SlotReference of(LivingEntity livingEntity, String slotName, int slot) {
        return new SlotReferenceImpl(livingEntity, slotName, slot);
    }

    static NestedSlotReferenceImpl ofNest(LivingEntity livingEntity, String slotName, int initialHolderSlot, List<Integer> innerSlotIndices) {
        return new NestedSlotReferenceImpl(livingEntity, slotName, initialHolderSlot, ImmutableList.copyOf(innerSlotIndices));
    }

    String slotName();

    LivingEntity entity();

    int slot();

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

    @Nullable
    default ItemStack getStack() {
        var container = this.slotContainer();

        if(container == null) return null;

        return container.getAccessories().getItem(slot());
    }

    default boolean setStack(ItemStack stack) {
        var container = this.slotContainer();

        if(container == null) return false;

        container.getAccessories().setItem(slot(), stack);

        return true;
    }
}
