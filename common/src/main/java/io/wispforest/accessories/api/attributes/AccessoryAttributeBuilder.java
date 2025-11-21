package io.wispforest.accessories.api.attributes;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.logging.LogUtils;
import io.wispforest.accessories.AccessoriesLoaderInternals;
import io.wispforest.accessories.api.slot.SlotPath;
import io.wispforest.accessories.api.slot.SlotReference;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.BiPredicate;

import static io.wispforest.accessories.api.attributes.AttributeModificationData.AllowedType;

/**
 * Builder used to collect the attribute modifications from a given Accessory with the ability
 * to specified if an Attribute modification can be stacked or is exclusive to one version
 */
public final class AccessoryAttributeBuilder {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final Map<Holder<Attribute>, Map<ResourceLocation, AttributeModificationData>> exclusiveAttributes;
    private final Multimap<Holder<Attribute>, AttributeModificationData> stackedAttributes;

    private final SlotPath slotPath;

    @ApiStatus.Internal
    public AccessoryAttributeBuilder(SlotPath slotPath, @Nullable AccessoryAttributeBuilder parentBuilder) {
        this.slotPath = slotPath;

        if (parentBuilder != null) {
            this.exclusiveAttributes = parentBuilder.exclusiveAttributes;
            this.stackedAttributes = parentBuilder.stackedAttributes;
        } else {
            this.exclusiveAttributes = new HashMap<>();
            this.stackedAttributes = LinkedHashMultimap.create();
        }
    }

    @ApiStatus.Internal
    public AccessoryAttributeBuilder(SlotPath slotPath) {
        this(slotPath, null);
    }

    @ApiStatus.Internal
    public AccessoryAttributeBuilder(String slotName, int slot) {
        this(SlotPath.of(slotName, slot));
    }

    @ApiStatus.Internal
    public AccessoryAttributeBuilder() {
        this(SlotPath.of("", 0));
    }

    //--

    /**
     * Adds a given attribute modifier as an exclusive modifier meaning that only one instance should ever exist
     */
    public AccessoryAttributeBuilder addExclusive(Holder<Attribute> attribute, ResourceLocation location, double amount, AttributeModifier.Operation operation) {
        return this.addExclusive(attribute, location, amount, operation, false);
    }

    /**
     * Adds a given attribute modifier as a stackable modifier meaning variants based on slot position is allowed. This is done by post process
     * step of appending slot information when adding to the living entity
     */
    public AccessoryAttributeBuilder addStackable(Holder<Attribute> attribute, ResourceLocation location, double amount, AttributeModifier.Operation operation) {
        return this.addStackable(attribute, location, amount, operation, false);
    }

    /**
     * Adds a given attribute modifier as an exclusive modifier meaning that only one instance should ever exist
     */
    public AccessoryAttributeBuilder addExclusive(Holder<Attribute> attribute, ResourceLocation location, double amount, AttributeModifier.Operation operation, boolean usedInSlotValidation) {
        return this.addExclusive(attribute, new AttributeModifier(location, amount, operation), usedInSlotValidation);
    }

    /**
     * Adds a given attribute modifier as a stackable modifier meaning variants based on slot position is allowed. This is done by post process
     * step of appending slot information when adding to the living entity
     */
    public AccessoryAttributeBuilder addStackable(Holder<Attribute> attribute, ResourceLocation location, double amount, AttributeModifier.Operation operation, boolean usedInSlotValidation) {
        return this.addStackable(attribute, new AttributeModifier(location, amount, operation), usedInSlotValidation);
    }

    public AccessoryAttributeBuilder addExclusive(Holder<Attribute> attribute, AttributeModifier modifier) {
        return this.addExclusive(attribute, modifier, false);
    }

    public AccessoryAttributeBuilder addStackable(Holder<Attribute> attribute, AttributeModifier modifier) {
        return this.addStackable(attribute, modifier, false);
    }

    private final Set<ResourceLocation> previouslyWarnedLocations = new HashSet<>();

    /**
     * Adds a given attribute modifier as an exclusive modifier meaning that only one instance should ever exist
     */
    public AccessoryAttributeBuilder addExclusive(Holder<Attribute> attribute, AttributeModifier modifier, boolean usedInSlotValidation) {
        var id = modifier.id();

        var innerMap = this.exclusiveAttributes.computeIfAbsent(attribute, attributeHolder -> new HashMap<>());

        if(AccessoriesLoaderInternals.INSTANCE.isDevelopmentEnv() && innerMap.containsKey(id) && !this.previouslyWarnedLocations.contains(id)) {
            LOGGER.warn("A given Modifier was found to have a duplicate location but was added as exclusive, was such on purpose as such will not stack with the other: {}", id);

            this.previouslyWarnedLocations.add(id);
        }

        innerMap.putIfAbsent(id, new AttributeModificationData(attribute, modifier, usedInSlotValidation));

        return this;
    }

