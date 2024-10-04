package io.wispforest.accessories.api.attributes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.AccessoriesInternals;
import io.wispforest.accessories.AccessoriesLoaderInternals;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;

/**
 * Builder used to collect the attribute modifications from a given Accessory with the ability
 * to specified if an Attribute modification can be stacked or is exclusive to one version
 */
public final class AccessoryAttributeBuilder {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final Map<Holder<Attribute>, Map<ResourceLocation, AttributeModificationData>> exclusiveAttributes = new HashMap<>();
    private final Multimap<Holder<Attribute>, AttributeModificationData> stackedAttributes = LinkedHashMultimap.create();

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
     * Adds a given attribute modifier as a stackable modifier meaning variants based on slot position is allowed. This is done by post process
     * step of appending slot information when adding to the living entity
     */
    public AccessoryAttributeBuilder addStackable(Holder<Attribute> attribute, ResourceLocation location, double amount, AttributeModifier.Operation operation) {
        this.addStackable(attribute, new AttributeModifier(location, amount, operation));

        return this;
    }

    private final Set<ResourceLocation> previouslyWarnedLocations = new HashSet<>();

    /**
     * Adds a given attribute modifier as an exclusive modifier meaning that only one instance should ever exist
     */
    public AccessoryAttributeBuilder addExclusive(Holder<Attribute> attribute, AttributeModifier modifier) {
        var id = modifier.id();

        var innerMap = this.exclusiveAttributes.computeIfAbsent(attribute, attributeHolder -> new HashMap<>());

        if(AccessoriesLoaderInternals.isDevelopmentEnv() && innerMap.containsKey(id) && !this.previouslyWarnedLocations.contains(id)) {
            LOGGER.warn("A given Modifier was found to have a duplicate location but was added as exclusive, was such on purpose as such will not stack with the other: {}", id);

            this.previouslyWarnedLocations.add(id);
        }

        innerMap.putIfAbsent(id, new AttributeModificationData(attribute, modifier));

        return this;
    }

    /**
     * Adds a given attribute modifier as a stackable modifier meaning variants based on slot position is allowed. This is done by post process
     * step of appending slot information when adding to the living entity
     */
    public AccessoryAttributeBuilder addStackable(Holder<Attribute> attribute, AttributeModifier modifier) {
        this.stackedAttributes.put(attribute, new AttributeModificationData(this.slotReference.createSlotPath(), attribute, modifier));

        return this;
    }

    @Nullable
    public AttributeModificationData getExclusive(Holder<Attribute> attribute, ResourceLocation location) {
        var innerMap = this.exclusiveAttributes.get(attribute);

        if(innerMap == null) return null;

        return innerMap.get(location);
    }

    public Collection<AttributeModificationData> getStacks(Holder<Attribute> attribute, ResourceLocation location) {
        return this.stackedAttributes.get(attribute).stream().filter(data -> data.modifier().id().equals(location)).toList();
    }

    @Nullable
    public AttributeModificationData removeExclusive(Holder<Attribute> attribute, ResourceLocation location) {
        var innerMap = this.exclusiveAttributes.get(attribute);

        if(innerMap == null) return null;

        return innerMap.remove(location);
    }

    public Collection<AttributeModificationData> removeStacks(Holder<Attribute> attribute, ResourceLocation location) {
        Set<AttributeModificationData> removedData = new HashSet<>();

        for (var data : List.copyOf(this.stackedAttributes.get(attribute))) {
            if(!data.modifier().id().equals(location)) continue;

            removedData.add(data);

            this.stackedAttributes.remove(attribute, data);
        }

        return removedData;
    }

    //--

    public Multimap<String, AttributeModifier> getSlotModifiers() {
        var map = LinkedHashMultimap.<String, AttributeModifier>create();

        this.exclusiveAttributes.forEach((attribute, innerMap) -> {
            innerMap.forEach((location, uniqueInstance) -> {
                if(!(uniqueInstance.attribute().value() instanceof SlotAttribute slotAttribute)) return;

                map.put(slotAttribute.slotName(), uniqueInstance.modifier());
            });
        });

        this.stackedAttributes.forEach((location, stackedInstance) -> {
            if(!(stackedInstance.attribute().value() instanceof SlotAttribute slotAttribute)) return;

            map.put(slotAttribute.slotName(), stackedInstance.modifier());
        });

        return map;
    }

    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(boolean filterSlots) {
        var map = LinkedHashMultimap.<Holder<Attribute>, AttributeModifier>create();

        this.exclusiveAttributes.forEach((attribute, innerMap) -> {
            innerMap.forEach((location, uniqueInstance) -> {
                if (filterSlots && uniqueInstance.attribute().value() instanceof SlotAttribute) return;

                map.put(uniqueInstance.attribute(), uniqueInstance.modifier());
            });
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

    public Map<Holder<Attribute>, Map<ResourceLocation, AttributeModificationData>> exclusiveAttributes() {
        return ImmutableMap.copyOf(this.exclusiveAttributes);
    }

    public Multimap<Holder<Attribute>, AttributeModificationData> stackedAttributes() {
        return ImmutableMultimap.copyOf(this.stackedAttributes);
    }

    public AccessoryAttributeBuilder addFrom(AccessoryAttributeBuilder builder) {
        builder.exclusiveAttributes.forEach(this.exclusiveAttributes::putIfAbsent);
        this.stackedAttributes.putAll(builder.stackedAttributes);

        return this;
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

    //--

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
}
