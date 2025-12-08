package io.wispforest.accessories.api.components;

import com.google.common.collect.ImmutableList;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.attributes.SlotAttribute;
import io.wispforest.accessories.api.data.AccessoriesBaseData;
import io.wispforest.accessories.api.slot.SlotReference;
import io.wispforest.accessories.api.slot.UniqueSlotHandling;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.utils.AttributeUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import io.wispforest.owo.serialization.RegistriesAttribute;
import io.wispforest.owo.serialization.endec.MinecraftEndecs;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
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

    public boolean hasModifier(Holder<Attribute> holder, ResourceLocation location) {
        return getModifier(holder, location) != null;
    }

    @Nullable
    public AttributeModifier getModifier(Holder<Attribute> holder, ResourceLocation location) {
        for (var entry : this.modifiers) {
            if(entry.attribute.equals(holder) && entry.modifier.id().equals(location)) return entry.modifier();
        }

        return null;
    }

    public AccessoryItemAttributeModifiers withModifierAddedForAny(Holder<Attribute> holder, AttributeModifier attributeModifier, boolean isStackable) {
        return withModifierAdded(holder, attributeModifier, AccessoriesBaseData.ANY_SLOT, isStackable);
    }

    public AccessoryItemAttributeModifiers withModifierAdded(Holder<Attribute> holder, AttributeModifier attributeModifier, String slotName, boolean isStackable) {
        return withModifierAdded(holder, attributeModifier, slotName, isStackable, false);
    }

    public AccessoryItemAttributeModifiers withModifierAdded(Holder<Attribute> holder, AttributeModifier attributeModifier, String slotName, boolean isStackable, boolean usedInSlotValidation) {
        var builder = ImmutableList.<AccessoryItemAttributeModifiers.Entry>builderWithExpectedSize(this.modifiers.size() + 1);

        this.modifiers.forEach(entry -> {
            if (!entry.matches(holder, attributeModifier.id())) builder.add(entry);
        });

        builder.add(new AccessoryItemAttributeModifiers.Entry(holder, attributeModifier, slotName, isStackable, usedInSlotValidation));

        return new AccessoryItemAttributeModifiers(builder.build(), this.showInTooltip());
    }

    public AccessoryItemAttributeModifiers withoutModifier(Holder<Attribute> holder, ResourceLocation location) {
        var builder = ImmutableList.<AccessoryItemAttributeModifiers.Entry>builderWithExpectedSize(this.modifiers.size() + 1);

        this.modifiers.forEach(entry -> {
            if (entry.matches(holder, location)) return;

            builder.add(entry);
        });

        return new AccessoryItemAttributeModifiers(builder.build(), this.showInTooltip());
    }

    public AccessoryAttributeBuilder gatherAttributes(SlotReference slotReference) {
        return gatherAttributes(slotReference, null);
    }

    @ApiStatus.Internal
    public AccessoryAttributeBuilder gatherAttributes(SlotReference slotReference, @Nullable AccessoryAttributeBuilder parentBuilder) {
        var builder = new AccessoryAttributeBuilder(slotReference, parentBuilder);

        if(this.modifiers().isEmpty()) return builder;

        var entity = slotReference.entity();
        var slots = (entity != null) ? SlotTypeLoader.INSTANCE.getEntries(entity.level()) : Map.of();

        for (var entry : this.modifiers()) {
            var attributeModifier = entry.modifier();
            var slotTarget = entry.slotName();

            // TODO: THIS CHECK IS NOT HELPFUL IF THE ATTRIBUTE PERTAINS TO SOMETHING NOT DIRECTLY APART OF THE GIVEN ENTITY SLOTS
            // OVERALL SHOULD BE BETTER INDICATE THAT THE USER DOSE NOT HAVE THE SLOT OR SOMETHING
            // if (slots.isEmpty() && !slots.containsKey(Accessories.parseLocationOrDefault(slotTarget))) continue;

            if(slotReference.slotName().equals(slotTarget) || slotTarget.equals(AccessoriesBaseData.ANY_SLOT)) {
                if (entry.isStackable()) {
                    builder.addStackable(entry.attribute(), attributeModifier, entry.usedInSlotValidation());
                } else {
                    builder.addExclusive(entry.attribute(), attributeModifier, entry.usedInSlotValidation());
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
        public AccessoryItemAttributeModifiers.Builder add(Holder<Attribute> holder, AttributeModifier attributeModifier, String slotName, boolean isStackable) {
            return this.addForSlot(holder, attributeModifier, slotName, isStackable);
        }

        public AccessoryItemAttributeModifiers.Builder addForAny(Holder<Attribute> holder, AttributeModifier attributeModifier, boolean isStackable) {
            return addForSlot(holder, attributeModifier, AccessoriesBaseData.ANY_SLOT, isStackable);
        }

        public AccessoryItemAttributeModifiers.Builder addForSlot(Holder<Attribute> holder, AttributeModifier attributeModifier, String slotName, boolean isStackable) {
            this.entries.add(new AccessoryItemAttributeModifiers.Entry(holder, attributeModifier, slotName, isStackable, false));
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

    public record Entry(Holder<Attribute> attribute, AttributeModifier modifier, String slotName, boolean isStackable, boolean usedInSlotValidation) {
        private static final Endec<Holder<Attribute>> ATTRIBUTE_ENDEC = MinecraftEndecs.IDENTIFIER.xmapWithContext(
                (context, attributeType) -> {
                    if(attributeType.getNamespace().equals(Accessories.MODID)) {
                        var path = attributeType.getPath();

                        if(path.contains("/")) {
                            path = path.replace("/", ":");
                        }

                        return SlotAttribute.getAttributeHolder(path);
                    }

                    return context.requireAttributeValue(RegistriesAttribute.REGISTRIES)
                            .infoGetter().lookup(Registries.ATTRIBUTE)
                            .orElseThrow(IllegalStateException::new)
                            .getter()
                            .get(ResourceKey.create(Registries.ATTRIBUTE, attributeType))
                            .orElseThrow(IllegalStateException::new);
                },
                (context, attributeHolder) -> {
                    var attribute = attributeHolder.value();

                    if(attribute instanceof SlotAttribute slotAttribute) {
                        var path = slotAttribute.slotName();

                        if(UniqueSlotHandling.isUniqueSlot(path)) {
                            path = path.replace(":", "/");
                        }

                        return Accessories.of(path);
                    }

                    return context.requireAttributeValue(RegistriesAttribute.REGISTRIES)
                            .registryManager()
                            .lookupOrThrow(Registries.ATTRIBUTE)
                            .getKey(attribute);
                }
        );

        public boolean matches(Holder<Attribute> attribute, ResourceLocation id) {
            return attribute.equals(this.attribute) && this.modifier.is(id);
        }

        public static final Endec<Entry> ENDEC = StructEndecBuilder.of(
                ATTRIBUTE_ENDEC.fieldOf("type", AccessoryItemAttributeModifiers.Entry::attribute),
                AttributeUtils.ATTRIBUTE_MODIFIER_ENDEC.flatFieldOf(AccessoryItemAttributeModifiers.Entry::modifier),
                Endec.STRING.fieldOf("slot_name", AccessoryItemAttributeModifiers.Entry::slotName),
                Endec.BOOLEAN.optionalFieldOf("is_stackable", AccessoryItemAttributeModifiers.Entry::isStackable, false),
                Endec.BOOLEAN.optionalFieldOf("used_in_slot_validation", AccessoryItemAttributeModifiers.Entry::usedInSlotValidation, false),
                AccessoryItemAttributeModifiers.Entry::new
        );
    }
}
