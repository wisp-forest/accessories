package io.wispforest.accessories.api.components;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;

import java.util.*;

public record AccessorySlotValidationComponent(SequencedSet<String> validSlotOverrides, SequencedSet<String> invalidSlotOverrides) {
    public static final AccessorySlotValidationComponent EMPTY = new AccessorySlotValidationComponent(Set.of(), Set.of());

    @Deprecated
    public AccessorySlotValidationComponent(Set<String> validSlotOverrides, Set<String> invalidSlotOverrides) {
        this(new LinkedHashSet<>(validSlotOverrides), new LinkedHashSet<>(invalidSlotOverrides));
    }

    private static final Endec<SequencedSet<String>> STRING_SET_ENDEC = Endec.STRING.collectionOf(LinkedHashSet::new);

    public static final Endec<AccessorySlotValidationComponent> ENDEC = StructEndecBuilder.of(
            STRING_SET_ENDEC.optionalFieldOf("valid_slots", AccessorySlotValidationComponent::validSlotOverrides, LinkedHashSet::new),
            STRING_SET_ENDEC.optionalFieldOf("invalid_slots", AccessorySlotValidationComponent::invalidSlotOverrides, LinkedHashSet::new),
            AccessorySlotValidationComponent::new
    );

    @Override
    public SequencedSet<String> validSlotOverrides() {
        return Collections.unmodifiableSequencedSet(validSlotOverrides);
    }

    @Override
    public SequencedSet<String> invalidSlotOverrides() {
        return Collections.unmodifiableSequencedSet(invalidSlotOverrides);
    }

    public AccessorySlotValidationComponent addValidSlot(String slotName) {
        var validSlotOverrides = new LinkedHashSet<>(this.validSlotOverrides);
        var invalidSlotOverrides = new LinkedHashSet<>(this.invalidSlotOverrides);

        validSlotOverrides.add(slotName);
        invalidSlotOverrides.remove(slotName);

        return new AccessorySlotValidationComponent(validSlotOverrides, invalidSlotOverrides);
    }

    public AccessorySlotValidationComponent addInvalidSlot(String slotName) {
        var validSlotOverrides = new LinkedHashSet<>(this.validSlotOverrides);
        var invalidSlotOverrides = new LinkedHashSet<>(this.invalidSlotOverrides);

        validSlotOverrides.remove(slotName);
        invalidSlotOverrides.add(slotName);

        return new AccessorySlotValidationComponent(validSlotOverrides, invalidSlotOverrides);
    }

    public AccessorySlotValidationComponent removeValidSlot(String slotName) {
        var validSlotOverrides = new LinkedHashSet<>(this.validSlotOverrides);

        validSlotOverrides.remove(slotName);

        return new AccessorySlotValidationComponent(validSlotOverrides, this.invalidSlotOverrides);
    }

    public AccessorySlotValidationComponent removeInvalidSlot(String slotName) {
        var invalidSlotOverrides = new LinkedHashSet<>(this.invalidSlotOverrides);

        invalidSlotOverrides.remove(slotName);

        return new AccessorySlotValidationComponent(this.validSlotOverrides, invalidSlotOverrides);
    }

    public boolean isEmpty() {
        return this.invalidSlotOverrides.isEmpty() && this.validSlotOverrides.isEmpty();
    }
}
