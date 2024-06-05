package io.wispforest.accessories.impl;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.AccessoryNest;
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

    private final static LoadingCache<ItemStack, StackData> CACHE = CacheBuilder.newBuilder()
            .concurrencyLevel(1)
            .expireAfterAccess(Duration.ofSeconds(1))
            //.maximumSize(1000)
            .weakKeys()
            .build(CacheLoader.from(StackData::new));

    @Nullable
    public static StackData getData(ItemStack stack){
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

    private static NestedSlotReferenceImpl create(SlotReference reference, int innerIndex) {
        var innerSlotIndices = new ArrayList<Integer>();

        if(reference instanceof NestedSlotReferenceImpl nestedSlotReference) {
            innerSlotIndices.addAll(nestedSlotReference.innerSlotIndices());
        }

        innerSlotIndices.add(innerIndex);

        return SlotReference.ofNest(reference.entity(), reference.slotName(), reference.slot(), innerSlotIndices);
    }

    public static class StackData {
        private final AccessoryNest accessoryNest;
        private final ItemStack stack;

        private final List<ItemStack> subStacks = new ArrayList<>();
        private final List<ItemStack> defensiveCopies = new ArrayList<>();
        private CompoundTag defensiveNbtData;

        private StackData(ItemStack stack) {
            this.accessoryNest = (AccessoryNest) AccessoriesAPI.getAccessory(stack.getItem());

            this.stack = stack;

            if (stack.hasTag()) {
                this.defensiveNbtData = stack.getTag().copy();

                var items = accessoryNest.getInnerStacks(stack);
                for (var item : items) {
                    this.subStacks.add(item);
                    this.defensiveCopies.add(item.copy());
                }
            }
        }

        public final boolean isInvalid() {
            return !Objects.equals(stack.getTag(), defensiveNbtData);
        }

        public final List<ItemStack> getStacks() {
            return subStacks;
        }

        public final List<ItemStack> getDefensiveCopies() {
            return defensiveCopies;
        }

        public final Map<ItemStack, Accessory> getMap() {
            var map = new LinkedHashMap<ItemStack, Accessory>();

            this.getStacks().forEach(stack1 -> map.put(stack1, AccessoriesAPI.getOrDefaultAccessory(stack1)));

            return map;
        }

        public final Map<SlotEntryReference, Accessory> getMap(SlotReference slotReference) {
            var map = new LinkedHashMap<SlotEntryReference, Accessory>();

            var innerStacks = this.getStacks();

            for (int i = 0; i < innerStacks.size(); i++) {
                var innerStack = innerStacks.get(i);

                if (innerStack.isEmpty()) continue;

                map.put(new SlotEntryReference(create(slotReference, i), innerStack), AccessoriesAPI.getOrDefaultAccessory(innerStack));
            }

            return map;
        }

        public final AccessoryNest getNest() {
            return this.accessoryNest;
        }
    }
}
