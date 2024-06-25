package io.wispforest.accessories.api.components;

import com.google.common.collect.ImmutableList;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.attributes.AccessoryAttributeBuilder;
import io.wispforest.accessories.api.attributes.SlotAttribute;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.endec.MinecraftEndecs;
import io.wispforest.accessories.endec.RegistriesAttribute;
import io.wispforest.accessories.utils.AttributeUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.List;
import java.util.Map;

public record AccessoryItemAttributeModifiers(List<AccessoryItemAttributeModifiers.Entry> modifiers) {

    public static final AccessoryItemAttributeModifiers EMPTY = new AccessoryItemAttributeModifiers(List.of());

    public static final Endec<AccessoryItemAttributeModifiers> ENDEC = StructEndecBuilder.of(
            Entry.ENDEC.listOf().fieldOf("modifiers", AccessoryItemAttributeModifiers::modifiers),
            AccessoryItemAttributeModifiers::new
    );

    public static AccessoryItemAttributeModifiers.Builder builder() {
        return new AccessoryItemAttributeModifiers.Builder();
    }

    public AccessoryItemAttributeModifiers withModifierAdded(Holder<Attribute> holder, AttributeModifier attributeModifier, String slotName, boolean isStackable) {
        var builder = ImmutableList.<AccessoryItemAttributeModifiers.Entry>builderWithExpectedSize(this.modifiers.size() + 1);

        this.modifiers.forEach(entry -> { if (!entry.modifier.id().equals(attributeModifier.id())) builder.add(entry); });

        builder.add(new AccessoryItemAttributeModifiers.Entry(holder, attributeModifier, slotName, isStackable));

        return new AccessoryItemAttributeModifiers(builder.build());
    }

    public AccessoryAttributeBuilder gatherAttributes(LivingEntity entity, String slotName, int slot) {
        var builder = new AccessoryAttributeBuilder(slotName, slot);

        if(this.modifiers().isEmpty()) return builder;

        var slots = (entity != null) ? SlotTypeLoader.getSlotTypes(entity.level()) : Map.of();

        for (var entry : this.modifiers()) {
            var attributeModifier = entry.modifier();

            if(!slots.containsKey(entry.slotName()) || !slotName.equals(entry.slotName())) {
                continue;
            }

            if(entry.isStackable()) {
                builder.addStackable(entry.attribute(), attributeModifier);
            } else {
                builder.addExclusive(entry.attribute(), attributeModifier);
            }
        }

        return builder;
    }

    public static class Builder {
        private final ImmutableList.Builder<AccessoryItemAttributeModifiers.Entry> entries = ImmutableList.builder();

        private Builder() {}

        public AccessoryItemAttributeModifiers.Builder add(Holder<Attribute> holder, AttributeModifier attributeModifier, String slotName, boolean isStackable) {
            this.entries.add(new AccessoryItemAttributeModifiers.Entry(holder, attributeModifier, slotName, isStackable));
            return this;
        }

        public AccessoryItemAttributeModifiers build() {
            return new AccessoryItemAttributeModifiers(this.entries.build());
        }
    }

    public record Entry(Holder<Attribute> attribute, AttributeModifier modifier, String slotName, boolean isStackable) {
        private static final Endec<Holder<Attribute>> ATTRIBUTE_ENDEC = MinecraftEndecs.IDENTIFIER.xmapWithContext(
                (context, attributeType) -> {
                    if(attributeType.getNamespace().equals(Accessories.MODID)) return Holder.direct(SlotAttribute.getSlotAttribute(attributeType.getPath()));

                    return context.requireAttributeValue(RegistriesAttribute.REGISTRIES)
                            .registryManager()
                            .registryOrThrow(Registries.ATTRIBUTE)
                            .getHolder(attributeType)
                            .orElseThrow(IllegalStateException::new);
                },
                (context, attributeHolder) -> {
                    var attribute = attributeHolder.value();

                    if(attribute instanceof SlotAttribute slotAttribute) return Accessories.of(slotAttribute.slotName());

                    return context.requireAttributeValue(RegistriesAttribute.REGISTRIES)
                            .registryManager()
                            .registryOrThrow(Registries.ATTRIBUTE)
                            .getKey(attribute);
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
