package io.wispforest.accessories.api.components;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;

import java.util.Set;

public record AccessorySlotValidationComponent(Set<String> validSlotOverrides, Set<String> invalidSlotOverrides) {
    public static final AccessorySlotValidationComponent EMPTY = new AccessorySlotValidationComponent(Set.of(), Set.of());

    public static final Endec<AccessorySlotValidationComponent> ENDEC = StructEndecBuilder.of(
            Endec.STRING.setOf().fieldOf("valid_slot_overrides", AccessorySlotValidationComponent::validSlotOverrides),
            Endec.STRING.setOf().fieldOf("invalid_slot_overrides", AccessorySlotValidationComponent::invalidSlotOverrides),
            AccessorySlotValidationComponent::new
    );
}
