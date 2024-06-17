package io.wispforest.accessories.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.AccessoryNest;
import io.wispforest.accessories.api.components.AccessoryNestContainerContents;
import io.wispforest.accessories.api.slot.NestedSlotReferenceImpl;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class AccessoryNestUtils {

    @Nullable
    public static AccessoryNestContainerContents getData(ItemStack stack){
        var accessory = AccessoriesAPI.getAccessory(stack.getItem());

        if(!(accessory instanceof AccessoryNest)) return null;

        return stack.get(AccessoriesDataComponents.NESTED_ACCESSORIES);
    }

    public static <T> @Nullable T recursiveStackHandling(ItemStack stack, SlotReference reference, BiFunction<ItemStack, SlotReference, @Nullable T> function) {
        var accessory = AccessoriesAPI.getOrDefaultAccessory(stack.getItem());

        var value = function.apply(stack, reference);

        if (accessory instanceof AccessoryNest holdable && value == null) {
            var innerStacks = holdable.getInnerStacks(stack);

            for (int i = 0; i < innerStacks.size(); i++) {
                var innerStack = innerStacks.get(i);

                if (innerStack.isEmpty()) continue;

                value = recursiveStackHandling(stack, create(reference, i), function);
            }
        }

        return value;
    }

    public static void recursiveStackConsumption(ItemStack stack, SlotReference reference,  BiConsumer<ItemStack, SlotReference> consumer) {
        var accessory = AccessoriesAPI.getOrDefaultAccessory(stack.getItem());

        consumer.accept(stack, reference);

        if (!(accessory instanceof AccessoryNest holdable)) return;

        var innerStacks = holdable.getInnerStacks(stack);

        for (int i = 0; i < innerStacks.size(); i++) {
            var innerStack = innerStacks.get(i);

            if (innerStack.isEmpty()) continue;

            recursiveStackConsumption(stack, create(reference, i), consumer);
        }
    }

    public static NestedSlotReferenceImpl create(SlotReference reference, int innerIndex) {
        var innerSlotIndices = new ArrayList<Integer>();

        if(reference instanceof NestedSlotReferenceImpl nestedSlotReference) {
            innerSlotIndices.addAll(nestedSlotReference.innerSlotIndices());
        }

        innerSlotIndices.add(innerIndex);

        return SlotReference.ofNest(reference.entity(), reference.slotName(), reference.slot(), innerSlotIndices);
    }
}
