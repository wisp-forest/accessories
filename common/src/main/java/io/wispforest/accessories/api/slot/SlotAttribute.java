package io.wispforest.accessories.api.slot;

import com.google.common.collect.Multimap;
import io.wispforest.accessories.api.AccessoriesAPI;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SlotAttribute extends Attribute {

    private static final Map<String, SlotAttribute> CACHED_ATTRIBUTES = new HashMap<>();

    private final String slotName;

    private SlotAttribute(String slotName) {
        super(slotName, 0);

        this.slotName = slotName;
    }

    public String slotName(){
        return this.slotName;
    }

    public static SlotAttribute getSlotAttribute(SlotType slotType){
        return getSlotAttribute(slotType.name());
    }

    public static SlotAttribute getSlotAttribute(String slotName){
        return CACHED_ATTRIBUTES.computeIfAbsent(slotName, SlotAttribute::new);
    }

    public static void addSlotModifier(Multimap<Attribute, AttributeModifier> map, SlotType slotType, UUID id, double amount, AttributeModifier.Operation operation) {
        addSlotModifier(map, slotType.name(), id, amount, operation);
    }

    public static void addSlotModifier(Multimap<Attribute, AttributeModifier> map, String slot, UUID id, double amount, AttributeModifier.Operation operation) {
        map.put(SlotAttribute.getSlotAttribute(slot), new AttributeModifier(id, slot, amount, operation));
    }

    public static void addSlotAttribute(ItemStack stack, String targetSlot, String boundSlot, String name, UUID id, double amount, AttributeModifier.Operation operation) {
        addSlotAttribute(stack.getOrCreateTag(), targetSlot, boundSlot, name, id, amount, operation);
    }

    public static void addSlotAttribute(CompoundTag tag, String targetSlot, String boundSlot, String name, UUID id, double amount, AttributeModifier.Operation operation) {
        AccessoriesAPI.addAttribute(tag, boundSlot, SlotAttribute.getSlotAttribute(targetSlot), name, id, amount, operation);
    }
}
