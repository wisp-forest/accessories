package io.wispforest.accessories.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.components.AccessoriesDataComponents;
import io.wispforest.accessories.api.AccessoryNest;
import io.wispforest.accessories.api.components.AccessoryNestContainerContents;
import io.wispforest.accessories.api.slot.NestedSlotReferenceImpl;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class AccessoryNestUtils {

    private final static LoadingCache<ItemStack, AccessoryNestContainerContents> CACHE = CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .expireAfterAccess(Duration.ofSeconds(1))
            //.maximumSize(1000)
            .weakKeys()
            .build(CacheLoader.from((stack) -> {
                return AccessoriesDataComponents.readOrDefault(AccessoriesDataComponents.NESTED_ACCESSORIES, stack);
            }));

    @Nullable
    public static AccessoryNestContainerContents getData(ItemStack stack){
        var accessory = AccessoriesAPI.getAccessory(stack.getItem());

        if(!(accessory instanceof AccessoryNest)) return null;

        var data = CACHE.getUnchecked(stack);

        if (data.isInvalid()) {
            CACHE.refresh(stack);
            data = CACHE.getUnchecked(stack);
        }

        return data;
    }

    public static <T> @Nullable T recursiveStackHandling(ItemStack stack, SlotReference reference, BiFunction<ItemStack, SlotReference, @Nullable T> function) {
        var accessory = AccessoriesAPI.getOrDefaultAccessory(stack.getItem());

        var value = function.apply(stack, reference);

        if (accessory instanceof AccessoryNest holdable && value == null) {
            var innerStacks = holdable.getInnerStacks(stack);

            for (int i = 0; i < innerStacks.size(); i++) {
                var innerStack = innerStacks.get(i);

                if (innerStack.isEmpty()) continue;

                value = recursiveStackHandling(innerStack, create(reference, i), function);
            }
        }

        return value;
    }

    public static void recursiveStackConsumption(ItemStack stack, SlotReference reference, BiConsumer<ItemStack, SlotReference> consumer) {
        var accessory = AccessoriesAPI.getOrDefaultAccessory(stack.getItem());

        consumer.accept(stack, reference);

        if (!(accessory instanceof AccessoryNest holdable)) return;

        var innerStacks = holdable.getInnerStacks(stack);

        for (int i = 0; i < innerStacks.size(); i++) {
            var innerStack = innerStacks.get(i);

            if (innerStack.isEmpty()) continue;

            recursiveStackConsumption(innerStack, create(reference, i), consumer);
        }
    }

    public static SlotReference create(SlotReference reference, int innerIndex) {
        var innerSlotIndices = new ArrayList<Integer>();

        if(reference instanceof NestedSlotReferenceImpl nestedSlotReference) {
            innerSlotIndices.addAll(nestedSlotReference.innerSlotIndices());
        }

        innerSlotIndices.add(innerIndex);

        return SlotReference.ofNest(reference.entity(), reference.slotName(), reference.slot(), innerSlotIndices);
    }
}
