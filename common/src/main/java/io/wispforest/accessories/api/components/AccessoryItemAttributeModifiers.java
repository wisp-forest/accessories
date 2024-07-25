package io.wispforest.accessories.api.components;

import com.google.common.collect.ImmutableList;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.attributes.SlotAttribute;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.endec.MinecraftEndecs;
import io.wispforest.accessories.utils.AttributeUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public record AccessoryItemAttributeModifiers(List<AccessoryItemAttributeModifiers.Entry> modifiers, boolean showInTooltip) {

    public static final AccessoryItemAttributeModifiers EMPTY = new AccessoryItemAttributeModifiers(List.of(), true);

    public static final Endec<AccessoryItemAttributeModifiers> ENDEC = StructEndecBuilder.of(
            Entry.ENDEC.listOf().fieldOf("modifiers", AccessoryItemAttributeModifiers::modifiers),
            Endec.BOOLEAN.optionalFieldOf("show_in_tooltip", AccessoryItemAttributeModifiers::showInTooltip, true),
            AccessoryItemAttributeModifiers::new
    );

    public static AccessoryItemAttributeModifiers.Builder builder() {
        return new AccessoryItemAttributeModifiers.Builder();
    }

    public boolean hasModifier(Attribute holder, ResourceLocation location) {
        return getModifier(holder, location) != null;
    }

    @Nullable
    public AttributeModifier getModifier(Attribute holder, ResourceLocation location) {
        for (var entry : this.modifiers) {
            var modifierLocation = AttributeUtils.getLocation(entry.modifier.getName());

            if(entry.attribute.equals(holder) && modifierLocation.equals(location)) return entry.modifier();
        }

        return null;
    }

    public AccessoryItemAttributeModifiers withModifierAddedForAny(Attribute holder, ResourceLocation location, double amount, AttributeModifier.Operation operation, String slotName, boolean isStackable) {
        var data = AttributeUtils.getModifierData(location);

        return withModifierAdded(holder, new AttributeModifier(data.second(), data.first(), amount, operation), "any", isStackable);
    }

    public AccessoryItemAttributeModifiers withModifierAdded(Attribute holder, ResourceLocation location, double amount, AttributeModifier.Operation operation, String slotName, boolean isStackable) {
        var data = AttributeUtils.getModifierData(location);

        return withModifierAdded(holder, new AttributeModifier(data.second(), data.first(), amount, operation), slotName, isStackable);
    }

    public AccessoryItemAttributeModifiers withModifierAddedForAny(Attribute holder, AttributeModifier attributeModifier, String slotName, boolean isStackable) {
        return withModifierAdded(holder, attributeModifier, "any", isStackable);
    }

    public AccessoryItemAttributeModifiers withModifierAdded(Attribute holder, AttributeModifier attributeModifier, String slotName, boolean isStackable) {
        var builder = ImmutableList.<AccessoryItemAttributeModifiers.Entry>builderWithExpectedSize(this.modifiers.size() + 1);

        this.modifiers.forEach(entry -> {
            if (!entry.modifier.getId().equals(attributeModifier.getId())) builder.add(entry);
        });

        builder.add(new AccessoryItemAttributeModifiers.Entry(holder, attributeModifier, slotName, isStackable));

        return new AccessoryItemAttributeModifiers(builder.build(), this.showInTooltip());
    }

    public AccessoryItemAttributeModifiers withoutModifier(Holder<Attribute> holder, ResourceLocation location) {
        var builder = ImmutableList.<AccessoryItemAttributeModifiers.Entry>builderWithExpectedSize(this.modifiers.size() + 1);

        this.modifiers.forEach(entry -> {
            var modifierLocation = AttributeUtils.getLocation(entry.modifier.getName());

            if (modifierLocation.equals(location) && entry.attribute().equals(holder)) return;

            builder.add(entry);
        });

        return new AccessoryItemAttributeModifiers(builder.build(), this.showInTooltip());
    }

    public AccessoryAttributeBuilder gatherAttributes(LivingEntity entity, String slotName, int slot) {
        var builder = new AccessoryAttributeBuilder(slotName, slot);

        if(this.modifiers().isEmpty()) return builder;

        var slots = (entity != null) ? SlotTypeLoader.getSlotTypes(entity.level()) : Map.of();

        for (var entry : this.modifiers()) {
            var modifer = entry.modifier();
            var slotTarget = entry.slotName();

            if(slots.containsKey(slotTarget) || slotName.equals(slotTarget) || slotTarget.equals("any")) {
                if (entry.isStackable()) {
                    builder.addStackable(entry.attribute(), AttributeUtils.getLocation(modifer.getName()), modifer.getAmount(), modifer.getOperation());
                } else {
                    builder.addExclusive(entry.attribute(), AttributeUtils.getLocation(modifer.getName()), modifer.getAmount(), modifer.getOperation());
                }
            }
        }

        return builder;
    }

    public static class Builder {
        private final List<AccessoryItemAttributeModifiers.Entry> entries = new ArrayList<>();
        private boolean showInTooltip = true;

        private Builder() {}

        @ApiStatus.ScheduledForRemoval(inVersion = "1.22")
        @Deprecated(forRemoval = true)
        public AccessoryItemAttributeModifiers.Builder add(Attribute attribute, AttributeModifier attributeModifier, String slotName, boolean isStackable) {
            return this.addForSlot(attribute, attributeModifier, slotName, isStackable);
        }

        public AccessoryItemAttributeModifiers.Builder addForSlot(Attribute attribute, AttributeModifier attributeModifier, String slotName, boolean isStackable) {
            this.entries.add(new AccessoryItemAttributeModifiers.Entry(attribute, attributeModifier, slotName, isStackable));
            return this;
        }

        public AccessoryItemAttributeModifiers.Builder addForAny(Attribute attribute, AttributeModifier attributeModifier, boolean isStackable) {
            this.entries.add(new AccessoryItemAttributeModifiers.Entry(attribute, attributeModifier, "any", isStackable));
            return this;
        }

        public AccessoryItemAttributeModifiers.Builder showInTooltip(boolean value) {
            this.showInTooltip = value;
            return this;
        }

        public boolean isEmpty() {
            return this.entries.isEmpty();
        }

        public AccessoryItemAttributeModifiers build() {
            return new AccessoryItemAttributeModifiers(Collections.unmodifiableList(this.entries), showInTooltip);
        }
    }

    public record Entry(Attribute attribute, AttributeModifier modifier, String slotName, boolean isStackable) {
        private static final Endec<Attribute> ATTRIBUTE_ENDEC = MinecraftEndecs.IDENTIFIER.xmapWithContext(
                (context, attributeType) -> {
                    if(attributeType.getNamespace().equals(Accessories.MODID)) {
                        var path = attributeType.getPath();

                        if(path.contains("/")) {
                            path = path.replace("/", ":");
                        }

                        return SlotAttribute.getSlotAttribute(path);
                    }

                    return BuiltInRegistries.ATTRIBUTE.getOptional(attributeType)
                            .orElseThrow(IllegalStateException::new);
                },
                (context, attribute) -> {
                    if(attribute instanceof SlotAttribute slotAttribute) {
                        var path = slotAttribute.slotName();

                        if(UniqueSlotHandling.isUniqueSlot(path)) {
                            path = path.replace(":", "/");
                        }

                        return Accessories.of(path);
                    }

                    return BuiltInRegistries.ATTRIBUTE.getKey(attribute);
                }
        );

        public static final Endec<Entry> ENDEC = StructEndecBuilder.of(
                ATTRIBUTE_ENDEC.fieldOf("type", AccessoryItemAttributeModifiers.Entry::attribute),
                AttributeUtils.ATTRIBUTE_MODIFIER_ENDEC.flatFieldOf(AccessoryItemAttributeModifiers.Entry::modifier),
                Endec.STRING.fieldOf("slot_name", AccessoryItemAttributeModifiers.Entry::slotName),
                Endec.BOOLEAN.optionalFieldOf("is_stackable", AccessoryItemAttributeModifiers.Entry::isStackable, false),
                AccessoryItemAttributeModifiers.Entry::new
        );
    }
}