    /**
     * Adds a given attribute modifier as a stackable modifier meaning variants based on slot position is allowed. This is done by post process
     * step of appending slot information when adding to the living entity
     */
    public AccessoryAttributeBuilder addStackable(Holder<Attribute> attribute, AttributeModifier modifier, boolean usedInSlotValidation) {
        this.stackedAttributes.put(attribute, new AttributeModificationData(this.slotPath.createString(), attribute, modifier, usedInSlotValidation));

        return this;
    }

    //--

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
        return getSlotModifiers(false);
    }

    public Multimap<String, AttributeModifier> getSlotModifiers(boolean usedWithinSlotPredicate) {
        var map = LinkedHashMultimap.<String, AttributeModifier>create();

        this.exclusiveAttributes.forEach((attribute, innerMap) -> {
            innerMap.forEach((location, uniqueInstance) -> {
                if (uniqueInstance.isValid(AllowedType.SLOT, usedWithinSlotPredicate)) {
                    map.put(((SlotAttribute) uniqueInstance.attribute().value()).slotName(), uniqueInstance.modifier());
                }
            });
        });

        this.stackedAttributes.forEach((location, stackedInstance) -> {
            if (stackedInstance.isValid(AllowedType.SLOT, usedWithinSlotPredicate)) {
                map.put(((SlotAttribute) stackedInstance.attribute().value()).slotName(), stackedInstance.modifier());
            }
        });

        return map;
    }

    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(boolean removeSlotAttributes) {
        return getAttributeModifiers(removeSlotAttributes, false);
    }

    public Multimap<Holder<Attribute>, AttributeModifier> getAttributeModifiers(boolean removeSlotAttributes, boolean usedWithinSlotPredicate) {
        var map = LinkedHashMultimap.<Holder<Attribute>, AttributeModifier>create();

        this.exclusiveAttributes.forEach((attribute, innerMap) -> {
            innerMap.forEach((location, uniqueInstance) -> {
                if (uniqueInstance.isValid(removeSlotAttributes, usedWithinSlotPredicate)) {
                    map.put(uniqueInstance.attribute(), uniqueInstance.modifier());
                }
            });
        });

        this.stackedAttributes.forEach((location, stackedInstance) -> {
            if (stackedInstance.isValid(removeSlotAttributes, usedWithinSlotPredicate)) {
                map.put(stackedInstance.attribute(), stackedInstance.modifier());
            }
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
        if(!areMapsEqual(this.stackedAttributes.asMap(), otherBuilder.stackedAttributes.asMap(), Record::equals)) return false;

        return this.exclusiveAttributes.equals(otherBuilder.exclusiveAttributes);
    }

    public boolean equalWithoutPaths(Object obj) {
        if(!(obj instanceof AccessoryAttributeBuilder otherBuilder)) return false;
        if(!areMapsEqual(this.stackedAttributes.asMap(), otherBuilder.stackedAttributes.asMap(), AttributeModificationData::equalsWithoutPath)) return false;

        return this.exclusiveAttributes.equals(otherBuilder.exclusiveAttributes);
    }

    private static <K, V, C extends Collection<V>> boolean areMapsEqual(Map<K, C> multimap1, Map<K, C> multimap2, BiPredicate<@NotNull V, @NotNull V> equalsCheck) {
        return areListValuesInMapEqual(multimap1, multimap2, (list1, list2) -> {
            for (V v : list1) {
                var result = false;

                for (V v1 : list2) {
                    result = equalsCheck.test(v, v1);

                    if (result) break;
                }

                if (!result) return false;
            }

            return true;
        });
    }

    private static <K, V, C extends Collection<V>> boolean areListValuesInMapEqual(Map<K, C> multimap1, Map<K, C> multimap2, BiPredicate<C, C> equalsCheck) {
        for (var entry : multimap1.entrySet()) {
            var list1 = entry.getValue();
            var list2 = multimap2.get(entry.getKey());

            if (list2 == null || !equalsCheck.test(list1, list2)) return false;
        }

        return true;
    }

    //--

    // slotPath          = {slot_name}/{slot_index}[{nested_layer_info}]
    // nested_layer_info = /nest_{layer_index}_{slot_index}
    @Deprecated
    public static String createSlotPath(SlotReference ref) {
        return ref.createString();
    }

    @Deprecated
    public static String createSlotPath(String slotname, int slot) {
        return SlotPath.createBaseSlotPath(slotname, slot);
    }
}
