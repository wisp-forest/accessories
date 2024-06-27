package dev.emi.trinkets.api;

import com.google.common.collect.ImmutableList;
import io.wispforest.accessories.Accessories;
import io.wispforest.accessories.api.attributes.SlotAttribute;
import io.wispforest.accessories.endec.CodecUtils;
import io.wispforest.accessories.endec.MinecraftEndecs;
import io.wispforest.accessories.endec.RegistriesAttribute;
import io.wispforest.accessories.utils.AttributeUtils;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

import java.util.List;
import java.util.Optional;

public record TrinketsAttributeModifiersComponent(List<Entry> modifiers, boolean showInTooltip) {
    public static final TrinketsAttributeModifiersComponent DEFAULT = new TrinketsAttributeModifiersComponent(List.of(), true);

    public static final Endec<TrinketsAttributeModifiersComponent> ENDEC = StructEndecBuilder.of(
            Entry.ENDEC.listOf().fieldOf("modifiers", TrinketsAttributeModifiersComponent::modifiers),
            Endec.BOOLEAN.optionalFieldOf("show_in_tooltip", TrinketsAttributeModifiersComponent::showInTooltip, true),
            TrinketsAttributeModifiersComponent::new
    );

    public static final DataComponentType<TrinketsAttributeModifiersComponent> TYPE = DataComponentType.<TrinketsAttributeModifiersComponent>builder()
            .persistent(CodecUtils.ofEndec(ENDEC))
            .networkSynchronized(CodecUtils.packetCodec(ENDEC))
            .build();

    public TrinketsAttributeModifiersComponent(List<Entry> modifiers, boolean showInTooltip) {
        this.modifiers = modifiers;
        this.showInTooltip = showInTooltip;
    }

    public TrinketsAttributeModifiersComponent withShowInTooltip(boolean showInTooltip) {
        return new TrinketsAttributeModifiersComponent(this.modifiers, showInTooltip);
    }

    public List<Entry> modifiers() {
        return this.modifiers;
    }

    public boolean showInTooltip() {
        return this.showInTooltip;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final ImmutableList.Builder<Entry> entries = ImmutableList.builder();

        Builder() {}

        public Builder add(Holder<Attribute> attribute, AttributeModifier modifier) {
            return add(attribute, modifier, Optional.empty());
        }

        public Builder add(Holder<Attribute> attribute, AttributeModifier modifier, String slot) {
            return add(attribute, modifier, Optional.of(slot));
        }

        public Builder add(Holder<Attribute> attribute, AttributeModifier modifier, Optional<String> slot) {
            this.entries.add(new Entry (attribute, modifier, slot));
            return this;
        }

        public TrinketsAttributeModifiersComponent build() {
            return new TrinketsAttributeModifiersComponent(this.entries.build(), true);
        }
    }

    public record Entry(Holder<Attribute> attribute, AttributeModifier modifier, Optional<String> slot) {
        private static final Endec<Holder<net.minecraft.world.entity.ai.attributes.Attribute>> ATTRIBUTE_ENDEC = MinecraftEndecs.IDENTIFIER.xmapWithContext(
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
                ATTRIBUTE_ENDEC.fieldOf("type", Entry::attribute),
                AttributeUtils.ATTRIBUTE_MODIFIER_ENDEC.flatFieldOf(Entry::modifier),
                Endec.STRING.optionalOf().fieldOf("slot", Entry::slot),
                Entry::new
        );
    }
}