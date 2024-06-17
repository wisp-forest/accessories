package io.wispforest.accessories.api.components;

import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.Accessory;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.endec.CodecUtils;
import io.wispforest.accessories.impl.AccessoryNestUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record AccessoryNestContainerContents(List<ItemStack> accessories) {

    public static final AccessoryNestContainerContents EMPTY = new AccessoryNestContainerContents(List.of());

    public static final Endec<AccessoryNestContainerContents> ENDEC = StructEndecBuilder.of(
            CodecUtils.ofCodec(ItemStack.CODEC).listOf().fieldOf("accessories", AccessoryNestContainerContents::accessories),
            AccessoryNestContainerContents::new
    );

    public AccessoryNestContainerContents setStack(int index, ItemStack stack) {
        var accessories = new ArrayList<>(accessories());

        accessories.set(index, stack);

        return new AccessoryNestContainerContents(accessories);
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
}
