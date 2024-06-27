package io.wispforest.accessories.api.components;

import io.wispforest.accessories.api.slot.SlotType;
import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;

import java.util.HashSet;
import java.util.Set;

public record AccessorySlotValidationComponent(Set<String> validSlotOverrides, Set<String> invalidSlotOverrides) {
    public static final AccessorySlotValidationComponent EMPTY = new AccessorySlotValidationComponent(Set.of(), Set.of());

    public static final Endec<AccessorySlotValidationComponent> ENDEC = StructEndecBuilder.of(
            Endec.STRING.setOf().fieldOf("valid_slots", AccessorySlotValidationComponent::validSlotOverrides),
            Endec.STRING.setOf().fieldOf("invalid_slots", AccessorySlotValidationComponent::invalidSlotOverrides),
            AccessorySlotValidationComponent::new
    );

    public AccessorySlotValidationComponent addValidSlot(String slotName) {
        var validSlotOverrides = new HashSet<>(this.validSlotOverrides);

        validSlotOverrides.add(slotName);

        return new AccessorySlotValidationComponent(validSlotOverrides, this.invalidSlotOverrides);
    }

    public AccessorySlotValidationComponent addInvalidSlot(String slotName) {
        var invalidSlotOverrides = new HashSet<>(this.invalidSlotOverrides);

        invalidSlotOverrides.add(slotName);

        return new AccessorySlotValidationComponent(this.validSlotOverrides, invalidSlotOverrides);
    }

    public AccessorySlotValidationComponent removeValidSlot(String slotName) {
        var validSlotOverrides = new HashSet<>(this.validSlotOverrides);

        validSlotOverrides.remove(slotName);

        return new AccessorySlotValidationComponent(validSlotOverrides, this.invalidSlotOverrides);
    }

    public AccessorySlotValidationComponent removeInvalidSlot(String slotName) {
        var invalidSlotOverrides = new HashSet<>(this.invalidSlotOverrides);

        invalidSlotOverrides.remove(slotName);

        return new AccessorySlotValidationComponent(this.validSlotOverrides, invalidSlotOverrides);
    }
}
