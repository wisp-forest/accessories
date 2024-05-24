package io.wispforest.accessories.api.slot;

import com.google.common.collect.Lists;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.AccessoryNest;
import it.unimi.dsi.fastutil.Pair;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public record NestedSlotReferenceImpl(LivingEntity entity, String slotName, int initialHolderSlot, List<Integer> innerSlotIndices) implements SlotReference {

    @Override
    public int slot() {
        return initialHolderSlot();
    }

    @Override
    @Nullable
    public ItemStack getStack() {
        var selectedStack = SlotReference.super.getStack();

        for (var innerSlotIndex : innerSlotIndices) {
            var innerData = tryAndGet(selectedStack, innerSlotIndex);

            if(innerData == null) return null;

            selectedStack = innerData.right();
        }

        return selectedStack;
    }

    @Nullable
    private static Pair<NestLayer, ItemStack> tryAndGet(ItemStack holderStack, int innerIndex) {
        var accessory = AccessoriesAPI.getAccessory(holderStack);

        if(!(accessory instanceof AccessoryNest accessoryNest)) return null;

        return Pair.of(new NestLayer(accessoryNest, holderStack, innerIndex), accessoryNest.getInnerStacks(holderStack).get(innerIndex));
    }

    @Override
    public boolean setStack(ItemStack stack) {
        var selectedStack = SlotReference.super.getStack();

        if(selectedStack == null) return false;

        var layerStack = new ArrayList<NestLayer>();

        for (var innerSlotIndex : innerSlotIndices) {
            var innerData = tryAndGet(selectedStack, innerSlotIndex);

            if(innerData == null) return false;

            layerStack.add(innerData.first());
            selectedStack = innerData.right();
        }

        var innerStack = stack;

        for (var layer : Lists.reverse(layerStack)){
            if(!layer.setStack(innerStack)) return false;

            innerStack = layer.holderStack;
        }

        SlotReference.super.setStack(innerStack);

        return true;
    }

    private record NestLayer(AccessoryNest accessoryNest, ItemStack holderStack, int index) {
        private boolean setStack(ItemStack innerStack) {
            return accessoryNest.setInnerStack(holderStack, index, innerStack);
        }
    }
}
