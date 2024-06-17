package io.wispforest.accessories.api.components;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.slot.SlotAttribute;
import io.wispforest.accessories.data.SlotTypeLoader;
import io.wispforest.accessories.endec.CodecUtils;
import io.wispforest.accessories.endec.MinecraftEndecs;
import io.wispforest.accessories.endec.RegistriesAttribute;
import io.wispforest.endec.Endec;
import io.wispforest.endec.StructEndec;
import io.wispforest.endec.impl.BuiltInEndecs;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.List;

public record AccessoryItemAttributeModifiers(List<AccessoryItemAttributeModifiers.Entry> modifiers) {

    public static final AccessoryItemAttributeModifiers EMPTY = new AccessoryItemAttributeModifiers(List.of());

    public static final Endec<AccessoryItemAttributeModifiers> ENDEC = StructEndecBuilder.of(
            Entry.ENDEC.listOf().fieldOf("modifiers", AccessoryItemAttributeModifiers::modifiers),
            AccessoryItemAttributeModifiers::new
    );

    public static AccessoryItemAttributeModifiers.Builder builder() {
        return new AccessoryItemAttributeModifiers.Builder();
    }

    public AccessoryItemAttributeModifiers withModifierAdded(Holder<Attribute> holder, AttributeModifier attributeModifier, String slotName) {
        var builder = ImmutableList.<AccessoryItemAttributeModifiers.Entry>builderWithExpectedSize(this.modifiers.size() + 1);

        this.modifiers.forEach(entry -> { if (!entry.modifier.id().equals(attributeModifier.id())) builder.add(entry); });

        builder.add(new AccessoryItemAttributeModifiers.Entry(holder, attributeModifier, slotName));

        return new AccessoryItemAttributeModifiers(builder.build());
    }


    public static class Builder {
        private final ImmutableList.Builder<AccessoryItemAttributeModifiers.Entry> entries = ImmutableList.builder();

        private Builder() {}

        public AccessoryItemAttributeModifiers.Builder add(Holder<Attribute> holder, AttributeModifier attributeModifier, String slotName) {
            this.entries.add(new AccessoryItemAttributeModifiers.Entry(holder, attributeModifier, slotName));
            return this;
        }

        public AccessoryItemAttributeModifiers build() {
            return new AccessoryItemAttributeModifiers(this.entries.build());
        }
    }

    public record Entry(Holder<Attribute> attribute, AttributeModifier modifier, String slotName) {
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

        public static final StructEndec<AttributeModifier> ATTRIBUTE_MODIFIER_ENDEC = StructEndecBuilder.of(
                BuiltInEndecs.UUID.fieldOf("uuid", AttributeModifier::id),
                Endec.STRING.fieldOf("name", AttributeModifier::name),
                Endec.DOUBLE.fieldOf("amount", AttributeModifier::amount),
                Endec.forEnum(AttributeModifier.Operation.class).fieldOf("operation", AttributeModifier::operation),
                AttributeModifier::new
        );

        public static final Endec<Entry> ENDEC = StructEndecBuilder.of(
                ATTRIBUTE_ENDEC.fieldOf("type", AccessoryItemAttributeModifiers.Entry::attribute),
                ATTRIBUTE_MODIFIER_ENDEC.flatFieldOf(AccessoryItemAttributeModifiers.Entry::modifier),
                Endec.STRING.fieldOf("slot_name", AccessoryItemAttributeModifiers.Entry::slotName),
                AccessoryItemAttributeModifiers.Entry::new
        );
    }
}
