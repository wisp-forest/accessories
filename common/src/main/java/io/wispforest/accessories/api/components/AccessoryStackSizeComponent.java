package io.wispforest.accessories.api.components;

import io.wispforest.endec.Endec;
import io.wispforest.endec.impl.StructEndecBuilder;

public record AccessoryStackSizeComponent(boolean useStackSize, int sizeOverride) {

    public static final AccessoryStackSizeComponent DEFAULT = new AccessoryStackSizeComponent(false, 1);

    public AccessoryStackSizeComponent useStackSize(boolean value) {
        return new AccessoryStackSizeComponent(value, 1);
    }

    public AccessoryStackSizeComponent sizeOverride(int value) {
        return new AccessoryStackSizeComponent(false, value);
    }

    public static final Endec<AccessoryStackSizeComponent> ENDEC = StructEndecBuilder.of(
            Endec.BOOLEAN.fieldOf("use_stack_size", AccessoryStackSizeComponent::useStackSize),
            Endec.INT.optionalFieldOf("size_override", AccessoryStackSizeComponent::sizeOverride, 1),
            AccessoryStackSizeComponent::new
    );
}
