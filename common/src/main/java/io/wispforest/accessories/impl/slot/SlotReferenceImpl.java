package io.wispforest.accessories.impl.slot;

import com.google.common.collect.Lists;
import io.wispforest.accessories.api.core.AccessoryNest;
import io.wispforest.accessories.api.core.AccessoryRegistry;
import io.wispforest.accessories.api.slot.SlotPath;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

@ApiStatus.Internal
public record SlotReferenceImpl(LivingEntity entity, SlotPath slotPath) implements SlotReference {
    public SlotReferenceImpl {
        if(slotPath.index() < -1) {
            throw new IndexOutOfBoundsException("A given Slot Reference was attempted to be created with a negative index!");
        }
    }

    //--

    @Override
    public boolean isValid() {
        var capability = this.capability();

        if(capability == null) return false;

        var container = capability.getContainers().get(this.slotName());

        if(container == null) return false;

        var invContainer = container.getAccessories();

        var validIndex = invContainer.validIndex(index());

        // If Valid index and not NestedSlotPath then we can just use the result
        // or if invalid index and is nested then just return the false result
        if (!(slotPath.isNested()) || !validIndex) return validIndex;

        var selectedStack = container.getAccessories().getItem(index());

        for (var innerSlotIndex : slotPath.innerIndices()) {
            var nestLayer = tryAndGet(selectedStack, innerSlotIndex);

            if(nestLayer == null) return false;

            selectedStack = nestLayer.getInnerStack();
        }

        return true;
    }

    @Override
    @Nullable
    public ItemStack getStack() {
        var container = this.slotContainer();

        if(container == null) return null;

        var invContainer = container.getAccessories();

        if (!invContainer.validIndex(index())) return null;

        var selectedStack = invContainer.getItem(index());

        // If not a NestedSlotPath then return the stack
        // but if we are then we will need to go though the paths inner indices
        if (!(slotPath.isNested())) return selectedStack;

        for (var innerSlotIndex : slotPath.innerIndices()) {
            var nestLayer = tryAndGet(selectedStack, innerSlotIndex);

            if(nestLayer == null) return null;

            selectedStack = nestLayer.getInnerStack();
        }

        return selectedStack;
    }

    @Nullable
    private static NestLayer tryAndGet(ItemStack holderStack, int innerIndex) {
        var accessory = AccessoryRegistry.getAccessoryOrDefault(holderStack);

        return (accessory instanceof AccessoryNest accessoryNest)
                ? new NestLayer(accessoryNest, holderStack, innerIndex)
                : null;
    }

    @Override
    public boolean setStack(ItemStack stack) {
        var container = this.slotContainer();

        if(container == null) return false;

        var invContainer = container.getAccessories();

        if (!invContainer.validIndex(index())) return false;

        if (!(slotPath.isNested())) {
            invContainer.setItem(index(), stack);

            return true;
        }

        var baseStack = invContainer.getItem(index());

        var layerStack = new ArrayList<NestLayer>();

        // First we build the Nest Layer stack for how far the innerSlotIndices go i.e. tracking how many nests deep we go
        for (var innerSlotIndex : slotPath.innerIndices()) {
            var layer = tryAndGet(baseStack, innerSlotIndex);

            if(layer == null) return false;

            layerStack.add(layer);
            baseStack = layer.getInnerStack();
        }

        var innerStack = stack;

        // Second we reverse the list of layers getting the most inner nest target and work are way out setting the stack to
        // properly track data component updates
        for (var layer : Lists.reverse(layerStack)){
            if(!layer.setInnerStack(innerStack)) return false;

            innerStack = layer.holderStack();
        }

        // Finally we set the base innerStack as the new stack in invContainer which is just a mutation of the stack but
        // better to set to properly update everything
        invContainer.setItem(index(), innerStack);

        return true;
    }

    private record NestLayer(AccessoryNest accessoryNest, ItemStack holderStack, int index) {
        private boolean setInnerStack(ItemStack innerStack) {
            return accessoryNest.setInnerStack(holderStack, index, innerStack);
        }

        private ItemStack getInnerStack() {
            return accessoryNest.getInnerStacks(holderStack).get(index);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SlotPath otherPath)) return false;

        return SlotPath.areEqual(this, otherPath);
    }
}
