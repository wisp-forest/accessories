package io.wispforest.accessories.api.components;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.events.SlotStateChange;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.impl.AccessoryNestUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.CodecUtils;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public final class AccessoryNestContainerContents {

    public static final AccessoryNestContainerContents EMPTY = new AccessoryNestContainerContents(List.of());

    public static final Endec<AccessoryNestContainerContents> ENDEC = StructEndecBuilder.of(
            CodecUtils.toEndec(ItemStack.OPTIONAL_CODEC).listOf().fieldOf("accessories", AccessoryNestContainerContents::accessories),
            AccessoryNestContainerContents::new
    );

    private final List<ItemStack> accessories;

    private final Map<Integer, SlotStateChange> slotChanges = new Int2ObjectOpenHashMap<>();

    public AccessoryNestContainerContents(List<ItemStack> accessories) {
        this.accessories = accessories;
    }

    public AccessoryNestContainerContents setStack(int index, ItemStack stack) {
        var accessories = new ArrayList<>(accessories());

        accessories.set(index, stack);

        var contents = new AccessoryNestContainerContents(accessories);

        contents.slotChanges.putAll(slotChanges);
        contents.slotChanges.put(index, SlotStateChange.REPLACEMENT);

        return contents;
    }

    public AccessoryNestContainerContents addStack(ItemStack stack) {
        var accessories = new ArrayList<>(accessories());

        var index = accessories.size();

        accessories.add(stack);

        var contents = new AccessoryNestContainerContents(accessories);

        contents.slotChanges.putAll(slotChanges);
        contents.slotChanges.put(index, SlotStateChange.REPLACEMENT);

        return contents;
    }

    public Map<Integer, SlotStateChange> slotChanges() {
        return this.slotChanges;
    }

    public Map<ItemStack, Accessory> getMap() {
        var map = new LinkedHashMap<ItemStack, Accessory>();

        this.accessories().forEach(stack1 -> map.put(stack1, AccessoriesAPI.getOrDefaultAccessory(stack1)));

        return map;
    }

    public Map<SlotEntryReference, Accessory> getMap(SlotReference slotReference) {
        var map = new LinkedHashMap<SlotEntryReference, Accessory>();

        var innerStacks = this.accessories();

        for (int i = 0; i < innerStacks.size(); i++) {
            var innerStack = innerStacks.get(i);

            if (innerStack.isEmpty()) continue;

            map.put(new SlotEntryReference(AccessoryNestUtils.create(slotReference, i), innerStack), AccessoriesAPI.getOrDefaultAccessory(innerStack));
        }

        return map;
    }

    public List<ItemStack> accessories() {
        return accessories;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (AccessoryNestContainerContents) obj;
        return Objects.equals(this.accessories, that.accessories);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accessories);
    }

    @Override
    public String toString() {
        return "AccessoryNestContainerContents[" +
                "accessories=" + accessories + ']';
    }

}
