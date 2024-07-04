package top.theillusivec4.curios.api;

import com.google.common.collect.ImmutableSet;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.advancements.critereon.MinMaxBounds;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public record SlotPredicate(List<String> slots, MinMaxBounds.Ints index) {

    public static final Codec<SlotPredicate> CODEC = RecordCodecBuilder.create(
            slotPredicateInstance -> slotPredicateInstance.group(
                            Codec.STRING.listOf().optionalFieldOf("slots", List.of())
                                    .forGetter(SlotPredicate::slots),
                            MinMaxBounds.Ints.CODEC.optionalFieldOf("index", MinMaxBounds.Ints.ANY)
                                    .forGetter(SlotPredicate::index)
                    )
                    .apply(slotPredicateInstance, SlotPredicate::new)
    );

    public boolean matches(SlotContext slotContext) {

        if (!this.slots.contains(slotContext.identifier())) {
            return false;
        } else {
            return this.index.matches(slotContext.index());
        }
    }

    public static class Builder {

        private Set<String> identifiers = new HashSet<>();
        private MinMaxBounds.Ints indices = MinMaxBounds.Ints.ANY;

        private Builder() {
        }

        public static Builder slot() {
            return new Builder();
        }

        public Builder of(String... identifiers) {
            this.identifiers = Stream.of(identifiers).collect(ImmutableSet.toImmutableSet());
            return this;
        }

        public Builder withIndex(MinMaxBounds.Ints index) {
            this.indices = index;
            return this;
        }

        public SlotPredicate build() {
            return new SlotPredicate(this.identifiers.stream().toList(), this.indices);
        }
    }
}
