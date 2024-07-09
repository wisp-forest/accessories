package io.wispforest.accessories.api.attributes;

import com.google.common.collect.*;
import io.wispforest.accessories.api.slot.NestedSlotReferenceImpl;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Builder used to collect the attribute modifications from a given Accessory with the ability
 * to specified if an Attribute modification can be stacked or is exclusive to one version
 */
public final class AccessoryAttributeBuilder {

    private final Map<ResourceLocation, AttributeModificationData> exclusiveAttributes = new HashMap<>();
    private final Multimap<ResourceLocation, AttributeModificationData> stackedAttributes = LinkedHashMultimap.create();

    private final SlotReference slotReference;

    @ApiStatus.Internal
    public AccessoryAttributeBuilder(SlotReference slotReference) {
        this.slotReference = slotReference;
    }

    @ApiStatus.Internal
    public AccessoryAttributeBuilder(String slotName, int slot) {
        this.slotReference = SlotReference.of(null, slotName, slot);
    }

    @ApiStatus.Internal
    public AccessoryAttributeBuilder() {
        this.slotReference = SlotReference.of(null, "", 0);
    }

    /**
     * Adds a given attribute modifier as an exclusive modifier meaning that only one instance should ever exist
     */
    public AccessoryAttributeBuilder addExclusive(Holder<Attribute> attribute, ResourceLocation location, double amount, AttributeModifier.Operation operation) {
        this.addExclusive(attribute, new AttributeModifier(location, amount, operation));

        return this;
    }

    /**
     * Adds a given attribute modifier as a stackable modifier meaning variants based on slot postion is allowed. Such is done by post process
     * step of appending slot information when adding to the living entity
     */
    public AccessoryAttributeBuilder addStackable(Holder<Attribute> attribute, ResourceLocation location, double amount, AttributeModifier.Operation operation) {
        this.addStackable(attribute, new AttributeModifier(location, amount, operation));

        return this;
    }

    /**
     * Adds a given attribute modifier as an exclusive modifier meaning that only one instance should ever exist
     */
    public AccessoryAttributeBuilder addExclusive(Holder<Attribute> attribute, AttributeModifier modifier) {
        exclusiveAttributes.putIfAbsent(modifier.id(), new AttributeModificationData(attribute, modifier));

        return this;
    }

    /**
     * Adds a given attribute modifier as a stackable modifier meaning variants based on slot postion is allowed. Such is done by post process
     * step of appending slot information when adding to the living entity
     */
    public AccessoryAttributeBuilder addStackable(Holder<Attribute> attribute, AttributeModifier modifier) {
        stackedAttributes.put(modifier.id(), new AttributeModificationData(createSlotPath(this.slotReference), attribute, modifier));

        return this;
    }

    @Nullable
    public AttributeModificationData getExclusive(ResourceLocation location) {
        return this.exclusiveAttributes.get(location);
    }

    public Collection<AttributeModificationData> getStacks(ResourceLocation location) {
        return this.stackedAttributes.get(location);
    }

    public AttributeModificationData removeExclusive(ResourceLocation location) {
        return this.exclusiveAttributes.remove(location);
    }

    public Collection<AttributeModificationData> removeStacks(ResourceLocation location) {
        return this.stackedAttributes.removeAll(location);
    }

    //--

    public Multimap<String, AttributeModifier> getSlotModifiers() {
        var map = LinkedHashMultimap.<String, AttributeModifier>create();

        this.exclusiveAttributes.forEach((location, uniqueInstance) -> {
            if(!(uniqueInstance.attribute().value() instanceof SlotAttribute slotAttribute)) return;

            map.put(slotAttribute.slotName(), uniqueInstance.modifier());
        });

        this.stackedAttributes.forEach((location, stackedInstance) -> {
            if(!(stackedInstance.attribute().value() instanceof SlotAttribute slotAttribute)) return;

            map.put(slotAttribute.slotName(), stackedInstance.modifier());
        });

        return map;
    }

    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(boolean filterSlots) {
        var map = LinkedHashMultimap.<Holder<Attribute>, AttributeModifier>create();

        this.exclusiveAttributes.forEach((location, uniqueInstance) -> {
            if(filterSlots && uniqueInstance.attribute().value() instanceof SlotAttribute) return;

            map.put(uniqueInstance.attribute(), uniqueInstance.modifier());
        });

        this.stackedAttributes.forEach((location, stackedInstance) -> {
            if(filterSlots && stackedInstance.attribute().value() instanceof SlotAttribute) return;

            map.put(stackedInstance.attribute(), stackedInstance.modifier());
        });

        return map;
    }

    public boolean isEmpty() {
        return this.exclusiveAttributes.isEmpty() && this.stackedAttributes.isEmpty();
    }

    public Map<ResourceLocation, AttributeModificationData> exclusiveAttributes() {
        return ImmutableMap.copyOf(this.exclusiveAttributes);
    }

    public Multimap<ResourceLocation, AttributeModificationData> stackedAttributes() {
        return ImmutableMultimap.copyOf(this.stackedAttributes);
    }

    public AccessoryAttributeBuilder addFrom(AccessoryAttributeBuilder builder) {
        builder.exclusiveAttributes.forEach(this.exclusiveAttributes::putIfAbsent);
        this.stackedAttributes.putAll(builder.stackedAttributes);

        return this;
    }

    // slotPath          = {slot_name}/{slot_index}[{nested_layer_info}]
    // nested_layer_info = /nest_{layer_index}_{slot_index}
    public static String createSlotPath(SlotReference ref) {
        var slotPath = new StringBuilder(ref.slotName().replace(":", "-") + "/" + ref.slot());

        if(ref instanceof NestedSlotReferenceImpl nestedRef) {
            var innerSlotIndices = nestedRef.innerSlotIndices();

            for (int i = 0; i < nestedRef.innerSlotIndices().size(); i++) {
                var innerIndex = innerSlotIndices.get(i);
                slotPath.append("/nest_")
                        .append(i)
                        .append("_")
                        .append(innerIndex);
            }
        }

        return slotPath.toString();
    }

    public static String createSlotPath(String slotname, int slot) {
        return slotname + "/" + slot;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof AccessoryAttributeBuilder otherBuilder)) return false;
        if(!areMapsEqual(this.stackedAttributes, otherBuilder.stackedAttributes)) return false;

        return this.exclusiveAttributes.equals(otherBuilder.exclusiveAttributes);
    }

    private static <K, V> boolean areMapsEqual(Multimap<K, V> multimap1, Multimap<K, V> multimap2) {
        for (var entry : multimap1.asMap().entrySet()) {
            if(entry.getValue().equals(multimap2.get(entry.getKey()))) return false;
        }

        return true;
    }
}
