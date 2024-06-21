package top.theillusivec4.curios.api;

import com.google.common.collect.ImmutableList;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import top.theillusivec4.curios.CuriosConstants;

import java.util.List;
import java.util.function.BiConsumer;

public record CurioAttributeModifiers(List<Entry> modifiers, boolean showInTooltip) {
    public static final CurioAttributeModifiers EMPTY = new CurioAttributeModifiers(List.of(), true);
    private static final Codec<CurioAttributeModifiers> FULL_CODEC = RecordCodecBuilder.create(
            p_337947_ -> p_337947_.group(
                            Entry.CODEC.listOf().fieldOf("modifiers").forGetter(CurioAttributeModifiers::modifiers),
                            Codec.BOOL.optionalFieldOf("show_in_tooltip", Boolean.TRUE)
                                    .forGetter(CurioAttributeModifiers::showInTooltip)
                    )
                    .apply(p_337947_, CurioAttributeModifiers::new)
    );
    public static final Codec<CurioAttributeModifiers> CODEC = Codec.withAlternative(
            FULL_CODEC, Entry.CODEC.listOf(), p_332621_ -> new CurioAttributeModifiers(p_332621_, true)
    );
    public static final StreamCodec<RegistryFriendlyByteBuf, CurioAttributeModifiers>
            STREAM_CODEC = StreamCodec.composite(
            Entry.STREAM_CODEC.apply(ByteBufCodecs.list()),
            CurioAttributeModifiers::modifiers,
            ByteBufCodecs.BOOL,
            CurioAttributeModifiers::showInTooltip,
            CurioAttributeModifiers::new
    );

    public CurioAttributeModifiers withTooltip(boolean showInTooltip) {
        return new CurioAttributeModifiers(this.modifiers, showInTooltip);
    }

    public static CurioAttributeModifiers.Builder builder() {
        return new CurioAttributeModifiers.Builder();
    }

    public CurioAttributeModifiers withModifierAdded(ResourceLocation attribute, AttributeModifier attributeModifier, String slot) {
        ImmutableList.Builder<Entry> builder = ImmutableList.builderWithExpectedSize(this.modifiers.size() + 1);

        for (Entry attributemodifiers$entry : this.modifiers) {
            if (!attributemodifiers$entry.modifier.id().equals(attributeModifier.id())) {
                builder.add(attributemodifiers$entry);
            }
        }

        builder.add(new Entry(attribute, attributeModifier, slot));
        return new CurioAttributeModifiers(builder.build(), this.showInTooltip);
    }

    public void forEach(String slot, BiConsumer<ResourceLocation, AttributeModifier> consumer) {
        for (Entry attributemodifiers$entry : this.modifiers) {
            if (attributemodifiers$entry.slot.equals(slot)) {
                consumer.accept(attributemodifiers$entry.attribute, attributemodifiers$entry.modifier);
            }
        }
    }

    public static class Builder {
        private final ImmutableList.Builder<Entry> entries = ImmutableList.builder();

        Builder() {}

        public Builder add(Holder<Attribute> attribute, AttributeModifier attributeModifier, String slot) {
            ResourceLocation rl;

            if (attribute.value() instanceof SlotAttribute wrapper) {
                rl = new ResourceLocation(CuriosConstants.MOD_ID, wrapper.getIdentifier());
            } else {
                rl = new ResourceLocation(attribute.getRegisteredName());
            }
            this.entries.add(new Entry(rl, attributeModifier, slot));
            return this;
        }

        public CurioAttributeModifiers build() {
            return new CurioAttributeModifiers(this.entries.build(), true);
        }
    }

    public record Entry(ResourceLocation attribute, AttributeModifier modifier, String slot) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(
                instance -> instance.group(
                                ResourceLocation.CODEC.fieldOf("type").forGetter(Entry::attribute),
                                AttributeModifier.MAP_CODEC.forGetter(Entry::modifier),
                                Codec.STRING.optionalFieldOf("slot", "").forGetter(Entry::slot))
                        .apply(instance, Entry::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry>
                STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC,
                Entry::attribute,
                AttributeModifier.STREAM_CODEC,
                Entry::modifier,
                ByteBufCodecs.STRING_UTF8,
                Entry::slot,
                Entry::new
        );
    }
}
