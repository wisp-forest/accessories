package io.wispforest.accessories.api.attributes;

import com.google.common.collect.Multimap;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.accessories.utils.AttributeUtils;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Custom Attribute used to target an accessories Slot for modification of its size
 * based on the given {@link AttributeModifier} within the Accessories System
 */
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

    //--

    public static void addSlotModifier(Multimap<Attribute, AttributeModifier> map, SlotType slotType, ResourceLocation location, double amount, AttributeModifier.Operation operation) {
        var data = AttributeUtils.getModifierData(location);

        addSlotModifier(map, slotType.name(), data.first(), data.second(), amount, operation);
    }

    public static void addSlotModifier(Multimap<Attribute, AttributeModifier> map, String slot, ResourceLocation location, double amount, AttributeModifier.Operation operation) {
        var data = AttributeUtils.getModifierData(location);

        addSlotModifier(map, slot, data.first(), data.second(), amount, operation);
    }

    public static void addSlotModifier(Multimap<Attribute, AttributeModifier> map, SlotType slotType, String name, UUID id, double amount, AttributeModifier.Operation operation) {
        addSlotModifier(map, slotType.name(), name, id, amount, operation);
    }

    public static void addSlotModifier(Multimap<Attribute, AttributeModifier> map, String slot, String name, UUID id, double amount, AttributeModifier.Operation operation) {
        map.put(SlotAttribute.getSlotAttribute(slot), new AttributeModifier(id, name, amount, operation));
    }

    public static void addSlotAttribute(AccessoryAttributeBuilder builder, String targetSlot, ResourceLocation location, double amount, AttributeModifier.Operation operation, boolean isStackable) {
        if(isStackable) {
            builder.addStackable(SlotAttribute.getSlotAttribute(targetSlot), location, amount, operation);
        } else {
            builder.addExclusive(SlotAttribute.getSlotAttribute(targetSlot), location, amount, operation);
        }
    }

    public static void addSlotAttribute(ItemStack stack, String targetSlot, String boundSlot, ResourceLocation location, double amount, AttributeModifier.Operation operation, boolean isStackable) {
        AccessoriesAPI.addAttribute(stack, boundSlot, SlotAttribute.getSlotAttribute(targetSlot), location, amount, operation, isStackable);
    }

    public static void addSlotAttribute(ItemStack stack, String targetSlot, String boundSlot, String name, UUID uuid, double amount, AttributeModifier.Operation operation, boolean isStackable) {
        AccessoriesAPI.addAttribute(stack, boundSlot, SlotAttribute.getSlotAttribute(targetSlot), name, uuid, amount, operation, isStackable);
    }
}
