package io.wispforest.accessories.api.attributes;

import com.google.common.collect.*;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.slot.NestedSlotReferenceImpl;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.utils.AttributeUtils;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

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
    public AccessoryAttributeBuilder addExclusive(Attribute attribute, ResourceLocation location, double amount, AttributeModifier.Operation operation) {
        var data = AttributeUtils.getModifierData(location);

        this.addExclusive(attribute, new AttributeModifier(data.second(), data.first(), amount, operation));

        return this;
    }

    /**
     * Adds a given attribute modifier as a stackable modifier meaning variants based on slot position is allowed. This is done by post process
     * step of appending slot information when adding to the living entity
     */
    public AccessoryAttributeBuilder addStackable(Attribute attribute, ResourceLocation location, double amount, AttributeModifier.Operation operation) {
        var data = AttributeUtils.getModifierData(location);

        this.addStackable(attribute, new AttributeModifier(data.second(), data.first(), amount, operation));

        return this;
    }

    /**
     * Adds a given attribute modifier as an exclusive modifier meaning that only one instance should ever exist
     */
    private AccessoryAttributeBuilder addExclusive(Attribute attribute, AttributeModifier modifier) {
        var location = AttributeUtils.getLocation(modifier.getName());

        exclusiveAttributes.putIfAbsent(location, new AttributeModificationData(attribute, modifier));

        return this;
    }

    /**
     * Adds a given attribute modifier as a stackable modifier meaning variants based on slot position is allowed. This is done by post process
     * step of appending slot information when adding to the living entity
     */
    private AccessoryAttributeBuilder addStackable(Attribute attribute, AttributeModifier modifier) {
        var location = AttributeUtils.getLocation(modifier.getName());

        stackedAttributes.put(location, new AttributeModificationData(createSlotPath(this.slotReference), attribute, modifier));

        return this;
    }

    //--

    @ApiStatus.Internal
    public AccessoryAttributeBuilder addModifier(Attribute attribute, AttributeModifier modifier, SlotReference slotReference, Function<String, ResourceLocation> locationBuilder) {
        var id = Accessories.of(AccessoryAttributeBuilder.createSlotPath(slotReference));

        var data = AttributeUtils.getModifierData(id);

        var validName = modifier.getName().toLowerCase()
                .replace(" ", "_")
                .replaceAll("([^a-z0-9/._-])", "");

        if(modifier.getId().equals(data.right())) {
            this.addStackable(attribute, locationBuilder.apply(validName), modifier.getAmount(), modifier.getOperation());
        } else {
            this.addExclusive(attribute, locationBuilder.apply(validName), modifier.getAmount(), modifier.getOperation());
        }

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
            if(!(uniqueInstance.attribute() instanceof SlotAttribute slotAttribute)) return;

            map.put(slotAttribute.slotName(), uniqueInstance.modifier());
        });

        this.stackedAttributes.forEach((location, stackedInstance) -> {
            if(!(stackedInstance.attribute() instanceof SlotAttribute slotAttribute)) return;

            map.put(slotAttribute.slotName(), stackedInstance.modifier());
        });

        return map;
    }

    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(boolean filterSlots) {
        var map = LinkedHashMultimap.<Attribute, AttributeModifier>create();

        this.exclusiveAttributes.forEach((location, uniqueInstance) -> {
            if(filterSlots && uniqueInstance.attribute() instanceof SlotAttribute) return;

            map.put(uniqueInstance.attribute(), uniqueInstance.modifier());
        });

        this.stackedAttributes.forEach((location, stackedInstance) -> {
            if(filterSlots && stackedInstance.attribute() instanceof SlotAttribute) return;

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
    @Deprecated
    public static String createSlotPath(SlotReference ref) {
        return ref.createSlotPath();
    }

    @Deprecated
    public static String createSlotPath(String slotname, int slot) {
        return slotname.replace(":", "-") + "/" + slot;
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
