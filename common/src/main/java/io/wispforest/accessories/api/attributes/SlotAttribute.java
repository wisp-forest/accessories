package io.wispforest.accessories.api.attributes;

import com.google.common.collect.Multimap;
import io.wispforest.accessories.api.AccessoriesAPI;
import io.wispforest.accessories.api.slot.SlotType;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom Attribute used to target an accessories Slot for modification of its size
 * based on the given {@link AttributeModifier} within the Accessories System
 */
public class SlotAttribute extends Attribute {

    private static final Map<String, Holder<Attribute>> CACHED_ATTRIBUTES = new HashMap<>();

    private final String slotName;

    protected SlotAttribute(String slotName) {
        super(slotName, 0);

        this.slotName = slotName;
    }

    public String slotName(){
        return this.slotName;
    }

    /**
     * @deprecated Use {{@link #getAttributeHolder(SlotType)}}
     */
    @Deprecated(forRemoval = true)
    public static SlotAttribute getSlotAttribute(SlotType slotType){
        return getSlotAttribute(slotType.name());
    }

    /**
     * @deprecated Use {{@link #getAttributeHolder(String)}}
     */
    @Deprecated(forRemoval = true)
    public static SlotAttribute getSlotAttribute(String slotName){
        return (SlotAttribute) getAttributeHolder(slotName).value();
    }

    public static Holder<Attribute> getAttributeHolder(SlotType slotType){
        return getAttributeHolder(slotType.name());
    }

    public static Holder<Attribute> getAttributeHolder(String slotName){
        return CACHED_ATTRIBUTES.computeIfAbsent(slotName, string -> Holder.direct(new SlotAttribute(slotName)));
    }

    //--

    public static void addSlotModifier(Multimap<Holder<Attribute>, AttributeModifier> map, SlotType slotType, ResourceLocation location, double amount, AttributeModifier.Operation operation) {
        addSlotModifier(map, slotType.name(), location, amount, operation);
    }

    public static void addSlotModifier(Multimap<Holder<Attribute>, AttributeModifier> map, String slot, ResourceLocation location, double amount, AttributeModifier.Operation operation) {
        map.put(SlotAttribute.getAttributeHolder(slot), new AttributeModifier(location, amount, operation));
    }

    public static void addSlotAttribute(AccessoryAttributeBuilder builder, String targetSlot, ResourceLocation location, double amount, AttributeModifier.Operation operation, boolean isStackable) {
        if(isStackable) {
            builder.addStackable(SlotAttribute.getAttributeHolder(targetSlot), location, amount, operation);
        } else {
            builder.addExclusive(SlotAttribute.getAttributeHolder(targetSlot), location, amount, operation);
        }
    }

    public static void addSlotAttribute(ItemStack stack, String targetSlot, String boundSlot, ResourceLocation location, double amount, AttributeModifier.Operation operation, boolean isStackable) {
        AccessoryAttributeUtils.addAttribute(stack, boundSlot, SlotAttribute.getAttributeHolder(targetSlot), location, amount, operation, isStackable);
    }
}
